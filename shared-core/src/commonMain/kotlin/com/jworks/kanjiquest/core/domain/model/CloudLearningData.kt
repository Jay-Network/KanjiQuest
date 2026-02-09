package com.jworks.kanjiquest.core.domain.model

data class CloudLearningData(
    val srsCards: List<SrsCard> = emptyList(),
    val vocabSrsCards: List<VocabSrsCard> = emptyList(),
    val profile: UserProfile? = null,
    val sessions: List<StudySession> = emptyList(),
    val dailyStats: List<DailyStatsData> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val pulledAt: String? = null
)

data class DailyStatsData(
    val date: String,
    val cardsReviewed: Int = 0,
    val xpEarned: Int = 0,
    val studyTimeSec: Int = 0
)
