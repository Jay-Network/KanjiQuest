import UIKit

/// Raw point data captured from Apple Pencil touches.
/// Mirrors shared-core CalligraphyPoint but in Swift-native form.
struct CalligraphyPointData {
    let x: CGFloat
    let y: CGFloat
    let force: CGFloat          // 0.0-1.0 normalized (UITouch.force / maximumPossibleForce)
    let altitude: CGFloat       // radians, π/2 = perpendicular
    let azimuth: CGFloat        // radians, 0-2π
    let timestamp: TimeInterval // seconds since stroke start
}

protocol CalligraphyCanvasDelegate: AnyObject {
    func canvasDidUpdateActiveStroke(_ points: [CalligraphyPointData])
    func canvasDidCompleteStroke(_ points: [CalligraphyPointData])
    func canvasDidClear()
}

/// Custom UIView that captures Apple Pencil touches with full pressure/tilt data
/// and renders ink using the FudeBrushEngine via Core Graphics.
final class CalligraphyCanvasUIView: UIView {

    weak var delegate: CalligraphyCanvasDelegate?

    /// SVG path strings for ghost reference strokes
    var referenceStrokePaths: [String] = [] {
        didSet { setNeedsDisplay() }
    }

    private var completedStrokes: [CompletedStrokeLayer] = []
    private var activePoints: [CalligraphyPointData] = []
    private var strokeStartTime: TimeInterval = 0

    private let brushEngine: BrushEngine = FudeBrushEngine()

    // Offscreen buffer for completed strokes (avoids re-rendering)
    private var completedImage: UIImage?

    override init(frame: CGRect) {
        super.init(frame: frame)
        commonInit()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        commonInit()
    }

    private func commonInit() {
        backgroundColor = .white
        isMultipleTouchEnabled = false
        contentMode = .redrawOnClear
    }

    // MARK: - Touch Handling

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = touches.first else { return }
        strokeStartTime = touch.timestamp
        activePoints = []

        // Process coalesced touches for higher resolution
        let coalesced = event?.coalescedTouches(for: touch) ?? [touch]
        for t in coalesced {
            activePoints.append(pointData(from: t))
        }

        setNeedsDisplay()
        delegate?.canvasDidUpdateActiveStroke(activePoints)
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = touches.first else { return }

        let coalesced = event?.coalescedTouches(for: touch) ?? [touch]
        for t in coalesced {
            activePoints.append(pointData(from: t))
        }

        // Also use predicted touches for low-latency rendering
        if let predicted = event?.predictedTouches(for: touch) {
            // Predicted touches are used for rendering only, not stored
            // They'll be replaced by actual coalesced touches next frame
        }

        setNeedsDisplay()
        delegate?.canvasDidUpdateActiveStroke(activePoints)
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = touches.first else { return }

        let coalesced = event?.coalescedTouches(for: touch) ?? [touch]
        for t in coalesced {
            activePoints.append(pointData(from: t))
        }

