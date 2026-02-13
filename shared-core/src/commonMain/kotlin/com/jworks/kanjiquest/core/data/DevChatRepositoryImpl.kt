package com.jworks.kanjiquest.core.data

import com.jworks.kanjiquest.core.data.remote.SupabaseClientFactory
import com.jworks.kanjiquest.core.domain.model.DevChatMessage
import com.jworks.kanjiquest.core.domain.model.MessageCategory
import com.jworks.kanjiquest.core.domain.model.MessageDirection
import com.jworks.kanjiquest.core.domain.model.SendMessageResult
import com.jworks.kanjiquest.core.domain.repository.DevChatRepository
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

class DevChatRepositoryImpl : DevChatRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun isDeveloper(email: String): Boolean = withContext(Dispatchers.Default) {
        if (!SupabaseClientFactory.isInitialized()) return@withContext false

        try {
            val supabase = SupabaseClientFactory.getInstance()
            val response = supabase.functions.invoke(
                function = "kq-dev-check?email=$email"
            )

            if (response.status.value !in 200..299) return@withContext false

            val body = json.parseToJsonElement(response.body<String>()).jsonObject
            val data = body["data"]?.jsonObject ?: return@withContext false
            data["is_developer"]?.jsonPrimitive?.boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendMessage(
        email: String,
        message: String,
        category: MessageCategory?
    ): SendMessageResult = withContext(Dispatchers.Default) {
        if (!SupabaseClientFactory.isInitialized()) {
            return@withContext SendMessageResult.Error("Backend not configured")
        }

        try {
            val supabase = SupabaseClientFactory.getInstance()
            val response = supabase.functions.invoke(
                function = "kq-dev-chat-send",
                body = buildJsonObject {
                    put("developer_email", email)
                    put("message", message)
                    if (category != null) put("category", category.value)
                }
            )

            val responseBody = json.parseToJsonElement(response.body<String>()).jsonObject

            if (response.status.value == 403) {
                return@withContext SendMessageResult.NotDeveloper
            }

            if (response.status.value !in 200..299) {
                val errorMsg = responseBody["error"]?.jsonObject?.get("message")
                    ?.jsonPrimitive?.content ?: "Send failed"
                return@withContext SendMessageResult.Error(errorMsg)
            }

            val data = responseBody["data"]?.jsonObject
                ?: return@withContext SendMessageResult.Error("Invalid response")

            SendMessageResult.Success(
                messageId = data["message_id"]?.jsonPrimitive?.long ?: 0,
                sentAt = data["sent_at"]?.jsonPrimitive?.content ?: ""
            )
        } catch (e: Exception) {
            SendMessageResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun getMessageHistory(
        email: String,
        limit: Int,
        offset: Int
    ): List<DevChatMessage> = withContext(Dispatchers.Default) {
        if (!SupabaseClientFactory.isInitialized()) return@withContext emptyList()

        try {
            val supabase = SupabaseClientFactory.getInstance()
            val response = supabase.functions.invoke(
                function = "kq-dev-chat-history?developer_email=$email&limit=$limit&offset=$offset"
            )

            if (response.status.value !in 200..299) return@withContext emptyList()

            val body = json.parseToJsonElement(response.body<String>()).jsonObject
            val data = body["data"]?.jsonObject ?: return@withContext emptyList()
            val messages = data["messages"]?.jsonArray ?: return@withContext emptyList()

            messages.map { element ->
                val msg = element.jsonObject
                DevChatMessage(
                    id = msg["id"]?.jsonPrimitive?.long ?: 0,
                    messageText = msg["message_text"]?.jsonPrimitive?.content ?: "",
                    direction = MessageDirection.fromString(
                        msg["direction"]?.jsonPrimitive?.content ?: "to_agent"
                    ),
                    category = msg["category"]?.jsonPrimitive?.content?.let {
                        MessageCategory.fromString(it)
                    },
                    sentAt = msg["sent_at"]?.jsonPrimitive?.content ?: "",
                    readAt = msg["read_at"]?.jsonPrimitive?.content
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
