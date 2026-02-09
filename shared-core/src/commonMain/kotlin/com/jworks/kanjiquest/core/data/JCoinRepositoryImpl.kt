package com.jworks.kanjiquest.core.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.jworks.kanjiquest.core.data.remote.SupabaseClientFactory
import com.jworks.kanjiquest.core.domain.model.ActiveBooster
import com.jworks.kanjiquest.core.domain.model.BoosterType
import com.jworks.kanjiquest.core.domain.model.CoinBalance
import com.jworks.kanjiquest.core.domain.model.CoinEarnResult
import com.jworks.kanjiquest.core.domain.model.CoinTier
import com.jworks.kanjiquest.core.domain.model.PurchaseResult
import com.jworks.kanjiquest.core.domain.model.ShopCategory
import com.jworks.kanjiquest.core.domain.model.ShopItem
import com.jworks.kanjiquest.core.domain.model.SyncStatus
import com.jworks.kanjiquest.core.domain.repository.JCoinRepository
import com.jworks.kanjiquest.db.KanjiQuestDatabase
import io.github.jan.supabase.functions.functions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class JCoinRepositoryImpl(
    private val database: KanjiQuestDatabase,
    private val clock: Clock = Clock.System
) : JCoinRepository {

    private val queries get() = database.jCoinQueries

    override suspend fun getBalance(userId: String): CoinBalance = withContext(Dispatchers.Default) {
        queries.initializeBalance(userId)
        queries.getBalance(userId).executeAsOneOrNull()?.toDomain()
            ?: CoinBalance.empty(userId)
    }

    override fun observeBalance(userId: String): Flow<CoinBalance> {
        return queries.getBalance(userId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toDomain() ?: CoinBalance.empty(userId) }
    }

    override suspend fun earnCoins(
        userId: String,
        sourceType: String,
        baseAmount: Int,
        description: String,
        metadata: String
    ): CoinEarnResult = withContext(Dispatchers.Default) {
        // Ensure balance row exists
        queries.initializeBalance(userId)

        val amount = baseAmount.toLong()

        // Update local balance optimistically
        queries.addToBalance(
            local_balance = amount,
            lifetime_earned = amount,
            user_id = userId
        )

        // Queue for future sync
        queries.insertSyncEvent(
            user_id = userId,
            event_type = "earn",
            source_type = sourceType,
            base_amount = amount,
            description = description,
            metadata = metadata,
            created_at = clock.now().epochSeconds
        )

        val updated = queries.getBalance(userId).executeAsOneOrNull()
        CoinEarnResult(
            earned = baseAmount,
            newBalance = updated?.local_balance ?: amount,
            sourceType = sourceType,
            queued = true
        )
    }

    override suspend fun getPendingSyncCount(): Long = withContext(Dispatchers.Default) {
        queries.getPendingCount().executeAsOne()
    }

    override suspend fun getSyncStatus(): SyncStatus = withContext(Dispatchers.Default) {
        val pending = queries.getPendingEvents().executeAsList()
        val pendingCount = pending.count { it.sync_status == "pending" }
        val failedCount = pending.count { it.sync_status == "failed" }
        SyncStatus(
            pendingCount = pendingCount + failedCount,
            syncedCount = 0,
            failedCount = failedCount
        )
    }

    override suspend fun syncPendingEvents(): Int = withContext(Dispatchers.Default) {
        // Check if Supabase is initialized
        if (!SupabaseClientFactory.isInitialized()) {
            return@withContext 0 // Skip sync if backend not configured
        }

        val supabase = SupabaseClientFactory.getInstance()
        val pending = queries.getPendingEvents().executeAsList()
        var syncedCount = 0

        for (event in pending) {
            try {
                // Call Supabase Edge Function: coin-earn
                val response = supabase.functions.invoke(
                    function = "coin-earn",
                    body = buildJsonObject {
                        put("customer_id", event.user_id)
                        put("source_business", event.source_business)
                        put("source_type", event.source_type)
                        put("base_amount", event.base_amount)
                        put("description", event.description)
                        putJsonObject("metadata") {
                            // Parse metadata string back to JSON if needed
                            // For now, just include as-is
                        }
                    }
                )

                if (response.status.value in 200..299) {
                    // Success - delete from queue
                    queries.deleteSyncEvent(event.id)
                    syncedCount++

                    // TODO: Update synced_balance from response
                } else {
                    // Mark as failed
                    queries.updateSyncStatus(
                        sync_status = "failed",
                        last_attempt_at = clock.now().epochSeconds,
                        error_message = "HTTP ${response.status.value}",
                        id = event.id
                    )
                }
            } catch (e: Exception) {
                // Network error or other exception
                queries.updateSyncStatus(
                    sync_status = "failed",
                    last_attempt_at = clock.now().epochSeconds,
                    error_message = e.message ?: "Unknown error",
                    id = event.id
                )
            }
        }

        return@withContext syncedCount
    }

    override suspend fun isPremiumUnlocked(
        userId: String,
        contentType: String,
        contentId: String
    ): Boolean = withContext(Dispatchers.Default) {
        queries.getUnlock(userId, contentType, contentId).executeAsOneOrNull() != null
    }

    override suspend fun getUnlockedContent(userId: String, contentType: String): List<String> =
        withContext(Dispatchers.Default) {
            queries.getUnlockedByType(userId, contentType)
                .executeAsList()
                .map { it.content_id }
        }

    override suspend fun getShopCatalog(): List<ShopItem> = withContext(Dispatchers.Default) {
        // Hardcoded catalog matching backend redemptions from jworks:35
        listOf(
            // Themes
            ShopItem("theme_sakura", "Sakura Theme", "Cherry blossom UI theme", 200, ShopCategory.THEME, "sakura"),
            ShopItem("theme_ocean", "Ocean Theme", "Ocean blue UI theme", 200, ShopCategory.THEME, "ocean"),
            ShopItem("theme_forest", "Forest Theme", "Nature green UI theme", 200, ShopCategory.THEME, "forest"),
            ShopItem("theme_night", "Night Theme", "Dark mode theme", 200, ShopCategory.THEME, "night"),
            ShopItem("theme_autumn", "Autumn Theme", "Fall colors theme", 200, ShopCategory.THEME, "autumn"),

            // Boosters
            ShopItem("booster_2x_24h", "Double XP (24h)", "2x XP multiplier for 24 hours", 150, ShopCategory.BOOSTER),
            ShopItem("booster_3x_12h", "Triple XP (12h)", "3x XP multiplier for 12 hours", 250, ShopCategory.BOOSTER),

            // Utilities
            ShopItem("srs_freeze", "SRS Freeze", "Skip a day without losing streak", 100, ShopCategory.UTILITY),
            ShopItem("hint_pack_10", "Hint Pack (10)", "10 hints for quizzes", 50, ShopCategory.UTILITY),
            ShopItem("hint_pack_50", "Hint Pack (50)", "50 hints for quizzes", 200, ShopCategory.UTILITY),

            // Content
            ShopItem("kanken_set", "Kanken Kanji Set", "Advanced kanji beyond JLPT", 750, ShopCategory.CONTENT, "kanken"),
            ShopItem("custom_avatar", "Custom Avatar", "Profile avatar customization", 300, ShopCategory.CONTENT),

            // Cross-Business
            ShopItem("tutoringjay_trial", "TutoringJay Trial Lesson", "Free 30-min Japanese lesson", 2000, ShopCategory.CROSS_BUSINESS, contentId = "tutoringjay_trial"),
            ShopItem("tutoringjay_credit_5", "TutoringJay $5 Credit", "$5 tuition credit", 500, ShopCategory.CROSS_BUSINESS, contentId = "tutoringjay_credit_5")
        )
    }

    override suspend fun purchaseItem(userId: String, item: ShopItem): PurchaseResult =
        withContext(Dispatchers.Default) {
            try {
                // Check if already owned (for non-consumables)
                if (item.contentId != null && item.category != ShopCategory.UTILITY && item.category != ShopCategory.BOOSTER) {
                    val alreadyOwned = queries.getUnlock(userId, item.category.name.lowercase(), item.contentId)
                        .executeAsOneOrNull() != null
                    if (alreadyOwned) {
                        return@withContext PurchaseResult.AlreadyOwned(item)
                    }
                }

                // Check balance
                queries.initializeBalance(userId)
                val balance = queries.getBalance(userId).executeAsOneOrNull()
                    ?: return@withContext PurchaseResult.Error("Failed to get balance")

                if (balance.local_balance < item.cost * 1000L) {
                    return@withContext PurchaseResult.InsufficientFunds(item.cost, balance.local_balance / 1000)
                }

                // Deduct coins
                queries.transaction {
                    queries.addToBalance(
                        local_balance = -(item.cost.toLong() * 1000),
                        lifetime_earned = 0,
                        user_id = userId
                    )

                    // Update lifetime_spent
                    val current = queries.getBalance(userId).executeAsOneOrNull()
                    if (current != null) {
                        queries.upsertBalance(
                            user_id = userId,
                            local_balance = current.local_balance,
                            synced_balance = current.synced_balance,
                            lifetime_earned = current.lifetime_earned,
                            lifetime_spent = current.lifetime_spent + (item.cost.toLong() * 1000),
                            tier = current.tier,
                            last_synced_at = current.last_synced_at,
                            needs_sync = 1
                        )
                    }

                    // Record unlock if applicable
                    if (item.contentId != null && item.category != ShopCategory.UTILITY) {
                        queries.insertUnlock(
                            user_id = userId,
                            content_type = item.category.name.lowercase(),
                            content_id = item.contentId,
                            unlocked_at = clock.now().epochSeconds,
                            cost_coins = item.cost.toLong()
                        )
                    }

                    // Queue spend event for backend sync
                    queries.insertSyncEvent(
                        user_id = userId,
                        event_type = "spend",
                        source_type = "purchase_${item.id}",
                        base_amount = item.cost.toLong(),
                        description = "Purchased: ${item.name}",
                        metadata = """{"item_id":"${item.id}","category":"${item.category.name}"}""",
                        created_at = clock.now().epochSeconds
                    )
                }

                val newBalance = queries.getBalance(userId).executeAsOneOrNull()
                PurchaseResult.Success(item, newBalance?.local_balance ?: 0, item.contentId)
            } catch (e: Exception) {
                PurchaseResult.Error(e.message ?: "Purchase failed", e)
            }
        }

    override suspend fun getActiveBoosters(userId: String): List<ActiveBooster> =
        withContext(Dispatchers.Default) {
            val now = clock.now().epochSeconds
            queries.getActiveBoosters(userId, now)
                .executeAsList()
                .map {
                    ActiveBooster(
                        id = it.id,
                        userId = it.user_id,
                        boosterType = BoosterType.fromString(it.booster_type),
                        multiplier = it.multiplier,
                        activatedAt = it.activated_at,
                        expiresAt = it.expires_at
                    )
                }
        }

    override suspend fun activateBooster(
        userId: String,
        boosterType: BoosterType,
        durationHours: Int,
        cost: Int
    ): PurchaseResult = withContext(Dispatchers.Default) {
        try {
            // Check balance
            queries.initializeBalance(userId)
            val balance = queries.getBalance(userId).executeAsOneOrNull()
                ?: return@withContext PurchaseResult.Error("Failed to get balance")

            if (balance.local_balance < cost * 1000L) {
                return@withContext PurchaseResult.InsufficientFunds(cost, balance.local_balance / 1000)
            }

            val now = clock.now().epochSeconds
            val expiresAt = now + (durationHours * 3600L)

            // Deduct coins and activate booster
            queries.transaction {
                queries.addToBalance(
                    local_balance = -(cost.toLong() * 1000),
                    lifetime_earned = 0,
                    user_id = userId
                )

                queries.insertBooster(
                    user_id = userId,
                    booster_type = boosterType.name,
                    multiplier = boosterType.defaultMultiplier,
                    activated_at = now,
                    expires_at = expiresAt
                )

                // Queue spend event
                queries.insertSyncEvent(
                    user_id = userId,
                    event_type = "spend",
                    source_type = "booster_${boosterType.name.lowercase()}",
                    base_amount = cost.toLong(),
                    description = "Activated ${boosterType.displayName} (${durationHours}h)",
                    metadata = """{"booster_type":"${boosterType.name}","duration_hours":$durationHours}""",
                    created_at = now
                )
            }

            val boosterItem = ShopItem(
                id = "booster_${boosterType.name.lowercase()}",
                name = boosterType.displayName,
                description = "${boosterType.defaultMultiplier}x XP for ${durationHours}h",
                cost = cost,
                category = ShopCategory.BOOSTER
            )

            val newBalance = queries.getBalance(userId).executeAsOneOrNull()
            PurchaseResult.Success(boosterItem, newBalance?.local_balance ?: 0)
        } catch (e: Exception) {
            PurchaseResult.Error(e.message ?: "Booster activation failed", e)
        }
    }
}

private fun com.jworks.kanjiquest.db.Coin_balance.toDomain() = CoinBalance(
    userId = user_id,
    localBalance = local_balance,
    syncedBalance = synced_balance,
    lifetimeEarned = lifetime_earned,
    lifetimeSpent = lifetime_spent,
    tier = CoinTier.fromString(tier),
    lastSyncedAt = last_synced_at,
    needsSync = needs_sync != 0L
)
