import CoreGraphics
import UIKit

/// Protocol for brush rendering engines.
/// Different implementations can simulate different brush types (筆, 筆ペン, 万年筆).
protocol BrushEngine {
    func render(points: [CalligraphyPointData], in context: CGContext, bounds: CGRect)
}

/// Fude (筆) brush engine using stamp-based Core Graphics rendering.
/// Places overlapping elliptical stamps along the stroke path to simulate
/// a traditional calligraphy brush with pressure sensitivity.
final class FudeBrushEngine: BrushEngine {

    // MARK: - Brush Parameters

    /// Minimum stroke width at zero pressure (thin hairline)
    private let minWidth: CGFloat = 1.0

    /// Maximum stroke width at full pressure
    private let maxWidth: CGFloat = 24.0

    /// Power curve exponent for pressure-to-width mapping.
    /// > 1.0 = more range in light pressure zone (better for calligraphy)
    private let pressureCurve: CGFloat = 1.8

    /// Ink color
    private let inkColor = UIColor.black

    /// Stamp spacing as fraction of current width (smaller = smoother, more GPU)
    private let spacingFactor: CGFloat = 0.15

    // MARK: - BrushEngine

    func render(points: [CalligraphyPointData], in context: CGContext, bounds: CGRect) {
        guard points.count >= 2 else { return }

        context.saveGState()
        context.setBlendMode(.normal)

        for i in 1..<points.count {
            let prev = points[i - 1]
            let curr = points[i]

            let dx = curr.x - prev.x
            let dy = curr.y - prev.y
            let distance = sqrt(dx * dx + dy * dy)

            guard distance > 0.1 else { continue }

            let width = widthForPressure(curr.force)
            let spacing = max(width * spacingFactor, 0.5)
            let steps = max(Int(distance / spacing), 1)

            for step in 0...steps {
                let t = CGFloat(step) / CGFloat(steps)
                let x = prev.x + dx * t
                let y = prev.y + dy * t

                // Interpolate pressure
                let pressure = prev.force + (curr.force - prev.force) * t
                let w = widthForPressure(pressure)
                let alpha = alphaForPressure(pressure)

                stampEllipse(
                    in: context,
                    center: CGPoint(x: x, y: y),
                    width: w,
                    height: w,  // Phase 1: circular stamps (Phase 2 adds tilt → ellipse)
                    angle: 0,
                    alpha: alpha
                )
            }
        }

        context.restoreGState()
    }

    // MARK: - Pressure Mapping

    /// Non-linear pressure-to-width curve.
    /// Low pressure = thin; high pressure = thick; non-linear for natural feel.
    private func widthForPressure(_ pressure: CGFloat) -> CGFloat {
        let normalizedPressure = max(0, min(1, pressure))
        let curved = pow(normalizedPressure, 1.0 / pressureCurve)
        return minWidth + (maxWidth - minWidth) * curved
    }

    /// Pressure-to-alpha for dry brush effect.
    /// Light pressure = semi-transparent (dry brush), heavy = full ink.
    private func alphaForPressure(_ pressure: CGFloat) -> CGFloat {
        let normalizedPressure = max(0, min(1, pressure))
        if normalizedPressure < 0.2 {
            return 0.3 + normalizedPressure * 1.5  // 0.3-0.6 range
        } else {
            return 0.6 + (normalizedPressure - 0.2) * 0.5  // 0.6-1.0 range
        }
    }

    // MARK: - Stamp Rendering

    private func stampEllipse(
        in context: CGContext,
        center: CGPoint,
        width: CGFloat,
        height: CGFloat,
        angle: CGFloat,
        alpha: CGFloat
    ) {
        context.saveGState()

        context.translateBy(x: center.x, y: center.y)
        context.rotate(by: angle)

        let rect = CGRect(
            x: -width / 2,
            y: -height / 2,
            width: width,
            height: height
        )

        context.setFillColor(inkColor.withAlphaComponent(alpha).cgColor)
        context.fillEllipse(in: rect)

        context.restoreGState()
    }
}
