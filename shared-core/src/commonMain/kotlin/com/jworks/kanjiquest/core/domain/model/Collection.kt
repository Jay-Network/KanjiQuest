package com.jworks.kanjiquest.core.domain.model

enum class Rarity(val label: String, val colorValue: Long) {
    COMMON("Common", 0xFF9E9E9E),
    UNCOMMON("Uncommon", 0xFF4CAF50),
    RARE("Rare", 0xFF2196F3),
    EPIC("Epic", 0xFF9C27B0),
    LEGENDARY("Legendary", 0xFFFFD700);

    companion object {
        fun fromString(value: String): Rarity =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: COMMON
    }
}

enum class CollectionItemType(val value: String) {
    KANJI("kanji"),
    HIRAGANA("hiragana"),
    KATAKANA("katakana"),
    RADICAL("radical");

    companion object {
        fun fromString(value: String): CollectionItemType =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: KANJI
    }
}

data class CollectedItem(
    val itemId: Int,
    val itemType: CollectionItemType,
    val rarity: Rarity,
    val itemLevel: Int,
    val itemXp: Int,
    val discoveredAt: Long,
    val source: String
) {
    val xpToNextLevel: Int get() = itemLevel * itemLevel * 25
    val levelProgress: Float get() = if (isMaxLevel) 1f else itemXp.toFloat() / xpToNextLevel
    val isMaxLevel: Boolean get() = itemLevel >= 10
}

data class CollectionStats(
    val totalCollected: Int,
    val kanjiCount: Int,
    val hiraganaCount: Int,
    val katakanaCount: Int,
    val radicalCount: Int,
    val commonCount: Int,
    val uncommonCount: Int,
    val rareCount: Int,
    val epicCount: Int,
    val legendaryCount: Int
)
