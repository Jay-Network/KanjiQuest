import UIKit

/// Renders drawn strokes as a color-coded PNG for AI analysis.
/// Mirrors Android's StrokeRenderer.renderToBase64.
enum WritingStrokeRenderer {
    private static let strokeColors: [UIColor] = [
        .red, .blue, .green, .orange, .purple,
        .cyan, .magenta, .brown, .systemPink, .systemTeal,
        UIColor(red: 0.5, green: 0.0, blue: 0.5, alpha: 1.0),
        UIColor(red: 0.0, green: 0.5, blue: 0.5, alpha: 1.0)
    ]

    private static let colorNames = [
        "red", "blue", "green", "orange", "purple",
        "cyan", "magenta", "brown", "pink", "teal",
        "dark purple", "dark teal"
    ]

    static func getColorName(_ index: Int) -> String {
        colorNames[index % colorNames.count]
    }

    static func renderToBase64(drawnStrokes: [[CGPoint]], canvasSize: CGFloat) -> String {
        let size = CGSize(width: canvasSize, height: canvasSize)
        let renderer = UIGraphicsImageRenderer(size: size)

        let image = renderer.image { ctx in
            // White background
            UIColor.white.setFill()
            ctx.fill(CGRect(origin: .zero, size: size))

            for (index, stroke) in drawnStrokes.enumerated() {
                guard stroke.count >= 2 else { continue }

                let color = strokeColors[index % strokeColors.count]
                color.setStroke()

                let path = UIBezierPath()
                path.lineWidth = 4
                path.lineCapStyle = .round
                path.lineJoinStyle = .round
                path.move(to: stroke[0])
                for point in stroke.dropFirst() {
                    path.addLine(to: point)
                }
                path.stroke()

                // Draw stroke number at start
                let numberStr = "\(index + 1)" as NSString
                let attrs: [NSAttributedString.Key: Any] = [
                    .font: UIFont.boldSystemFont(ofSize: 12),
                    .foregroundColor: color,
                    .backgroundColor: UIColor.white.withAlphaComponent(0.8)
                ]
                let labelPoint = CGPoint(
                    x: stroke[0].x - 6,
                    y: stroke[0].y - 16
                )
                numberStr.draw(at: labelPoint, withAttributes: attrs)
            }
        }

        return image.pngData()?.base64EncodedString() ?? ""
    }
}
