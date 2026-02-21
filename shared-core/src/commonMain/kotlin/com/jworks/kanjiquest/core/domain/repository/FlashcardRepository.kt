package com.jworks.kanjiquest.core.domain.repository

import com.jworks.kanjiquest.core.domain.model.FlashcardDeckGroup
import com.jworks.kanjiquest.core.domain.model.FlashcardEntry

interface FlashcardRepository {
    // Deck group operations
    suspend fun getAllDeckGroups(): List<FlashcardDeckGroup>
    suspend fun createDeckGroup(name: String): Long
    suspend fun renameDeckGroup(id: Long, name: String)
    suspend fun deleteDeckGroup(id: Long)
    suspend fun getDeckGroupCount(): Long
    suspend fun ensureDefaultDeck()

    // Flashcard operations (deck-aware)
    suspend fun isInDeck(deckId: Long, kanjiId: Int): Boolean
    suspend fun isInAnyDeck(kanjiId: Int): Boolean
    suspend fun getDecksForKanji(kanjiId: Int): List<Long>
    suspend fun addToDeck(deckId: Long, kanjiId: Int)
    suspend fun removeFromDeck(deckId: Long, kanjiId: Int)
    suspend fun getFlashcardsByDeck(deckId: Long): List<FlashcardEntry>
    suspend fun getDeckCount(): Long
    suspend fun getDeckCountByDeck(deckId: Long): Long
    suspend fun markStudied(deckId: Long, kanjiId: Int)
    suspend fun getKanjiIdsByDeck(deckId: Long): List<Int>

    // Legacy compatibility (operates on all decks)
    suspend fun isInDeck(kanjiId: Int): Boolean
    suspend fun addToDeck(kanjiId: Int)
    suspend fun removeFromDeck(kanjiId: Int)
    suspend fun toggleInDeck(kanjiId: Int): Boolean
    suspend fun getAllFlashcards(): List<FlashcardEntry>
    suspend fun markStudied(kanjiId: Int)
    suspend fun getAllKanjiIds(): List<Int>
}
