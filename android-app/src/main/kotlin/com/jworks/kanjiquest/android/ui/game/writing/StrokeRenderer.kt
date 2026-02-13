package com.jworks.kanjiquest.android.ui.game.writing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Base64
import com.jworks.kanjiquest.core.writing.Point
import java.io.ByteArrayOutputStream

object StrokeRenderer {

    private const val IMAGE_SIZE = 512
    private val STROKE_COLORS = intArrayOf(
        Color.RED,
        Color.BLUE,
        Color.rgb(0, 150, 0),   // green
        Color.rgb(255, 140, 0), // orange
        Color.rgb(128, 0, 128), // purple
        Color.rgb(0, 180, 180), // teal
        Color.rgb(180, 0, 60),  // crimson
        Color.rgb(100, 100, 0), // olive
        Color.MAGENTA,
        Color.DKGRAY
    )

    private val STROKE_COLOR_NAMES = arrayOf(
        "red", "blue", "green", "orange", "purple",
        "teal", "crimson", "olive", "magenta", "gray"
    )

    fun getColorName(index: Int): String = STROKE_COLOR_NAMES[index % STROKE_COLOR_NAMES.size]

    fun renderToBase64(strokes: List<List<Point>>, canvasSize: Float): String {
        val bitmap = Bitmap.createBitmap(IMAGE_SIZE, IMAGE_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // White background
        canvas.drawColor(Color.WHITE)

        val scale = if (canvasSize > 0f) IMAGE_SIZE.toFloat() / canvasSize else 1f

        val strokePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val textPaint = Paint().apply {
            textSize = 22f
            isAntiAlias = true
            isFakeBoldText = true
        }

        val bgPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.WHITE
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.DKGRAY
            strokeWidth = 1.5f
            isAntiAlias = true
        }

        for ((index, stroke) in strokes.withIndex()) {
            if (stroke.size < 2) continue

            val color = STROKE_COLORS[index % STROKE_COLORS.size]
            strokePaint.color = color
            textPaint.color = color

            // Draw stroke path
            for (i in 0 until stroke.size - 1) {
                val x1 = stroke[i].x * scale
                val y1 = stroke[i].y * scale
                val x2 = stroke[i + 1].x * scale
                val y2 = stroke[i + 1].y * scale
                canvas.drawLine(x1, y1, x2, y2, strokePaint)
            }

            // Draw stroke number at the start of each stroke
            val startX = stroke.first().x * scale
            val startY = stroke.first().y * scale
            val label = "${index + 1}"
            val textWidth = textPaint.measureText(label)
            val textHeight = textPaint.textSize

            // Offset the label slightly above-left of stroke start to avoid overlap
            val labelX = (startX - textWidth / 2f).coerceIn(2f, IMAGE_SIZE - textWidth - 2f)
            val labelY = (startY - 4f).coerceIn(textHeight + 2f, IMAGE_SIZE - 2f)
            val circleRadius = textWidth * 0.9f

            // White background circle with dark border for readability
            canvas.drawCircle(labelX + textWidth / 2f, labelY - textHeight / 3f, circleRadius, bgPaint)
            canvas.drawCircle(labelX + textWidth / 2f, labelY - textHeight / 3f, circleRadius, borderPaint)
            canvas.drawText(label, labelX, labelY, textPaint)
        }

        // Encode to base64
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        bitmap.recycle()
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
