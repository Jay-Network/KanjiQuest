package com.jworks.kanjiquest.core.domain.model

data class VocabSrsCard(
    val vocabId: Long,
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
