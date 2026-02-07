package com.jworks.kanjiquest.core.data

import com.jworks.kanjiquest.core.domain.model.SrsState
import com.jworks.kanjiquest.core.domain.model.VocabSrsCard
import com.jworks.kanjiquest.core.domain.repository.VocabSrsRepository
import com.jworks.kanjiquest.db.KanjiQuestDatabase

class VocabSrsRepositoryImpl(
    private val db: KanjiQuestDatabase
) : VocabSrsRepository {

    override suspend fun getCard(vocabId: Long): VocabSrsCard? {
        return db.vocabSrsCardQueries.getByVocabId(vocabId).executeAsOneOrNull()?.toVocabSrsCard()
    }

    override suspend fun getDueCards(currentTime: Long): List<VocabSrsCard> {
        return db.vocabSrsCardQueries.getDueCards(currentTime).executeAsList().map { it.toVocabSrsCard() }
    }

    override suspend fun getNewCards(limit: Int): List<VocabSrsCard> {
        return db.vocabSrsCardQueries.getNewCards(limit.toLong()).executeAsList().map { it.toVocabSrsCard() }
    }

    override suspend fun saveCard(card: VocabSrsCard) {
        db.vocabSrsCardQueries.upsert(
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

    override suspend fun ensureCardExists(vocabId: Long) {
        db.vocabSrsCardQueries.insertNew(vocabId)
    }
}

internal fun com.jworks.kanjiquest.db.Vocab_srs_card.toVocabSrsCard(): VocabSrsCard = VocabSrsCard(
    vocabId = vocab_id,
    easeFactor = ease_factor,
    interval = interval.toInt(),
    repetitions = repetitions.toInt(),
    nextReview = next_review,
    state = SrsState.fromString(state),
    totalReviews = total_reviews.toInt(),
    correctCount = correct_count.toInt()
)
