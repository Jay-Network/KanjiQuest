package com.jworks.kanjiquest.core.data

import com.jworks.kanjiquest.core.domain.model.SrsCard
import com.jworks.kanjiquest.core.domain.model.SrsState
import com.jworks.kanjiquest.core.domain.repository.RadicalSrsRepository
import com.jworks.kanjiquest.db.KanjiQuestDatabase

class RadicalSrsRepositoryImpl(
    private val db: KanjiQuestDatabase
) : RadicalSrsRepository {

    override suspend fun getCard(radicalId: Int): SrsCard? {
        return db.radicalSrsCardQueries.getByRadicalId(radicalId.toLong()).executeAsOneOrNull()?.toSrsCard()
    }

    override suspend fun getDueCards(currentTime: Long): List<SrsCard> {
        return db.radicalSrsCardQueries.getDueCards(currentTime).executeAsList().map { it.toSrsCard() }
    }

    override suspend fun getNewCards(limit: Int): List<SrsCard> {
        return db.radicalSrsCardQueries.getNewCards(limit.toLong()).executeAsList().map { it.toSrsCard() }
    }

    override suspend fun getDueCount(currentTime: Long): Long {
        return db.radicalSrsCardQueries.getDueCount(currentTime).executeAsOne()
    }

    override suspend fun getNewCount(): Long {
        return db.radicalSrsCardQueries.getNewCount().executeAsOne()
    }

    override suspend fun getLearningCards(limit: Int): List<SrsCard> {
        return db.radicalSrsCardQueries.getLearningCards(limit.toLong()).executeAsList().map { it.toSrsCard() }
    }

    override suspend fun getMasteredCount(): Long {
        return db.radicalSrsCardQueries.getMasteredCount().executeAsOne()
    }

    override suspend fun saveCard(card: SrsCard) {
        db.radicalSrsCardQueries.upsert(
            radical_id = card.kanjiId.toLong(),
            ease_factor = card.easeFactor,
            interval = card.interval.toLong(),
            repetitions = card.repetitions.toLong(),
            next_review = card.nextReview,
            state = card.state.value,
            total_reviews = card.totalReviews.toLong(),
            correct_count = card.correctCount.toLong()
        )
    }

    override suspend fun ensureCardExists(radicalId: Int) {
        db.radicalSrsCardQueries.insertNew(radicalId.toLong())
    }

    override suspend fun getNonNewCardCount(): Long {
        return db.radicalSrsCardQueries.getNonNewCount().executeAsOne()
    }

    override suspend fun getStudiedCount(): Long {
        val stats = db.radicalSrsCardQueries.getOverallStats().executeAsOneOrNull()
        return stats?.studied_count ?: 0L
    }
}

internal fun com.jworks.kanjiquest.db.Radical_srs_card.toSrsCard(): SrsCard = SrsCard(
    kanjiId = radical_id.toInt(),
    easeFactor = ease_factor,
    interval = interval.toInt(),
    repetitions = repetitions.toInt(),
    nextReview = next_review,
    state = SrsState.fromString(state),
    totalReviews = total_reviews.toInt(),
    correctCount = correct_count.toInt()
)
