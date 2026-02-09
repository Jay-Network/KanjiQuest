package com.jworks.kanjiquest.core

import com.jworks.kanjiquest.core.domain.model.SrsCard
import com.jworks.kanjiquest.core.domain.model.SrsState
import com.jworks.kanjiquest.core.srs.Sm2Algorithm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Sm2AlgorithmTest {

    private val algorithm = Sm2Algorithm()
    private val now = 1700000000L

    @Test
    fun newCard_failedReview_resetsToLearning() {
        val card = SrsCard(kanjiId = 1)
        val result = algorithm.review(card, quality = 1, currentTime = now)

        assertEquals(SrsState.LEARNING, result.state)
        assertEquals(0, result.repetitions)
        assertEquals(0, result.interval)
        assertEquals(1, result.totalReviews)
        assertEquals(0, result.correctCount)
    }

    @Test
    fun newCard_passedReview_advancesToLearning() {
        val card = SrsCard(kanjiId = 1)
        val result = algorithm.review(card, quality = 4, currentTime = now)

        assertEquals(SrsState.LEARNING, result.state)
        assertEquals(1, result.repetitions)
        assertEquals(1, result.interval)
        assertEquals(1, result.totalReviews)
        assertEquals(1, result.correctCount)
        assertEquals(now + 86400L, result.nextReview)
    }

    @Test
    fun firstRepetition_passedReview_setsIntervalTo6() {
        val card = SrsCard(kanjiId = 1, repetitions = 1, interval = 1, state = SrsState.LEARNING)
        val result = algorithm.review(card, quality = 4, currentTime = now)

        assertEquals(SrsState.REVIEW, result.state)
        assertEquals(2, result.repetitions)
        assertEquals(6, result.interval)
        assertEquals(now + 6 * 86400L, result.nextReview)
    }

    @Test
    fun subsequentReview_passedReview_multipliesInterval() {
        val card = SrsCard(
            kanjiId = 1,
            repetitions = 2,
            interval = 6,
            easeFactor = 2.5,
            state = SrsState.REVIEW
        )
        val result = algorithm.review(card, quality = 4, currentTime = now)

        assertEquals(3, result.repetitions)
        assertTrue(result.interval > 6, "Interval should increase: ${result.interval}")
    }

    @Test
    fun perfectReview_increasesEaseFactor() {
        val card = SrsCard(kanjiId = 1, easeFactor = 2.5)
        val result = algorithm.review(card, quality = 5, currentTime = now)

        assertTrue(result.easeFactor > 2.5, "Ease factor should increase for quality 5")
    }

    @Test
    fun difficultReview_decreasesEaseFactor() {
        val card = SrsCard(kanjiId = 1, easeFactor = 2.5)
        val result = algorithm.review(card, quality = 3, currentTime = now)

        assertTrue(result.easeFactor < 2.5, "Ease factor should decrease for quality 3")
    }

    @Test
    fun easeFactor_neverGoesBelow1_3() {
        val card = SrsCard(kanjiId = 1, easeFactor = 1.3)
        val result = algorithm.review(card, quality = 3, currentTime = now)

        assertTrue(result.easeFactor >= 1.3, "Ease factor should not go below 1.3")
    }

    @Test
    fun failedReview_resetsRepetitionsButKeepsEaseFactor() {
        val card = SrsCard(
            kanjiId = 1,
            repetitions = 5,
            interval = 30,
            easeFactor = 2.2,
            state = SrsState.REVIEW
        )
        val result = algorithm.review(card, quality = 2, currentTime = now)

        assertEquals(0, result.repetitions)
        assertEquals(0, result.interval)
        assertEquals(2.2, result.easeFactor) // Keeps ease factor
        assertEquals(SrsState.LEARNING, result.state)
    }

    @Test
    fun card_graduatesAfter8Repetitions() {
        var card = SrsCard(kanjiId = 1)
        for (i in 0 until 8) {
            card = algorithm.review(card, quality = 4, currentTime = now + i * 86400L)
        }

        assertEquals(SrsState.GRADUATED, card.state)
        assertEquals(8, card.repetitions)
    }

    @Test
    fun accuracy_trackedCorrectly() {
        var card = SrsCard(kanjiId = 1)

        // 3 correct, 1 wrong = 75% accuracy
        card = algorithm.review(card, quality = 4, currentTime = now)
        card = algorithm.review(card, quality = 5, currentTime = now + 86400)
        card = algorithm.review(card, quality = 1, currentTime = now + 86400 * 2)
        card = algorithm.review(card, quality = 4, currentTime = now + 86400 * 3)

        assertEquals(4, card.totalReviews)
        assertEquals(3, card.correctCount)
        assertEquals(0.75f, card.accuracy)
    }
}
