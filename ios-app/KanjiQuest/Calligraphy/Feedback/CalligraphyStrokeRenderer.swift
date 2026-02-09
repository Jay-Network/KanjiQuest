import UIKit

/// Renders calligraphy strokes as a pressure-aware 512x512 PNG specifically
/// designed for AI calligraphy evaluation. Unlike InkRenderer (which uses
/// color-coded numbered strokes), this produces a single-color output that
/// emphasizes brush pressure variation for the AI model.
enum CalligraphyStrokeRenderer {

    static let imageSize: CGFloat = 512

    /// Render calligraphy strokes with pressure-based width variation.
    /// Produces black ink on white paper, with variable width showing brush pressure.
    static func renderForAI(
        strokes: [[CalligraphyPointData]],
        canvasSize: CGSize
    ) -> UIImage? {
        let size = CGSize(width: imageSize, height: imageSize)
        let renderer = UIGraphicsImageRenderer(size: size)

        return renderer.image { ctx in
            let context = ctx.cgContext

            // White background (半紙)
            context.setFillColor(UIColor.white.cgColor)
            context.fill(CGRect(origin: .zero, size: size))

            let scaleX = imageSize / max(canvasSize.width, 1)
            let scaleY = imageSize / max(canvasSize.height, 1)

            let brush = FudeBrushEngine()

            for stroke in strokes {
                // Scale points to image coordinates
                let scaledPoints = stroke.map { p in
                    CalligraphyPointData(
                        x: p.x * scaleX,
                        y: p.y * scaleY,
                        force: p.force,
                        altitude: p.altitude,
                        azimuth: p.azimuth,
                        timestamp: p.timestamp
                    )
                }
                brush.render(
                    points: scaledPoints,
                    in: context,
                    bounds: CGRect(origin: .zero, size: size)
                )
            }
        }
    }

    /// Render to base64-encoded PNG for Gemini API.
    static func renderToBase64(
        strokes: [[CalligraphyPointData]],
        canvasSize: CGSize
    ) -> String? {
        guard let image = renderForAI(strokes: strokes, canvasSize: canvasSize),
              let data = image.pngData() else { return nil }
        return data.base64EncodedString()
    }
}
