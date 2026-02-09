import UIKit
import CoreGraphics

/// Renders calligraphy strokes to a bitmap image for AI evaluation.
/// Produces a pressure-aware 512x512 PNG where stroke width varies with pressure,
/// giving the AI model visual information about brush technique.
enum InkRenderer {

    static let imageSize: CGFloat = 512

    /// Render strokes to a UIImage with pressure-aware variable width.
    /// Used for sending to Gemini AI for calligraphy feedback.
    static func renderToImage(
        strokes: [[CalligraphyPointData]],
        canvasSize: CGSize
    ) -> UIImage? {
        let size = CGSize(width: imageSize, height: imageSize)
        let renderer = UIGraphicsImageRenderer(size: size)

        return renderer.image { ctx in
            let context = ctx.cgContext

            // White background
            context.setFillColor(UIColor.white.cgColor)
            context.fill(CGRect(origin: .zero, size: size))

            let scaleX = imageSize / max(canvasSize.width, 1)
            let scaleY = imageSize / max(canvasSize.height, 1)

            let strokeColors: [UIColor] = [
                .systemRed, .systemBlue, .systemGreen, .systemOrange,
                .systemPurple, .systemTeal, .systemPink, .systemBrown,
                .magenta, .darkGray
            ]

            for (strokeIndex, stroke) in strokes.enumerated() {
                guard stroke.count >= 2 else { continue }

                let color = strokeColors[strokeIndex % strokeColors.count]

                // Draw pressure-aware stroke
                for i in 1..<stroke.count {
                    let p0 = stroke[i - 1]
                    let p1 = stroke[i]

                    let x0 = p0.x * scaleX
                    let y0 = p0.y * scaleY
                    let x1 = p1.x * scaleX
                    let y1 = p1.y * scaleY

                    // Width varies with pressure: 1pt (light) to 8pt (heavy)
                    let width = 1.0 + p1.force * 7.0

                    context.setStrokeColor(color.cgColor)
                    context.setLineWidth(width)
                    context.setLineCap(.round)
                    context.beginPath()
                    context.move(to: CGPoint(x: x0, y: y0))
                    context.addLine(to: CGPoint(x: x1, y: y1))
                    context.strokePath()
                }

                // Draw stroke number label at start
                let startX = stroke[0].x * scaleX
                let startY = stroke[0].y * scaleY
                let label = "\(strokeIndex + 1)" as NSString
                let attrs: [NSAttributedString.Key: Any] = [
                    .font: UIFont.boldSystemFont(ofSize: 16),
                    .foregroundColor: color
                ]
                let labelSize = label.size(withAttributes: attrs)

                // White background circle for readability
                context.setFillColor(UIColor.white.cgColor)
                context.fillEllipse(in: CGRect(
                    x: startX - labelSize.width / 2 - 2,
                    y: startY - labelSize.height - 2,
                    width: labelSize.width + 4,
                    height: labelSize.height + 4
                ))

                label.draw(
                    at: CGPoint(x: startX - labelSize.width / 2, y: startY - labelSize.height),
                    withAttributes: attrs
                )
            }
        }
    }

    /// Render to base64-encoded PNG string for Gemini API.
    static func renderToBase64(
        strokes: [[CalligraphyPointData]],
        canvasSize: CGSize
    ) -> String? {
        guard let image = renderToImage(strokes: strokes, canvasSize: canvasSize),
              let data = image.pngData() else { return nil }
        return data.base64EncodedString()
    }
}
