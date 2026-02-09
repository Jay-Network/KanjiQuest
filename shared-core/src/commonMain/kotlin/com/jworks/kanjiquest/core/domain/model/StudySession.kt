package com.jworks.kanjiquest.core.domain.model

data class StudySession(
    val id: Long = 0,
    val gameMode: String,
    val startedAt: Long,
    val cardsStudied: Int = 0,
    val correctCount: Int = 0,
    val xpEarned: Int = 0,
    val durationSec: Int = 0
) {
    val accuracy: Float
        get() = if (cardsStudied > 0) correctCount.toFloat() / cardsStudied else 0f
}
