package com.jworks.kanjiquest.core.data.sync

import com.jworks.kanjiquest.core.domain.model.Achievement
import com.jworks.kanjiquest.core.domain.model.DailyStatsData
import com.jworks.kanjiquest.core.domain.model.SrsCard
import com.jworks.kanjiquest.core.domain.model.StudySession
import com.jworks.kanjiquest.core.domain.model.UserProfile
import com.jworks.kanjiquest.core.domain.model.VocabSrsCard

data class DeviceInfo(
    val deviceName: String,
    val platform: String,  // "android", "ios_ipad", "ios_iphone"
    val appVersion: String
)

enum class SyncTrigger {
    APP_OPEN,
    SESSION_COMPLETE,
    BACKGROUND_PERIODIC,
    MANUAL
}

sealed class SyncResult {
    data class Success(
        val pushed: Int,
        val pulled: Int,
        val newVersion: Long
    ) : SyncResult()

    data class Error(val message: String) : SyncResult()

    data object NotLoggedIn : SyncResult()
    data object AlreadySyncing : SyncResult()
}

data class ChangedData(
    val srsCards: List<SrsCard> = emptyList(),
    val vocabSrsCards: List<VocabSrsCard> = emptyList(),
    val profile: UserProfile? = null,
    val sessions: List<StudySession> = emptyList(),
    val dailyStats: List<DailyStatsData> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val modeStats: List<ModeStatData> = emptyList(),
    val collection: List<CollectionItemData> = emptyList()
)

data class ModeStatData(
    val kanjiId: Int,
    val gameMode: String,
    val reviewCount: Int,
    val correctCount: Int
)

data class CollectionItemData(
    val itemId: Int,
    val itemType: String,
    val rarity: String = "common",
    val itemLevel: Int = 1,
    val itemXp: Int = 0,
    val discoveredAt: Long,
    val source: String = "gameplay"
)

data class PushRequest(
    val userId: String,
    val deviceId: String?,
    val clientVersion: Long,
    val mergeVersion: Int = 1,
    val data: ChangedData
)

data class PushResult(
    val newVersion: Long,
    val mergedBack: ChangedData
)

data class PullDelta(
    val srsCards: List<SrsCard> = emptyList(),
    val vocabSrsCards: List<VocabSrsCard> = emptyList(),
    val profile: UserProfile? = null,
    val sessions: List<StudySession> = emptyList(),
    val dailyStats: List<DailyStatsData> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val modeStats: List<ModeStatData> = emptyList(),
    val collection: List<CollectionItemData> = emptyList(),
    val serverVersion: Long = 0
)
