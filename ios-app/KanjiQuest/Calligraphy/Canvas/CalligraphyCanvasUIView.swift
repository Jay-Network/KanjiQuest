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
///
/// Enhanced with:
/// - Warm 半紙 (hanshi) paper texture with subtle grain
/// - Ink pooling at stroke start/stop points
/// - Soft-edge rendering via the gradient-based FudeBrushEngine
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

    // Paper texture (generated once, cached)
    private var paperTextureImage: UIImage?

    /// Warm paper background color (half-white 半紙 tone)
    private let paperColor = UIColor(red: 1.0, green: 0.973, blue: 0.941, alpha: 1.0) // #FFF8F0

    /// Velocity threshold below which ink pooling occurs (px/sec)
    private let poolingVelocityThreshold: CGFloat = 50.0

    /// Extra radius multiplier for ink pooling stamps
    private let poolingRadiusMultiplier: CGFloat = 1.35

    /// Extra alpha for ink pooling (darker puddle)
    private let poolingAlphaBoost: CGFloat = 0.15

    override init(frame: CGRect) {
        super.init(frame: frame)
        commonInit()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        commonInit()
    }

    private func commonInit() {
        backgroundColor = paperColor
        isMultipleTouchEnabled = false
        contentMode = .redraw
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        // Load paper texture when bounds change
        if paperTextureImage?.size != bounds.size, bounds.size.width > 0, bounds.size.height > 0 {
            paperTextureImage = loadPaperTexture(size: bounds.size)
            setNeedsDisplay()
        }
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

        // 1. Draw warm paper background
        context.setFillColor(paperColor.cgColor)
        context.fill(rect)

        // 2. Overlay paper texture (subtle grain)
        if let texture = paperTextureImage {
            context.saveGState()
            context.setBlendMode(.multiply)
            context.setAlpha(0.08) // Very subtle
            texture.draw(in: bounds)
            context.restoreGState()
        }

        // 3. Draw ghost reference strokes
        drawReferenceStrokes(in: context)

        // 4. Draw completed strokes (from cached image)
        completedImage?.draw(in: bounds)

        // 5. Draw active stroke
        if !activePoints.isEmpty {
            brushEngine.render(points: activePoints, in: context, bounds: bounds)
            // Render ink pooling at start of active stroke
            renderInkPooling(points: activePoints, in: context)
        }
    }

    // MARK: - Paper Texture

    /// Load the 半紙 (hanshi) rice paper texture from assets, scaled to the canvas size.
    /// Falls back to procedural generation if the asset is missing.
    private func loadPaperTexture(size: CGSize) -> UIImage {
        if let asset = UIImage(named: "Calligraphy/HanshiTexture") {
            let renderer = UIGraphicsImageRenderer(size: size)
            return renderer.image { ctx in
                asset.draw(in: CGRect(origin: .zero, size: size))
            }
        }
        // Fallback: procedural generation
        return generatePaperTextureFallback(size: size)
    }

    /// Procedural fallback if the hanshi texture asset is unavailable.
    private func generatePaperTextureFallback(size: CGSize) -> UIImage {
        let renderer = UIGraphicsImageRenderer(size: size)
        return renderer.image { ctx in
            let context = ctx.cgContext
            let width = Int(size.width)
            let height = Int(size.height)

            context.setFillColor(UIColor(white: 0.0, alpha: 1.0).cgColor)

            let grainSize: Int = 3
            let stepX = max(1, width / 200)
            let stepY = max(1, height / 200)

            for y in stride(from: 0, to: height, by: stepY) {
                for x in stride(from: 0, to: width, by: stepX) {
                    let hash = (x * 73856093) ^ (y * 19349663)
                    let normalizedHash = Float(hash & 0xFF) / 255.0

                    if normalizedHash > 0.4 {
                        let alpha = CGFloat((normalizedHash - 0.4) * 0.5)
                        context.setFillColor(UIColor(white: 0.0, alpha: alpha).cgColor)
                        context.fill(CGRect(x: x, y: y, width: grainSize, height: grainSize))
                    }
                }
            }

            context.setStrokeColor(UIColor(white: 0.0, alpha: 0.03).cgColor)
            context.setLineWidth(0.5)
            for i in 0..<15 {
                let hash1 = (i * 48271) & 0xFFFF
                let hash2 = (i * 65521) & 0xFFFF
                let x1 = CGFloat(hash1 % width)
                let y1 = CGFloat(hash2 % height)
                let len = CGFloat(10 + (hash1 % 30))
                let angle = CGFloat(hash2 % 314) / 100.0

                context.beginPath()
                context.move(to: CGPoint(x: x1, y: y1))
                context.addLine(to: CGPoint(x: x1 + cos(angle) * len, y: y1 + sin(angle) * len))
                context.strokePath()
            }
        }
    }

    // MARK: - Ink Pooling

    /// Render darker ink puddles where velocity is near zero (start/end of stroke).
    private func renderInkPooling(points: [CalligraphyPointData], in context: CGContext) {
        guard points.count >= 3 else { return }

        // Check start of stroke (first few points)
        let startPoolPoints = min(5, points.count)
        for i in 0..<startPoolPoints {
            if i == 0 {
                // First point: always pool (brush resting on paper)
                renderPoolStamp(point: points[i], in: context, intensity: 0.8)
            } else {
                let velocity = computePointVelocity(points: points, index: i)
                if velocity < poolingVelocityThreshold {
                    let factor = 1.0 - velocity / poolingVelocityThreshold
                    renderPoolStamp(point: points[i], in: context, intensity: CGFloat(factor) * 0.6)
                }
            }
        }

        // Check end of stroke (last few points)
        let endStart = max(startPoolPoints, points.count - 5)
        for i in endStart..<points.count {
            let velocity = computePointVelocity(points: points, index: i)
            if velocity < poolingVelocityThreshold {
                let factor = 1.0 - velocity / poolingVelocityThreshold
                renderPoolStamp(point: points[i], in: context, intensity: CGFloat(factor) * 0.5)
            }
        }
    }

    /// Render a single ink pool stamp — slightly larger and darker than normal.
    private func renderPoolStamp(point: CalligraphyPointData, in context: CGContext, intensity: CGFloat) {
        let baseWidth = 1.5 + (32.0 - 1.5) * pow(max(0, min(1, point.force)), 1.0 / 1.8)
        let poolRadius = baseWidth * poolingRadiusMultiplier / 2

        context.saveGState()
        context.translateBy(x: point.x, y: point.y)

        let poolAlpha = min(1.0, point.force * 0.5 + poolingAlphaBoost) * intensity

        // Draw a soft radial pool
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        let poolColor = UIColor.black
        let colors = [
            poolColor.withAlphaComponent(poolAlpha).cgColor,
            poolColor.withAlphaComponent(poolAlpha * 0.3).cgColor,
            poolColor.withAlphaComponent(0).cgColor,
        ] as CFArray
        let locations: [CGFloat] = [0.0, 0.5, 1.0]

        if let gradient = CGGradient(colorsSpace: colorSpace, colors: colors, locations: locations) {
            context.drawRadialGradient(
                gradient,
                startCenter: .zero,
                startRadius: 0,
                endCenter: .zero,
                endRadius: poolRadius,
                options: [.drawsAfterEndLocation]
            )
        }

        context.restoreGState()
    }

    private func computePointVelocity(points: [CalligraphyPointData], index: Int) -> CGFloat {
        guard index > 0 else { return 0 }
        let prev = points[index - 1]
        let curr = points[index]
        let dx = curr.x - prev.x
        let dy = curr.y - prev.y
        let dist = sqrt(dx * dx + dy * dy)
        let dt = curr.timestamp - prev.timestamp
        return dt > 0.0001 ? dist / CGFloat(dt) : 0
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
            renderInkPooling(points: activePoints, in: ctx.cgContext)
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

    /// Programmatic clear for when a new kanji loads or the canvas is reset externally.
    /// Does not notify delegate (avoids recursive binding updates).
    func clearDrawing() {
        completedStrokes = []
        activePoints = []
        completedImage = nil
        setNeedsDisplay()
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
                renderInkPooling(points: stroke.points, in: ctx.cgContext)
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
