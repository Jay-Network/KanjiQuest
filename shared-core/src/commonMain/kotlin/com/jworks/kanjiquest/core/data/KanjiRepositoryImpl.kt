package com.jworks.kanjiquest.core.data

import com.jworks.kanjiquest.core.domain.model.ExampleSentence
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.model.Vocabulary
import com.jworks.kanjiquest.core.domain.model.parseJsonStringArray
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import com.jworks.kanjiquest.db.KanjiQuestDatabase

class KanjiRepositoryImpl(
    private val db: KanjiQuestDatabase
) : KanjiRepository {

    override suspend fun getKanjiById(id: Int): Kanji? {
        return db.kanjiQueries.getById(id.toLong()).executeAsOneOrNull()?.toKanji()
    }

    override suspend fun getKanjiByLiteral(literal: String): Kanji? {
        return db.kanjiQueries.getByLiteral(literal).executeAsOneOrNull()?.toKanji()
    }

    override suspend fun getKanjiByGrade(grade: Int): List<Kanji> {
        return db.kanjiQueries.getByGrade(grade.toLong()).executeAsList().map { it.toKanji() }
    }

    override suspend fun getKanjiByJlptLevel(level: Int): List<Kanji> {
        return db.kanjiQueries.getByJlptLevel(level.toLong()).executeAsList().map { it.toKanji() }
    }

    override suspend fun getVocabularyForKanji(kanjiId: Int): List<Vocabulary> {
        return db.vocabularyQueries.getForKanji(kanjiId.toLong()).executeAsList().map { it.toVocabulary() }
    }

    override suspend fun getVocabularyByIds(ids: List<Long>): List<Vocabulary> {
        if (ids.isEmpty()) return emptyList()
        return db.vocabularyQueries.getByIds(ids).executeAsList().map { it.toVocabulary() }
    }

    override suspend fun getStudiedKanjiVocabulary(): List<Vocabulary> {
        return db.vocabularyQueries.getForStudiedKanji().executeAsList().map { it.toVocabulary() }
    }

    override suspend fun getExampleSentence(vocabId: Long): ExampleSentence? {
        return db.exampleSentenceQueries.getRandomForVocab(vocabId).executeAsOneOrNull()?.toExampleSentence()
    }

    override suspend fun getVocabularyAtOffset(offset: Long): Vocabulary? {
        return db.vocabularyQueries.getAtOffset(offset).executeAsOneOrNull()?.toVocabulary()
    }

    override suspend fun getVocabularyCount(): Long {
        return db.vocabularyQueries.countAll().executeAsOne()
    }

    override suspend fun getKanjiIdsForVocab(vocabId: Long): List<Long> {
        return db.kanjiVocabularyQueries.getKanjiIdsForVocab(vocabId).executeAsList()
    }

    override suspend fun getUnseenKanjiByGrade(grade: Int, limit: Int): List<Kanji> {
        return db.kanjiQueries.getUnseenByGrade(grade.toLong(), limit.toLong()).executeAsList().map { it.toKanji() }
    }

    override suspend fun searchKanji(query: String, limit: Int): List<Kanji> {
        val pattern = "%$query%"
        return db.kanjiQueries.search(pattern, pattern, limit.toLong()).executeAsList().map { it.toKanji() }
    }

    override suspend fun getKanjiCount(): Long {
        return db.kanjiQueries.countAll().executeAsOne()
    }

    override suspend fun getKanjiCountByGrade(grade: Int): Long {
        return db.kanjiQueries.countByGrade(grade.toLong()).executeAsOne()
    }
}

internal fun com.jworks.kanjiquest.db.Kanji.toKanji(): Kanji = Kanji(
    id = id.toInt(),
    literal = literal,
    grade = grade?.toInt(),
    jlptLevel = jlpt_level?.toInt(),
    frequency = frequency?.toInt(),
    strokeCount = stroke_count.toInt(),
    meaningsEn = parseJsonStringArray(meanings_en),
    onReadings = parseJsonStringArray(on_readings),
    kunReadings = parseJsonStringArray(kun_readings),
    strokeSvg = stroke_svg
)

internal fun com.jworks.kanjiquest.db.Vocabulary.toVocabulary(): Vocabulary = Vocabulary(
    id = id,
    kanjiForm = kanji_form,
    reading = reading,
    meaningsEn = parseJsonStringArray(meanings_en),
    jlptLevel = jlpt_level?.toInt(),
    frequency = frequency?.toInt()
)

internal fun com.jworks.kanjiquest.db.Example_sentence.toExampleSentence(): ExampleSentence = ExampleSentence(
    id = id,
    vocabId = vocab_id,
    japanese = japanese,
    english = english
)
