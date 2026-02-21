package com.jworks.kanjiquest.core.collection

import com.jworks.kanjiquest.core.domain.model.CollectedItem
import com.jworks.kanjiquest.core.domain.model.CollectionItemType
import com.jworks.kanjiquest.core.domain.model.Rarity
import com.jworks.kanjiquest.core.domain.repository.CollectionRepository
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import kotlin.random.Random

data class EncounterResult(
    val collectedItem: CollectedItem
)

class EncounterEngine(
    private val collectionRepository: CollectionRepository,
    private val kanjiRepository: KanjiRepository
) {
    // Pity counters: correct answers since last discovery per rarity
    private var pityCounts = mutableMapOf(
        Rarity.COMMON to 0,
        Rarity.UNCOMMON to 0,
        Rarity.RARE to 0,
        Rarity.EPIC to 0,
        Rarity.LEGENDARY to 0
    )

    companion object {
        // Base encounter rates per correct answer
        private val ENCOUNTER_RATES = mapOf(
            Rarity.COMMON to 0.40f,
            Rarity.UNCOMMON to 0.25f,
            Rarity.RARE to 0.12f,
            Rarity.EPIC to 0.05f,
            Rarity.LEGENDARY to 0.02f
        )

        // Pity thresholds: guaranteed encounter after N correct answers without discovery
        private val PITY_THRESHOLDS = mapOf(
            Rarity.COMMON to 3,
            Rarity.UNCOMMON to 5,
            Rarity.RARE to 12,
            Rarity.EPIC to 25,
            Rarity.LEGENDARY to 50
        )
    }

    /**
     * Roll for an encounter after a correct answer.
     * @param unlockedGrades The grades the player has unlocked
     * @param currentTime Epoch seconds for discovered_at
     * @return EncounterResult if a new item was discovered, null otherwise
     */
    suspend fun rollEncounter(
        unlockedGrades: List<Int>,
        currentTime: Long
    ): EncounterResult? {
        // Increment all pity counters
        for (rarity in Rarity.entries) {
            pityCounts[rarity] = (pityCounts[rarity] ?: 0) + 1
        }

        // Determine which rarity to try (highest first for excitement)
        val targetRarity = determineEncounterRarity()
            ?: return null

        // Find an uncollected item of this rarity from unlocked grades
        val item = findUncollectedItem(targetRarity, unlockedGrades, currentTime)
            ?: return null

        // Reset pity counter for the discovered rarity (and all lower rarities)
        resetPityCounters(targetRarity)

        // Add to collection
        collectionRepository.collect(item)

        return EncounterResult(item)
    }

    /**
     * Determine which rarity tier triggers based on probability + pity.
     * Returns null if no encounter this round.
     */
    private fun determineEncounterRarity(): Rarity? {
        // Check from highest to lowest rarity
        for (rarity in Rarity.entries.reversed()) {
            val pityCount = pityCounts[rarity] ?: 0
            val pityThreshold = PITY_THRESHOLDS[rarity] ?: Int.MAX_VALUE
            val baseRate = ENCOUNTER_RATES[rarity] ?: 0f

            // Pity guarantee
            if (pityCount >= pityThreshold) {
                return rarity
            }

            // Probability roll
            if (Random.nextFloat() < baseRate) {
                return rarity
            }
        }
        return null
    }

    private fun resetPityCounters(discoveredRarity: Rarity) {
        // Reset the discovered rarity and all lower ones
        for (rarity in Rarity.entries) {
            if (rarity.ordinal <= discoveredRarity.ordinal) {
                pityCounts[rarity] = 0
            }
        }
    }

    /**
     * Find a random uncollected kanji of the target rarity from unlocked grades.
     */
    private suspend fun findUncollectedItem(
        targetRarity: Rarity,
        unlockedGrades: List<Int>,
        currentTime: Long
    ): CollectedItem? {
        val collectedKanjiIds = collectionRepository.getCollectedKanjiIds().toSet()

        // Get all kanji from unlocked grades
        for (grade in unlockedGrades.shuffled()) {
            val gradeKanji = kanjiRepository.getKanjiByGrade(grade)
            val candidates = gradeKanji.filter { kanji ->
                kanji.id !in collectedKanjiIds &&
                    RarityCalculator.calculateKanjiRarity(
                        kanji.grade, kanji.frequency, kanji.strokeCount
                    ) == targetRarity
            }

            if (candidates.isNotEmpty()) {
                val chosen = candidates.random()
                return CollectedItem(
                    itemId = chosen.id,
                    itemType = CollectionItemType.KANJI,
                    rarity = targetRarity,
                    itemLevel = 1,
                    itemXp = 0,
                    discoveredAt = currentTime,
                    source = "gameplay"
                )
            }
        }

        // If no exact rarity match, try one tier lower
        val fallbackRarity = if (targetRarity.ordinal > 0) {
            Rarity.entries[targetRarity.ordinal - 1]
        } else null

        if (fallbackRarity != null) {
            for (grade in unlockedGrades.shuffled()) {
                val gradeKanji = kanjiRepository.getKanjiByGrade(grade)
                val candidates = gradeKanji.filter { kanji ->
                    kanji.id !in collectedKanjiIds &&
                        RarityCalculator.calculateKanjiRarity(
                            kanji.grade, kanji.frequency, kanji.strokeCount
                        ) == fallbackRarity
                }
                if (candidates.isNotEmpty()) {
                    val chosen = candidates.random()
                    return CollectedItem(
                        itemId = chosen.id,
                        itemType = CollectionItemType.KANJI,
                        rarity = fallbackRarity,
                        itemLevel = 1,
                        itemXp = 0,
                        discoveredAt = currentTime,
                        source = "gameplay"
                    )
                }
            }
        }

        // Last resort: any uncollected kanji from unlocked grades
        for (grade in unlockedGrades.shuffled()) {
            val gradeKanji = kanjiRepository.getKanjiByGrade(grade)
            val candidates = gradeKanji.filter { it.id !in collectedKanjiIds }
            if (candidates.isNotEmpty()) {
                val chosen = candidates.random()
                val rarity = RarityCalculator.calculateKanjiRarity(
                    chosen.grade, chosen.frequency, chosen.strokeCount
                )
                return CollectedItem(
                    itemId = chosen.id,
                    itemType = CollectionItemType.KANJI,
                    rarity = rarity,
                    itemLevel = 1,
                    itemXp = 0,
                    discoveredAt = currentTime,
                    source = "gameplay"
                )
            }
        }

        return null
    }

    fun resetPity() {
        pityCounts.replaceAll { _, _ -> 0 }
    }
}
