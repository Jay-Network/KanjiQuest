package com.jworks.kanjiquest.android.ui.game.writing

import com.jworks.kanjiquest.android.network.GeminiClient
import com.jworks.kanjiquest.core.writing.Point
import org.json.JSONObject

data class HandwritingFeedback(
    val overallComment: String,
    val strokeFeedback: List<String>,
    val qualityRating: Int,
    val isAvailable: Boolean = true
)

class HandwritingChecker(private val geminiClient: GeminiClient) {

    suspend fun evaluate(
        drawnStrokes: List<List<Point>>,
        targetKanji: String,
        strokeCount: Int,
        canvasSize: Float
    ): HandwritingFeedback {
        if (drawnStrokes.isEmpty()) {
            return unavailable()
        }

        val imageBase64 = StrokeRenderer.renderToBase64(drawnStrokes, canvasSize)

        val prompt = buildPrompt(targetKanji, strokeCount)

        return try {
            val response = geminiClient.generateWithImage(prompt, imageBase64)
            parseResponse(response)
        } catch (_: Exception) {
            unavailable()
        }
    }

    private fun buildPrompt(kanji: String, strokeCount: Int): String = """
Evaluate the handwriting in this image. The student is writing the kanji「$kanji」which has exactly $strokeCount stroke(s). Each stroke is drawn in a different color with a number label.

Rules:
- ONLY discuss strokes visible in the image. There are at most $strokeCount strokes.
- Do NOT invent or hallucinate extra strokes beyond what you see.
- If the drawing looks good, the "strokes" array should be empty.
- Be encouraging but specific about any issues you see.

Respond with ONLY this JSON (no other text):
{"rating":3,"overall":"one sentence","strokes":["issue with stroke N"]}

rating: 1=poor 3=okay 5=excellent. strokes: empty array if no issues.
""".trimIndent()

    private fun parseResponse(response: String): HandwritingFeedback {
        // Try to extract JSON from the response (model may include extra text)
        val jsonStr = extractJson(response) ?: return fallbackParse(response)

        return try {
            val json = JSONObject(jsonStr)
            val rating = json.optInt("rating", 3).coerceIn(1, 5)
            val overall = json.optString("overall", "")
            val strokesArray = json.optJSONArray("strokes")
            val strokes = mutableListOf<String>()
            if (strokesArray != null) {
                for (i in 0 until strokesArray.length()) {
                    val tip = strokesArray.optString(i, "")
                    if (tip.isNotBlank()) strokes.add(tip)
                }
            }
            HandwritingFeedback(
                overallComment = overall,
                strokeFeedback = strokes,
                qualityRating = rating
            )
        } catch (_: Exception) {
            fallbackParse(response)
        }
    }

    private fun extractJson(text: String): String? {
        // Find first { and last } to extract JSON object
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return text.substring(start, end + 1)
    }

    private fun fallbackParse(response: String): HandwritingFeedback {
        // If JSON parsing fails, use the raw text as a comment
        val trimmed = response.trim()
        if (trimmed.isEmpty()) return unavailable()
        return HandwritingFeedback(
            overallComment = trimmed.take(200),
            strokeFeedback = emptyList(),
            qualityRating = 3
        )
    }

    private fun unavailable() = HandwritingFeedback(
        overallComment = "",
        strokeFeedback = emptyList(),
        qualityRating = 0,
        isAvailable = false
    )
}
