import CoreGraphics
import UIKit

/// Protocol for brush rendering engines.
/// Different implementations can simulate different brush types (筆, 筆ペン, 万年筆).
protocol BrushEngine {
    func render(points: [CalligraphyPointData], in context: CGContext, bounds: CGRect)
}

/// Fude (筆) brush engine with full Apple Pencil tilt/azimuth support.
///
/// Simulates a traditional calligraphy brush by:
/// - **Tilt (altitude)** → elliptical stamp shape (perpendicular = round, flat = elongated)
/// - **Azimuth** → stamp rotation following pencil direction
/// - **Pressure (force)** → stamp width + ink saturation
/// - **Velocity** → stamp spacing for 飛白 (kasure/dry brush) at speed
/// - **Edge softness** → radial gradient stamps instead of solid fill
final class FudeBrushEngine: BrushEngine {

    // MARK: - Brush Parameters

    /// Minimum stroke width at zero pressure
    private let minWidth: CGFloat = 1.5

    /// Maximum stroke width at full pressure
    private let maxWidth: CGFloat = 32.0

    /// Power curve exponent for pressure-to-width mapping.
    /// > 1.0 = more range in light pressure zone (better for calligraphy)
    private let pressureCurve: CGFloat = 1.8

    /// Tilt-to-height ratio range: altitude maps to [flatRatio, 1.0]
    /// At flatRatio the stamp is a flat ellipse (brush laid sideways)
    private let flatRatio: CGFloat = 0.3

    /// Soft edge multiplier — how far the gradient fades beyond the core
    private let inkBleedRadius: CGFloat = 1.4

    /// Base spacing factor (fraction of width between stamps)
    /// Slower strokes use this directly; faster strokes increase it
    private let baseSpacingFactor: CGFloat = 0.12

    /// Maximum spacing factor for fast strokes (飛白 dry brush effect)
    private let maxSpacingFactor: CGFloat = 0.30

    /// Velocity normalization — pixels/second considered "fast"
    private let fastVelocityThreshold: CGFloat = 2000.0

    /// Ink color (sumi)
    private let inkColor = UIColor.black

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

            // Compute velocity for this segment
            let dt = curr.timestamp - prev.timestamp
            let velocity: CGFloat = dt > 0.0001 ? distance / CGFloat(dt) : 0

            // Velocity-adjusted spacing: faster = wider gaps (飛白/かすれ)
            let velocityNorm = min(1.0, velocity / fastVelocityThreshold)
            let spacingFactor = baseSpacingFactor + (maxSpacingFactor - baseSpacingFactor) * velocityNorm

            let width = widthForPressure(curr.force)
            let spacing = max(width * spacingFactor, 0.5)
            let steps = max(Int(distance / spacing), 1)

