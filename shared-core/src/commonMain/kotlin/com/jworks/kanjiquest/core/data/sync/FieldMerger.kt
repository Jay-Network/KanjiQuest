package com.jworks.kanjiquest.core.data.sync

import com.jworks.kanjiquest.core.domain.model.Achievement
import com.jworks.kanjiquest.core.domain.model.DailyStatsData
import com.jworks.kanjiquest.core.domain.model.SrsCard
import com.jworks.kanjiquest.core.domain.model.SrsState
import com.jworks.kanjiquest.core.domain.model.UserProfile
import com.jworks.kanjiquest.core.domain.model.VocabSrsCard
import kotlin.math.max
import kotlin.math.min

/**
 * Pure merge functions for per-field conflict resolution.
 * Used on pull to merge incoming server delta with local state.
 */
object FieldMerger {

    fun mergeProfile(local: UserProfile, remote: UserProfile): UserProfile {
        val mergedXp = max(local.totalXp, remote.totalXp)
        // Derive level from XP: level²×50
        var mergedLevel = 1
        while ((mergedLevel + 1) * (mergedLevel + 1) * 50 <= mergedXp) mergedLevel++

        return UserProfile(
            totalXp = mergedXp,
            level = mergedLevel,
            currentStreak = max(local.currentStreak, remote.currentStreak),
            longestStreak = max(local.longestStreak, remote.longestStreak),
            lastStudyDate = mergeLastStudyDate(local.lastStudyDate, remote.lastStudyDate),
            dailyGoal = remote.dailyGoal // server-authoritative for settings
        )
    }

    fun mergeSrsCard(local: SrsCard, remote: SrsCard): SrsCard {
        // Highest repetitions wins; tied → MAX(interval)
        val winner = when {
            remote.repetitions > local.repetitions -> remote
            local.repetitions > remote.repetitions -> local
            remote.interval > local.interval -> remote
            local.interval > remote.interval -> local
            else -> remote // tie-break: prefer remote (newer server data)
        }
        return winner.copy(
            totalReviews = max(local.totalReviews, remote.totalReviews),
            correctCount = max(local.correctCount, remote.correctCount),
            nextReview = max(local.nextReview, remote.nextReview)
        )
    }

    fun mergeVocabSrsCard(local: VocabSrsCard, remote: VocabSrsCard): VocabSrsCard {
        val winner = when {
            remote.repetitions > local.repetitions -> remote
            local.repetitions > remote.repetitions -> local
            remote.interval > local.interval -> remote
            local.interval > remote.interval -> local
            else -> remote
        }
        return winner.copy(
            totalReviews = max(local.totalReviews, remote.totalReviews),
            correctCount = max(local.correctCount, remote.correctCount),
            nextReview = max(local.nextReview, remote.nextReview)
        )
    }

    fun mergeDailyStats(local: DailyStatsData, remote: DailyStatsData): DailyStatsData {
        return DailyStatsData(
            date = local.date,
            cardsReviewed = max(local.cardsReviewed, remote.cardsReviewed),
            xpEarned = max(local.xpEarned, remote.xpEarned),
            studyTimeSec = max(local.studyTimeSec, remote.studyTimeSec)
        )
    }

    fun mergeAchievement(local: Achievement, remote: Achievement): Achievement {
        val mergedUnlocked = when {
            local.unlockedAt != null && remote.unlockedAt != null ->
                min(local.unlockedAt, remote.unlockedAt)
            else -> local.unlockedAt ?: remote.unlockedAt
        }
        return Achievement(
            id = local.id,
            progress = max(local.progress, remote.progress),
            target = remote.target, // server-authoritative for targets
            unlockedAt = mergedUnlocked
        )
    }

    fun mergeModeStat(local: ModeStatData, remote: ModeStatData): ModeStatData {
        return ModeStatData(
            kanjiId = local.kanjiId,
            gameMode = local.gameMode,
            reviewCount = max(local.reviewCount, remote.reviewCount),
            correctCount = max(local.correctCount, remote.correctCount)
        )
    }

    fun mergeCollectionItem(local: CollectionItemData, remote: CollectionItemData): CollectionItemData {
        return CollectionItemData(
            itemId = local.itemId,
            itemType = local.itemType,
            rarity = if (rarityOrder(remote.rarity) > rarityOrder(local.rarity)) remote.rarity else local.rarity,
            itemLevel = max(local.itemLevel, remote.itemLevel),
            itemXp = max(local.itemXp, remote.itemXp),
            discoveredAt = min(local.discoveredAt, remote.discoveredAt),
            source = local.source
        )
    }

    private fun mergeLastStudyDate(local: String?, remote: String?): String? {
        if (local == null) return remote
        if (remote == null) return local
        return if (local > remote) local else remote
    }

    private fun rarityOrder(rarity: String): Int = when (rarity) {
        "common" -> 0
        "uncommon" -> 1
        "rare" -> 2
        "epic" -> 3
        "legendary" -> 4
        else -> 0
    }
}
