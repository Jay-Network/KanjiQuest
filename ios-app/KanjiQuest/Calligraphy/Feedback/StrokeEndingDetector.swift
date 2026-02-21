import Foundation
import CoreGraphics

/// Detects Japanese calligraphy stroke ending types from Apple Pencil sensor data.
///
/// Three canonical stroke endings in 書道:
/// - 止め (tome): Firm stop — brush pressed down, halted, lifted vertically
/// - はね (hane): Flick — pressure drops then briefly spikes as brush flicks upward
/// - はらい (harai): Taper — pressure gradually decreases to zero as brush lifts away
enum StrokeEndingDetector {

    // MARK: - Types

    enum StrokeEndingType: String, CaseIterable {
        case tome    // 止め - firm stop
        case hane    // はね - flick
        case harai   // はらい - taper
        case unknown
    }

    struct StrokeEndingAnalysis {
        let type: StrokeEndingType
        let confidence: Float           // 0.0-1.0
        let pressureProfile: String     // human-readable description
    }

    // MARK: - Detection Parameters

    /// Fraction of stroke points to analyze for ending detection (last 20%)
    private static let endingFraction: Float = 0.20

    /// Minimum points needed for reliable detection
    private static let minimumEndingPoints: Int = 4

    // MARK: - Public API

    /// Analyze the ending of a stroke from Apple Pencil point data.
    static func detect(points: [CalligraphyPointData]) -> StrokeEndingAnalysis {
        guard points.count >= 6 else {
            return StrokeEndingAnalysis(type: .unknown, confidence: 0, pressureProfile: "insufficient data")
        }

        let endingCount = max(minimumEndingPoints, Int(Float(points.count) * endingFraction))
        let endingPoints = Array(points.suffix(endingCount))

        let tomeScore = scoreTome(endingPoints)
        let haneScore = scoreHane(endingPoints)
        let haraiScore = scoreHarai(endingPoints)

        // Pick the highest-scoring type
        let scores: [(StrokeEndingType, Float, String)] = [
            (.tome, tomeScore.score, tomeScore.profile),
            (.hane, haneScore.score, haneScore.profile),
            (.harai, haraiScore.score, haraiScore.profile),
        ]

        let best = scores.max(by: { $0.1 < $1.1 })!

        // Require minimum confidence threshold
        if best.1 < 0.25 {
            return StrokeEndingAnalysis(type: .unknown, confidence: best.1, pressureProfile: "ambiguous ending")
        }

        return StrokeEndingAnalysis(type: best.0, confidence: best.1, pressureProfile: best.2)
    }

    // MARK: - 止め (Tome) Detection

    /// Firm stop: pressure remains steady at end, velocity drops to near-zero.
    private static func scoreTome(_ points: [CalligraphyPointData]) -> (score: Float, profile: String) {
        let pressures = points.map { Float($0.force) }
        let velocities = computeVelocities(points)

        // 1. Pressure stability: should remain >= 0.3 in final segment
        let avgEndPressure = pressures.reduce(0, +) / Float(pressures.count)
        let pressureAboveThreshold: Float = avgEndPressure >= 0.3 ? 1.0 : avgEndPressure / 0.3

        // 2. Pressure consistency: low variance in final segment
        let pressureVariance = computeVariance(pressures)
        let pressureStability: Float = max(0, 1.0 - pressureVariance * 10.0)

        // 3. Velocity decay: should approach zero
        let finalVelocity = velocities.last ?? 0
        let avgVelocity = velocities.isEmpty ? 0 : velocities.reduce(0, +) / Float(velocities.count)
        let velocityDecay: Float
        if avgVelocity > 0.01 {
            velocityDecay = max(0, 1.0 - finalVelocity / avgVelocity)
        } else {
            velocityDecay = 1.0
        }

        // 4. No direction change at end
        let directionStability = computeDirectionStability(points)

        let score = pressureAboveThreshold * 0.3 + pressureStability * 0.3 + velocityDecay * 0.25 + directionStability * 0.15
        let profile = String(format: "steady→stop (p=%.2f, v_decay=%.2f)", avgEndPressure, velocityDecay)

        return (score, profile)
    }

    // MARK: - はね (Hane) Detection

