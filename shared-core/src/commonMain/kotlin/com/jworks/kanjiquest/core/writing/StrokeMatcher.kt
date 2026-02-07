package com.jworks.kanjiquest.core.writing

import kotlin.math.sqrt

data class MatchResult(val similarity: Float, val isCorrect: Boolean)

data class WritingResult(
    val strokeResults: List<MatchResult>,
    val overallSimilarity: Float,
    val isCorrect: Boolean,
    val quality: Int // SM-2 quality 0-5
)

object StrokeMatcher {

    private const val CORRECT_THRESHOLD = 0.55f
    private const val SAMPLE_COUNT = 32

    /**
     * Normalize points to 0-1 range and resample to uniform count.
     */
    fun normalizeAndResample(points: List<Point>, sampleCount: Int = SAMPLE_COUNT): List<Point> {
        if (points.size < 2) return points

        // Find bounding box
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }

        val rangeX = (maxX - minX).coerceAtLeast(1f)
        val rangeY = (maxY - minY).coerceAtLeast(1f)
        val range = maxOf(rangeX, rangeY) // use max to preserve aspect ratio

        // Normalize to 0-1
        val normalized = points.map { p ->
            Point(
                (p.x - minX) / range,
                (p.y - minY) / range
            )
        }

        // Resample to uniform spacing
        return resample(normalized, sampleCount)
    }

    /**
     * Match a drawn stroke against a reference stroke.
     * Returns similarity (0-1, higher = better) and whether it passes the threshold.
     */
    fun matchStroke(drawn: List<Point>, reference: List<Point>, threshold: Float = CORRECT_THRESHOLD): MatchResult {
        if (drawn.size < 2 || reference.size < 2) {
            return MatchResult(0f, false)
        }

        val drawnNorm = normalizeAndResample(drawn)
        val refNorm = normalizeAndResample(reference)

        val distance = meanEuclideanDistance(drawnNorm, refNorm)
        // Convert distance to similarity: 0 distance = 1.0 similarity
        // Max reasonable distance ~1.0 (diagonal of unit square), so clamp
        val similarity = (1f - distance).coerceIn(0f, 1f)

        return MatchResult(similarity, similarity >= threshold)
    }

    /**
     * SRS-aware validation with adjusted pass threshold.
     */
    fun validateWriting(
        drawnStrokes: List<List<Point>>,
        referenceStrokes: List<List<Point>>,
        srsState: String
    ): WritingResult {
        val threshold = when (srsState) {
            "new" -> 0.40f
            "learning" -> 0.50f
            "review" -> CORRECT_THRESHOLD // 0.55f
            "graduated" -> 0.65f
            else -> CORRECT_THRESHOLD
        }
        return validateWritingInternal(drawnStrokes, referenceStrokes, threshold)
    }

    /**
     * Validate a complete writing attempt (all strokes).
     * Returns overall result with SM-2 quality score.
     */
    fun validateWriting(
        drawnStrokes: List<List<Point>>,
        referenceStrokes: List<List<Point>>
    ): WritingResult {
        return validateWritingInternal(drawnStrokes, referenceStrokes, CORRECT_THRESHOLD)
    }

    private fun validateWritingInternal(
        drawnStrokes: List<List<Point>>,
        referenceStrokes: List<List<Point>>,
        threshold: Float
    ): WritingResult {
        if (drawnStrokes.isEmpty() || referenceStrokes.isEmpty()) {
            return WritingResult(emptyList(), 0f, false, 0)
        }

        // Match each drawn stroke to corresponding reference stroke
        val strokeResults = drawnStrokes.zip(referenceStrokes).map { (drawn, ref) ->
            matchStroke(drawn, ref, threshold)
        }

        val overallSimilarity = if (strokeResults.isNotEmpty()) {
            strokeResults.map { it.similarity }.average().toFloat()
        } else 0f

        // Must have correct stroke count and enough strokes must pass
        val correctStrokeCount = drawnStrokes.size == referenceStrokes.size
        val correctStrokeRatio = strokeResults.count { it.isCorrect }.toFloat() /
                referenceStrokes.size.coerceAtLeast(1)
        val isCorrect = correctStrokeCount && correctStrokeRatio >= 0.7f

        // Map similarity to SM-2 quality (0-5)
        val quality = when {
            overallSimilarity >= 0.85f && correctStrokeCount -> 5
            overallSimilarity >= 0.75f && isCorrect -> 4
            overallSimilarity >= 0.65f && isCorrect -> 3
            isCorrect -> 2
            overallSimilarity >= 0.4f -> 1
            else -> 0
        }

        return WritingResult(strokeResults, overallSimilarity, isCorrect, quality)
    }

    private fun meanEuclideanDistance(a: List<Point>, b: List<Point>): Float {
        if (a.isEmpty() || b.isEmpty()) return 1f
        val minSize = minOf(a.size, b.size)
        var totalDist = 0f
        for (i in 0 until minSize) {
            val dx = a[i].x - b[i].x
            val dy = a[i].y - b[i].y
            totalDist += sqrt(dx * dx + dy * dy)
        }
        return totalDist / minSize
    }

    private fun resample(points: List<Point>, targetCount: Int): List<Point> {
        if (points.size < 2 || targetCount < 2) return points

        // Calculate total path length
        val totalLength = pathLength(points)
        if (totalLength == 0f) return List(targetCount) { points.first() }

        val segmentLength = totalLength / (targetCount - 1)
        val resampled = mutableListOf(points.first())
        var accumulated = 0f
        var pointIndex = 1

        while (resampled.size < targetCount && pointIndex < points.size) {
            val prev = points[pointIndex - 1]
            val curr = points[pointIndex]
            val dist = distance(prev, curr)

            if (accumulated + dist >= segmentLength) {
                val ratio = (segmentLength - accumulated) / dist
                val newX = prev.x + ratio * (curr.x - prev.x)
                val newY = prev.y + ratio * (curr.y - prev.y)
                resampled.add(Point(newX, newY))
                accumulated = 0f
                // Don't advance pointIndex - there may be more sample points on this segment
            } else {
                accumulated += dist
                pointIndex++
            }
        }

        // Fill remaining with last point if needed
        while (resampled.size < targetCount) {
            resampled.add(points.last())
        }

        return resampled
    }

    private fun pathLength(points: List<Point>): Float {
        var length = 0f
        for (i in 1 until points.size) {
            length += distance(points[i - 1], points[i])
        }
        return length
    }

    private fun distance(a: Point, b: Point): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }
}
