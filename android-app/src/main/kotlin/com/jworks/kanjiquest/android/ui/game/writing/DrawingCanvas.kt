package com.jworks.kanjiquest.android.ui.game.writing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun DrawingCanvas(
    referenceStrokePaths: List<String>,
    currentStrokeIndex: Int,
    completedStrokes: List<List<Offset>>,
    activeStroke: List<Offset>,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    enabled: Boolean = true,
    srsState: String = "new",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset -> onDragStart(offset) },
                        onDrag = { change, _ ->
                            change.consume()
                            onDrag(change.position)
                        },
                        onDragEnd = { onDragEnd() }
                    )
                }
        ) {
            val canvasSize = size.minDimension

            // Draw ghost reference strokes
            drawGhostStrokes(referenceStrokePaths, currentStrokeIndex, canvasSize, srsState)

            // Draw completed user strokes (black)
            for (stroke in completedStrokes) {
                drawUserStroke(stroke, Color.Black)
            }

            // Draw active stroke (blue)
            if (activeStroke.isNotEmpty()) {
                drawUserStroke(activeStroke, Color(0xFF2196F3))
            }
        }
    }
}

private fun DrawScope.drawGhostStrokes(
    strokePaths: List<String>,
    currentStrokeIndex: Int,
    canvasSize: Float,
    srsState: String = "new"
) {
    // No ghost strokes for review/graduated (recall from memory)
    if (srsState == "review" || srsState == "graduated") return

    // SRS-aware alpha: new = full tracing, learning = fading hints
    val currentAlpha = when (srsState) {
        "new" -> 0.50f
        "learning" -> 0.25f
        else -> 0.35f
    }
    val otherAlpha = when (srsState) {
        "new" -> 0.30f
        "learning" -> 0.10f
        else -> 0.15f
    }

    for ((index, pathData) in strokePaths.withIndex()) {
        val path = SvgPathRenderer.svgToComposePath(pathData, canvasSize)
        val alpha = when {
            index < currentStrokeIndex -> otherAlpha * 0.5f  // already drawn strokes
            index == currentStrokeIndex -> currentAlpha       // current stroke to trace
            else -> otherAlpha                                // upcoming strokes
        }
        val strokeWidth = if (index == currentStrokeIndex) 6f else 4f

        drawPath(
            path = path,
            color = Color.Gray.copy(alpha = alpha),
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

private fun DrawScope.drawUserStroke(points: List<Offset>, color: Color) {
    if (points.size < 2) return

    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            // Use quadratic bezier for smooth curves between adjacent points
            if (i < points.size - 1) {
                val mid = Offset(
                    (points[i].x + points[i + 1].x) / 2f,
                    (points[i].y + points[i + 1].y) / 2f
                )
                quadraticTo(points[i].x, points[i].y, mid.x, mid.y)
            } else {
                lineTo(points[i].x, points[i].y)
            }
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = 8f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
