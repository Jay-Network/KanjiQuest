package com.jworks.kanjiquest.core

import com.jworks.kanjiquest.core.writing.Point
import com.jworks.kanjiquest.core.writing.StrokeMatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StrokeMatcherTest {

    @Test
    fun matchStroke_identicalStrokes_highSimilarity() {
        val stroke = listOf(
            Point(0f, 0f), Point(10f, 0f), Point(20f, 0f),
            Point(30f, 0f), Point(40f, 0f), Point(50f, 0f)
        )
        val result = StrokeMatcher.matchStroke(stroke, stroke)
        assertTrue(result.similarity > 0.95f)
        assertTrue(result.isCorrect)
    }

    @Test
    fun matchStroke_similarStrokes_correct() {
        // Reference: horizontal line
        val reference = listOf(
            Point(0f, 50f), Point(20f, 50f), Point(40f, 50f),
            Point(60f, 50f), Point(80f, 50f), Point(100f, 50f)
        )
        // Drawn: slightly wavy horizontal line
        val drawn = listOf(
            Point(2f, 52f), Point(18f, 48f), Point(38f, 51f),
            Point(58f, 49f), Point(78f, 52f), Point(98f, 50f)
        )
        val result = StrokeMatcher.matchStroke(drawn, reference)
        assertTrue(result.similarity > 0.7f)
        assertTrue(result.isCorrect)
    }

    @Test
    fun matchStroke_completelyDifferent_incorrect() {
        // Reference: left-to-right horizontal line at top
        val reference = listOf(
            Point(0f, 10f), Point(20f, 10f), Point(40f, 10f),
            Point(60f, 10f), Point(80f, 10f), Point(100f, 10f)
        )
        // Drawn: right-to-left diagonal going to opposite corner
        val drawn = listOf(
            Point(100f, 100f), Point(80f, 80f), Point(60f, 60f),
            Point(40f, 40f), Point(20f, 20f), Point(0f, 0f)
        )
        val result = StrokeMatcher.matchStroke(drawn, reference)
        assertTrue(result.similarity < 0.55f, "Expected similarity < 0.55 but got ${result.similarity}")
        assertFalse(result.isCorrect)
    }

    @Test
    fun matchStroke_emptyInput_returnsZero() {
        val reference = listOf(Point(0f, 0f), Point(10f, 10f))
        val result = StrokeMatcher.matchStroke(emptyList(), reference)
        assertEquals(0f, result.similarity)
        assertFalse(result.isCorrect)
    }

    @Test
    fun matchStroke_singlePoint_returnsZero() {
        val reference = listOf(Point(0f, 0f), Point(10f, 10f))
        val result = StrokeMatcher.matchStroke(listOf(Point(0f, 0f)), reference)
        assertEquals(0f, result.similarity)
    }

    @Test
    fun normalizeAndResample_uniformOutput() {
        val points = listOf(
            Point(0f, 0f), Point(50f, 50f), Point(100f, 100f)
        )
        val resampled = StrokeMatcher.normalizeAndResample(points, 10)
        assertEquals(10, resampled.size)
        // First point should be at normalized origin
        assertEquals(0f, resampled.first().x, 0.01f)
        assertEquals(0f, resampled.first().y, 0.01f)
    }

    @Test
    fun validateWriting_allStrokesCorrect_highQuality() {
        val stroke = listOf(
            Point(0f, 50f), Point(25f, 50f), Point(50f, 50f),
            Point(75f, 50f), Point(100f, 50f)
        )
        val result = StrokeMatcher.validateWriting(
            drawnStrokes = listOf(stroke),
            referenceStrokes = listOf(stroke)
        )
        assertTrue(result.isCorrect)
        assertTrue(result.quality >= 4)
    }

    @Test
    fun validateWriting_wrongStrokeCount_incorrect() {
        val stroke = listOf(
            Point(0f, 50f), Point(25f, 50f), Point(50f, 50f),
            Point(75f, 50f), Point(100f, 50f)
        )
        // Draw only 1 stroke when reference has 2
        val result = StrokeMatcher.validateWriting(
            drawnStrokes = listOf(stroke),
            referenceStrokes = listOf(stroke, stroke)
        )
        assertFalse(result.isCorrect)
    }

    @Test
    fun validateWriting_emptyInput_returnsIncorrect() {
        val result = StrokeMatcher.validateWriting(emptyList(), emptyList())
        assertFalse(result.isCorrect)
        assertEquals(0, result.quality)
    }

    @Test
    fun validateWriting_qualityMapping_correctRange() {
        val stroke = listOf(
            Point(0f, 50f), Point(25f, 50f), Point(50f, 50f),
            Point(75f, 50f), Point(100f, 50f)
        )
        val result = StrokeMatcher.validateWriting(
            drawnStrokes = listOf(stroke),
            referenceStrokes = listOf(stroke)
        )
        assertTrue(result.quality in 0..5)
    }
}
