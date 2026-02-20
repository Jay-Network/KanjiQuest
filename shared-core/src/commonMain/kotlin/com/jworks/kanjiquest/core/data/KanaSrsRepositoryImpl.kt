package com.jworks.kanjiquest.core.data

import com.jworks.kanjiquest.core.domain.model.SrsCard
import com.jworks.kanjiquest.core.domain.model.SrsState
import com.jworks.kanjiquest.core.domain.repository.KanaSrsRepository
import com.jworks.kanjiquest.db.KanjiQuestDatabase

class KanaSrsRepositoryImpl(
    private val db: KanjiQuestDatabase
) : KanaSrsRepository {

    override suspend fun getCard(kanaId: Int): SrsCard? {
        return db.kanaSrsCardQueries.getByKanaId(kanaId.toLong()).executeAsOneOrNull()?.toSrsCard()
    }

    override suspend fun getDueCards(currentTime: Long): List<SrsCard> {
        return db.kanaSrsCardQueries.getDueCards(currentTime).executeAsList().map { it.toSrsCard() }
    }

    override suspend fun getNewCards(limit: Int): List<SrsCard> {
        return db.kanaSrsCardQueries.getNewCards(limit.toLong()).executeAsList().map { it.toSrsCard() }
    }

    override suspend fun getDueCount(currentTime: Long): Long {
        return db.kanaSrsCardQueries.getDueCount(currentTime).executeAsOne()
    }

    override suspend fun getNewCount(): Long {
        return db.kanaSrsCardQueries.getNewCount().executeAsOne()
    }

    override suspend fun getLearningCards(limit: Int): List<SrsCard> {
        return db.kanaSrsCardQueries.getLearningCards(limit.toLong()).executeAsList().map { it.toSrsCard() }
    }

    override suspend fun getMasteredCount(): Long {
        return db.kanaSrsCardQueries.getMasteredCount().executeAsOne()
    }

    override suspend fun saveCard(card: SrsCard) {
        db.kanaSrsCardQueries.upsert(
            kana_id = card.kanjiId.toLong(),
            ease_factor = card.easeFactor,
            interval = card.interval.toLong(),
            repetitions = card.repetitions.toLong(),
            next_review = card.nextReview,
            state = card.state.value,
            total_reviews = card.totalReviews.toLong(),
            correct_count = card.correctCount.toLong()
        )
    }

    override suspend fun ensureCardExists(kanaId: Int) {
        db.kanaSrsCardQueries.insertNew(kanaId.toLong())
    }

    override suspend fun getNonNewCardCount(): Long {
        return db.kanaSrsCardQueries.getNonNewCount().executeAsOne()
    }

    override suspend fun getTypeStudiedCount(kanaType: String): Long {
        val stats = db.kanaSrsCardQueries.getTypeStats(kanaType).executeAsOneOrNull()
        return stats?.studied_count ?: 0L
    }

    override suspend fun getTypeMasteredCount(kanaType: String): Long {
        val stats = db.kanaSrsCardQueries.getTypeStats(kanaType).executeAsOneOrNull()
        return stats?.mastered_count ?: 0L
    }
}

internal fun com.jworks.kanjiquest.db.Kana_srs_card.toSrsCard(): SrsCard = SrsCard(
    kanjiId = kana_id.toInt(),
    easeFactor = ease_factor,
    interval = interval.toInt(),
    repetitions = repetitions.toInt(),
    nextReview = next_review,
    state = SrsState.fromString(state),
    totalReviews = total_reviews.toInt(),
    correctCount = correct_count.toInt()
)
