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
        val baseXp = when {
            // Writing modes (kanji or kana) — full XP
            gameMode == GameMode.WRITING || gameMode == GameMode.KANA_WRITING -> when {
                quality >= 5 -> 20
                quality >= 4 -> 15
                quality >= 3 -> 10
                else -> 0
            }
            // Radical builder — higher XP (composition is harder)
            gameMode == GameMode.RADICAL_BUILDER -> when {
                quality >= 5 -> 18
                quality >= 4 -> 14
                quality >= 3 -> 10
                else -> 0
            }
            // Kana/radical recognition — slightly lower XP
            gameMode == GameMode.KANA_RECOGNITION || gameMode == GameMode.RADICAL_RECOGNITION -> when {
                quality >= 5 -> 10
                quality >= 4 -> 8
                quality >= 3 -> 5
                else -> 0
            }
            // Standard recognition, vocabulary, camera
            else -> when {
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
