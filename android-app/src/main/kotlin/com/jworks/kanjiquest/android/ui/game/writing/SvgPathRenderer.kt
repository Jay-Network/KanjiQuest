package com.jworks.kanjiquest.android.ui.game.writing

import androidx.compose.ui.graphics.Path

/**
 * Convert an SVG path `d` attribute string to a Compose [Path],
 * scaling from KanjiVG's ~109x109 coordinate space to the target canvas size.
 */
object SvgPathRenderer {

    private const val KANJIVG_SIZE = 109f

    fun svgToComposePath(pathData: String, canvasSize: Float): Path {
        val path = Path()
        if (pathData.isBlank()) return path

        val scale = canvasSize / KANJIVG_SIZE
        val tokens = tokenize(pathData)
        var i = 0
        var currentX = 0f
        var currentY = 0f

        while (i < tokens.size) {
            when (tokens[i]) {
                "M" -> {
                    i++
                    val x = tokens.getFloat(i++) * scale
                    val y = tokens.getFloat(i++) * scale
                    path.moveTo(x, y)
                    currentX = x
                    currentY = y

                    // Implicit lineTo pairs after M
                    while (i < tokens.size && tokens[i].isNumeric()) {
                        val lx = tokens.getFloat(i++) * scale
                        val ly = tokens.getFloat(i++) * scale
                        path.lineTo(lx, ly)
                        currentX = lx
                        currentY = ly
                    }
                }
                "c" -> {
                    i++
                    while (i + 5 < tokens.size && tokens[i].isNumeric()) {
                        val dx1 = tokens.getFloat(i++) * scale
                        val dy1 = tokens.getFloat(i++) * scale
                        val dx2 = tokens.getFloat(i++) * scale
                        val dy2 = tokens.getFloat(i++) * scale
                        val dx = tokens.getFloat(i++) * scale
                        val dy = tokens.getFloat(i++) * scale
                        path.relativeCubicTo(dx1, dy1, dx2, dy2, dx, dy)
                        currentX += dx
                        currentY += dy
                    }
                }
                "C" -> {
                    i++
                    while (i + 5 < tokens.size && tokens[i].isNumeric()) {
                        val x1 = tokens.getFloat(i++) * scale
                        val y1 = tokens.getFloat(i++) * scale
                        val x2 = tokens.getFloat(i++) * scale
                        val y2 = tokens.getFloat(i++) * scale
                        val x = tokens.getFloat(i++) * scale
                        val y = tokens.getFloat(i++) * scale
                        path.cubicTo(x1, y1, x2, y2, x, y)
                        currentX = x
                        currentY = y
                    }
                }
                else -> i++
            }
        }

        return path
    }

    private fun tokenize(pathData: String): List<String> {
        val tokens = mutableListOf<String>()
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

    private fun List<String>.getFloat(index: Int): Float =
        getOrNull(index)?.toFloatOrNull() ?: 0f

    private fun String.isNumeric(): Boolean {
        val first = firstOrNull() ?: return false
        return first.isDigit() || first == '-' || first == '.'
    }
}
