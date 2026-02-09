package com.jworks.kanjiquest.core

import com.jworks.kanjiquest.core.domain.model.UserProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserProfileTest {

    @Test
    fun xpProgress_atLevelStart_is0() {
        val profile = UserProfile(totalXp = 50, level = 1)
        assertEquals(0f, profile.xpProgress, 0.01f)
    }

    @Test
    fun xpProgress_halfwayThroughLevel() {
        // Level 1 = 50 XP, Level 2 = 200 XP. Midpoint = 125 XP
        val profile = UserProfile(totalXp = 125, level = 1)
        assertEquals(0.5f, profile.xpProgress, 0.01f)
    }

    @Test
    fun xpForLevel_quadraticFormula() {
        assertEquals(50, UserProfile.xpForLevel(1))
        assertEquals(200, UserProfile.xpForLevel(2))
        assertEquals(450, UserProfile.xpForLevel(3))
    }

    @Test
    fun defaultProfile_isLevel1() {
        val profile = UserProfile()
        assertEquals(1, profile.level)
        assertEquals(0, profile.totalXp)
        assertEquals(0, profile.currentStreak)
        assertEquals(20, profile.dailyGoal)
    }
}
