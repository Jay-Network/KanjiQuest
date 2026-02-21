package com.jworks.kanjiquest.core.data

import com.jworks.kanjiquest.core.domain.model.CollectedItem
import com.jworks.kanjiquest.core.domain.model.CollectionItemType
import com.jworks.kanjiquest.core.domain.model.CollectionStats
import com.jworks.kanjiquest.core.domain.model.Rarity
import com.jworks.kanjiquest.core.domain.repository.CollectionRepository
import com.jworks.kanjiquest.db.KanjiQuestDatabase

class CollectionRepositoryImpl(
    private val db: KanjiQuestDatabase
) : CollectionRepository {

    override suspend fun isCollected(itemId: Int, itemType: CollectionItemType): Boolean {
        return db.collectionQueries.isCollected(itemId.toLong(), itemType.value).executeAsOne() > 0
    }

    override suspend fun collect(item: CollectedItem) {
        db.collectionQueries.insert(
            item_id = item.itemId.toLong(),
            item_type = item.itemType.value,
            rarity = item.rarity.name.lowercase(),
            item_level = item.itemLevel.toLong(),
            item_xp = item.itemXp.toLong(),
            discovered_at = item.discoveredAt,
            source = item.source
        )
    }

    override suspend fun getCollectedByType(type: CollectionItemType): List<CollectedItem> {
        return db.collectionQueries.getByType(type.value).executeAsList().map { it.toCollectedItem() }
    }

    override suspend fun getItem(itemId: Int, itemType: CollectionItemType): CollectedItem? {
        return db.collectionQueries.getItem(itemId.toLong(), itemType.value)
            .executeAsOneOrNull()?.toCollectedItem()
    }

    override suspend fun addItemXp(itemId: Int, itemType: CollectionItemType, xp: Int): CollectedItem? {
        db.collectionQueries.addXp(xp.toLong(), itemId.toLong(), itemType.value)
        return getItem(itemId, itemType)
    }

    override suspend fun updateLevel(itemId: Int, itemType: CollectionItemType, newLevel: Int, newXp: Int) {
        db.collectionQueries.updateLevel(newLevel.toLong(), newXp.toLong(), itemId.toLong(), itemType.value)
    }

    override suspend fun getCollectionStats(): CollectionStats {
        val total = db.collectionQueries.getTotalCount().executeAsOne().toInt()
        val kanjiCount = db.collectionQueries.getCollectionCount("kanji").executeAsOne().toInt()
        val hiraganaCount = db.collectionQueries.getCollectionCount("hiragana").executeAsOne().toInt()
        val katakanaCount = db.collectionQueries.getCollectionCount("katakana").executeAsOne().toInt()
        val radicalCount = db.collectionQueries.getCollectionCount("radical").executeAsOne().toInt()

        val commonCount = db.collectionQueries.getCountByRarity("kanji", "common").executeAsOne().toInt() +
                db.collectionQueries.getCountByRarity("hiragana", "common").executeAsOne().toInt() +
                db.collectionQueries.getCountByRarity("katakana", "common").executeAsOne().toInt() +
                db.collectionQueries.getCountByRarity("radical", "common").executeAsOne().toInt()
        val uncommonCount = db.collectionQueries.getCountByRarity("kanji", "uncommon").executeAsOne().toInt() +
                db.collectionQueries.getCountByRarity("hiragana", "uncommon").executeAsOne().toInt() +
                db.collectionQueries.getCountByRarity("katakana", "uncommon").executeAsOne().toInt() +
                db.collectionQueries.getCountByRarity("radical", "uncommon").executeAsOne().toInt()
        val rareCount = db.collectionQueries.getCountByRarity("kanji", "rare").executeAsOne().toInt() +
                db.collectionQueries.getCountByRarity("hiragana", "rare").executeAsOne().toInt() +
                db.collectionQueries.getCountByRarity("katakana", "rare").executeAsOne().toInt() +
                db.collectionQueries.getCountByRarity("radical", "rare").executeAsOne().toInt()
        val epicCount = db.collectionQueries.getCountByRarity("kanji", "epic").executeAsOne().toInt() +
                db.collectionQueries.getCountByRarity("hiragana", "epic").executeAsOne().toInt() +
                db.collectionQueries.getCountByRarity("katakana", "epic").executeAsOne().toInt() +
                db.collectionQueries.getCountByRarity("radical", "epic").executeAsOne().toInt()
        val legendaryCount = db.collectionQueries.getCountByRarity("kanji", "legendary").executeAsOne().toInt() +
                db.collectionQueries.getCountByRarity("hiragana", "legendary").executeAsOne().toInt() +
                db.collectionQueries.getCountByRarity("katakana", "legendary").executeAsOne().toInt() +
                db.collectionQueries.getCountByRarity("radical", "legendary").executeAsOne().toInt()

        return CollectionStats(
            totalCollected = total,
            kanjiCount = kanjiCount,
            hiraganaCount = hiraganaCount,
            katakanaCount = katakanaCount,
            radicalCount = radicalCount,
            commonCount = commonCount,
            uncommonCount = uncommonCount,
            rareCount = rareCount,
            epicCount = epicCount,
            legendaryCount = legendaryCount
        )
    }

    override suspend fun getCollectedKanjiIds(): List<Int> {
        return db.collectionQueries.getCollectedKanjiIds().executeAsList().map { it.toInt() }
    }

    override suspend fun getTotalCount(): Int {
        return db.collectionQueries.getTotalCount().executeAsOne().toInt()
    }

    private fun com.jworks.kanjiquest.db.Collection.toCollectedItem(): CollectedItem {
        return CollectedItem(
            itemId = item_id.toInt(),
            itemType = CollectionItemType.fromString(item_type),
            rarity = Rarity.fromString(rarity),
            itemLevel = item_level.toInt(),
            itemXp = item_xp.toInt(),
            discoveredAt = discovered_at,
            source = source
        )
    }
}
