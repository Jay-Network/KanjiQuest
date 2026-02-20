package com.jworks.kanjiquest.core.domain.repository

import com.jworks.kanjiquest.core.domain.model.SrsCard

interface RadicalSrsRepository {
    suspend fun getCard(radicalId: Int): SrsCard?
    suspend fun getDueCards(currentTime: Long): List<SrsCard>
    suspend fun getNewCards(limit: Int): List<SrsCard>
    suspend fun getDueCount(currentTime: Long): Long
    suspend fun getNewCount(): Long
    suspend fun getLearningCards(limit: Int): List<SrsCard>
    suspend fun getMasteredCount(): Long
    suspend fun saveCard(card: SrsCard)
    suspend fun ensureCardExists(radicalId: Int)
    suspend fun getNonNewCardCount(): Long
    suspend fun getStudiedCount(): Long
}
