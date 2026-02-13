package com.jworks.kanjiquest.core.data

import com.jworks.kanjiquest.core.domain.model.FlashcardEntry
import com.jworks.kanjiquest.core.domain.repository.FlashcardRepository
import com.jworks.kanjiquest.db.KanjiQuestDatabase

class FlashcardRepositoryImpl(
    private val db: KanjiQuestDatabase
) : FlashcardRepository {

    override suspend fun isInDeck(kanjiId: Int): Boolean {
        return db.flashcardDeckQueries.isInDeck(kanjiId.toLong()).executeAsOne() > 0
    }

    override suspend fun addToDeck(kanjiId: Int) {
        db.flashcardDeckQueries.insert(kanjiId.toLong())
    }

    override suspend fun removeFromDeck(kanjiId: Int) {
        db.flashcardDeckQueries.remove(kanjiId.toLong())
    }

    override suspend fun toggleInDeck(kanjiId: Int): Boolean {
        val inDeck = isInDeck(kanjiId)
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

    override suspend fun getDeckCount(): Long {
        return db.flashcardDeckQueries.getCount().executeAsOne()
    }

    override suspend fun markStudied(kanjiId: Int) {
        db.flashcardDeckQueries.updateStudied(kanjiId.toLong())
    }

    override suspend fun getAllKanjiIds(): List<Int> {
        return db.flashcardDeckQueries.getAllKanjiIds().executeAsList().map { it.toInt() }
    }
}

internal fun com.jworks.kanjiquest.db.Flashcard_deck.toFlashcardEntry(): FlashcardEntry = FlashcardEntry(
    kanjiId = kanji_id.toInt(),
    addedAt = added_at,
    lastStudiedAt = last_studied_at,
    studyCount = study_count.toInt(),
    notes = notes
)
