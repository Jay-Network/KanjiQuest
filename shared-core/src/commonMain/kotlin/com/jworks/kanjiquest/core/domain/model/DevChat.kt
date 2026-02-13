package com.jworks.kanjiquest.core.domain.model

data class DevChatMessage(
    val id: Long,
    val messageText: String,
    val direction: MessageDirection,
    val category: MessageCategory?,
    val sentAt: String,
    val readAt: String?
)

enum class MessageDirection(val value: String) {
    TO_AGENT("to_agent"),
    FROM_AGENT("from_agent");

    companion object {
        fun fromString(value: String): MessageDirection =
            entries.find { it.value == value } ?: TO_AGENT
    }
}

enum class MessageCategory(val value: String, val label: String) {
    BUG("bug", "Bug"),
    FEATURE("feature", "Feature"),
    FEEDBACK("feedback", "Feedback"),
    QUESTION("question", "Question"),
    GENERAL("general", "General");

    companion object {
        fun fromString(value: String): MessageCategory =
            entries.find { it.value == value } ?: GENERAL
    }
}

sealed class SendMessageResult {
    data class Success(val messageId: Long, val sentAt: String) : SendMessageResult()
    data class Error(val message: String) : SendMessageResult()
    data object NotDeveloper : SendMessageResult()
}
