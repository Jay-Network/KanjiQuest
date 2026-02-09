package com.jworks.kanjiquest.core.domain.repository

import com.jworks.kanjiquest.core.domain.model.Achievement
import com.jworks.kanjiquest.core.domain.model.CloudLearningData
import com.jworks.kanjiquest.core.domain.model.DailyStatsData
import com.jworks.kanjiquest.core.domain.model.StudySession
import com.jworks.kanjiquest.core.domain.model.UserProfile

interface LearningSyncRepository {
    suspend fun queueSessionSync(
        userId: String,
        touchedKanjiIds: List<Int>,
        touchedVocabIds: List<Long>,
        profile: UserProfile,
        session: StudySession,
        dailyStats: DailyStatsData,
        achievements: List<Achievement>
    )

    suspend fun syncPendingEvents(): Int

    suspend fun getPendingSyncCount(): Long

    suspend fun pullCloudData(userId: String): CloudLearningData?

    suspend fun applyCloudData(data: CloudLearningData)
}