    /// Flick: pressure drops below 0.2 then briefly spikes, velocity spikes upward at end.
    private static func scoreHane(_ points: [CalligraphyPointData]) -> (score: Float, profile: String) {
        let pressures = points.map { Float($0.force) }
        let velocities = computeVelocities(points)

        guard pressures.count >= 4 else {
            return (0, "too few points for hane")
        }

        // 1. Look for pressure dip-then-spike pattern in final points
        // Split into: main body (first 60%) and tip (last 40%)
        let splitIdx = max(1, pressures.count * 3 / 5)
        let bodyPressures = Array(pressures.prefix(splitIdx))
        let tipPressures = Array(pressures.suffix(from: splitIdx))

        let bodyMin = bodyPressures.min() ?? 0
        let tipMax = tipPressures.max() ?? 0
        let bodyAvg = bodyPressures.reduce(0, +) / Float(bodyPressures.count)

        // Spike detection: tip should have a peak above a dip in body
        let hasDip = bodyMin < 0.25
        let hasSpike = tipMax > bodyMin + 0.15
        let dipSpikeScore: Float
        if hasDip && hasSpike {
            dipSpikeScore = min(1.0, (tipMax - bodyMin) / 0.3)
        } else if hasSpike {
            dipSpikeScore = 0.4 * min(1.0, (tipMax - bodyMin) / 0.2)
        } else {
            dipSpikeScore = 0.0
        }

        // 2. Velocity spike at end
        let velocitySpike: Float
        if velocities.count >= 2 {
            let maxV = velocities.max() ?? 0
            let finalV = velocities.suffix(2).max() ?? 0
            velocitySpike = maxV > 0.01 ? finalV / maxV : 0
        } else {
            velocitySpike = 0
        }

        // 3. Direction change: hane typically goes upward or upward-left
        let directionChange = computeEndDirectionChange(points)

        let score = dipSpikeScore * 0.45 + velocitySpike * 0.30 + directionChange * 0.25
        let profile = String(format: "drop→spike (dip=%.2f, spike=%.2f)", bodyMin, tipMax)

        return (score, profile)
    }

    // MARK: - はらい (Harai) Detection

    /// Taper: pressure monotonically decreases to < 0.1, velocity maintained or increases.
    private static func scoreHarai(_ points: [CalligraphyPointData]) -> (score: Float, profile: String) {
        let pressures = points.map { Float($0.force) }
        let velocities = computeVelocities(points)

        // 1. Monotonic pressure decrease
        var decreaseCount = 0
        for i in 1..<pressures.count {
            if pressures[i] <= pressures[i - 1] + 0.02 { // small tolerance
                decreaseCount += 1
            }
        }
        let monotonicRatio = Float(decreaseCount) / Float(max(1, pressures.count - 1))

        // 2. Final pressure should be near zero
        let finalPressure = pressures.last ?? 0
        let taperComplete: Float = finalPressure < 0.1 ? 1.0 : max(0, 1.0 - (finalPressure - 0.1) / 0.3)

        // 3. Pressure gradient smoothness (no sudden drops)
        let gradients = zip(pressures, pressures.dropFirst()).map { $1 - $0 }
        let gradientVariance = computeVariance(gradients)
        let smoothness: Float = max(0, 1.0 - gradientVariance * 50.0)

        // 4. Velocity maintained or slightly increasing
        let velocityMaintained: Float
        if velocities.count >= 2 {
            let firstHalf = Array(velocities.prefix(velocities.count / 2))
            let secondHalf = Array(velocities.suffix(velocities.count / 2))
            let avgFirst = firstHalf.isEmpty ? 0 : firstHalf.reduce(0, +) / Float(firstHalf.count)
            let avgSecond = secondHalf.isEmpty ? 0 : secondHalf.reduce(0, +) / Float(secondHalf.count)
            // Velocity should not drop significantly
            if avgFirst > 0.01 {
                velocityMaintained = min(1.0, avgSecond / avgFirst)
            } else {
                velocityMaintained = 0.5
            }
        } else {
            velocityMaintained = 0.5
        }

        // 5. Direction consistency (no sudden changes)
        let directionStability = computeDirectionStability(points)

        let score = monotonicRatio * 0.25 + taperComplete * 0.25 + smoothness * 0.20 + velocityMaintained * 0.15 + directionStability * 0.15
        let profile = String(format: "gradual→taper (final_p=%.2f, mono=%.0f%%)", finalPressure, monotonicRatio * 100)

        return (score, profile)
    }

