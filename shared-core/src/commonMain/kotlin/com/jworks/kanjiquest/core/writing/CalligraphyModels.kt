package com.jworks.kanjiquest.core.writing

/**
 * A point captured from a pressure/tilt-sensitive stylus (e.g. Apple Pencil).
 * Extends the basic [Point] with calligraphy-specific metadata.
 */
data class CalligraphyPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 0.5f,       // 0.0-1.0 (0 = no contact, 1 = max force)
    val altitude: Float = 1.5708f,    // radians, π/2 = perpendicular to surface
    val azimuth: Float = 0f,          // radians, 0-2π, rotation around z-axis
    val timestamp: Double = 0.0       // seconds since stroke start
) {
    /** Downcast to basic [Point] for reuse with [StrokeMatcher]. */
    fun toPoint(): Point = Point(x, y)
}

/**
 * A single calligraphy stroke: an ordered sequence of [CalligraphyPoint]s
 * from pen-down to pen-up.
 */
data class CalligraphyStroke(val points: List<CalligraphyPoint>) {
    /** Convert to basic point list for [StrokeMatcher] compatibility. */
    fun toPointList(): List<Point> = points.map { it.toPoint() }

    val duration: Double
        get() = if (points.size >= 2) {
            points.last().timestamp - points.first().timestamp
        } else 0.0

    val averagePressure: Float
        get() = if (points.isNotEmpty()) {
            points.map { it.pressure }.average().toFloat()
        } else 0f

    val maxPressure: Float
        get() = points.maxOfOrNull { it.pressure } ?: 0f

    /** Pressure of the last 3 points — useful for detecting stroke ending type. */
    val endingPressure: Float
        get() = if (points.size >= 3) {
            points.takeLast(3).map { it.pressure }.average().toFloat()
        } else averagePressure
}
