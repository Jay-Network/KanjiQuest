package com.jworks.kanjiquest.core.writing

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

data class Point(val x: Float, val y: Float)

/**
 * Parses SVG path `d` attributes from KanjiVG data into lists of points.
 * KanjiVG uses ~109x109 coordinate space with M (moveTo) and c (relative cubicBezier) commands.
 */
object SvgPathParser {

    /**
     * Parse a JSON array of SVG path strings into a list of stroke point lists.
     * Input format: ["M 50.25,16.5 c 0.12,1.75 ...", "M 27.5,28.25 c ..."]
     */
    fun parseStrokePaths(jsonArray: String): List<List<Point>> {
        return try {
            val array = Json.parseToJsonElement(jsonArray).jsonArray
            array.map { element ->
                parseSvgPath(element.jsonPrimitive.content)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Parse a single SVG path `d` attribute into a list of points.
     * Supports M (absolute moveTo) and c (relative cubicBezierTo) commands.
     */
    fun parseSvgPath(pathData: String): List<Point> {
        if (pathData.isBlank()) return emptyList()

        val points = mutableListOf<Point>()
        var currentX = 0f
        var currentY = 0f

        val tokens = tokenize(pathData)
        var i = 0

        while (i < tokens.size) {
            when (tokens[i]) {
                "M" -> {
                    // Absolute moveTo: M x,y
                    i++
                    val (x, y) = parseCoordPair(tokens, i)
                    i += 2
                    currentX = x
                    currentY = y
                    points.add(Point(currentX, currentY))

                    // Implicit lineTo pairs after M
                    while (i < tokens.size && tokens[i].firstOrNull()?.let { it.isDigit() || it == '-' || it == '.' } == true) {
                        val (lx, ly) = parseCoordPair(tokens, i)
                        i += 2
                        currentX = lx
                        currentY = ly
                        points.add(Point(currentX, currentY))
                    }
                }
                "c" -> {
                    // Relative cubic bezier: c dx1,dy1 dx2,dy2 dx,dy
                    i++
                    while (i + 5 < tokens.size && tokens[i].firstOrNull()?.let { it.isDigit() || it == '-' || it == '.' } == true) {
                        // Control point 1 (skip for point sampling)
                        i += 2
                        // Control point 2 (skip for point sampling)
                        i += 2
                        // End point (relative)
                        val (dx, dy) = parseCoordPair(tokens, i)
                        i += 2
                        currentX += dx
                        currentY += dy
                        points.add(Point(currentX, currentY))
                    }
                }
                "C" -> {
                    // Absolute cubic bezier: C x1,y1 x2,y2 x,y
                    i++
                    while (i + 5 < tokens.size && tokens[i].firstOrNull()?.let { it.isDigit() || it == '-' || it == '.' } == true) {
                        i += 2 // skip control point 1
                        i += 2 // skip control point 2
                        val (x, y) = parseCoordPair(tokens, i)
                        i += 2
                        currentX = x
                        currentY = y
                        points.add(Point(currentX, currentY))
                    }
                }
                else -> i++ // skip unknown commands
            }
        }

        return points
    }

    /**
     * Tokenize SVG path data into command letters and numeric values.
     * Handles formats like "M 50.25,16.5" and "c 0.12,1.75 -0.5,3.25 -1,4.5"
     */
    internal fun tokenize(pathData: String): List<String> {
        val tokens = mutableListOf<String>()
        // Insert spaces before command letters for easier splitting
        val normalized = buildString {
            for (ch in pathData) {
                when {
                    ch.isLetter() -> {
                        append(' ')
                        append(ch)
                        append(' ')
                    }
                    ch == ',' -> append(' ')
                    ch == '-' && isNotEmpty() && last() != ' ' && !last().isLetter() -> {
                        append(' ')
                        append(ch)
                    }
                    else -> append(ch)
                }
            }
        }

        for (token in normalized.split(' ').map { it.trim() }.filter { it.isNotEmpty() }) {
            tokens.add(token)
        }

        return tokens
    }

    private fun parseCoordPair(tokens: List<String>, index: Int): Pair<Float, Float> {
        val x = tokens.getOrNull(index)?.toFloatOrNull() ?: 0f
        val y = tokens.getOrNull(index + 1)?.toFloatOrNull() ?: 0f
        return x to y
    }
}