        // Commit stroke to completed buffer
        commitActiveStroke()
        delegate?.canvasDidCompleteStroke(activePoints)
        activePoints = []
        setNeedsDisplay()
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        activePoints = []
        setNeedsDisplay()
    }

    // MARK: - Drawing

    override func draw(_ rect: CGRect) {
        guard let context = UIGraphicsGetCurrentContext() else { return }

        // 1. Draw background
        context.setFillColor(UIColor.white.cgColor)
        context.fill(rect)

        // 2. Draw ghost reference strokes
        drawReferenceStrokes(in: context)

        // 3. Draw completed strokes (from cached image)
        completedImage?.draw(in: bounds)

        // 4. Draw active stroke
        if !activePoints.isEmpty {
            brushEngine.render(points: activePoints, in: context, bounds: bounds)
        }
    }

    // MARK: - Reference Strokes

    private func drawReferenceStrokes(in context: CGContext) {
        guard !referenceStrokePaths.isEmpty else { return }

        context.saveGState()
        context.setStrokeColor(UIColor.systemGray4.cgColor)
        context.setLineWidth(2.0)
        context.setLineCap(.round)
        context.setLineJoin(.round)
        context.setAlpha(0.4)

        // KanjiVG uses ~109x109 coordinate space, scale to canvas
        let scaleX = bounds.width / 109.0
        let scaleY = bounds.height / 109.0

        for pathString in referenceStrokePaths {
            let points = parseSvgPathSimple(pathString)
            guard points.count >= 2 else { continue }

            context.beginPath()
            context.move(to: CGPoint(x: points[0].x * scaleX, y: points[0].y * scaleY))
            for i in 1..<points.count {
                context.addLine(to: CGPoint(x: points[i].x * scaleX, y: points[i].y * scaleY))
            }
            context.strokePath()
        }

        context.restoreGState()
    }

    /// Minimal SVG M/c path parser matching shared-core's SvgPathParser output.
    private func parseSvgPathSimple(_ pathData: String) -> [(x: CGFloat, y: CGFloat)] {
        var points: [(x: CGFloat, y: CGFloat)] = []
        var currentX: CGFloat = 0
        var currentY: CGFloat = 0

        let scanner = Scanner(string: pathData)
        scanner.charactersToBeSkipped = CharacterSet.whitespaces.union(CharacterSet(charactersIn: ","))

        while !scanner.isAtEnd {
            if scanner.scanString("M") != nil {
                if let x = scanner.scanDouble(), let y = scanner.scanDouble() {
                    currentX = CGFloat(x)
                    currentY = CGFloat(y)
                    points.append((currentX, currentY))
                }
            } else if scanner.scanString("c") != nil {
                // Relative cubic bezier: skip control points, use endpoint
                while let _ = scanner.scanDouble(),
                      let _ = scanner.scanDouble(),
                      let _ = scanner.scanDouble(),
                      let _ = scanner.scanDouble(),
                      let dx = scanner.scanDouble(),
                      let dy = scanner.scanDouble() {
                    currentX += CGFloat(dx)
                    currentY += CGFloat(dy)
                    points.append((currentX, currentY))
                }
            } else if scanner.scanString("C") != nil {
                while let _ = scanner.scanDouble(),
                      let _ = scanner.scanDouble(),
                      let _ = scanner.scanDouble(),
                      let _ = scanner.scanDouble(),
                      let x = scanner.scanDouble(),
                      let y = scanner.scanDouble() {
                    currentX = CGFloat(x)
                    currentY = CGFloat(y)
                    points.append((currentX, currentY))
                }
            } else {
                // Skip unknown character
                scanner.currentIndex = scanner.string.index(after: scanner.currentIndex)
            }
        }

        return points
    }

    // MARK: - Stroke Management

    private func commitActiveStroke() {
        guard !activePoints.isEmpty else { return }

        let renderer = UIGraphicsImageRenderer(size: bounds.size)
        completedImage = renderer.image { ctx in
            completedImage?.draw(in: bounds)
            brushEngine.render(points: activePoints, in: ctx.cgContext, bounds: bounds)
        }

        completedStrokes.append(CompletedStrokeLayer(points: activePoints))
    }

    func clear() {
        completedStrokes = []
        activePoints = []
        completedImage = nil
        setNeedsDisplay()
        delegate?.canvasDidClear()
    }

    func undo() {
        guard !completedStrokes.isEmpty else { return }
        completedStrokes.removeLast()
        rebuildCompletedImage()
        setNeedsDisplay()
    }

    private func rebuildCompletedImage() {
        guard !completedStrokes.isEmpty else {
            completedImage = nil
            return
        }

        let renderer = UIGraphicsImageRenderer(size: bounds.size)
        completedImage = renderer.image { ctx in
            for stroke in completedStrokes {
                brushEngine.render(points: stroke.points, in: ctx.cgContext, bounds: bounds)
            }
        }
    }

    // MARK: - Helpers

    private func pointData(from touch: UITouch) -> CalligraphyPointData {
        let location = touch.location(in: self)
        let maxForce = touch.maximumPossibleForce > 0 ? touch.maximumPossibleForce : 1.0
        return CalligraphyPointData(
            x: location.x,
            y: location.y,
            force: touch.force / maxForce,
            altitude: touch.altitudeAngle,
            azimuth: touch.azimuthAngle(in: self),
            timestamp: touch.timestamp - strokeStartTime
        )
    }
}

private struct CompletedStrokeLayer {
    let points: [CalligraphyPointData]
}
