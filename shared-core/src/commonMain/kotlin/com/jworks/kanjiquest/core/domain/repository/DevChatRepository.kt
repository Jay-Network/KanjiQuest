package com.jworks.kanjiquest.core.domain.repository

import com.jworks.kanjiquest.core.domain.model.DevChatMessage
import com.jworks.kanjiquest.core.domain.model.MessageCategory
import com.jworks.kanjiquest.core.domain.model.SendMessageResult

interface DevChatRepository {
    suspend fun isDeveloper(email: String): Boolean
    suspend fun sendMessage(email: String, message: String, category: MessageCategory?): SendMessageResult
    suspend fun getMessageHistory(email: String, limit: Int = 50, offset: Int = 0): List<DevChatMessage>
}
