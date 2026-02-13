package com.jworks.kanjiquest.core.domain.repository

import com.jworks.kanjiquest.core.domain.model.GradeMastery
import com.jworks.kanjiquest.core.domain.model.SrsCard

interface SrsRepository {
    suspend fun getCard(kanjiId: Int): SrsCard?
    suspend fun getDueCards(currentTime: Long): List<SrsCard>
    suspend fun getNewCards(limit: Int): List<SrsCard>
    suspend fun getDueCount(currentTime: Long): Long
    suspend fun getNewCount(): Long
    suspend fun getLearningCards(limit: Int): List<SrsCard>
    suspend fun getMasteredCount(): Long
    suspend fun saveCard(card: SrsCard)
    suspend fun ensureCardExists(kanjiId: Int)
    suspend fun getGradeMastery(grade: Int, totalKanjiInGrade: Long): GradeMastery
    suspend fun getCardsByIds(kanjiIds: List<Long>): List<SrsCard>
    suspend fun getNonNewCardCount(): Long
    suspend fun incrementModeStats(kanjiId: Int, gameMode: String, correct: Boolean)
    suspend fun getModeStatsByIds(kanjiIds: List<Long>): Map<Int, Map<String, Int>>
}
