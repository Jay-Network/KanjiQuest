package com.jworks.kanjiquest.core.domain.repository

import com.jworks.kanjiquest.core.domain.model.FlashcardEntry

interface FlashcardRepository {
    suspend fun isInDeck(kanjiId: Int): Boolean
    suspend fun addToDeck(kanjiId: Int)
    suspend fun removeFromDeck(kanjiId: Int)
    suspend fun toggleInDeck(kanjiId: Int): Boolean
    suspend fun getAllFlashcards(): List<FlashcardEntry>
    suspend fun getDeckCount(): Long
    suspend fun markStudied(kanjiId: Int)
    suspend fun getAllKanjiIds(): List<Int>
}
