package com.jworks.kanjiquest.core.domain.usecase

import com.jworks.kanjiquest.core.domain.UserSessionProvider
import com.jworks.kanjiquest.core.domain.model.DailyStatsData
import com.jworks.kanjiquest.core.domain.model.LOCAL_USER_ID
import com.jworks.kanjiquest.core.domain.model.StudySession
import com.jworks.kanjiquest.core.domain.model.UserProfile
import com.jworks.kanjiquest.core.domain.repository.AchievementRepository
import com.jworks.kanjiquest.core.domain.repository.JCoinRepository
import com.jworks.kanjiquest.core.domain.repository.LearningSyncRepository
import com.jworks.kanjiquest.core.domain.repository.SessionRepository
import com.jworks.kanjiquest.core.domain.repository.UserRepository
import com.jworks.kanjiquest.core.engine.SessionStats
import com.jworks.kanjiquest.core.scoring.ScoringEngine
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class CompleteSessionUseCase(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val scoringEngine: ScoringEngine,
    private val jCoinRepository: JCoinRepository? = null,
    private val userSessionProvider: UserSessionProvider? = null,
    private val learningSyncRepository: LearningSyncRepository? = null,
    private val achievementRepository: AchievementRepository? = null
) {
    suspend fun execute(stats: SessionStats): SessionResult {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

        // Calculate adaptive XP bonus based on accuracy
        val accuracy = if (stats.cardsStudied > 0) {
            stats.correctCount.toFloat() / stats.cardsStudied.toFloat()
        } else 0f

        val adaptiveXpBonus: Int
        val adaptiveMessage: String?
        when {
            accuracy >= 0.90f && stats.cardsStudied >= 10 -> {
                adaptiveXpBonus = (stats.xpEarned * 0.25f).toInt()
                adaptiveMessage = "Excellent accuracy! +25% XP bonus"
            }
            accuracy >= 0.85f && stats.cardsStudied >= 5 -> {
                adaptiveXpBonus = (stats.xpEarned * 0.15f).toInt()
                adaptiveMessage = "Great accuracy! +15% XP bonus"
            }
            else -> {
                adaptiveXpBonus = 0
                adaptiveMessage = null
            }
        }

        val totalXpEarned = stats.xpEarned + adaptiveXpBonus

        // 1. Record the study session (with bonus-included XP)
        val session = StudySession(
            gameMode = stats.gameMode.value,
            startedAt = Clock.System.now().epochSeconds - stats.durationSec,
            cardsStudied = stats.cardsStudied,
            correctCount = stats.correctCount,
            xpEarned = totalXpEarned,
            durationSec = stats.durationSec
        )
        sessionRepository.recordSession(session)

        // 2. Update daily stats
        sessionRepository.recordDailyStats(
            date = today,
            cardsReviewed = stats.cardsStudied,
            xpEarned = totalXpEarned,
            studyTimeSec = stats.durationSec
        )

        // 3. Update user XP and level
        val profile = userRepository.getProfile()
        val newTotalXp = profile.totalXp + totalXpEarned
        val newLevel = scoringEngine.calculateLevel(newTotalXp)
        val leveledUp = newLevel > profile.level
        userRepository.updateXpAndLevel(newTotalXp, newLevel)

        // 4. Update streak
        val streakResult = calculateStreak(profile, today)
        userRepository.updateStreak(
            current = streakResult.currentStreak,
            longest = streakResult.longestStreak,
            lastStudyDate = today
        )

        // 5. Award J Coins (only for premium users)
        val isPremium = userSessionProvider?.isPremium() ?: false
        val coinsEarned = if (isPremium) {
            awardCoins(stats, streakResult)
        } else {
            0
        }

        // 6. Queue learning data sync (for logged-in users)
        val userId = userSessionProvider?.getUserId()
        if (userId != null && userId != LOCAL_USER_ID) {
            try {
                val updatedProfile = userRepository.getProfile()
                val dailyStatsData = DailyStatsData(
                    date = today,
                    cardsReviewed = stats.cardsStudied,
                    xpEarned = totalXpEarned,
                    studyTimeSec = stats.durationSec
                )
                val achievements = achievementRepository?.getAllAchievements() ?: emptyList()

                learningSyncRepository?.queueSessionSync(
                    userId = userId,
                    touchedKanjiIds = stats.touchedKanjiIds,
                    touchedVocabIds = stats.touchedVocabIds,
                    profile = updatedProfile,
                    session = session,
                    dailyStats = dailyStatsData,
                    achievements = achievements
                )
            } catch (_: Exception) {
                // Sync queueing is best-effort; don't fail the session
            }
        }

        return SessionResult(
            xpEarned = totalXpEarned,
            newTotalXp = newTotalXp,
            newLevel = newLevel,
            leveledUp = leveledUp,
            currentStreak = streakResult.currentStreak,
            streakIncreased = streakResult.increased,
            coinsEarned = coinsEarned,
            adaptiveXpBonus = adaptiveXpBonus,
            adaptiveMessage = adaptiveMessage
        )
    }

    private suspend fun awardCoins(stats: SessionStats, streakResult: StreakResult): Int {
        val repo = jCoinRepository ?: return 0
        val userId = userSessionProvider?.getUserId() ?: LOCAL_USER_ID
        var total = 0

        // Session completion: 10 coins for 20+ cards, 5 for 10+
        val sessionCoins = when {
            stats.cardsStudied >= 20 -> 10
            stats.cardsStudied >= 10 -> 5
            else -> 0
        }
        if (sessionCoins > 0) {
            repo.earnCoins(
                userId = userId,
                sourceType = "srs_review_complete",
                baseAmount = sessionCoins,
                description = "Completed review session (${stats.cardsStudied} cards)"
            )
            total += sessionCoins
        }

        // Perfect score bonus: 25 coins for 100% accuracy with 10+ cards
        if (stats.correctCount == stats.cardsStudied && stats.cardsStudied >= 10) {
            repo.earnCoins(
                userId = userId,
                sourceType = "perfect_quiz",
                baseAmount = 25,
                description = "Perfect score! ${stats.cardsStudied}/${stats.cardsStudied}"
            )
            total += 25
        }

        // Streak milestones (only when streak increases)
        if (streakResult.increased) {
            when (streakResult.currentStreak) {
                7 -> {
                    repo.earnCoins(
                        userId = userId,
                        sourceType = "streak_7_days",
                        baseAmount = 50,
                        description = "7-day study streak!"
                    )
                    total += 50
                }
                30 -> {
                    repo.earnCoins(
                        userId = userId,
                        sourceType = "streak_30_days",
                        baseAmount = 300,
                        description = "30-day study streak!"
                    )
                    total += 300
                }
            }
        }

        return total
    }

    private fun calculateStreak(profile: UserProfile, today: String): StreakResult {
        val lastDate = profile.lastStudyDate

        return when {
            lastDate == null -> {
                StreakResult(
                    currentStreak = 1,
                    longestStreak = maxOf(profile.longestStreak, 1),
                    increased = true
                )
            }
            lastDate == today -> {
                StreakResult(
                    currentStreak = profile.currentStreak,
                    longestStreak = profile.longestStreak,
                    increased = false
                )
            }
            isYesterday(lastDate, today) -> {
                val newStreak = profile.currentStreak + 1
                StreakResult(
                    currentStreak = newStreak,
                    longestStreak = maxOf(profile.longestStreak, newStreak),
                    increased = true
                )
            }
            else -> {
                StreakResult(
                    currentStreak = 1,
                    longestStreak = profile.longestStreak,
                    increased = true
                )
            }
        }
    }

    private fun isYesterday(lastDate: String, today: String): Boolean {
        return try {
            val last = kotlinx.datetime.LocalDate.parse(lastDate)
            val current = kotlinx.datetime.LocalDate.parse(today)
            val diff = current.toEpochDays() - last.toEpochDays()
            diff == 1
        } catch (_: Exception) {
            false
        }
    }
}

data class SessionResult(
    val xpEarned: Int,
    val newTotalXp: Int,
    val newLevel: Int,
    val leveledUp: Boolean,
    val currentStreak: Int,
    val streakIncreased: Boolean,
    val coinsEarned: Int = 0,
    val adaptiveXpBonus: Int = 0,
    val adaptiveMessage: String? = null
)

private data class StreakResult(
    val currentStreak: Int,
    val longestStreak: Int,
    val increased: Boolean
)
