package com.jworks.kanjiquest.android.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class OllamaClient(private val baseUrl: String) {

    suspend fun generateWithImage(prompt: String, imageBase64: String): String =
        withContext(Dispatchers.IO) {
            val url = URL("$baseUrl/api/generate")
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 10_000
                conn.readTimeout = 180_000

                val body = JSONObject().apply {
                    put("model", "llama3.2-vision:11b")
                    put("prompt", prompt)
                    put("images", JSONArray().put(imageBase64))
                    put("stream", false)
                }

                conn.outputStream.bufferedWriter().use { it.write(body.toString()) }

                if (conn.responseCode !in 200..299) {
                    val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: ""
                    throw OllamaException("HTTP ${conn.responseCode}: $errorBody")
                }

                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                json.optString("response", "")
            } finally {
                conn.disconnect()
            }
        }

    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/api/tags")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 2_000
            conn.readTimeout = 2_000
            val code = conn.responseCode
            conn.disconnect()
            code == 200
        } catch (_: Exception) {
            false
        }
    }
}

class OllamaException(message: String) : Exception(message)
