package com.jworks.kanjiquest.core.srs

import com.jworks.kanjiquest.core.domain.model.SrsCard
import com.jworks.kanjiquest.core.domain.model.SrsState

interface SrsAlgorithm {
    fun review(card: SrsCard, quality: Int, currentTime: Long): SrsCard
}

/**
 * SM-2 algorithm implementation.
 * quality: 0-5 (0=complete failure, 5=perfect response)
 * quality < 3 = failed (reset), >= 3 = passed (advance interval)
 */
class Sm2Algorithm : SrsAlgorithm {

    override fun review(card: SrsCard, quality: Int, currentTime: Long): SrsCard {
        require(quality in 0..5) { "Quality must be 0-5, got $quality" }

        val newTotalReviews = card.totalReviews + 1
        val newCorrectCount = if (quality >= 3) card.correctCount + 1 else card.correctCount

        return if (quality < 3) {
            // Failed: reset repetitions, short retry interval (10 minutes)
            card.copy(
                repetitions = 0,
                interval = 0,
                nextReview = currentTime + LEARNING_STEP_SECONDS,
                state = SrsState.LEARNING,
                totalReviews = newTotalReviews,
                correctCount = newCorrectCount
            )
        } else {
            // Passed: advance interval
            val newEaseFactor = maxOf(
                MIN_EASE_FACTOR,
                card.easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
            )

            val newRepetitions = card.repetitions + 1
            val newState = when {
                newRepetitions >= GRADUATION_THRESHOLD -> SrsState.GRADUATED
                newRepetitions >= 2 -> SrsState.REVIEW
                else -> SrsState.LEARNING
            }

            // Learning cards use shorter intervals before graduating to day-based scheduling
            val newInterval: Int
            val nextReviewTime: Long
            if (newState == SrsState.LEARNING) {
                // Still learning: review again in 10 minutes
                newInterval = 0
                nextReviewTime = currentTime + LEARNING_STEP_SECONDS
            } else {
                // Review/Graduated: day-based intervals
                newInterval = when (card.repetitions) {
                    0 -> 1
                    1 -> 6
                    else -> (card.interval * newEaseFactor).toInt()
                }
                nextReviewTime = currentTime + newInterval.toLong() * SECONDS_PER_DAY
            }

            card.copy(
                easeFactor = newEaseFactor,
                interval = newInterval,
                repetitions = newRepetitions,
                nextReview = nextReviewTime,
                state = newState,
                totalReviews = newTotalReviews,
                correctCount = newCorrectCount
            )
        }
    }

    companion object {
        private const val MIN_EASE_FACTOR = 1.3
        private const val SECONDS_PER_DAY = 86400L
        private const val LEARNING_STEP_SECONDS = 600L  // 10 minutes
        private const val GRADUATION_THRESHOLD = 8
    }
}
