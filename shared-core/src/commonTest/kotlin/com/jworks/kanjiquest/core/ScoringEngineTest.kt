package com.jworks.kanjiquest.core

import com.jworks.kanjiquest.core.scoring.ScoringEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScoringEngineTest {

    private val engine = ScoringEngine()

    @Test
    fun perfectAnswer_noCombo_notNew_gives15xp() {
        val result = engine.calculateScore(quality = 5, comboCount = 0, isNewCard = false)
        assertEquals(15, result.baseXp)
        assertEquals(1.0f, result.comboMultiplier)
        assertEquals(15, result.totalXp)
    }

    @Test
    fun goodAnswer_gives12xp() {
        val result = engine.calculateScore(quality = 4, comboCount = 0, isNewCard = false)
        assertEquals(12, result.baseXp)
    }

    @Test
    fun passAnswer_gives8xp() {
        val result = engine.calculateScore(quality = 3, comboCount = 0, isNewCard = false)
        assertEquals(8, result.baseXp)
    }

    @Test
    fun failedAnswer_gives0xp() {
        val result = engine.calculateScore(quality = 2, comboCount = 0, isNewCard = false)
        assertEquals(0, result.totalXp)
    }

    @Test
    fun comboMultiplier_3to4_gives1_2x() {
        val result = engine.calculateScore(quality = 5, comboCount = 3, isNewCard = false)
        assertEquals(1.2f, result.comboMultiplier)
        assertEquals(18, result.totalXp) // 15 * 1.2 = 18
    }

    @Test
    fun comboMultiplier_5to9_gives1_5x() {
        val result = engine.calculateScore(quality = 5, comboCount = 7, isNewCard = false)
        assertEquals(1.5f, result.comboMultiplier)
        assertEquals(22, result.totalXp) // 15 * 1.5 = 22.5 -> 22
    }

    @Test
    fun comboMultiplier_10plus_gives2x() {
        val result = engine.calculateScore(quality = 5, comboCount = 10, isNewCard = false)
        assertEquals(2.0f, result.comboMultiplier)
        assertEquals(30, result.totalXp)
    }

    @Test
    fun newCardBonus_gives1_5x() {
        val result = engine.calculateScore(quality = 5, comboCount = 0, isNewCard = true)
        assertTrue(result.isNewCardBonus)
        assertEquals(22, result.totalXp) // 15 * 1.5 = 22.5 -> 22
    }

    @Test
    fun comboAndNewCardStack() {
        val result = engine.calculateScore(quality = 5, comboCount = 10, isNewCard = true)
        assertEquals(45, result.totalXp) // 15 * 2.0 * 1.5 = 45
    }

    @Test
    fun levelCalculation_level1() {
        assertEquals(1, engine.calculateLevel(0))
        assertEquals(1, engine.calculateLevel(49))
    }

    @Test
    fun levelCalculation_level2() {
        assertEquals(2, engine.calculateLevel(200)) // level 2 = 200, level 3 = 450
    }

    @Test
    fun levelCalculation_level10() {
        assertEquals(10, engine.calculateLevel(5000)) // level 10 = 5000
    }

    @Test
    fun xpForLevel_quadraticCurve() {
        assertEquals(50, ScoringEngine.xpForLevel(1))
        assertEquals(200, ScoringEngine.xpForLevel(2))
        assertEquals(450, ScoringEngine.xpForLevel(3))
        assertEquals(5000, ScoringEngine.xpForLevel(10))
    }
}
