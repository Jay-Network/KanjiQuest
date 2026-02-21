package com.jworks.kanjiquest.core.data

import com.jworks.kanjiquest.core.domain.model.FieldJournalEntry
import com.jworks.kanjiquest.core.domain.repository.FieldJournalRepository
import com.jworks.kanjiquest.db.KanjiQuestDatabase
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class FieldJournalRepositoryImpl(
    private val db: KanjiQuestDatabase
) : FieldJournalRepository {

    override suspend fun getAll(): List<FieldJournalEntry> {
        return db.fieldJournalQueries.getAll().executeAsList().map { it.toEntry() }
    }

    override suspend fun getById(id: Long): FieldJournalEntry? {
        return db.fieldJournalQueries.getById(id).executeAsOneOrNull()?.toEntry()
    }

    override suspend fun getRecent(limit: Int): List<FieldJournalEntry> {
        return db.fieldJournalQueries.getRecent(limit.toLong()).executeAsList().map { it.toEntry() }
    }

    override suspend fun countAll(): Long {
        return db.fieldJournalQueries.countAll().executeAsOne()
    }

    override suspend fun totalKanjiCaught(): Long {
        return db.fieldJournalQueries.totalKanjiCaught().executeAsOne()
    }

    override suspend fun insert(
        imagePath: String,
        locationLabel: String,
        kanjiFound: List<String>,
        capturedAt: Long
    ): Long {
        val kanjiJson = "[${kanjiFound.joinToString(",") { "\"$it\"" }}]"
        db.fieldJournalQueries.insert(
            image_path = imagePath,
            location_label = locationLabel,
            kanji_found = kanjiJson,
            kanji_count = kanjiFound.size.toLong(),
            captured_at = capturedAt
        )
        // Return the last inserted ID
        return db.fieldJournalQueries.getRecent(1).executeAsOne().id
    }

    override suspend fun delete(id: Long) {
        db.fieldJournalQueries.delete(id)
    }
}

private fun com.jworks.kanjiquest.db.Field_journal.toEntry(): FieldJournalEntry {
    val kanjiList = try {
        Json.parseToJsonElement(kanji_found).jsonArray.map { it.jsonPrimitive.content }
    } catch (_: Exception) {
        emptyList()
    }
    return FieldJournalEntry(
        id = id,
        imagePath = image_path,
        locationLabel = location_label,
        kanjiFound = kanjiList,
        kanjiCount = kanji_count.toInt(),
        capturedAt = captured_at
    )
}
