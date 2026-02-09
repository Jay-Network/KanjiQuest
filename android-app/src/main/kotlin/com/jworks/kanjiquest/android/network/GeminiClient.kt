package com.jworks.kanjiquest.android.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GeminiClient(private val apiKey: String) {

    private val endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    suspend fun generateWithImage(prompt: String, imageBase64: String): String =
        withContext(Dispatchers.IO) {
            val url = URL("$endpoint?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 10_000
                conn.readTimeout = 30_000

                val body = JSONObject().apply {
                    put("contents", JSONArray().put(
                        JSONObject().put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                            put(JSONObject().put("inline_data", JSONObject().apply {
                                put("mime_type", "image/png")
                                put("data", imageBase64)
                            }))
                        })
                    ))
                }

                conn.outputStream.bufferedWriter().use { it.write(body.toString()) }

                if (conn.responseCode !in 200..299) {
                    val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: ""
                    throw GeminiException("HTTP ${conn.responseCode}: $errorBody")
                }

                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)

                // Extract text from Gemini response:
                // { candidates: [{ content: { parts: [{ text: "..." }] } }] }
                json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            } finally {
                conn.disconnect()
            }
        }

    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        apiKey.isNotBlank()
    }
}

class GeminiException(message: String) : Exception(message)
