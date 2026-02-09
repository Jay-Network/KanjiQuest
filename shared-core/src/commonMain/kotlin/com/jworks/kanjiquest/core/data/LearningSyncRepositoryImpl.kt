package com.jworks.kanjiquest.core.data

import com.jworks.kanjiquest.core.data.remote.SupabaseClientFactory
import com.jworks.kanjiquest.core.domain.model.Achievement
import com.jworks.kanjiquest.core.domain.model.CloudLearningData
import com.jworks.kanjiquest.core.domain.model.DailyStatsData
import com.jworks.kanjiquest.core.domain.model.SrsCard
import com.jworks.kanjiquest.core.domain.model.SrsState
import com.jworks.kanjiquest.core.domain.model.StudySession
import com.jworks.kanjiquest.core.domain.model.UserProfile
import com.jworks.kanjiquest.core.domain.model.VocabSrsCard
import com.jworks.kanjiquest.core.domain.repository.LearningSyncRepository
import com.jworks.kanjiquest.db.KanjiQuestDatabase
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

class LearningSyncRepositoryImpl(
    private val database: KanjiQuestDatabase,
    private val clock: Clock = Clock.System
) : LearningSyncRepository {

    private val syncQueries get() = database.learningSyncQueueQueries
    private val srsQueries get() = database.srsCardQueries
    private val vocabSrsQueries get() = database.vocabSrsCardQueries
    private val userQueries get() = database.userProfileQueries
    private val dailyQueries get() = database.dailyStatsQueries
    private val achievementQueries get() = database.achievementQueries

    override suspend fun queueSessionSync(
        userId: String,
        touchedKanjiIds: List<Int>,
        touchedVocabIds: List<Long>,
        profile: UserProfile,
        session: StudySession,
        dailyStats: DailyStatsData,
        achievements: List<Achievement>
    ) = withContext(Dispatchers.Default) {
        // Read touched SRS cards from local DB
        val srsCards = if (touchedKanjiIds.isNotEmpty()) {
            srsQueries.getCardsByIds(touchedKanjiIds.map { it.toLong() }).executeAsList()
        } else emptyList()

        val vocabCards = if (touchedVocabIds.isNotEmpty()) {
            vocabSrsQueries.getCardsByIds(touchedVocabIds).executeAsList()
        } else emptyList()

        // Build JSON payload
        val payload = buildJsonObject {
            put("srs_cards", buildJsonArray {
                for (card in srsCards) {
                    add(buildJsonObject {
                        put("kanji_id", card.kanji_id.toInt())
                        put("ease_factor", card.ease_factor)
                        put("interval", card.interval.toInt())
                        put("repetitions", card.repetitions.toInt())
                        put("next_review", card.next_review)
                        put("state", card.state)
                        put("total_reviews", card.total_reviews.toInt())
                        put("correct_count", card.correct_count.toInt())
                    })
                }
            })
            put("vocab_srs_cards", buildJsonArray {
                for (card in vocabCards) {
                    add(buildJsonObject {
                        put("vocab_id", card.vocab_id)
                        put("ease_factor", card.ease_factor)
                        put("interval", card.interval.toInt())
                        put("repetitions", card.repetitions.toInt())
                        put("next_review", card.next_review)
                        put("state", card.state)
                        put("total_reviews", card.total_reviews.toInt())
                        put("correct_count", card.correct_count.toInt())
                    })
                }
            })
            put("profile", buildJsonObject {
                put("total_xp", profile.totalXp)
                put("level", profile.level)
                put("current_streak", profile.currentStreak)
                put("longest_streak", profile.longestStreak)
                put("last_study_date", profile.lastStudyDate)
                put("daily_goal", profile.dailyGoal)
            })
            put("sessions", buildJsonArray {
                add(buildJsonObject {
                    put("game_mode", session.gameMode)
                    put("started_at", session.startedAt)
                    put("cards_studied", session.cardsStudied)
                    put("correct_count", session.correctCount)
                    put("xp_earned", session.xpEarned)
                    put("duration_sec", session.durationSec)
                })
            })
            put("daily_stats", buildJsonArray {
                add(buildJsonObject {
                    put("date", dailyStats.date)
                    put("cards_reviewed", dailyStats.cardsReviewed)
                    put("xp_earned", dailyStats.xpEarned)
                    put("study_time_sec", dailyStats.studyTimeSec)
                })
            })
            put("achievements", buildJsonArray {
                for (ach in achievements) {
                    add(buildJsonObject {
                        put("achievement_id", ach.id)
                        put("progress", ach.progress)
                        put("target", ach.target)
                        put("unlocked_at", ach.unlockedAt)
                    })
                }
            })
        }

        syncQueries.insertEvent(
            user_id = userId,
            event_type = "session_sync",
            payload = payload.toString(),
            created_at = clock.now().epochSeconds
        )
    }

    override suspend fun syncPendingEvents(): Int = withContext(Dispatchers.Default) {
        if (!SupabaseClientFactory.isInitialized()) return@withContext 0

        val supabase = SupabaseClientFactory.getInstance()
        val pending = syncQueries.getPendingEvents().executeAsList()
        var syncedCount = 0

        for (event in pending) {
            try {
                val payloadJson = Json.parseToJsonElement(event.payload).jsonObject

                val response = supabase.functions.invoke(
                    function = "learning-sync",
                    body = buildJsonObject {
                        put("action", "push")
                        put("user_id", event.user_id)
                        // Spread payload fields
                        for ((key, value) in payloadJson) {
                            put(key, value)
                        }
                    }
                )

                if (response.status.value in 200..299) {
                    // Parse response for server-wins conflicts
                    val responseBody = response.body<String>()
                    val responseJson = Json.parseToJsonElement(responseBody).jsonObject
                    val data = responseJson["data"]?.jsonObject

                    // Apply server-wins conflicts to local DB
                    val srsConflicts = data?.get("srs_conflicts")?.jsonArray
                    if (srsConflicts != null && srsConflicts.isNotEmpty()) {
                        applyServerSrsConflicts(srsConflicts)
                    }
                    val vocabConflicts = data?.get("vocab_srs_conflicts")?.jsonArray
                    if (vocabConflicts != null && vocabConflicts.isNotEmpty()) {
                        applyServerVocabConflicts(vocabConflicts)
                    }

                    syncQueries.deleteEvent(event.id)
                    syncedCount++
                } else {
                    syncQueries.updateSyncStatus(
                        sync_status = "failed",
                        last_attempt_at = clock.now().epochSeconds,
                        error_message = "HTTP ${response.status.value}",
                        id = event.id
                    )
                }
            } catch (e: Exception) {
                syncQueries.updateSyncStatus(
                    sync_status = "failed",
                    last_attempt_at = clock.now().epochSeconds,
                    error_message = e.message ?: "Unknown error",
                    id = event.id
                )
            }
        }

        if (syncedCount > 0) {
            syncQueries.upsertSyncMetadata(
                user_id = pending.first().user_id,
                last_synced_at = clock.now().epochSeconds,
                last_push_at = clock.now().epochSeconds,
                last_pull_at = 0
            )
        }

        syncedCount
    }

    override suspend fun getPendingSyncCount(): Long = withContext(Dispatchers.Default) {
        syncQueries.getPendingCount().executeAsOne()
    }

    override suspend fun pullCloudData(userId: String): CloudLearningData? =
        withContext(Dispatchers.Default) {
            if (!SupabaseClientFactory.isInitialized()) return@withContext null

            try {
                val supabase = SupabaseClientFactory.getInstance()
                val meta = syncQueries.getSyncMetadata(userId).executeAsOneOrNull()
                val since = if (meta != null && meta.last_pull_at > 0) {
                    kotlinx.datetime.Instant.fromEpochSeconds(meta.last_pull_at).toString()
                } else null

                val response = supabase.functions.invoke(
                    function = "learning-sync",
                    body = buildJsonObject {
                        put("action", "pull")
                        put("user_id", userId)
                        if (since != null) put("since", since)
                    }
                )

                if (response.status.value !in 200..299) return@withContext null

                val responseBody = response.body<String>()
                val json = Json.parseToJsonElement(responseBody).jsonObject
                val data = json["data"]?.jsonObject ?: return@withContext null

                parseCloudData(data)
            } catch (e: Exception) {
                null
            }
        }

    override suspend fun applyCloudData(data: CloudLearningData) = withContext(Dispatchers.Default) {
        // UPSERT SRS cards
        for (card in data.srsCards) {
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

        // UPSERT vocab SRS cards
        for (card in data.vocabSrsCards) {
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

        // Update profile
        data.profile?.let { p ->
            userQueries.upsert(
                total_xp = p.totalXp.toLong(),
                level = p.level.toLong(),
                current_streak = p.currentStreak.toLong(),
                longest_streak = p.longestStreak.toLong(),
                last_study_date = p.lastStudyDate,
                daily_goal = p.dailyGoal.toLong()
            )
        }

        // UPSERT daily stats
        for (ds in data.dailyStats) {
            dailyQueries.upsert(
                date = ds.date,
                cards_reviewed = ds.cardsReviewed.toLong(),
                xp_earned = ds.xpEarned.toLong(),
                study_time_sec = ds.studyTimeSec.toLong()
            )
        }

        // UPSERT achievements
        for (ach in data.achievements) {
            achievementQueries.upsert(
                id = ach.id,
                progress = ach.progress.toLong(),
                target = ach.target.toLong(),
                unlocked_at = ach.unlockedAt
            )
        }
    }

    private fun applyServerSrsConflicts(conflicts: JsonArray) {
        for (element in conflicts) {
            val card = element.jsonObject
            srsQueries.upsert(
                kanji_id = card["kanji_id"]!!.jsonPrimitive.long,
                ease_factor = card["ease_factor"]!!.jsonPrimitive.double,
                interval = card["interval"]!!.jsonPrimitive.long,
                repetitions = card["repetitions"]!!.jsonPrimitive.long,
                next_review = card["next_review"]!!.jsonPrimitive.long,
                state = card["state"]!!.jsonPrimitive.content,
                total_reviews = card["total_reviews"]!!.jsonPrimitive.long,
                correct_count = card["correct_count"]!!.jsonPrimitive.long
            )
        }
    }

    private fun applyServerVocabConflicts(conflicts: JsonArray) {
        for (element in conflicts) {
            val card = element.jsonObject
            vocabSrsQueries.upsert(
                vocab_id = card["vocab_id"]!!.jsonPrimitive.long,
                ease_factor = card["ease_factor"]!!.jsonPrimitive.double,
                interval = card["interval"]!!.jsonPrimitive.long,
                repetitions = card["repetitions"]!!.jsonPrimitive.long,
                next_review = card["next_review"]!!.jsonPrimitive.long,
                state = card["state"]!!.jsonPrimitive.content,
                total_reviews = card["total_reviews"]!!.jsonPrimitive.long,
                correct_count = card["correct_count"]!!.jsonPrimitive.long
            )
        }
    }

    private fun parseCloudData(data: JsonObject): CloudLearningData {
        val srsCards = data["srs_cards"]?.jsonArray?.map { element ->
            val obj = element.jsonObject
            SrsCard(
                kanjiId = obj["kanji_id"]!!.jsonPrimitive.int,
                easeFactor = obj["ease_factor"]!!.jsonPrimitive.double,
                interval = obj["interval"]!!.jsonPrimitive.int,
                repetitions = obj["repetitions"]!!.jsonPrimitive.int,
                nextReview = obj["next_review"]!!.jsonPrimitive.long,
                state = SrsState.fromString(obj["state"]!!.jsonPrimitive.content),
                totalReviews = obj["total_reviews"]!!.jsonPrimitive.int,
                correctCount = obj["correct_count"]!!.jsonPrimitive.int
            )
        } ?: emptyList()

        val vocabSrsCards = data["vocab_srs_cards"]?.jsonArray?.map { element ->
            val obj = element.jsonObject
            VocabSrsCard(
                vocabId = obj["vocab_id"]!!.jsonPrimitive.long,
                easeFactor = obj["ease_factor"]!!.jsonPrimitive.double,
                interval = obj["interval"]!!.jsonPrimitive.int,
                repetitions = obj["repetitions"]!!.jsonPrimitive.int,
                nextReview = obj["next_review"]!!.jsonPrimitive.long,
                state = SrsState.fromString(obj["state"]!!.jsonPrimitive.content),
                totalReviews = obj["total_reviews"]!!.jsonPrimitive.int,
                correctCount = obj["correct_count"]!!.jsonPrimitive.int
            )
        } ?: emptyList()

        val profileObj = data["profile"]?.jsonObject
        val profile = if (profileObj != null && profileObj.containsKey("total_xp")) {
            UserProfile(
                totalXp = profileObj["total_xp"]!!.jsonPrimitive.int,
                level = profileObj["level"]!!.jsonPrimitive.int,
                currentStreak = profileObj["current_streak"]!!.jsonPrimitive.int,
                longestStreak = profileObj["longest_streak"]!!.jsonPrimitive.int,
                lastStudyDate = profileObj["last_study_date"]?.jsonPrimitive?.content,
                dailyGoal = profileObj["daily_goal"]!!.jsonPrimitive.int
            )
        } else null

        val sessions = data["sessions"]?.jsonArray?.map { element ->
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

        val dailyStats = data["daily_stats"]?.jsonArray?.map { element ->
            val obj = element.jsonObject
            DailyStatsData(
                date = obj["date"]!!.jsonPrimitive.content,
                cardsReviewed = obj["cards_reviewed"]!!.jsonPrimitive.int,
                xpEarned = obj["xp_earned"]!!.jsonPrimitive.int,
                studyTimeSec = obj["study_time_sec"]!!.jsonPrimitive.int
            )
        } ?: emptyList()

        val achievements = data["achievements"]?.jsonArray?.map { element ->
            val obj = element.jsonObject
            Achievement(
                id = obj["achievement_id"]!!.jsonPrimitive.content,
                progress = obj["progress"]!!.jsonPrimitive.int,
                target = obj["target"]!!.jsonPrimitive.int,
                unlockedAt = obj["unlocked_at"]?.jsonPrimitive?.longOrNull
            )
        } ?: emptyList()

        return CloudLearningData(
            srsCards = srsCards,
            vocabSrsCards = vocabSrsCards,
            profile = profile,
            sessions = sessions,
            dailyStats = dailyStats,
            achievements = achievements,
            pulledAt = data["pulled_at"]?.jsonPrimitive?.content
        )
    }
}
