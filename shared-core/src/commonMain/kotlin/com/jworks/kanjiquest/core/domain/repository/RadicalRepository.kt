package com.jworks.kanjiquest.core.domain.repository

import com.jworks.kanjiquest.core.domain.model.Radical

interface RadicalRepository {
    suspend fun getRadicalById(id: Int): Radical?
    suspend fun getRadicalByLiteral(literal: String): Radical?
    suspend fun getAllRadicals(): List<Radical>
    suspend fun countAll(): Long
    suspend fun getRandomRadicals(limit: Int): List<Radical>
    suspend fun getUnseenRadicals(limit: Int): List<Radical>
    suspend fun getRadicalsForKanji(kanjiId: Int): List<Radical>
    suspend fun getKanjiIdsForRadical(radicalId: Int): List<Long>
    suspend fun getKanjiContainingAllRadicals(radicalIds: List<Int>): List<Long>
    suspend fun getKanjiContainingSomeRadicals(radicalIds: List<Int>, maxMatchCount: Int, limit: Int): List<Long>
}
