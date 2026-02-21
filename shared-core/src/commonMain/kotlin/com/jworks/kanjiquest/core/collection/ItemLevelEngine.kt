package com.jworks.kanjiquest.core.collection

import com.jworks.kanjiquest.core.domain.model.CollectedItem
import com.jworks.kanjiquest.core.domain.model.CollectionItemType
import com.jworks.kanjiquest.core.domain.repository.CollectionRepository

data class ItemLevelResult(
    val updatedItem: CollectedItem,
    val leveledUp: Boolean,
    val xpGained: Int
)

class ItemLevelEngine(
    private val collectionRepository: CollectionRepository
) {
    companion object {
        private const val XP_CORRECT = 10
        private const val XP_WRONG = 2
        private const val MAX_LEVEL = 10

        fun xpForLevel(level: Int): Int = level * level * 25
    }

    /**
     * Add XP to a collected item after it appears in a game session.
     * @param itemId The item ID
     * @param itemType The item type
     * @param isCorrect Whether the player answered correctly
     * @param comboCount Current combo count for bonus XP
     * @return ItemLevelResult with updated item and whether it leveled up, or null if not collected
     */
    suspend fun addXp(
        itemId: Int,
        itemType: CollectionItemType,
        isCorrect: Boolean,
        comboCount: Int = 0
    ): ItemLevelResult? {
        if (!collectionRepository.isCollected(itemId, itemType)) return null

        val baseXp = if (isCorrect) XP_CORRECT else XP_WRONG
        val comboBonus = if (isCorrect && comboCount >= 5) baseXp / 2 else 0
        val totalXp = baseXp + comboBonus

        val updated = collectionRepository.addItemXp(itemId, itemType, totalXp)
            ?: return null

        // Check for level up
        if (!updated.isMaxLevel && updated.itemXp >= updated.xpToNextLevel) {
            val newLevel = (updated.itemLevel + 1).coerceAtMost(MAX_LEVEL)
            val overflowXp = updated.itemXp - updated.xpToNextLevel
            collectionRepository.updateLevel(itemId, itemType, newLevel, overflowXp.coerceAtLeast(0))
            val finalItem = collectionRepository.getItem(itemId, itemType) ?: updated
            return ItemLevelResult(
                updatedItem = finalItem,
                leveledUp = true,
                xpGained = totalXp
            )
        }

        return ItemLevelResult(
            updatedItem = updated,
            leveledUp = false,
            xpGained = totalXp
        )
    }
}
