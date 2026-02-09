package com.jworks.kanjiquest.core.domain.repository

import com.jworks.kanjiquest.core.domain.model.StudySession

interface SessionRepository {
    suspend fun recordSession(session: StudySession): Long
    suspend fun getRecentSessions(limit: Int): List<StudySession>
    suspend fun recordDailyStats(date: String, cardsReviewed: Int, xpEarned: Int, studyTimeSec: Int)
    suspend fun getDailyStats(date: String): DailyStatsData?
    suspend fun getDailyStatsRange(startDate: String, endDate: String): List<DailyStatsData>
}

data class DailyStatsData(
    val date: String,
    val cardsReviewed: Int,
    val xpEarned: Int,
    val studyTimeSec: Int
)
