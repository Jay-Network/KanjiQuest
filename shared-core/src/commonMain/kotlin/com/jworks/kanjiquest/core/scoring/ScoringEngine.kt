package com.jworks.kanjiquest.core.scoring

import com.jworks.kanjiquest.core.domain.model.GameMode

data class ScoreResult(
    val baseXp: Int,
    val comboMultiplier: Float,
    val isNewCardBonus: Boolean,
    val totalXp: Int
)

class ScoringEngine {

    fun calculateScore(
        quality: Int,
        comboCount: Int,
        isNewCard: Boolean,
        gameMode: GameMode = GameMode.RECOGNITION
    ): ScoreResult {
        val baseXp = if (gameMode == GameMode.WRITING) {
            when {
                quality >= 5 -> 20
                quality >= 4 -> 15
                quality >= 3 -> 10
                else -> 0
            }
        } else {
            when {
                quality >= 5 -> 15
                quality >= 4 -> 12
                quality >= 3 -> 8
                else -> 0
            }
        }

        val comboMultiplier = when {
            comboCount >= 10 -> 2.0f
            comboCount >= 5 -> 1.5f
            comboCount >= 3 -> 1.2f
            else -> 1.0f
        }

        val newCardMultiplier = if (isNewCard) 1.5f else 1.0f

        val totalXp = (baseXp * comboMultiplier * newCardMultiplier).toInt()

        return ScoreResult(
            baseXp = baseXp,
            comboMultiplier = comboMultiplier,
            isNewCardBonus = isNewCard,
            totalXp = totalXp
        )
    }

    fun calculateLevel(totalXp: Int): Int {
        var level = 1
        while (xpForLevel(level + 1) <= totalXp) {
            level++
        }
        return level
    }

    companion object {
        fun xpForLevel(level: Int): Int = level * level * 50
    }
}
