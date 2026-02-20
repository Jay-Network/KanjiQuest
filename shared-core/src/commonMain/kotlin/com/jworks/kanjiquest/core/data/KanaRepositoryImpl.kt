package com.jworks.kanjiquest.core.data

import com.jworks.kanjiquest.core.domain.model.Kana
import com.jworks.kanjiquest.core.domain.model.KanaType
import com.jworks.kanjiquest.core.domain.model.KanaVariant
import com.jworks.kanjiquest.core.domain.repository.KanaRepository
import com.jworks.kanjiquest.db.KanjiQuestDatabase

class KanaRepositoryImpl(
    private val db: KanjiQuestDatabase
) : KanaRepository {

    override suspend fun getKanaById(id: Int): Kana? {
        return db.kanaQueries.getById(id.toLong()).executeAsOneOrNull()?.toKana()
    }

    override suspend fun getKanaByLiteral(literal: String): Kana? {
        return db.kanaQueries.getByLiteral(literal).executeAsOneOrNull()?.toKana()
    }

    override suspend fun getKanaByType(type: KanaType): List<Kana> {
        return db.kanaQueries.getByType(type.value).executeAsList().map { it.toKana() }
    }

    override suspend fun getKanaByTypeAndVariant(type: KanaType, variant: String): List<Kana> {
        return db.kanaQueries.getByTypeAndVariant(type.value, variant).executeAsList().map { it.toKana() }
    }

    override suspend fun getKanaByGroup(type: KanaType, group: String): List<Kana> {
        return db.kanaQueries.getByGroup(type.value, group).executeAsList().map { it.toKana() }
    }

    override suspend fun countByType(type: KanaType): Long {
        return db.kanaQueries.countByType(type.value).executeAsOne()
    }

    override suspend fun countAll(): Long {
        return db.kanaQueries.countAll().executeAsOne()
    }

    override suspend fun getRandomByType(type: KanaType, limit: Int): List<Kana> {
        return db.kanaQueries.getRandomByType(type.value, limit.toLong()).executeAsList().map { it.toKana() }
    }

    override suspend fun getGroupDistractors(type: KanaType, group: String, excludeId: Int, limit: Int): List<Kana> {
        return db.kanaQueries.getByGroupForDistractors(type.value, group, excludeId.toLong(), limit.toLong())
            .executeAsList().map { it.toKana() }
    }

    override suspend fun getUnseenKana(type: KanaType, limit: Int): List<Kana> {
        return db.kanaQueries.getUnseen(type.value, limit.toLong()).executeAsList().map { it.toKana() }
    }
}

internal fun com.jworks.kanjiquest.db.Kana.toKana(): Kana = Kana(
    id = id.toInt(),
    literal = literal,
    type = KanaType.fromString(type),
    romanization = romanization,
    group = kana_group,
    strokeCount = stroke_count.toInt(),
    strokeSvg = stroke_svg,
    variant = KanaVariant.fromString(variant),
    baseKanaId = base_kana_id?.toInt()
)
