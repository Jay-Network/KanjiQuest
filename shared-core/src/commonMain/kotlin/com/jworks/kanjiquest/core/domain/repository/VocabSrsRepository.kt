package com.jworks.kanjiquest.core.domain.repository

import com.jworks.kanjiquest.core.domain.model.VocabSrsCard

interface VocabSrsRepository {
    suspend fun getCard(vocabId: Long): VocabSrsCard?
    suspend fun getDueCards(currentTime: Long): List<VocabSrsCard>
    suspend fun getNewCards(limit: Int): List<VocabSrsCard>
    suspend fun saveCard(card: VocabSrsCard)
    suspend fun ensureCardExists(vocabId: Long)
    suspend fun getNotDueVocabIds(currentTime: Long): Set<Long>
}
