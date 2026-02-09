package com.jworks.kanjiquest.core

import com.jworks.kanjiquest.core.domain.model.GameMode
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.model.SrsState
import com.jworks.kanjiquest.core.domain.model.StudySession
import com.jworks.kanjiquest.core.domain.model.parseJsonStringArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DomainModelTest {

    @Test
    fun kanji_unicodeHex_formatsCorrectly() {
        val kanji = Kanji(
            id = 0x4E00, literal = "一", grade = 1, jlptLevel = 5,
            frequency = 2, strokeCount = 1,
            meaningsEn = listOf("one"), onReadings = listOf("イチ"),
            kunReadings = listOf("ひと.つ"), strokeSvg = null
        )
        assertEquals("U+4E00", kanji.unicodeHex)
    }

    @Test
    fun kanji_gradeLabel_formatsCorrectly() {
        val g1 = Kanji(id = 1, literal = "一", grade = 1, jlptLevel = null,
            frequency = null, strokeCount = 1, meaningsEn = listOf("one"),
            onReadings = emptyList(), kunReadings = emptyList(), strokeSvg = null)
        assertEquals("Grade 1", g1.gradeLabel)

        val g8 = g1.copy(grade = 8)
        assertEquals("Junior High", g8.gradeLabel)

        val noGrade = g1.copy(grade = null)
        assertNull(noGrade.gradeLabel)
    }

    @Test
    fun kanji_jlptLabel_formatsCorrectly() {
        val n5 = Kanji(id = 1, literal = "一", grade = 1, jlptLevel = 5,
            frequency = null, strokeCount = 1, meaningsEn = listOf("one"),
            onReadings = emptyList(), kunReadings = emptyList(), strokeSvg = null)
        assertEquals("N5", n5.jlptLabel)
    }

    @Test
    fun srsState_fromString_parsesAll() {
        assertEquals(SrsState.NEW, SrsState.fromString("new"))
        assertEquals(SrsState.LEARNING, SrsState.fromString("learning"))
        assertEquals(SrsState.REVIEW, SrsState.fromString("review"))
        assertEquals(SrsState.GRADUATED, SrsState.fromString("graduated"))
        assertEquals(SrsState.NEW, SrsState.fromString("unknown"))
    }

    @Test
    fun gameMode_fromString_parsesAll() {
        assertEquals(GameMode.RECOGNITION, GameMode.fromString("recognition"))
        assertEquals(GameMode.WRITING, GameMode.fromString("writing"))
        assertEquals(GameMode.VOCABULARY, GameMode.fromString("vocabulary"))
        assertEquals(GameMode.CAMERA_CHALLENGE, GameMode.fromString("camera_challenge"))
        assertEquals(GameMode.RECOGNITION, GameMode.fromString("unknown"))
    }

    @Test
    fun studySession_accuracy_calculatesCorrectly() {
        val session = StudySession(gameMode = "recognition", startedAt = 0, cardsStudied = 10, correctCount = 7)
        assertEquals(0.7f, session.accuracy)
    }

    @Test
    fun studySession_accuracy_zeroCards_returnsZero() {
        val session = StudySession(gameMode = "recognition", startedAt = 0, cardsStudied = 0, correctCount = 0)
        assertEquals(0f, session.accuracy)
    }

    @Test
    fun parseJsonStringArray_validJson() {
        val result = parseJsonStringArray("""["one","two","three"]""")
        assertEquals(listOf("one", "two", "three"), result)
    }

    @Test
    fun parseJsonStringArray_emptyArray() {
        val result = parseJsonStringArray("[]")
        assertEquals(emptyList(), result)
    }

    @Test
    fun parseJsonStringArray_invalidJson_returnsEmpty() {
        val result = parseJsonStringArray("not json")
        assertEquals(emptyList(), result)
    }
}
