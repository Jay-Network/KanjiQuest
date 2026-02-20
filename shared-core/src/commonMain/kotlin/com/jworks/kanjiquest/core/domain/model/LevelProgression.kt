package com.jworks.kanjiquest.core.domain.model

data class PlayerTier(
    val ordinal: Int,
    val nameEn: String,
    val nameJp: String,
    val levelRange: IntRange,
    val unlockedGrades: List<Int>,
    val featureUnlocks: List<String> = emptyList()
)

object LevelProgression {

    val tiers: List<PlayerTier> = listOf(
        PlayerTier(
            ordinal = 1, nameEn = "Newcomer", nameJp = "新参者",
            levelRange = 1..1, unlockedGrades = emptyList(),
            featureUnlocks = listOf("Hiragana")
        ),
        PlayerTier(
            ordinal = 2, nameEn = "Script Learner", nameJp = "文字習い",
            levelRange = 2..2, unlockedGrades = emptyList(),
            featureUnlocks = listOf("Katakana")
        ),
        PlayerTier(
            ordinal = 3, nameEn = "Foundation", nameJp = "基礎",
            levelRange = 3..3, unlockedGrades = emptyList(),
            featureUnlocks = listOf("Radicals")
        ),
        PlayerTier(
            ordinal = 4, nameEn = "Beginner", nameJp = "入門",
            levelRange = 4..4, unlockedGrades = listOf(1),
            featureUnlocks = listOf("Recognition", "MEANING vocab")
        ),
        PlayerTier(
            ordinal = 5, nameEn = "Novice", nameJp = "初級",
            levelRange = 5..9, unlockedGrades = listOf(1, 2),
            featureUnlocks = listOf("READING vocab")
        ),
        PlayerTier(
            ordinal = 6, nameEn = "Apprentice", nameJp = "見習い",
            levelRange = 10..14, unlockedGrades = listOf(1, 2, 3),
            featureUnlocks = listOf("KANJI_FILL vocab")
        ),
        PlayerTier(
            ordinal = 7, nameEn = "Intermediate", nameJp = "中級",
            levelRange = 15..19, unlockedGrades = listOf(1, 2, 3, 4),
            featureUnlocks = listOf("SENTENCE vocab")
        ),
        PlayerTier(
            ordinal = 8, nameEn = "Advanced", nameJp = "上級",
            levelRange = 20..24, unlockedGrades = listOf(1, 2, 3, 4, 5)
        ),
        PlayerTier(
            ordinal = 9, nameEn = "Scholar", nameJp = "学者",
            levelRange = 25..29, unlockedGrades = listOf(1, 2, 3, 4, 5, 6)
        ),
        PlayerTier(
            ordinal = 10, nameEn = "Expert", nameJp = "達人",
            levelRange = 30..36, unlockedGrades = listOf(1, 2, 3, 4, 5, 6, 8)
        ),
        PlayerTier(
            ordinal = 11, nameEn = "Master", nameJp = "師範",
            levelRange = 37..Int.MAX_VALUE, unlockedGrades = listOf(1, 2, 3, 4, 5, 6, 8)
        )
    )

    fun getTierForLevel(level: Int): PlayerTier {
        return tiers.lastOrNull { level >= it.levelRange.first } ?: tiers.first()
    }

    fun getUnlockedGrades(level: Int): List<Int> {
        return getTierForLevel(level).unlockedGrades
    }

    fun getNextTier(level: Int): PlayerTier? {
        val current = getTierForLevel(level)
        return tiers.getOrNull(tiers.indexOf(current) + 1)
    }

    fun getTierProgress(level: Int): Float {
        val tier = getTierForLevel(level)
        val rangeSize = (tier.levelRange.last - tier.levelRange.first + 1).toFloat()
        if (rangeSize <= 0 || tier.levelRange.last == Int.MAX_VALUE) {
            // Master tier has no upper bound — use levels since tier start
            val levelsIn = (level - tier.levelRange.first).toFloat()
            return (levelsIn / 10f).coerceIn(0f, 1f)
        }
        val levelsIn = (level - tier.levelRange.first).toFloat()
        return (levelsIn / rangeSize).coerceIn(0f, 1f)
    }
}
