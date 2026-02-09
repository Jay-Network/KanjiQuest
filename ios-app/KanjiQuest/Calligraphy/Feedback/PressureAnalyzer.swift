import Foundation

/// Extracts text metadata from calligraphy stroke data for AI analysis.
/// This metadata is sent alongside the visual image to give the AI model
/// numerical insight into brush pressure and movement patterns.
enum PressureAnalyzer {

    struct StrokeMetadata {
        let strokeNumber: Int
        let pointCount: Int
        let averagePressure: Float
        let maxPressure: Float
        let minPressure: Float
        let endingPressure: Float  // avg of last 3 points
        let duration: Double       // seconds
        let pressureVariance: Float
    }

    struct AnalysisResult {
        let strokes: [StrokeMetadata]
        let overallAveragePressure: Float
        let overallPressureRange: Float
        let totalDuration: Double
    }

    static func analyze(strokes: [[CalligraphyPointData]]) -> AnalysisResult {
        let metadata = strokes.enumerated().map { index, points in
            analyzeStroke(points, number: index + 1)
        }

        let allPressures = strokes.flatMap { $0.map { Float($0.force) } }
        let avgPressure = allPressures.isEmpty ? 0 : allPressures.reduce(0, +) / Float(allPressures.count)
        let pressureRange = (allPressures.max() ?? 0) - (allPressures.min() ?? 0)
        let totalDuration = metadata.map(\.duration).reduce(0, +)

        return AnalysisResult(
            strokes: metadata,
            overallAveragePressure: avgPressure,
            overallPressureRange: pressureRange,
            totalDuration: totalDuration
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

        // Variance: mean of squared deviations
        let variance: Float
        if pressures.count >= 2 {
            let sumSquaredDev = pressures.map { ($0 - avg) * ($0 - avg) }.reduce(0, +)
            variance = sumSquaredDev / Float(pressures.count)
        } else {
            variance = 0
        }

        return StrokeMetadata(
            strokeNumber: number,
            pointCount: points.count,
            averagePressure: avg,
            maxPressure: maxP,
            minPressure: minP,
            endingPressure: endingPressure,
            duration: duration,
            pressureVariance: variance
        )
    }

    /// Generate a human-readable text summary for inclusion in the AI prompt.
    static func generateTextMetadata(from result: AnalysisResult) -> String {
        var lines: [String] = []
        lines.append("=== Pressure & Movement Metadata ===")
        lines.append(String(format: "Overall avg pressure: %.2f, range: %.2f, total time: %.1fs",
                            result.overallAveragePressure, result.overallPressureRange, result.totalDuration))
        lines.append("")

        for stroke in result.strokes {
            lines.append(String(format: "Stroke %d: %d pts, %.1fs, pressure avg=%.2f max=%.2f min=%.2f ending=%.2f var=%.3f",
                                stroke.strokeNumber, stroke.pointCount, stroke.duration,
                                stroke.averagePressure, stroke.maxPressure, stroke.minPressure,
                                stroke.endingPressure, stroke.pressureVariance))
        }

        return lines.joined(separator: "\n")
    }
}
