package com.jworks.kanjiquest.core.collection

import com.jworks.kanjiquest.core.domain.model.CollectionItemType
import com.jworks.kanjiquest.core.domain.model.Rarity

object RarityCalculator {

    /**
     * Calculate rarity for a kanji based on grade, frequency, and stroke count.
     */
    fun calculateKanjiRarity(grade: Int?, frequency: Int?, strokeCount: Int): Rarity {
        val g = grade ?: 8
        val freq = frequency ?: Int.MAX_VALUE

        return when {
            g == 8 && strokeCount >= 15 -> Rarity.LEGENDARY
            g == 6 || (g < 6 && (freq > 3000 || freq == Int.MAX_VALUE)) -> Rarity.EPIC
            g in 4..5 && freq in 1501..3000 -> Rarity.RARE
            g in 2..3 && freq in 501..1500 -> Rarity.UNCOMMON
            g == 1 && freq <= 500 -> Rarity.COMMON
            // Fallback based on grade
            g >= 8 -> Rarity.EPIC
            g >= 6 -> Rarity.EPIC
            g >= 4 -> Rarity.RARE
            g >= 2 -> Rarity.UNCOMMON
            else -> Rarity.COMMON
        }
    }

    /**
     * Calculate rarity for kana based on variant type.
     */
    fun calculateKanaRarity(variant: String): Rarity {
        return when (variant) {
            "basic" -> Rarity.COMMON
            "dakuten", "handakuten" -> Rarity.UNCOMMON
            "combination" -> Rarity.RARE
            else -> Rarity.COMMON
        }
    }

    /**
     * Calculate rarity for radicals based on priority level.
     */
    fun calculateRadicalRarity(priority: Int): Rarity {
        return when (priority) {
            1 -> Rarity.COMMON
            2 -> Rarity.UNCOMMON
            3 -> Rarity.RARE
            else -> Rarity.COMMON
        }
    }

    /**
     * Generic rarity calculation by item type with relevant attributes.
     */
    fun calculateRarity(
        itemType: CollectionItemType,
        grade: Int? = null,
        frequency: Int? = null,
        strokeCount: Int = 0,
        variant: String = "basic",
        priority: Int = 1
    ): Rarity {
        return when (itemType) {
            CollectionItemType.KANJI -> calculateKanjiRarity(grade, frequency, strokeCount)
            CollectionItemType.HIRAGANA, CollectionItemType.KATAKANA -> calculateKanaRarity(variant)
            CollectionItemType.RADICAL -> calculateRadicalRarity(priority)
        }
    }
}
