package com.jworks.kanjiquest.core.data

import com.jworks.kanjiquest.core.data.remote.SupabaseClientFactory
import com.jworks.kanjiquest.core.domain.model.Feedback
import com.jworks.kanjiquest.core.domain.model.FeedbackCategory
import com.jworks.kanjiquest.core.domain.model.FeedbackStatus
import com.jworks.kanjiquest.core.domain.model.FeedbackStatusChange
import com.jworks.kanjiquest.core.domain.model.FeedbackWithHistory
import com.jworks.kanjiquest.core.domain.model.SubmitFeedbackResult
import com.jworks.kanjiquest.core.domain.repository.FeedbackRepository
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class FeedbackRepositoryImpl : FeedbackRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun submitFeedback(
        email: String,
        appId: String,
        category: FeedbackCategory,
        feedbackText: String,
        deviceInfo: Map<String, String>?
    ): SubmitFeedbackResult = withContext(Dispatchers.Default) {
        if (!SupabaseClientFactory.isInitialized()) {
            return@withContext SubmitFeedbackResult.Error("Backend not configured")
        }

        try {
            val supabase = SupabaseClientFactory.getInstance()
            val response = supabase.functions.invoke(
                function = "feedback-submit",
                body = buildJsonObject {
                    put("user_email", email)
                    put("app_id", appId)
                    put("category", category.value)
                    put("feedback_text", feedbackText)
                    if (deviceInfo != null) {
                        putJsonObject("device_info") {
                            deviceInfo.forEach { (key, value) ->
                                put(key, value)
                            }
                        }
                    }
                }
            )

            val responseBody = json.parseToJsonElement(response.body<String>()).jsonObject

            if (response.status.value == 429) {
                val errorMsg = responseBody["error"]?.jsonObject?.get("message")
                    ?.jsonPrimitive?.content ?: "Rate limit exceeded"
                return@withContext SubmitFeedbackResult.RateLimited(errorMsg)
            }

            if (response.status.value !in 200..299) {
                val errorMsg = responseBody["error"]?.jsonObject?.get("message")
                    ?.jsonPrimitive?.content ?: "Submit failed"
                return@withContext SubmitFeedbackResult.Error(errorMsg)
            }

            val data = responseBody["data"]?.jsonObject
                ?: return@withContext SubmitFeedbackResult.Error("Invalid response")

            SubmitFeedbackResult.Success(
                feedbackId = data["feedback_id"]?.jsonPrimitive?.long ?: 0,
                createdAt = data["created_at"]?.jsonPrimitive?.content ?: ""
            )
        } catch (e: Exception) {
            SubmitFeedbackResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun getFeedbackUpdates(
        email: String,
        appId: String,
        sinceId: Long?
    ): List<FeedbackWithHistory> = withContext(Dispatchers.Default) {
        if (!SupabaseClientFactory.isInitialized()) return@withContext emptyList()

        try {
            val supabase = SupabaseClientFactory.getInstance()

            // Build query string
            val queryParams = buildString {
                append("user_email=$email")
                append("&app_id=$appId")
                if (sinceId != null) {
                    append("&since_id=$sinceId")
                }
            }

            val response = supabase.functions.invoke(
                function = "feedback-get-updates?$queryParams"
            )

            if (response.status.value !in 200..299) return@withContext emptyList()

            val body = json.parseToJsonElement(response.body<String>()).jsonObject
            val data = body["data"]?.jsonObject ?: return@withContext emptyList()
            val feedbackList = data["feedback"]?.jsonArray ?: return@withContext emptyList()

            feedbackList.map { element ->
                val item = element.jsonObject

                val feedback = Feedback(
                    id = item["id"]?.jsonPrimitive?.long ?: 0,
                    category = FeedbackCategory.fromString(
                        item["category"]?.jsonPrimitive?.content ?: "other"
                    ),
                    feedbackText = item["feedback_text"]?.jsonPrimitive?.content ?: "",
                    status = FeedbackStatus.fromString(
                        item["status"]?.jsonPrimitive?.content ?: "pending"
                    ),
                    createdAt = item["created_at"]?.jsonPrimitive?.content ?: "",
                    updatedAt = item["updated_at"]?.jsonPrimitive?.content ?: "",
                    completionNote = item["completion_note"]?.let {
                        if (it is kotlinx.serialization.json.JsonNull) null
                        else it.jsonPrimitive.contentOrNull
                    }
                )

                val history = item["status_history"]?.jsonArray?.map { historyElement ->
                    val h = historyElement.jsonObject
                    FeedbackStatusChange(
                        oldStatus = h["old_status"]?.jsonPrimitive?.content?.let {
                            FeedbackStatus.fromString(it)
                        },
                        newStatus = FeedbackStatus.fromString(
                            h["new_status"]?.jsonPrimitive?.content ?: "pending"
                        ),
                        changedBy = h["changed_by"]?.jsonPrimitive?.content ?: "",
                        changedAt = h["changed_at"]?.jsonPrimitive?.content ?: "",
                        note = h["note"]?.jsonPrimitive?.content
                    )
                } ?: emptyList()

                FeedbackWithHistory(
                    feedback = feedback,
                    statusHistory = history
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun registerFcmToken(
        email: String,
        appId: String,
        fcmToken: String,
        deviceInfo: Map<String, String>?
    ): Boolean = withContext(Dispatchers.Default) {
        if (!SupabaseClientFactory.isInitialized()) return@withContext false

        try {
            val supabase = SupabaseClientFactory.getInstance()
            val response = supabase.functions.invoke(
                function = "fcm-register-token",
                body = buildJsonObject {
                    put("user_email", email)
                    put("app_id", appId)
                    put("fcm_token", fcmToken)
                    if (deviceInfo != null) {
                        putJsonObject("device_info") {
                            deviceInfo.forEach { (key, value) ->
                                put(key, value)
                            }
                        }
                    }
                }
            )

            response.status.value in 200..299
        } catch (e: Exception) {
            false
        }
    }
}
