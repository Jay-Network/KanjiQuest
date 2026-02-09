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
You are a Japanese calligraphy teacher evaluating a student's handwritten kanji on a smartphone touchscreen. The kanji is「$kanji」with $strokeCount stroke(s). Each stroke is color-coded and numbered at its starting point.

Focus on these 3 aspects IN ORDER OF IMPORTANCE:

1. OVERALL BALANCE (バランス): Are the components proportioned correctly? Is the kanji centered? Are radicals/parts spaced properly relative to each other?

2. STROKE ORDER (筆順): The numbered labels show the order the student drew each stroke. Check if the stroke order follows standard Japanese conventions (top-to-bottom, left-to-right, horizontal before vertical).

3. STROKE ENDINGS (止め・はね・はらい): Check if strokes end correctly:
   - 止め (tome): firm stop where required
   - はね (hane): upward flick where required
   - はらい (harai): gradual taper/sweep where required

DO NOT criticize:
- Wobbly or non-straight lines (this is a smartphone touchscreen, not paper)
- Minor thickness variations
- Small positioning imprecision

Rules:
- ONLY discuss strokes visible in the image (at most $strokeCount).
- Do NOT invent extra strokes beyond what you see.
- If the writing looks good, the "strokes" array should be empty.
- Be encouraging. Praise what is done well before noting issues.

Respond with ONLY this JSON (no other text):
{"rating":3,"overall":"one sentence summary","strokes":["specific feedback for stroke N"]}

rating: 1=wrong kanji/major errors, 2=recognizable but significant balance/order issues, 3=acceptable with minor issues, 4=good with correct order and balance, 5=excellent form. strokes: empty if no issues.
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
