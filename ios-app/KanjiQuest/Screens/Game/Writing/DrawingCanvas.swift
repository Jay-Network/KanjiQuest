import SwiftUI
import UIKit

/// Stroke-based drawing canvas for writing practice.
/// Renders reference strokes as guides and captures user drawing.
/// Mirrors Android's DrawingCanvas.kt with stroke reference overlay.
struct WritingDrawingCanvas: View {
    let referenceStrokePaths: [String]
    let currentStrokeIndex: Int
    let completedStrokes: [[CGPoint]]
    let activeStroke: [CGPoint]
    let writingDifficulty: WritingDifficulty
    var onDragStart: (CGPoint) -> Void
    var onDrag: (CGPoint) -> Void
    var onDragEnd: () -> Void
    var onSizeChanged: (CGFloat) -> Void

    var body: some View {
        GeometryReader { geo in
            let size = min(geo.size.width, geo.size.height)
            ZStack {
                // White background
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color.white)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                    )

                // Grid lines (light gray cross)
                Path { path in
                    path.move(to: CGPoint(x: size / 2, y: 0))
                    path.addLine(to: CGPoint(x: size / 2, y: size))
                    path.move(to: CGPoint(x: 0, y: size / 2))
                    path.addLine(to: CGPoint(x: size, y: size / 2))
                }
                .stroke(Color.gray.opacity(0.15), lineWidth: 1)

                // Reference strokes (guides)
                if writingDifficulty != .blank {
                    referenceStrokesView(size: size)
                }

                // User's completed strokes (black)
                ForEach(Array(completedStrokes.enumerated()), id: \.offset) { _, stroke in
                    Path { path in
                        guard stroke.count > 1 else { return }
                        path.move(to: stroke[0])
                        for point in stroke.dropFirst() {
                            path.addLine(to: point)
                        }
                    }
                    .stroke(Color.black, style: StrokeStyle(lineWidth: 4, lineCap: .round, lineJoin: .round))
                }

                // Active stroke (black)
                if activeStroke.count > 1 {
                    Path { path in
                        path.move(to: activeStroke[0])
                        for point in activeStroke.dropFirst() {
                            path.addLine(to: point)
                        }
                    }
                    .stroke(Color.black, style: StrokeStyle(lineWidth: 4, lineCap: .round, lineJoin: .round))
                }
            }
            .frame(width: size, height: size)
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        let point = value.location
                        if value.translation == .zero {
                            onDragStart(point)
                        } else {
                            onDrag(point)
                        }
                    }
                    .onEnded { _ in
                        onDragEnd()
                    }
            )
            .onAppear {
                onSizeChanged(size)
            }
            .onChange(of: geo.size) { newSize in
                onSizeChanged(min(newSize.width, newSize.height))
            }
        }
    }

    @ViewBuilder
    private func referenceStrokesView(size: CGFloat) -> some View {
        let scale = size / 109.0 // KanjiVG viewBox = 109x109

        Canvas { context, canvasSize in
            for (index, pathData) in referenceStrokePaths.enumerated() {
                let color: Color
                switch writingDifficulty {
                case .guided:
                    if index < currentStrokeIndex {
                        color = .gray.opacity(0.3) // Already drawn
                    } else if index == currentStrokeIndex {
                        color = .red.opacity(0.5) // Current stroke to draw
                    } else {
                        color = .gray.opacity(0.15) // Future strokes
                    }
                case .noOrder:
                    color = .gray.opacity(0.3) // All shown equally
                case .blank:
                    continue // Should not reach here
                }

                if let cgPath = parseSvgPath(pathData, scale: scale) {
                    context.stroke(Path(cgPath), with: .color(color), lineWidth: 2)
                }
            }
        }
        .allowsHitTesting(false)
    }

    private func parseSvgPath(_ d: String, scale: CGFloat) -> CGPath? {
        let path = CGMutablePath()
        let scanner = Scanner(string: d)
        scanner.charactersToBeSkipped = CharacterSet.whitespaces.union(.init(charactersIn: ","))
        var currentX: CGFloat = 0
        var currentY: CGFloat = 0

        while !scanner.isAtEnd {
            var cmd: NSString?
            scanner.scanCharacters(from: CharacterSet.letters, into: &cmd)
            guard let command = cmd as? String, let c = command.first else { continue }

            switch c {
            case "M":
                if let x = scanDouble(scanner), let y = scanDouble(scanner) {
                    currentX = x * scale; currentY = y * scale
                    path.move(to: CGPoint(x: currentX, y: currentY))
                }
            case "m":
                if let dx = scanDouble(scanner), let dy = scanDouble(scanner) {
                    currentX += dx * scale; currentY += dy * scale
                    path.move(to: CGPoint(x: currentX, y: currentY))
                }
            case "L":
                if let x = scanDouble(scanner), let y = scanDouble(scanner) {
                    currentX = x * scale; currentY = y * scale
                    path.addLine(to: CGPoint(x: currentX, y: currentY))
                }
            case "l":
                if let dx = scanDouble(scanner), let dy = scanDouble(scanner) {
                    currentX += dx * scale; currentY += dy * scale
                    path.addLine(to: CGPoint(x: currentX, y: currentY))
                }
            case "C":
                if let x1 = scanDouble(scanner), let y1 = scanDouble(scanner),
                   let x2 = scanDouble(scanner), let y2 = scanDouble(scanner),
                   let x = scanDouble(scanner), let y = scanDouble(scanner) {
                    currentX = x * scale; currentY = y * scale
                    path.addCurve(to: CGPoint(x: currentX, y: currentY),
                                  control1: CGPoint(x: x1 * scale, y: y1 * scale),
                                  control2: CGPoint(x: x2 * scale, y: y2 * scale))
                }
            case "c":
                if let dx1 = scanDouble(scanner), let dy1 = scanDouble(scanner),
                   let dx2 = scanDouble(scanner), let dy2 = scanDouble(scanner),
                   let dx = scanDouble(scanner), let dy = scanDouble(scanner) {
                    let cp1 = CGPoint(x: currentX + dx1 * scale, y: currentY + dy1 * scale)
                    let cp2 = CGPoint(x: currentX + dx2 * scale, y: currentY + dy2 * scale)
                    currentX += dx * scale; currentY += dy * scale
                    path.addCurve(to: CGPoint(x: currentX, y: currentY), control1: cp1, control2: cp2)
                }
            case "S":
                if let x2 = scanDouble(scanner), let y2 = scanDouble(scanner),
                   let x = scanDouble(scanner), let y = scanDouble(scanner) {
                    currentX = x * scale; currentY = y * scale
                    path.addCurve(to: CGPoint(x: currentX, y: currentY),
                                  control1: CGPoint(x: x2 * scale, y: y2 * scale),
                                  control2: CGPoint(x: x2 * scale, y: y2 * scale))
                }
            case "s":
                if let dx2 = scanDouble(scanner), let dy2 = scanDouble(scanner),
                   let dx = scanDouble(scanner), let dy = scanDouble(scanner) {
                    let cp = CGPoint(x: currentX + dx2 * scale, y: currentY + dy2 * scale)
                    currentX += dx * scale; currentY += dy * scale
                    path.addCurve(to: CGPoint(x: currentX, y: currentY), control1: cp, control2: cp)
                }
            case "Z", "z":
                path.closeSubpath()
            default:
                break
            }
        }
        return path
    }

    private func scanDouble(_ scanner: Scanner) -> CGFloat? {
        var value: Double = 0
        return scanner.scanDouble(&value) ? CGFloat(value) : nil
    }
}
