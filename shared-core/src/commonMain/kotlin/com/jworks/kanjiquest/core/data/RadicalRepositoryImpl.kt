package com.jworks.kanjiquest.core.data

import com.jworks.kanjiquest.core.domain.model.Radical
import com.jworks.kanjiquest.core.domain.repository.RadicalRepository
import com.jworks.kanjiquest.db.KanjiQuestDatabase

class RadicalRepositoryImpl(
    private val db: KanjiQuestDatabase
) : RadicalRepository {

    override suspend fun getRadicalById(id: Int): Radical? {
        return db.radicalQueries.getById(id.toLong()).executeAsOneOrNull()?.toRadical()
    }

    override suspend fun getRadicalByLiteral(literal: String): Radical? {
        return db.radicalQueries.getByLiteral(literal).executeAsOneOrNull()?.toRadical()
    }

    override suspend fun getAllRadicals(): List<Radical> {
        return db.radicalQueries.getAll().executeAsList().map { it.toRadical() }
    }

    override suspend fun countAll(): Long {
        return db.radicalQueries.countAll().executeAsOne()
    }

    override suspend fun getRandomRadicals(limit: Int): List<Radical> {
        return db.radicalQueries.getRandom(limit.toLong()).executeAsList().map { it.toRadical() }
    }

    override suspend fun getUnseenRadicals(limit: Int): List<Radical> {
        return db.radicalQueries.getUnseen(limit.toLong()).executeAsList().map { it.toRadical() }
    }

    override suspend fun getRadicalsForKanji(kanjiId: Int): List<Radical> {
        return db.radicalQueries.getRadicalsForKanji(kanjiId.toLong()).executeAsList().map { it.toRadical() }
    }

    override suspend fun getKanjiIdsForRadical(radicalId: Int): List<Long> {
        return db.radicalQueries.getKanjiForRadical(radicalId.toLong()).executeAsList()
    }

    override suspend fun getKanjiContainingAllRadicals(radicalIds: List<Int>): List<Long> {
        if (radicalIds.isEmpty()) return emptyList()
        return db.radicalQueries.getKanjiContainingAllRadicals(
            radicalIds.map { it.toLong() },
            radicalIds.size.toLong()
        ).executeAsList()
    }

    override suspend fun getKanjiContainingSomeRadicals(radicalIds: List<Int>, maxMatchCount: Int, limit: Int): List<Long> {
        if (radicalIds.isEmpty()) return emptyList()
        return db.radicalQueries.getKanjiContainingSomeRadicals(
            radicalIds.map { it.toLong() },
            maxMatchCount.toLong(),
            limit.toLong()
        ).executeAsList().map { it.kanji_id }
    }
}

internal fun com.jworks.kanjiquest.db.Radical.toRadical(): Radical = Radical(
    id = id.toInt(),
    literal = literal,
    meaningEn = meaning_en,
    meaningJp = meaning_jp,
    strokeCount = stroke_count.toInt(),
    strokeSvg = stroke_svg,
    frequency = frequency.toInt(),
    exampleKanji = example_kanji,
    position = position
)
