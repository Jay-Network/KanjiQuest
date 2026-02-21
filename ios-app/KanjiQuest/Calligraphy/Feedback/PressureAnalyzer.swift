import Foundation

/// Extracts text metadata from calligraphy stroke data for AI analysis.
/// This metadata is sent alongside the visual image to give the AI model
/// numerical insight into brush pressure, tilt, velocity, and stroke endings.
enum PressureAnalyzer {

    struct StrokeMetadata {
        let strokeNumber: Int
        let pointCount: Int
        let averagePressure: Float
        let maxPressure: Float
        let minPressure: Float
        let endingPressure: Float       // avg of last 3 points
        let duration: Double            // seconds
        let pressureVariance: Float
        let averageAltitude: Float      // mean tilt angle (radians)
        let altitudeVariance: Float     // tilt consistency
        let averageVelocity: Float      // mean speed (px/sec)
        let maxVelocity: Float          // peak speed
        let strokeEnding: StrokeEndingDetector.StrokeEndingAnalysis
    }

    struct AnalysisResult {
        let strokes: [StrokeMetadata]
        let overallAveragePressure: Float
        let overallPressureRange: Float
        let totalDuration: Double
        let overallAverageAltitude: Float
        let overallAverageVelocity: Float
    }

    static func analyze(strokes: [[CalligraphyPointData]]) -> AnalysisResult {
        let metadata = strokes.enumerated().map { index, points in
            analyzeStroke(points, number: index + 1)
        }

        let allPressures = strokes.flatMap { $0.map { Float($0.force) } }
        let avgPressure = allPressures.isEmpty ? 0 : allPressures.reduce(0, +) / Float(allPressures.count)
        let pressureRange = (allPressures.max() ?? 0) - (allPressures.min() ?? 0)
        let totalDuration = metadata.map(\.duration).reduce(0, +)

        let allAltitudes = strokes.flatMap { $0.map { Float($0.altitude) } }
        let avgAltitude = allAltitudes.isEmpty ? 0 : allAltitudes.reduce(0, +) / Float(allAltitudes.count)

        let avgVelocity: Float
        if metadata.isEmpty {
            avgVelocity = 0
        } else {
            avgVelocity = metadata.map(\.averageVelocity).reduce(0, +) / Float(metadata.count)
        }

        return AnalysisResult(
            strokes: metadata,
            overallAveragePressure: avgPressure,
            overallPressureRange: pressureRange,
            totalDuration: totalDuration,
            overallAverageAltitude: avgAltitude,
            overallAverageVelocity: avgVelocity
        )
    }

    private static func analyzeStroke(_ points: [CalligraphyPointData], number: Int) -> StrokeMetadata {
        let pressures = points.map { Float($0.force) }
        let avg = pressures.isEmpty ? 0 : pressures.reduce(0, +) / Float(pressures.count)
        let maxP = pressures.max() ?? 0
        let minP = pressures.min() ?? 0

        let endingPressure: Float
        if pressures.count >= 3 {
            let last3 = pressures.suffix(3)
            endingPressure = last3.reduce(0, +) / Float(last3.count)
        } else {
            endingPressure = avg
        }

        let duration: Double
        if points.count >= 2 {
            duration = points.last!.timestamp - points.first!.timestamp
        } else {
            duration = 0
        }

        // Pressure variance
        let pressureVariance = computeVariance(pressures)

        // Altitude (tilt) analysis
        let altitudes = points.map { Float($0.altitude) }
        let avgAltitude = altitudes.isEmpty ? 0 : altitudes.reduce(0, +) / Float(altitudes.count)
        let altitudeVariance = computeVariance(altitudes)

        // Velocity analysis
        let velocities = computeVelocities(points)
        let avgVelocity = velocities.isEmpty ? 0 : velocities.reduce(0, +) / Float(velocities.count)
        let maxVelocity = velocities.max() ?? 0

        // Stroke ending detection
        let strokeEnding = StrokeEndingDetector.detect(points: points)

        return StrokeMetadata(
            strokeNumber: number,
            pointCount: points.count,
            averagePressure: avg,
            maxPressure: maxP,
            minPressure: minP,
            endingPressure: endingPressure,
            duration: duration,
            pressureVariance: pressureVariance,
            averageAltitude: avgAltitude,
            altitudeVariance: altitudeVariance,
            averageVelocity: avgVelocity,
            maxVelocity: maxVelocity,
            strokeEnding: strokeEnding
        )
    }

    /// Compute per-segment velocities (px/sec) from consecutive points.
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

    private static func computeVariance(_ values: [Float]) -> Float {
        guard values.count >= 2 else { return 0 }
        let mean = values.reduce(0, +) / Float(values.count)
        let sumSquaredDev = values.map { ($0 - mean) * ($0 - mean) }.reduce(0, +)
        return sumSquaredDev / Float(values.count)
    }

    /// Generate a human-readable text summary for inclusion in the AI prompt.
    static func generateTextMetadata(from result: AnalysisResult) -> String {
        var lines: [String] = []
        lines.append("=== Pressure, Tilt & Movement Metadata ===")
        lines.append(String(format: "Overall: avg_pressure=%.2f, pressure_range=%.2f, avg_altitude=%.2f rad (%.0f°), avg_velocity=%.0f px/s, total_time=%.1fs",
                            result.overallAveragePressure, result.overallPressureRange,
                            result.overallAverageAltitude, result.overallAverageAltitude * 180 / .pi,
                            result.overallAverageVelocity, result.totalDuration))
        lines.append("")

        for stroke in result.strokes {
            lines.append(String(format: "Stroke %d: %d pts, %.1fs", stroke.strokeNumber, stroke.pointCount, stroke.duration))
            lines.append(String(format: "  Pressure: avg=%.2f max=%.2f min=%.2f ending=%.2f var=%.3f",
                                stroke.averagePressure, stroke.maxPressure, stroke.minPressure,
                                stroke.endingPressure, stroke.pressureVariance))
            lines.append(String(format: "  Tilt: avg_altitude=%.2f rad (%.0f°), alt_variance=%.3f",
                                stroke.averageAltitude, stroke.averageAltitude * 180 / .pi,
                                stroke.altitudeVariance))
            lines.append(String(format: "  Velocity: avg=%.0f px/s, max=%.0f px/s",
                                stroke.averageVelocity, stroke.maxVelocity))
            lines.append(String(format: "  Ending: %@ (confidence=%.0f%%) — %@",
                                stroke.strokeEnding.type.rawValue,
                                stroke.strokeEnding.confidence * 100,
                                stroke.strokeEnding.pressureProfile))
        }

        return lines.joined(separator: "\n")
    }
}
