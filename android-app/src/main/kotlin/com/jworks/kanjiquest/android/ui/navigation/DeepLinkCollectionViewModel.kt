package com.jworks.kanjiquest.android.ui.navigation

import androidx.lifecycle.ViewModel
import com.jworks.kanjiquest.core.collection.RarityCalculator
import com.jworks.kanjiquest.core.domain.model.CollectedItem
import com.jworks.kanjiquest.core.domain.model.CollectionItemType
import com.jworks.kanjiquest.core.domain.repository.CollectionRepository
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class DeepLinkCollectionViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val kanjiRepository: KanjiRepository
) : ViewModel() {

    /**
     * Collect a kanji from a deep link (e.g., KanjiLens).
     * Returns (CollectedItem, kanjiLiteral) if newly collected, null if already collected.
     */
    suspend fun collectFromDeepLink(kanjiId: Int, source: String): Pair<CollectedItem, String>? {
        // Check if already collected
        if (collectionRepository.isCollected(kanjiId, CollectionItemType.KANJI)) {
            return null
        }

        // Look up kanji info for rarity calculation
        val kanji = kanjiRepository.getKanjiById(kanjiId) ?: return null
        val rarity = RarityCalculator.calculateKanjiRarity(
            grade = kanji.grade,
            frequency = kanji.frequency,
            strokeCount = kanji.strokeCount
        )

        val item = CollectedItem(
            itemId = kanjiId,
            itemType = CollectionItemType.KANJI,
            rarity = rarity,
            itemLevel = 1,
            itemXp = 0,
            discoveredAt = Clock.System.now().epochSeconds,
            source = source
        )

        collectionRepository.collect(item)
        return item to kanji.literal
    }
}