    // MARK: - Velocity Computation

    /// Compute per-segment velocities (pixels/second) from consecutive points.
    private static func computeVelocities(_ points: [CalligraphyPointData]) -> [Float] {
        guard points.count >= 2 else { return [] }

        var velocities: [Float] = []
        for i in 1..<points.count {
            let dx = Float(points[i].x - points[i - 1].x)
            let dy = Float(points[i].y - points[i - 1].y)
            let dist = sqrt(dx * dx + dy * dy)
            let dt = Float(points[i].timestamp - points[i - 1].timestamp)

            if dt > 0.0001 {
                velocities.append(dist / dt)
            } else {
                velocities.append(velocities.last ?? 0)
            }
        }
        return velocities
    }

    // MARK: - Direction Analysis

    /// Compute direction stability (0 = chaotic, 1 = consistent direction).
    private static func computeDirectionStability(_ points: [CalligraphyPointData]) -> Float {
        guard points.count >= 3 else { return 1.0 }

        var angles: [Float] = []
        for i in 1..<points.count {
            let dx = Float(points[i].x - points[i - 1].x)
            let dy = Float(points[i].y - points[i - 1].y)
            if dx * dx + dy * dy > 0.01 {
                angles.append(atan2(dy, dx))
            }
        }

        guard angles.count >= 2 else { return 1.0 }

        // Compute angular changes between consecutive segments
        var totalChange: Float = 0
        for i in 1..<angles.count {
            var diff = angles[i] - angles[i - 1]
            // Normalize to [-pi, pi]
            while diff > Float.pi { diff -= 2 * Float.pi }
            while diff < -Float.pi { diff += 2 * Float.pi }
            totalChange += abs(diff)
        }

        let avgChange = totalChange / Float(angles.count - 1)
        // 0 change = perfect stability (1.0), pi change = chaotic (0.0)
        return max(0, 1.0 - avgChange / Float.pi)
    }

    /// Detect direction change at stroke end (for hane detection).
    /// Returns higher score when there's a distinct upward direction change.
    private static func computeEndDirectionChange(_ points: [CalligraphyPointData]) -> Float {
        guard points.count >= 4 else { return 0 }

        let midIdx = points.count / 2

        // Direction of first half
        let firstDx = Float(points[midIdx].x - points[0].x)
        let firstDy = Float(points[midIdx].y - points[0].y)
        let firstAngle = atan2(firstDy, firstDx)

        // Direction of second half
        let lastIdx = points.count - 1
        let secondDx = Float(points[lastIdx].x - points[midIdx].x)
        let secondDy = Float(points[lastIdx].y - points[midIdx].y)
        let secondAngle = atan2(secondDy, secondDx)

        var angleDiff = secondAngle - firstAngle
        while angleDiff > Float.pi { angleDiff -= 2 * Float.pi }
        while angleDiff < -Float.pi { angleDiff += 2 * Float.pi }

        // Hane typically has upward direction change (negative dy in screen coords)
        // Score based on magnitude of direction change
        let changeMagnitude = abs(angleDiff)
        // Ideal hane has ~45-135 degree change
        if changeMagnitude > Float.pi / 6 && changeMagnitude < Float.pi * 3 / 4 {
            return min(1.0, changeMagnitude / (Float.pi / 3))
        } else if changeMagnitude >= Float.pi * 3 / 4 {
            return 0.5 // Too sharp, might be error
        } else {
            return changeMagnitude / (Float.pi / 3) * 0.5 // Too subtle
        }
    }

    // MARK: - Statistics

    private static func computeVariance(_ values: [Float]) -> Float {
        guard values.count >= 2 else { return 0 }
        let mean = values.reduce(0, +) / Float(values.count)
        let sumSquaredDev = values.map { ($0 - mean) * ($0 - mean) }.reduce(0, +)
        return sumSquaredDev / Float(values.count)
    }
}
