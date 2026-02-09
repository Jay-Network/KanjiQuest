package com.jworks.kanjiquest.core.domain.model

data class SrsCard(
    val kanjiId: Int,
    val easeFactor: Double = 2.5,
    val interval: Int = 0,
    val repetitions: Int = 0,
    val nextReview: Long = 0L,
    val state: SrsState = SrsState.NEW,
    val totalReviews: Int = 0,
    val correctCount: Int = 0
) {
    val accuracy: Float
        get() = if (totalReviews > 0) correctCount.toFloat() / totalReviews else 0f
}

enum class SrsState(val value: String) {
    NEW("new"),
    LEARNING("learning"),
    REVIEW("review"),
    GRADUATED("graduated");

    companion object {
        fun fromString(value: String): SrsState =
            entries.find { it.value == value } ?: NEW
    }
}
