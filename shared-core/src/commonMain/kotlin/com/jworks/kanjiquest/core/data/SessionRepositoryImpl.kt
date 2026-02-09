package com.jworks.kanjiquest.core.data

import com.jworks.kanjiquest.core.domain.model.StudySession
import com.jworks.kanjiquest.core.domain.repository.DailyStatsData
import com.jworks.kanjiquest.core.domain.repository.SessionRepository
import com.jworks.kanjiquest.db.KanjiQuestDatabase

class SessionRepositoryImpl(
    private val db: KanjiQuestDatabase
) : SessionRepository {

    override suspend fun recordSession(session: StudySession): Long {
        db.studySessionQueries.insert(
            game_mode = session.gameMode,
            started_at = session.startedAt,
            cards_studied = session.cardsStudied.toLong(),
            correct_count = session.correctCount.toLong(),
            xp_earned = session.xpEarned.toLong(),
            duration_sec = session.durationSec.toLong()
        )
        // Return the last inserted ID
        return db.studySessionQueries.getTotalXp().executeAsOne()
    }

    override suspend fun getRecentSessions(limit: Int): List<StudySession> {
        return db.studySessionQueries.getRecent(limit.toLong()).executeAsList().map {
            StudySession(
                id = it.id,
                gameMode = it.game_mode,
                startedAt = it.started_at,
                cardsStudied = it.cards_studied.toInt(),
                correctCount = it.correct_count.toInt(),
                xpEarned = it.xp_earned.toInt(),
                durationSec = it.duration_sec.toInt()
            )
        }
    }

    override suspend fun recordDailyStats(date: String, cardsReviewed: Int, xpEarned: Int, studyTimeSec: Int) {
        db.dailyStatsQueries.incrementForToday(
            date = date,
            cards_reviewed = cardsReviewed.toLong(),
            xp_earned = xpEarned.toLong(),
            study_time_sec = studyTimeSec.toLong()
        )
    }

    override suspend fun getDailyStats(date: String): DailyStatsData? {
        return db.dailyStatsQueries.getByDate(date).executeAsOneOrNull()?.let {
            DailyStatsData(
                date = it.date,
                cardsReviewed = it.cards_reviewed.toInt(),
                xpEarned = it.xp_earned.toInt(),
                studyTimeSec = it.study_time_sec.toInt()
            )
        }
    }

    override suspend fun getDailyStatsRange(startDate: String, endDate: String): List<DailyStatsData> {
        return db.dailyStatsQueries.getRange(startDate, endDate).executeAsList().map {
            DailyStatsData(
                date = it.date,
                cardsReviewed = it.cards_reviewed.toInt(),
                xpEarned = it.xp_earned.toInt(),
                studyTimeSec = it.study_time_sec.toInt()
            )
        }
    }
}
