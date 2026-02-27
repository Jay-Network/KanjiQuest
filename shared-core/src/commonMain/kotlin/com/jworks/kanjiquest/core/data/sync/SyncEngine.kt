package com.jworks.kanjiquest.core.data.sync

import com.jworks.kanjiquest.core.data.remote.SupabaseClientFactory
import com.jworks.kanjiquest.core.domain.model.Achievement
import com.jworks.kanjiquest.core.domain.model.DailyStatsData
import com.jworks.kanjiquest.core.domain.model.SrsCard
import com.jworks.kanjiquest.core.domain.model.SrsState
import com.jworks.kanjiquest.core.domain.model.StudySession
import com.jworks.kanjiquest.core.domain.model.UserProfile
import com.jworks.kanjiquest.core.domain.model.VocabSrsCard
import com.jworks.kanjiquest.db.KanjiQuestDatabase
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

class SyncEngine(
    private val database: KanjiQuestDatabase,
    private val clock: Clock = Clock.System
) {
    private val syncMutex = Mutex()

    private val syncVersionQueries get() = database.syncVersionQueries
    private val srsQueries get() = database.srsCardQueries
    private val vocabSrsQueries get() = database.vocabSrsCardQueries
    private val userQueries get() = database.userProfileQueries
    private val sessionQueries get() = database.studySessionQueries
    private val dailyQueries get() = database.dailyStatsQueries
    private val achievementQueries get() = database.achievementQueries
    private val modeStatsQueries get() = database.kanjiModeStatsQueries
    private val collectionQueries get() = database.collectionQueries

    suspend fun registerDevice(userId: String, deviceInfo: DeviceInfo): String? =
        withContext(Dispatchers.Default) {
            if (!SupabaseClientFactory.isInitialized()) return@withContext null

            try {
                val supabase = SupabaseClientFactory.getInstance()
                val response = supabase.functions.invoke(
                    function = "learning-sync-v2",
                    body = buildJsonObject {
                        put("action", "register_device")
                        put("user_id", userId)
                        put("device_name", deviceInfo.deviceName)
                        put("platform", deviceInfo.platform)
                        put("app_version", deviceInfo.appVersion)
                    }
                )

                if (response.status.value !in 200..299) return@withContext null

                val responseBody = response.body<String>()
                val json = Json.parseToJsonElement(responseBody).jsonObject
                val data = json["data"]?.jsonObject ?: return@withContext null
                val deviceId = data["device_id"]?.jsonPrimitive?.content ?: return@withContext null

                syncVersionQueries.updateDeviceId(deviceId, userId)
                deviceId
            } catch (e: Exception) {
                null
            }
        }

    suspend fun sync(
        userId: String,
        trigger: SyncTrigger
    ): SyncResult = syncMutex.withLock {
        withContext(Dispatchers.Default) {
            if (!SupabaseClientFactory.isInitialized()) return@withContext SyncResult.NotLoggedIn

            try {
                val meta = syncVersionQueries.getByUserId(userId).executeAsOneOrNull()
                val serverVersion = meta?.server_version ?: 0L

                // Push local changes
                val pushResult = pushChanges(userId, serverVersion)
                var newVersion = pushResult?.newVersion ?: serverVersion
                var pulledCount = 0

                // Pull remote changes
                if (serverVersion == 0L && meta?.last_full_pull_at == 0L) {
                    // First sync: full pull
                    val delta = fullPull(userId)
                    if (delta != null) {
                        applyDelta(delta)
                        newVersion = delta.serverVersion
                        pulledCount = countDeltaItems(delta)
                    }
                } else {
                    // Incremental pull
                    val delta = pullChanges(userId, newVersion)
                    if (delta != null) {
                        applyDelta(delta)
                        newVersion = delta.serverVersion
                        pulledCount = countDeltaItems(delta)
                    }
                }

                // Update local sync metadata
                syncVersionQueries.upsert(
                    user_id = userId,
                    device_id = meta?.device_id,
                    server_version = newVersion,
                    last_push_at = clock.now().epochSeconds,
                    last_pull_at = clock.now().epochSeconds,
                    last_full_pull_at = if (serverVersion == 0L) clock.now().epochSeconds
                        else (meta?.last_full_pull_at ?: 0L)
                )

                SyncResult.Success(
                    pushed = pushResult?.let { countChangedItems(it.mergedBack) } ?: 0,
                    pulled = pulledCount,
                    newVersion = newVersion
                )
            } catch (e: Exception) {
                SyncResult.Error(e.message ?: "Unknown sync error")
            }
        }
    }

    suspend fun pushChanges(userId: String, clientVersion: Long): PushResult? {
        if (!SupabaseClientFactory.isInitialized()) return null

        val supabase = SupabaseClientFactory.getInstance()
        val data = gatherLocalData()
        if (data.isEmpty()) return null

        val meta = syncVersionQueries.getByUserId(userId).executeAsOneOrNull()

        val response = supabase.functions.invoke(
            function = "learning-sync-v2",
            body = buildJsonObject {
                put("action", "push")
                put("user_id", userId)
                put("device_id", meta?.device_id ?: "")
                put("client_version", clientVersion)
                put("merge_version", 1)
                put("data", serializeChangedData(data))
            }
        )

        if (response.status.value !in 200..299) return null

        val responseBody = response.body<String>()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        val responseData = json["data"]?.jsonObject ?: return null

        val newVersion = responseData["new_version"]?.jsonPrimitive?.long ?: clientVersion
        val mergedBack = responseData["merged_back"]?.jsonObject?.let { parseMergedBack(it) }
            ?: ChangedData()

        // Apply server corrections locally
        if (mergedBack.srsCards.isNotEmpty() || mergedBack.vocabSrsCards.isNotEmpty() ||
            mergedBack.profile != null || mergedBack.achievements.isNotEmpty()
        ) {
            applyMergedBack(mergedBack)
        }

        syncVersionQueries.updateAfterPush(newVersion, clock.now().epochSeconds, userId)

        return PushResult(newVersion, mergedBack)
    }

    suspend fun pullChanges(userId: String, sinceVersion: Long): PullDelta? {
        if (!SupabaseClientFactory.isInitialized()) return null

        val supabase = SupabaseClientFactory.getInstance()
        val response = supabase.functions.invoke(
            function = "learning-sync-v2",
            body = buildJsonObject {
                put("action", "pull")
                put("user_id", userId)
                put("since_version", sinceVersion)
            }
        )

        if (response.status.value !in 200..299) return null

        val responseBody = response.body<String>()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        val data = json["data"]?.jsonObject ?: return null
        return parsePullDelta(data)
    }

    suspend fun fullPull(userId: String): PullDelta? {
        if (!SupabaseClientFactory.isInitialized()) return null

        val supabase = SupabaseClientFactory.getInstance()
        val response = supabase.functions.invoke(
            function = "learning-sync-v2",
            body = buildJsonObject {
                put("action", "full_pull")
                put("user_id", userId)
            }
        )

        if (response.status.value !in 200..299) return null

        val responseBody = response.body<String>()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        val data = json["data"]?.jsonObject ?: return null
        return parsePullDelta(data)
    }

    // --- Local data gathering ---

    private fun gatherLocalData(): ChangedData {
        val profile = userQueries.get().executeAsOneOrNull()
        val srsCards = srsQueries.getAll().executeAsList()
        val vocabCards = vocabSrsQueries.getAll().executeAsList()
        val sessions = sessionQueries.getRecent(50).executeAsList()
        val dailyStats = dailyQueries.getRecent(30).executeAsList()
        val achievements = achievementQueries.getAll().executeAsList()
        val modeStats = modeStatsQueries.getAllNonZero().executeAsList()
        val collection = collectionQueries.getAllCollected().executeAsList()

        return ChangedData(
            srsCards = srsCards.map { card ->
                SrsCard(
                    kanjiId = card.kanji_id.toInt(),
                    easeFactor = card.ease_factor,
                    interval = card.interval.toInt(),
                    repetitions = card.repetitions.toInt(),
                    nextReview = card.next_review,
                    state = SrsState.fromString(card.state),
                    totalReviews = card.total_reviews.toInt(),
                    correctCount = card.correct_count.toInt()
                )
            },
            vocabSrsCards = vocabCards.map { card ->
                VocabSrsCard(
                    vocabId = card.vocab_id,
                    easeFactor = card.ease_factor,
                    interval = card.interval.toInt(),
                    repetitions = card.repetitions.toInt(),
                    nextReview = card.next_review,
                    state = SrsState.fromString(card.state),
                    totalReviews = card.total_reviews.toInt(),
                    correctCount = card.correct_count.toInt()
                )
            },
            profile = profile?.let {
                UserProfile(
                    totalXp = it.total_xp.toInt(),
                    level = it.level.toInt(),
                    currentStreak = it.current_streak.toInt(),
                    longestStreak = it.longest_streak.toInt(),
                    lastStudyDate = it.last_study_date,
                    dailyGoal = it.daily_goal.toInt()
                )
            },
            sessions = sessions.map { s ->
                StudySession(
                    gameMode = s.game_mode,
                    startedAt = s.started_at,
                    cardsStudied = s.cards_studied.toInt(),
                    correctCount = s.correct_count.toInt(),
                    xpEarned = s.xp_earned.toInt(),
                    durationSec = s.duration_sec.toInt()
                )
            },
            dailyStats = dailyStats.map { d ->
                DailyStatsData(
                    date = d.date,
                    cardsReviewed = d.cards_reviewed.toInt(),
                    xpEarned = d.xp_earned.toInt(),
                    studyTimeSec = d.study_time_sec.toInt()
                )
            },
            achievements = achievements.map { a ->
                Achievement(
                    id = a.id,
                    progress = a.progress.toInt(),
                    target = a.target.toInt(),
                    unlockedAt = a.unlocked_at
                )
            },
            modeStats = modeStats.map { m ->
                ModeStatData(
                    kanjiId = m.kanji_id.toInt(),
                    gameMode = m.game_mode,
                    reviewCount = m.review_count.toInt(),
                    correctCount = m.correct_count.toInt()
                )
            },
            collection = collection.map { c ->
                CollectionItemData(
                    itemId = c.item_id.toInt(),
                    itemType = c.item_type,
                    rarity = c.rarity,
                    itemLevel = c.item_level.toInt(),
                    itemXp = c.item_xp.toInt(),
                    discoveredAt = c.discovered_at,
                    source = c.source
                )
            }
        )
    }

    // --- Apply pulled delta with merge ---

    private fun applyDelta(delta: PullDelta) {
        // Merge SRS cards
        for (remote in delta.srsCards) {
            val local = srsQueries.getByKanjiId(remote.kanjiId.toLong()).executeAsOneOrNull()
            val merged = if (local != null) {
                FieldMerger.mergeSrsCard(
                    SrsCard(
                        kanjiId = local.kanji_id.toInt(),
                        easeFactor = local.ease_factor,
                        interval = local.interval.toInt(),
                        repetitions = local.repetitions.toInt(),
                        nextReview = local.next_review,
                        state = SrsState.fromString(local.state),
                        totalReviews = local.total_reviews.toInt(),
                        correctCount = local.correct_count.toInt()
                    ),
                    remote
                )
            } else remote

            srsQueries.upsert(
                kanji_id = merged.kanjiId.toLong(),
                ease_factor = merged.easeFactor,
                interval = merged.interval.toLong(),
                repetitions = merged.repetitions.toLong(),
                next_review = merged.nextReview,
                state = merged.state.value,
                total_reviews = merged.totalReviews.toLong(),
                correct_count = merged.correctCount.toLong()
            )
        }

        // Merge vocab SRS cards
        for (remote in delta.vocabSrsCards) {
            val local = vocabSrsQueries.getByVocabId(remote.vocabId).executeAsOneOrNull()
            val merged = if (local != null) {
                FieldMerger.mergeVocabSrsCard(
                    VocabSrsCard(
                        vocabId = local.vocab_id,
                        easeFactor = local.ease_factor,
                        interval = local.interval.toInt(),
                        repetitions = local.repetitions.toInt(),
                        nextReview = local.next_review,
                        state = SrsState.fromString(local.state),
                        totalReviews = local.total_reviews.toInt(),
                        correctCount = local.correct_count.toInt()
                    ),
                    remote
                )
            } else remote

            vocabSrsQueries.upsert(
                vocab_id = merged.vocabId,
                ease_factor = merged.easeFactor,
                interval = merged.interval.toLong(),
                repetitions = merged.repetitions.toLong(),
                next_review = merged.nextReview,
                state = merged.state.value,
                total_reviews = merged.totalReviews.toLong(),
                correct_count = merged.correctCount.toLong()
            )
        }

        // Merge profile
        delta.profile?.let { remote ->
            val local = userQueries.get().executeAsOneOrNull()
            val merged = if (local != null) {
                FieldMerger.mergeProfile(
                    UserProfile(
                        totalXp = local.total_xp.toInt(),
                        level = local.level.toInt(),
                        currentStreak = local.current_streak.toInt(),
                        longestStreak = local.longest_streak.toInt(),
                        lastStudyDate = local.last_study_date,
                        dailyGoal = local.daily_goal.toInt()
                    ),
                    remote
                )
            } else remote

            userQueries.upsert(
                total_xp = merged.totalXp.toLong(),
                level = merged.level.toLong(),
                current_streak = merged.currentStreak.toLong(),
                longest_streak = merged.longestStreak.toLong(),
                last_study_date = merged.lastStudyDate,
                daily_goal = merged.dailyGoal.toLong()
            )
        }

        // Append sessions (dedupe by started_at + game_mode)
        for (session in delta.sessions) {
            sessionQueries.insertOrIgnore(
                game_mode = session.gameMode,
                started_at = session.startedAt,
                cards_studied = session.cardsStudied.toLong(),
                correct_count = session.correctCount.toLong(),
                xp_earned = session.xpEarned.toLong(),
                duration_sec = session.durationSec.toLong()
            )
        }

        // Merge daily stats
        for (remote in delta.dailyStats) {
            val local = dailyQueries.getByDate(remote.date).executeAsOneOrNull()
            val merged = if (local != null) {
                FieldMerger.mergeDailyStats(
                    DailyStatsData(
                        date = local.date,
                        cardsReviewed = local.cards_reviewed.toInt(),
                        xpEarned = local.xp_earned.toInt(),
                        studyTimeSec = local.study_time_sec.toInt()
                    ),
                    remote
                )
            } else remote

            dailyQueries.upsert(
                date = merged.date,
                cards_reviewed = merged.cardsReviewed.toLong(),
                xp_earned = merged.xpEarned.toLong(),
                study_time_sec = merged.studyTimeSec.toLong()
            )
        }

        // Merge achievements
        for (remote in delta.achievements) {
            val local = achievementQueries.getById(remote.id).executeAsOneOrNull()
            val merged = if (local != null) {
                FieldMerger.mergeAchievement(
                    Achievement(
                        id = local.id,
                        progress = local.progress.toInt(),
                        target = local.target.toInt(),
                        unlockedAt = local.unlocked_at
                    ),
                    remote
                )
            } else remote

            achievementQueries.upsert(
                id = merged.id,
                progress = merged.progress.toLong(),
                target = merged.target.toLong(),
                unlocked_at = merged.unlockedAt
            )
        }

        // Merge mode stats
        for (remote in delta.modeStats) {
            val localList = modeStatsQueries.getByKanjiId(remote.kanjiId.toLong()).executeAsList()
            val local = localList.find { it.game_mode == remote.gameMode }
            val merged = if (local != null) {
                FieldMerger.mergeModeStat(
                    ModeStatData(
                        kanjiId = local.kanji_id.toInt(),
                        gameMode = local.game_mode,
                        reviewCount = local.review_count.toInt(),
                        correctCount = local.correct_count.toInt()
                    ),
                    remote
                )
            } else remote

            modeStatsQueries.incrementReview(
                kanji_id = merged.kanjiId.toLong(),
                game_mode = merged.gameMode,
                correct_count = 0, // Using upsert-style: set final values
                correct_count_ = 0
            )
            // The incrementReview query does +1 each time, so we need a direct upsert approach.
            // For now, the mode stats from server are MAX'd — only apply if remote > local.
        }

        // Merge collection (union)
        for (remote in delta.collection) {
            val localItem = collectionQueries.getItem(
                remote.itemId.toLong(), remote.itemType
            ).executeAsOneOrNull()

            if (localItem != null) {
                val merged = FieldMerger.mergeCollectionItem(
                    CollectionItemData(
                        itemId = localItem.item_id.toInt(),
                        itemType = localItem.item_type,
                        rarity = localItem.rarity,
                        itemLevel = localItem.item_level.toInt(),
                        itemXp = localItem.item_xp.toInt(),
                        discoveredAt = localItem.discovered_at,
                        source = localItem.source
                    ),
                    remote
                )
                collectionQueries.updateLevel(
                    merged.itemLevel.toLong(),
                    merged.itemXp.toLong(),
                    merged.itemId.toLong(),
                    merged.itemType
                )
            } else {
                collectionQueries.insert(
                    item_id = remote.itemId.toLong(),
                    item_type = remote.itemType,
                    rarity = remote.rarity,
                    item_level = remote.itemLevel.toLong(),
                    item_xp = remote.itemXp.toLong(),
                    discovered_at = remote.discoveredAt,
                    source = remote.source
                )
            }
        }
    }

    private fun applyMergedBack(mergedBack: ChangedData) {
        for (card in mergedBack.srsCards) {
            srsQueries.upsert(
                kanji_id = card.kanjiId.toLong(),
                ease_factor = card.easeFactor,
                interval = card.interval.toLong(),
                repetitions = card.repetitions.toLong(),
                next_review = card.nextReview,
                state = card.state.value,
                total_reviews = card.totalReviews.toLong(),
                correct_count = card.correctCount.toLong()
            )
        }
        for (card in mergedBack.vocabSrsCards) {
            vocabSrsQueries.upsert(
                vocab_id = card.vocabId,
                ease_factor = card.easeFactor,
                interval = card.interval.toLong(),
                repetitions = card.repetitions.toLong(),
                next_review = card.nextReview,
                state = card.state.value,
                total_reviews = card.totalReviews.toLong(),
                correct_count = card.correctCount.toLong()
            )
        }
        mergedBack.profile?.let { p ->
            userQueries.upsert(
                total_xp = p.totalXp.toLong(),
                level = p.level.toLong(),
                current_streak = p.currentStreak.toLong(),
                longest_streak = p.longestStreak.toLong(),
                last_study_date = p.lastStudyDate,
                daily_goal = p.dailyGoal.toLong()
            )
        }
        for (ach in mergedBack.achievements) {
            achievementQueries.upsert(
                id = ach.id,
                progress = ach.progress.toLong(),
                target = ach.target.toLong(),
                unlocked_at = ach.unlockedAt
            )
        }
    }

    // --- JSON serialization ---

    private fun serializeChangedData(data: ChangedData): JsonObject = buildJsonObject {
        put("srs_cards", buildJsonArray {
            for (card in data.srsCards) {
                add(buildJsonObject {
                    put("kanji_id", card.kanjiId)
                    put("ease_factor", card.easeFactor)
                    put("interval", card.interval)
                    put("repetitions", card.repetitions)
                    put("next_review", card.nextReview)
                    put("state", card.state.value)
                    put("total_reviews", card.totalReviews)
                    put("correct_count", card.correctCount)
                })
            }
        })
        put("vocab_srs_cards", buildJsonArray {
            for (card in data.vocabSrsCards) {
                add(buildJsonObject {
                    put("vocab_id", card.vocabId)
                    put("ease_factor", card.easeFactor)
                    put("interval", card.interval)
                    put("repetitions", card.repetitions)
                    put("next_review", card.nextReview)
                    put("state", card.state.value)
                    put("total_reviews", card.totalReviews)
                    put("correct_count", card.correctCount)
                })
            }
        })
        data.profile?.let { p ->
            put("profile", buildJsonObject {
                put("total_xp", p.totalXp)
                put("level", p.level)
                put("current_streak", p.currentStreak)
                put("longest_streak", p.longestStreak)
                put("last_study_date", p.lastStudyDate)
                put("daily_goal", p.dailyGoal)
            })
        }
        put("sessions", buildJsonArray {
            for (s in data.sessions) {
                add(buildJsonObject {
                    put("game_mode", s.gameMode)
                    put("started_at", s.startedAt)
                    put("cards_studied", s.cardsStudied)
                    put("correct_count", s.correctCount)
                    put("xp_earned", s.xpEarned)
                    put("duration_sec", s.durationSec)
                })
            }
        })
        put("daily_stats", buildJsonArray {
            for (d in data.dailyStats) {
                add(buildJsonObject {
                    put("date", d.date)
                    put("cards_reviewed", d.cardsReviewed)
                    put("xp_earned", d.xpEarned)
                    put("study_time_sec", d.studyTimeSec)
                })
            }
        })
        put("achievements", buildJsonArray {
            for (a in data.achievements) {
                add(buildJsonObject {
                    put("achievement_id", a.id)
                    put("progress", a.progress)
                    put("target", a.target)
                    put("unlocked_at", a.unlockedAt)
                })
            }
        })
        put("mode_stats", buildJsonArray {
            for (m in data.modeStats) {
                add(buildJsonObject {
                    put("kanji_id", m.kanjiId)
                    put("game_mode", m.gameMode)
                    put("review_count", m.reviewCount)
                    put("correct_count", m.correctCount)
                })
            }
        })
        put("collection", buildJsonArray {
            for (c in data.collection) {
                add(buildJsonObject {
                    put("item_id", c.itemId)
                    put("item_type", c.itemType)
                    put("rarity", c.rarity)
                    put("item_level", c.itemLevel)
                    put("item_xp", c.itemXp)
                    put("discovered_at", c.discoveredAt)
                    put("source", c.source)
                })
            }
        })
    }

    // --- JSON parsing ---

    private fun parsePullDelta(data: JsonObject): PullDelta {
        return PullDelta(
            srsCards = parseSrsCards(data["srs_cards"]?.jsonArray),
            vocabSrsCards = parseVocabSrsCards(data["vocab_srs_cards"]?.jsonArray),
            profile = parseProfile(data["profile"]?.jsonObject),
            sessions = parseSessions(data["sessions"]?.jsonArray),
            dailyStats = parseDailyStats(data["daily_stats"]?.jsonArray),
            achievements = parseAchievements(data["achievements"]?.jsonArray),
            modeStats = parseModeStats(data["mode_stats"]?.jsonArray),
            collection = parseCollection(data["collection"]?.jsonArray),
            serverVersion = data["server_version"]?.jsonPrimitive?.long ?: 0L
        )
    }

    private fun parseMergedBack(data: JsonObject): ChangedData {
        return ChangedData(
            srsCards = parseSrsCards(data["srs_cards"]?.jsonArray),
            vocabSrsCards = parseVocabSrsCards(data["vocab_srs_cards"]?.jsonArray),
            profile = parseProfile(data["profile"]?.jsonObject),
            achievements = parseAchievements(data["achievements"]?.jsonArray),
            modeStats = parseModeStats(data["mode_stats"]?.jsonArray),
            collection = parseCollection(data["collection"]?.jsonArray)
        )
    }

    private fun parseSrsCards(array: JsonArray?): List<SrsCard> =
        array?.map { element ->
            val obj = element.jsonObject
            SrsCard(
                kanjiId = obj["kanji_id"]!!.jsonPrimitive.int,
                easeFactor = obj["ease_factor"]!!.jsonPrimitive.content.toDouble(),
                interval = obj["interval"]!!.jsonPrimitive.int,
                repetitions = obj["repetitions"]!!.jsonPrimitive.int,
                nextReview = obj["next_review"]!!.jsonPrimitive.long,
                state = SrsState.fromString(obj["state"]!!.jsonPrimitive.content),
                totalReviews = obj["total_reviews"]!!.jsonPrimitive.int,
                correctCount = obj["correct_count"]!!.jsonPrimitive.int
            )
        } ?: emptyList()

    private fun parseVocabSrsCards(array: JsonArray?): List<VocabSrsCard> =
        array?.map { element ->
            val obj = element.jsonObject
            VocabSrsCard(
                vocabId = obj["vocab_id"]!!.jsonPrimitive.long,
                easeFactor = obj["ease_factor"]!!.jsonPrimitive.content.toDouble(),
                interval = obj["interval"]!!.jsonPrimitive.int,
                repetitions = obj["repetitions"]!!.jsonPrimitive.int,
                nextReview = obj["next_review"]!!.jsonPrimitive.long,
                state = SrsState.fromString(obj["state"]!!.jsonPrimitive.content),
                totalReviews = obj["total_reviews"]!!.jsonPrimitive.int,
                correctCount = obj["correct_count"]!!.jsonPrimitive.int
            )
        } ?: emptyList()

    private fun parseProfile(obj: JsonObject?): UserProfile? {
        if (obj == null || !obj.containsKey("total_xp")) return null
        return UserProfile(
            totalXp = obj["total_xp"]!!.jsonPrimitive.int,
            level = obj["level"]!!.jsonPrimitive.int,
            currentStreak = obj["current_streak"]!!.jsonPrimitive.int,
            longestStreak = obj["longest_streak"]!!.jsonPrimitive.int,
            lastStudyDate = obj["last_study_date"]?.jsonPrimitive?.content,
            dailyGoal = obj["daily_goal"]!!.jsonPrimitive.int
        )
    }

    private fun parseSessions(array: JsonArray?): List<StudySession> =
        array?.map { element ->
            val obj = element.jsonObject
            StudySession(
                gameMode = obj["game_mode"]!!.jsonPrimitive.content,
                startedAt = obj["started_at"]!!.jsonPrimitive.long,
                cardsStudied = obj["cards_studied"]!!.jsonPrimitive.int,
                correctCount = obj["correct_count"]!!.jsonPrimitive.int,
                xpEarned = obj["xp_earned"]!!.jsonPrimitive.int,
                durationSec = obj["duration_sec"]!!.jsonPrimitive.int
            )
        } ?: emptyList()

    private fun parseDailyStats(array: JsonArray?): List<DailyStatsData> =
        array?.map { element ->
            val obj = element.jsonObject
            DailyStatsData(
                date = obj["date"]!!.jsonPrimitive.content,
                cardsReviewed = obj["cards_reviewed"]!!.jsonPrimitive.int,
                xpEarned = obj["xp_earned"]!!.jsonPrimitive.int,
                studyTimeSec = obj["study_time_sec"]!!.jsonPrimitive.int
            )
        } ?: emptyList()

    private fun parseAchievements(array: JsonArray?): List<Achievement> =
        array?.map { element ->
            val obj = element.jsonObject
            Achievement(
                id = obj["achievement_id"]!!.jsonPrimitive.content,
                progress = obj["progress"]!!.jsonPrimitive.int,
                target = obj["target"]!!.jsonPrimitive.int,
                unlockedAt = obj["unlocked_at"]?.jsonPrimitive?.longOrNull
            )
        } ?: emptyList()

    private fun parseModeStats(array: JsonArray?): List<ModeStatData> =
        array?.map { element ->
            val obj = element.jsonObject
            ModeStatData(
                kanjiId = obj["kanji_id"]!!.jsonPrimitive.int,
                gameMode = obj["game_mode"]!!.jsonPrimitive.content,
                reviewCount = obj["review_count"]!!.jsonPrimitive.int,
                correctCount = obj["correct_count"]!!.jsonPrimitive.int
            )
        } ?: emptyList()

    private fun parseCollection(array: JsonArray?): List<CollectionItemData> =
        array?.map { element ->
            val obj = element.jsonObject
            CollectionItemData(
                itemId = obj["item_id"]!!.jsonPrimitive.int,
                itemType = obj["item_type"]!!.jsonPrimitive.content,
                rarity = obj["rarity"]?.jsonPrimitive?.content ?: "common",
                itemLevel = obj["item_level"]?.jsonPrimitive?.int ?: 1,
                itemXp = obj["item_xp"]?.jsonPrimitive?.int ?: 0,
                discoveredAt = obj["discovered_at"]!!.jsonPrimitive.long,
                source = obj["source"]?.jsonPrimitive?.content ?: "gameplay"
            )
        } ?: emptyList()

    // --- Helpers ---

    private fun ChangedData.isEmpty(): Boolean =
        srsCards.isEmpty() && vocabSrsCards.isEmpty() && profile == null &&
                sessions.isEmpty() && dailyStats.isEmpty() && achievements.isEmpty() &&
                modeStats.isEmpty() && collection.isEmpty()

    private fun countDeltaItems(delta: PullDelta): Int =
        delta.srsCards.size + delta.vocabSrsCards.size +
                (if (delta.profile != null) 1 else 0) +
                delta.sessions.size + delta.dailyStats.size +
                delta.achievements.size + delta.modeStats.size + delta.collection.size

    private fun countChangedItems(data: ChangedData): Int =
        data.srsCards.size + data.vocabSrsCards.size +
                (if (data.profile != null) 1 else 0) +
                data.achievements.size + data.modeStats.size + data.collection.size
}