            for step in 0...steps {
                let t = CGFloat(step) / CGFloat(steps)
                let x = prev.x + dx * t
                let y = prev.y + dy * t

                // Interpolate all pencil parameters
                let pressure = lerp(prev.force, curr.force, t)
                let altitude = lerp(prev.altitude, curr.altitude, t)
                let azimuth = lerpAngle(prev.azimuth, curr.azimuth, t)

                let w = widthForPressure(pressure)
                let alpha = alphaForPressure(pressure, altitude: altitude)

                // Tilt → ellipse height ratio
                let altitudeNorm = min(1.0, max(0.0, altitude / (.pi / 2)))
                let heightRatio = flatRatio + (1.0 - flatRatio) * altitudeNorm
                let h = w * heightRatio

                stampGradientEllipse(
                    in: context,
                    center: CGPoint(x: x, y: y),
                    width: w,
                    height: h,
                    angle: azimuth,
                    alpha: alpha,
                    velocityNorm: velocityNorm
                )
            }
        }

        context.restoreGState()
    }

    // MARK: - Pressure Mapping

    /// Non-linear pressure-to-width curve.
    private func widthForPressure(_ pressure: CGFloat) -> CGFloat {
        let normalizedPressure = max(0, min(1, pressure))
        let curved = pow(normalizedPressure, 1.0 / pressureCurve)
        return minWidth + (maxWidth - minWidth) * curved
    }

    /// Pressure + tilt → ink alpha.
    /// Light pressure = semi-transparent (dry brush), heavy = full ink.
    /// Flat tilt = slightly drier ink (brush edge dragging).
    private func alphaForPressure(_ pressure: CGFloat, altitude: CGFloat) -> CGFloat {
        let normalizedPressure = max(0, min(1, pressure))
        let altitudeNorm = min(1.0, max(0.0, altitude / (.pi / 2)))

        let basePressureAlpha: CGFloat
        if normalizedPressure < 0.15 {
            basePressureAlpha = 0.2 + normalizedPressure * 2.0  // 0.2-0.5 range
        } else if normalizedPressure < 0.4 {
            basePressureAlpha = 0.5 + (normalizedPressure - 0.15) * 1.2 // 0.5-0.8 range
        } else {
            basePressureAlpha = 0.8 + (normalizedPressure - 0.4) * 0.33 // 0.8-1.0 range
        }

        // Altitude influence: flat tilt = drier ink
        return basePressureAlpha * (0.7 + 0.3 * altitudeNorm)
    }

    // MARK: - Gradient Stamp Rendering

    /// Renders an elliptical stamp with a radial gradient for soft edges.
    /// Core is solid ink; edge fades to transparent over the outer portion.
    private func stampGradientEllipse(
        in context: CGContext,
        center: CGPoint,
        width: CGFloat,
        height: CGFloat,
        angle: CGFloat,
        alpha: CGFloat,
        velocityNorm: CGFloat
    ) {
        context.saveGState()

        // Transform: translate to center, rotate by azimuth
        context.translateBy(x: center.x, y: center.y)
        context.rotate(by: angle)

        // Scale context to make the gradient circular in transformed space,
        // then we draw a circle that maps to the desired ellipse
        let radius = max(width, height) / 2 * inkBleedRadius
        let scaleX = width / max(width, height)
        let scaleY = height / max(width, height)
        context.scaleBy(x: scaleX, y: scaleY)

        // Create radial gradient: solid core → transparent edge
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        let coreAlpha = alpha
        // Edge becomes more transparent at high velocity (dry brush texture)
        let edgeAlpha = coreAlpha * (0.15 - 0.1 * velocityNorm)

        let colors = [
            inkColor.withAlphaComponent(coreAlpha).cgColor,
            inkColor.withAlphaComponent(coreAlpha * 0.85).cgColor,
            inkColor.withAlphaComponent(edgeAlpha).cgColor,
        ] as CFArray

        let locations: [CGFloat] = [0.0, 0.65, 1.0]

        guard let gradient = CGGradient(
            colorsSpace: colorSpace,
            colors: colors,
            locations: locations
        ) else {
            // Fallback to solid fill
            context.setFillColor(inkColor.withAlphaComponent(alpha).cgColor)
            context.fillEllipse(in: CGRect(x: -radius, y: -radius, width: radius * 2, height: radius * 2))
            context.restoreGState()
            return
        }

        // Clip to ellipse bounds to prevent gradient bleed
        let clipRect = CGRect(x: -radius, y: -radius, width: radius * 2, height: radius * 2)
        context.addEllipse(in: clipRect)
        context.clip()

        context.drawRadialGradient(
            gradient,
            startCenter: .zero,
            startRadius: 0,
            endCenter: .zero,
            endRadius: radius,
            options: [.drawsAfterEndLocation]
        )

        context.restoreGState()
    }

    // MARK: - Interpolation Helpers

    private func lerp(_ a: CGFloat, _ b: CGFloat, _ t: CGFloat) -> CGFloat {
        a + (b - a) * t
    }

    /// Angle interpolation that handles wraparound at 2π boundary.
    private func lerpAngle(_ a: CGFloat, _ b: CGFloat, _ t: CGFloat) -> CGFloat {
        var diff = b - a
        while diff > .pi { diff -= 2 * .pi }
        while diff < -.pi { diff += 2 * .pi }
        return a + diff * t
    }
}
