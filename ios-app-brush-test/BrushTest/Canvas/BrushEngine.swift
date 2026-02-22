import CoreGraphics
import UIKit

// MARK: - BrushSize

/// Three brush sizes matching traditional calligraphy brush categories.
enum BrushSize: CaseIterable, Identifiable {
    case small   // 細筆 (hosofude)
    case medium  // 中筆 (chufude)
    case large   // 太筆 (futofude)

    var id: Self { self }

    var minWidth: CGFloat {
        switch self {
        case .small:  return 0.8
        case .medium: return 1.5
        case .large:  return 3.0
        }
    }

    var maxWidth: CGFloat {
        switch self {
        case .small:  return 14.0
        case .medium: return 32.0
        case .large:  return 55.0
        }
    }

    var bristleCount: Int {
        switch self {
        case .small:  return 8
        case .medium: return 14
        case .large:  return 20
        }
    }

    /// Visual indicator size for the picker UI
    var dotSize: CGFloat {
        switch self {
        case .small:  return 8
        case .medium: return 14
        case .large:  return 22
        }
    }
}

// MARK: - BrushEngine Protocol

/// Protocol for brush rendering engines.
/// Different implementations can simulate different brush types (筆, 筆ペン, 万年筆).
protocol BrushEngine {
    func render(points: [CalligraphyPointData], in context: CGContext, bounds: CGRect)
}

// MARK: - FudeBrushEngine

/// Fude (筆) brush engine with bristle-strip rendering and ink physics.
///
/// Simulates a traditional calligraphy brush by:
/// - **Bristle strips** → visible texture instead of smooth ellipses
/// - **Tilt (altitude)** → stamp shape (perpendicular = round, flat = elongated)
/// - **Azimuth** → stamp rotation following pencil direction
/// - **Pressure (force)** → stamp width + ink saturation
/// - **Velocity** → bristle dropout for structured 飛白 (kasure/dry brush)
/// - **Ink depletion** → natural fading over long strokes
/// - **Edge irregularity** → outer bristles fade and perturb for organic edges
final class FudeBrushEngine: BrushEngine {

    // MARK: - Configurable Properties

    /// Current brush size (affects width range and bristle count)
    var brushSize: BrushSize = .medium

    /// Ink concentration: 1.0 = 濃墨 kouboku (dense black), 0.2 = 淡墨 tanboku (diluted warm gray)
    var inkConcentration: CGFloat = 1.0

    /// 8x8 absorption map from the paper surface (0.92–1.08 multipliers)
    var absorptionMap: [[CGFloat]]?

    /// Canvas bounds for absorption map lookup
    var canvasBounds: CGRect = .zero

    // MARK: - Brush Parameters

    /// Power curve exponent for pressure-to-width mapping (>1 = more range in light pressure)
    private let pressureCurve: CGFloat = 1.8

    /// Minimum height-to-width ratio at flat tilt
    private let flatRatio: CGFloat = 0.3

    /// Base spacing factor (fraction of width between stamps)
    private let baseSpacingFactor: CGFloat = 0.12

    /// Maximum spacing factor for fast strokes (飛白)
    private let maxSpacingFactor: CGFloat = 0.30

    /// Pixels/second considered "fast"
    private let fastVelocityThreshold: CGFloat = 2000.0

    // MARK: - Ink Depletion

    /// Distance in points over which ink fully depletes
    private let inkDepletionDistance: CGFloat = 500.0

    /// Minimum ink level (never fully dry)
    private let minInkLevel: CGFloat = 0.4

    // MARK: - Per-Stroke State

    private var inkLevel: CGFloat = 1.0
    private var strokeSeed: UInt64 = 0

    // MARK: - Computed Properties

    /// Ink color shifts from pure black to warm brown-gray as concentration decreases.
    /// 濃墨 = black, 淡墨 = warm diluted gray-brown
    private var inkUIColor: UIColor {
        let dilution = 1.0 - inkConcentration
        let r = 0.22 * dilution
        let g = 0.18 * dilution
        let b = 0.14 * dilution
        return UIColor(red: r, green: g, blue: b, alpha: 1.0)
    }

    /// Opacity ceiling — diluted ink is more transparent even at full pressure
    private var opacityCeiling: CGFloat {
        0.5 + 0.5 * inkConcentration
    }

    // MARK: - BrushEngine

    func render(points: [CalligraphyPointData], in context: CGContext, bounds: CGRect) {
        guard points.count >= 2 else { return }

        strokeSeed = computeStrokeSeed(from: points[0])
        inkLevel = 1.0

        context.saveGState()
        context.setBlendMode(.normal)

        var stampIndex = 0

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

            // Deplete ink over distance
            inkLevel = max(minInkLevel, inkLevel - distance / inkDepletionDistance)

            for step in 0...steps {
                let t = CGFloat(step) / CGFloat(steps)
                let x = prev.x + dx * t
                let y = prev.y + dy * t

                // Interpolate all pencil parameters
                let pressure = lerp(prev.force, curr.force, t)
                let altitude = lerp(prev.altitude, curr.altitude, t)
                let azimuth = lerpAngle(prev.azimuth, curr.azimuth, t)

                let w = widthForPressure(pressure)
                let baseAlpha = alphaForPressure(pressure, altitude: altitude)
                let alpha = baseAlpha * inkLevel * opacityCeiling

                // Tilt → ellipse height ratio
                let altitudeNorm = min(1.0, max(0.0, altitude / (.pi / 2)))
                let heightRatio = flatRatio + (1.0 - flatRatio) * altitudeNorm
                let h = w * heightRatio

                // Absorption map lookup
                let absorption = lookupAbsorption(at: CGPoint(x: x, y: y))

                stampBristles(
                    in: context,
                    center: CGPoint(x: x, y: y),
                    width: w,
                    height: h,
                    angle: azimuth,
                    alpha: alpha * absorption,
                    velocityNorm: velocityNorm,
                    stampIndex: stampIndex
                )

                stampIndex += 1
            }
        }

