package com.jworks.kanjiquest.core.domain.repository

import com.jworks.kanjiquest.core.domain.model.ExampleSentence
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.model.Vocabulary

interface KanjiRepository {
    suspend fun getKanjiById(id: Int): Kanji?
    suspend fun getKanjiByLiteral(literal: String): Kanji?
    suspend fun getKanjiByGrade(grade: Int): List<Kanji>
    suspend fun getKanjiByJlptLevel(level: Int): List<Kanji>
    suspend fun getVocabularyForKanji(kanjiId: Int): List<Vocabulary>
    suspend fun getVocabularyByIds(ids: List<Long>): List<Vocabulary>
    suspend fun getStudiedKanjiVocabulary(): List<Vocabulary>
    suspend fun getRandomStudiedVocabulary(): Vocabulary?
    suspend fun getVocabularyById(id: Long): Vocabulary?
    suspend fun getExampleSentence(vocabId: Long): ExampleSentence?
    suspend fun getVocabularyAtOffset(offset: Long): Vocabulary?
    suspend fun getVocabularyCount(): Long
    suspend fun getCommonVocabularyAtOffset(offset: Long): Vocabulary?
    suspend fun getCommonVocabularyCount(): Long
    suspend fun getKanjiIdsForVocab(vocabId: Long): List<Long>
    suspend fun getUnseenKanjiByGrade(grade: Int, limit: Int): List<Kanji>
    suspend fun searchKanji(query: String, limit: Int = 20): List<Kanji>
    suspend fun getKanjiCount(): Long
    suspend fun getKanjiCountByGrade(grade: Int): Long
    suspend fun getKanjiCountByJlptLevel(level: Int): Long
    suspend fun getKanjiByStrokeCount(strokeCount: Int): List<Kanji>
    suspend fun getDistinctStrokeCounts(): List<Int>
    suspend fun getKanjiByFrequencyRange(from: Int, to: Int): List<Kanji>
}
