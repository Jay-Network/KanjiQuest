package com.jworks.kanjiquest.core.domain.model

data class UserProfile(
    val totalXp: Int = 0,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastStudyDate: String? = null,
    val dailyGoal: Int = 20
) {
    val xpForCurrentLevel: Int get() = xpForLevel(level)
    val xpForNextLevel: Int get() = xpForLevel(level + 1)
    val xpProgress: Float
        get() {
            val current = totalXp - xpForCurrentLevel
            val needed = xpForNextLevel - xpForCurrentLevel
            return if (needed > 0) current.toFloat() / needed else 1f
        }

    companion object {
        fun xpForLevel(level: Int): Int = level * level * 50
    }
}
