package com.jworks.kanjiquest.core.data

import com.jworks.kanjiquest.core.domain.model.FlashcardDeckGroup
import com.jworks.kanjiquest.core.domain.model.FlashcardEntry
import com.jworks.kanjiquest.core.domain.repository.FlashcardRepository
import com.jworks.kanjiquest.db.KanjiQuestDatabase

class FlashcardRepositoryImpl(
    private val db: KanjiQuestDatabase
) : FlashcardRepository {

    // Deck group operations
    override suspend fun getAllDeckGroups(): List<FlashcardDeckGroup> {
        return db.flashcardDeckGroupQueries.getAll().executeAsList().map { it.toDeckGroup() }
    }

    override suspend fun createDeckGroup(name: String): Long {
        db.flashcardDeckGroupQueries.insert(name)
        return db.flashcardDeckGroupQueries.getLastInsertId().executeAsOne()
    }

    override suspend fun renameDeckGroup(id: Long, name: String) {
        db.flashcardDeckGroupQueries.rename(name, id)
    }

    override suspend fun deleteDeckGroup(id: Long) {
        db.flashcardDeckQueries.removeAllFromDeck(id)
        db.flashcardDeckGroupQueries.delete(id)
    }

    override suspend fun getDeckGroupCount(): Long {
        return db.flashcardDeckGroupQueries.getCount().executeAsOne()
    }

    override suspend fun ensureDefaultDeck() {
        val count = getDeckGroupCount()
        if (count == 0L) {
            db.flashcardDeckGroupQueries.insertWithId(1, "My Flashcards")
        }
    }

    // Deck-aware flashcard operations
    override suspend fun isInDeck(deckId: Long, kanjiId: Int): Boolean {
        return db.flashcardDeckQueries.isInDeck(deckId, kanjiId.toLong()).executeAsOne() > 0
    }

    override suspend fun isInAnyDeck(kanjiId: Int): Boolean {
        return db.flashcardDeckQueries.isInAnyDeck(kanjiId.toLong()).executeAsOne() > 0
    }

    override suspend fun getDecksForKanji(kanjiId: Int): List<Long> {
        return db.flashcardDeckQueries.getDecksForKanji(kanjiId.toLong()).executeAsList()
    }

    override suspend fun addToDeck(deckId: Long, kanjiId: Int) {
        db.flashcardDeckQueries.insert(deckId, kanjiId.toLong())
    }

    override suspend fun removeFromDeck(deckId: Long, kanjiId: Int) {
        db.flashcardDeckQueries.remove(deckId, kanjiId.toLong())
    }

    override suspend fun getFlashcardsByDeck(deckId: Long): List<FlashcardEntry> {
        return db.flashcardDeckQueries.getByDeckId(deckId).executeAsList().map { it.toFlashcardEntry() }
    }

    override suspend fun getDeckCount(): Long {
        return db.flashcardDeckQueries.getCount().executeAsOne()
    }

    override suspend fun getDeckCountByDeck(deckId: Long): Long {
        return db.flashcardDeckQueries.getCountByDeck(deckId).executeAsOne()
    }

    override suspend fun markStudied(deckId: Long, kanjiId: Int) {
        db.flashcardDeckQueries.updateStudied(deckId, kanjiId.toLong())
    }

    override suspend fun getKanjiIdsByDeck(deckId: Long): List<Int> {
        return db.flashcardDeckQueries.getKanjiIdsByDeck(deckId).executeAsList().map { it.toInt() }
    }

    // Legacy compatibility (operates on default deck id=1)
    override suspend fun isInDeck(kanjiId: Int): Boolean {
        return isInAnyDeck(kanjiId)
    }

    override suspend fun addToDeck(kanjiId: Int) {
        ensureDefaultDeck()
        addToDeck(1L, kanjiId)
    }

    override suspend fun removeFromDeck(kanjiId: Int) {
        db.flashcardDeckQueries.removeFromAllDecks(kanjiId.toLong())
    }

    override suspend fun toggleInDeck(kanjiId: Int): Boolean {
        val inDeck = isInAnyDeck(kanjiId)
        if (inDeck) {
            removeFromDeck(kanjiId)
        } else {
            addToDeck(kanjiId)
        }
        return !inDeck
    }

    override suspend fun getAllFlashcards(): List<FlashcardEntry> {
        return db.flashcardDeckQueries.getAll().executeAsList().map { it.toFlashcardEntry() }
    }

    override suspend fun markStudied(kanjiId: Int) {
        markStudied(1L, kanjiId)
    }

    override suspend fun getAllKanjiIds(): List<Int> {
        return db.flashcardDeckQueries.getAllKanjiIds().executeAsList().map { it.toInt() }
    }
}

internal fun com.jworks.kanjiquest.db.Flashcard_deck.toFlashcardEntry(): FlashcardEntry = FlashcardEntry(
    deckId = deck_id,
    kanjiId = kanji_id.toInt(),
    addedAt = added_at,
    lastStudiedAt = last_studied_at,
    studyCount = study_count.toInt(),
    notes = notes
)

internal fun com.jworks.kanjiquest.db.Flashcard_deck_group.toDeckGroup(): FlashcardDeckGroup = FlashcardDeckGroup(
    id = id,
    name = name,
    createdAt = created_at
)