        context.restoreGState()
    }

    // MARK: - Pressure Mapping

    /// Non-linear pressure-to-width curve.
    private func widthForPressure(_ pressure: CGFloat) -> CGFloat {
        let normalizedPressure = max(0, min(1, pressure))
        let curved = pow(normalizedPressure, 1.0 / pressureCurve)
        return brushSize.minWidth + (brushSize.maxWidth - brushSize.minWidth) * curved
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

    // MARK: - Bristle Strip Rendering

    /// Renders a stamp as parallel bristle strips instead of a single smooth ellipse.
    ///
    /// Each bristle has:
    /// - Deterministic alpha variation (from hash of bristleIndex + strokeSeed)
    /// - Edge fading (outer ~25% of bristles have reduced alpha for stray fiber effect)
    /// - Velocity-based dropout (fast strokes lose outer bristles → structured 飛白)
    /// - Height perturbation (±15% for irregular edges)
    private func stampBristles(
        in context: CGContext,
        center: CGPoint,
        width: CGFloat,
        height: CGFloat,
        angle: CGFloat,
        alpha: CGFloat,
        velocityNorm: CGFloat,
        stampIndex: Int
    ) {
        let bristleCount = brushSize.bristleCount
        guard bristleCount > 0, width > 0.5 else { return }

        context.saveGState()
        context.translateBy(x: center.x, y: center.y)
        context.rotate(by: angle)

        let stripWidth = width / CGFloat(bristleCount)
        let halfCount = CGFloat(bristleCount - 1) / 2.0
        let gap: CGFloat = max(0.2, stripWidth * 0.08) // tiny gap between bristles
        let effectiveStripWidth = max(0.3, stripWidth - gap)
        let inkCG = inkUIColor

        for i in 0..<bristleCount {
            let hash = bristleHash(bristleIndex: i, seed: strokeSeed, stampIndex: stampIndex)

            // Alpha modulation from hash (0.65–1.0 range)
            let hashNorm = CGFloat(hash & 0xFF) / 255.0
            let alphaModulation = 0.65 + hashNorm * 0.35

            // Distance from center (0 = center, 1 = edge)
            let distFromCenter = abs(CGFloat(i) - halfCount) / max(halfCount, 1.0)

            // Edge bristles: stray fiber effect (outer ~25% faded)
            let edgeFactor: CGFloat
            if distFromCenter > 0.75 {
                let edgeDepth = (distFromCenter - 0.75) / 0.25  // 0→1 within edge zone
                edgeFactor = 1.0 - edgeDepth * 0.6  // fade to 0.4 at extreme edge
            } else {
                edgeFactor = 1.0
            }

            // Velocity-based bristle dropout: fast strokes → outer bristles disappear
            // Center bristles survive even at high velocity; edge bristles drop out first
            let survivalThreshold = 0.3 + 0.7 * (1.0 - distFromCenter)
            if velocityNorm > survivalThreshold {
                continue  // bristle drops out → visible 飛白 gap with structure
            }

            // Height perturbation ±15% from hash
            let heightHash = (hash >> 8) & 0xFF
            let heightPerturb = 1.0 + (CGFloat(heightHash) / 255.0 - 0.5) * 0.3
            let stripHeight = height * heightPerturb

            let bristleAlpha = alpha * alphaModulation * edgeFactor

            // Position of this bristle strip along the width axis
            let x = -width / 2.0 + CGFloat(i) * stripWidth + (stripWidth - effectiveStripWidth) / 2.0

            // Draw bristle as a filled ellipse (capsule shape at narrow widths)
            context.setFillColor(inkCG.withAlphaComponent(bristleAlpha).cgColor)
            context.fillEllipse(in: CGRect(
                x: x,
                y: -stripHeight / 2.0,
                width: effectiveStripWidth,
                height: stripHeight
            ))
        }

        context.restoreGState()
    }

    // MARK: - Absorption Map

    /// Look up paper absorption multiplier for a given canvas position.
    private func lookupAbsorption(at point: CGPoint) -> CGFloat {
        guard let map = absorptionMap,
              canvasBounds.width > 0, canvasBounds.height > 0 else { return 1.0 }
        let col = max(0, min(7, Int(point.x / canvasBounds.width * 8.0)))
        let row = max(0, min(7, Int(point.y / canvasBounds.height * 8.0)))
        return map[row][col]
    }

    // MARK: - Hashing

    /// Deterministic hash for bristle properties (splitmix64 variant).
    /// Same inputs → same result on every redraw.
    private func bristleHash(bristleIndex: Int, seed: UInt64, stampIndex: Int) -> UInt64 {
        var h = seed
        h ^= UInt64(bitPattern: Int64(bristleIndex)) &* 0x517cc1b727220a95
        h ^= UInt64(bitPattern: Int64(stampIndex)) &* 0x6c62272e07bb0142
        h = (h ^ (h >> 30)) &* 0xbf58476d1ce4e5b9
        h = (h ^ (h >> 27)) &* 0x94d049bb133111eb
        h = h ^ (h >> 31)
        return h
    }

    /// Compute a stroke seed from the first point's position.
    /// Ensures consistent bristle pattern on redraw.
    private func computeStrokeSeed(from point: CalligraphyPointData) -> UInt64 {
        let xBits = UInt64(bitPattern: Int64(point.x * 1000))
        let yBits = UInt64(bitPattern: Int64(point.y * 1000))
        return xBits &* 0x9E3779B97F4A7C15 ^ yBits &* 0x517CC1B727220A95
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
