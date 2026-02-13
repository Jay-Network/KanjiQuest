package com.jworks.kanjiquest.core.data

import com.jworks.kanjiquest.core.domain.model.GradeMastery
import com.jworks.kanjiquest.core.domain.model.SrsCard
import com.jworks.kanjiquest.core.domain.model.SrsState
import com.jworks.kanjiquest.core.domain.repository.SrsRepository
import com.jworks.kanjiquest.db.KanjiQuestDatabase

class SrsRepositoryImpl(
    private val db: KanjiQuestDatabase
) : SrsRepository {

    override suspend fun getCard(kanjiId: Int): SrsCard? {
        return db.srsCardQueries.getByKanjiId(kanjiId.toLong()).executeAsOneOrNull()?.toSrsCard()
    }

    override suspend fun getDueCards(currentTime: Long): List<SrsCard> {
        return db.srsCardQueries.getDueCards(currentTime).executeAsList().map { it.toSrsCard() }
    }

    override suspend fun getNewCards(limit: Int): List<SrsCard> {
        return db.srsCardQueries.getNewCards(limit.toLong()).executeAsList().map { it.toSrsCard() }
    }

    override suspend fun getLearningCards(limit: Int): List<SrsCard> {
        return db.srsCardQueries.getLearningCards(limit.toLong()).executeAsList().map { it.toSrsCard() }
    }

    override suspend fun getDueCount(currentTime: Long): Long {
        return db.srsCardQueries.getDueCount(currentTime).executeAsOne()
    }

    override suspend fun getNewCount(): Long {
        return db.srsCardQueries.getNewCount().executeAsOne()
    }

    override suspend fun getMasteredCount(): Long {
        return db.srsCardQueries.getMasteredCount().executeAsOne()
    }

    override suspend fun saveCard(card: SrsCard) {
        db.srsCardQueries.upsert(
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

    override suspend fun ensureCardExists(kanjiId: Int) {
        db.srsCardQueries.insertNew(kanjiId.toLong())
    }

    override suspend fun getCardsByIds(kanjiIds: List<Long>): List<SrsCard> {
        return db.srsCardQueries.getCardsByIds(kanjiIds).executeAsList().map { it.toSrsCard() }
    }

    override suspend fun getNonNewCardCount(): Long {
        return db.srsCardQueries.getAllNonNewCards().executeAsList().size.toLong()
    }

    override suspend fun incrementModeStats(kanjiId: Int, gameMode: String, correct: Boolean) {
        val correctInt = if (correct) 1L else 0L
        db.kanjiModeStatsQueries.incrementReview(
            kanjiId.toLong(), gameMode, correctInt, correctInt
        )
    }

    override suspend fun getModeStatsByIds(kanjiIds: List<Long>): Map<Int, Map<String, Int>> {
        if (kanjiIds.isEmpty()) return emptyMap()
        val rows = db.kanjiModeStatsQueries.getByKanjiIds(kanjiIds).executeAsList()
        val result = mutableMapOf<Int, MutableMap<String, Int>>()
        for (row in rows) {
            val kanjiId = row.kanji_id.toInt()
            val modeMap = result.getOrPut(kanjiId) { mutableMapOf() }
            modeMap[row.game_mode] = row.review_count.toInt()
        }
        return result
    }

    override suspend fun getGradeMastery(grade: Int, totalKanjiInGrade: Long): GradeMastery {
        val stats = db.srsCardQueries.getGradeStats(grade.toLong()).executeAsOneOrNull()
        val studiedCount = stats?.studied_count ?: 0L
        val masteredCount = stats?.mastered_count ?: 0L
        val avgAccuracy = stats?.avg_accuracy?.toFloat() ?: 0f
        val coverage = if (totalKanjiInGrade > 0) studiedCount.toFloat() / totalKanjiInGrade else 0f
        return GradeMastery(grade, totalKanjiInGrade, studiedCount, masteredCount, avgAccuracy, coverage)
    }
}

internal fun com.jworks.kanjiquest.db.Srs_card.toSrsCard(): SrsCard = SrsCard(
    kanjiId = kanji_id.toInt(),
    easeFactor = ease_factor,
    interval = interval.toInt(),
    repetitions = repetitions.toInt(),
    nextReview = next_review,
    state = SrsState.fromString(state),
    totalReviews = total_reviews.toInt(),
    correctCount = correct_count.toInt()
)
