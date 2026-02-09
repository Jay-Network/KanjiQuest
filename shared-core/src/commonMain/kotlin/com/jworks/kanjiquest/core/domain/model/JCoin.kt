package com.jworks.kanjiquest.core.domain.model

data class CoinBalance(
    val userId: String,
    val localBalance: Long,
    val syncedBalance: Long,
    val lifetimeEarned: Long,
    val lifetimeSpent: Long,
    val tier: CoinTier,
    val lastSyncedAt: Long,
    val needsSync: Boolean
) {
    val displayBalance: Long get() = localBalance

    companion object {
        fun empty(userId: String = LOCAL_USER_ID) = CoinBalance(
            userId = userId,
            localBalance = 0,
            syncedBalance = 0,
            lifetimeEarned = 0,
            lifetimeSpent = 0,
            tier = CoinTier.BRONZE,
            lastSyncedAt = 0,
            needsSync = false
        )
    }
}

enum class CoinTier(val value: String, val label: String) {
    BRONZE("bronze", "Bronze"),
    SILVER("silver", "Silver"),
    GOLD("gold", "Gold"),
    PLATINUM("platinum", "Platinum");

    companion object {
        fun fromString(value: String): CoinTier =
            entries.find { it.value == value } ?: BRONZE
    }
}

data class CoinEarnResult(
    val earned: Int,
    val newBalance: Long,
    val sourceType: String,
    val queued: Boolean
)

data class SyncStatus(
    val pendingCount: Int,
    val syncedCount: Int,
    val failedCount: Int
)

const val LOCAL_USER_ID = "local_user"
