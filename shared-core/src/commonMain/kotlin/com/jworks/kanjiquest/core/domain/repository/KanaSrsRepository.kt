package com.jworks.kanjiquest.core.domain.repository

import com.jworks.kanjiquest.core.domain.model.SrsCard

interface KanaSrsRepository {
    suspend fun getCard(kanaId: Int): SrsCard?
    suspend fun getDueCards(currentTime: Long): List<SrsCard>
    suspend fun getNewCards(limit: Int): List<SrsCard>
    suspend fun getDueCount(currentTime: Long): Long
    suspend fun getNewCount(): Long
    suspend fun getLearningCards(limit: Int): List<SrsCard>
    suspend fun getMasteredCount(): Long
    suspend fun saveCard(card: SrsCard)
    suspend fun ensureCardExists(kanaId: Int)
    suspend fun getNonNewCardCount(): Long
    suspend fun getTypeStudiedCount(kanaType: String): Long
    suspend fun getTypeMasteredCount(kanaType: String): Long
}
