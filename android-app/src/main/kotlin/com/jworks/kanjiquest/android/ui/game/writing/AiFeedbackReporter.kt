package com.jworks.kanjiquest.android.ui.game.writing

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AiFeedbackReporter(private val context: Context) {

    private val reportFile: File
        get() = File(context.filesDir, "ai_feedback_reports.json")

    fun submitReport(
        kanji: String,
        feedback: HandwritingFeedback,
        userNote: String = ""
    ) {
        val reports = loadReports()

        val report = JSONObject().apply {
            put("kanji", kanji)
            put("timestamp", System.currentTimeMillis())
            put("ai_rating", feedback.qualityRating)
            put("ai_comment", feedback.overallComment)
            put("ai_strokes", JSONArray(feedback.strokeFeedback))
            put("user_note", userNote)
            // Store image reference (not the full base64 to keep file small)
            put("has_image", feedback.analyzedImageBase64 != null)
        }

        // Save the analyzed image separately for this report
        if (feedback.analyzedImageBase64 != null) {
            val imageFile = File(context.filesDir, "ai_report_${reports.length()}.png.b64")
            imageFile.writeText(feedback.analyzedImageBase64)
            report.put("image_file", imageFile.name)
        }

        reports.put(report)
        reportFile.writeText(reports.toString())
    }

    fun getReportCount(): Int = loadReports().length()

    private fun loadReports(): JSONArray {
        return try {
            if (reportFile.exists()) {
                JSONArray(reportFile.readText())
            } else {
                JSONArray()
            }
        } catch (_: Exception) {
            JSONArray()
        }
    }
}
