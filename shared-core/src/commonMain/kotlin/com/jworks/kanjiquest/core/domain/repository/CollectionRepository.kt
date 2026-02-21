package com.jworks.kanjiquest.core.domain.repository

import com.jworks.kanjiquest.core.domain.model.CollectedItem
import com.jworks.kanjiquest.core.domain.model.CollectionItemType
import com.jworks.kanjiquest.core.domain.model.CollectionStats

interface CollectionRepository {
    suspend fun isCollected(itemId: Int, itemType: CollectionItemType): Boolean
    suspend fun collect(item: CollectedItem)
    suspend fun getCollectedByType(type: CollectionItemType): List<CollectedItem>
    suspend fun getItem(itemId: Int, itemType: CollectionItemType): CollectedItem?
    suspend fun addItemXp(itemId: Int, itemType: CollectionItemType, xp: Int): CollectedItem?
    suspend fun updateLevel(itemId: Int, itemType: CollectionItemType, newLevel: Int, newXp: Int)
    suspend fun getCollectionStats(): CollectionStats
    suspend fun getCollectedKanjiIds(): List<Int>
    suspend fun getTotalCount(): Int
}
