package com.jworks.kanjiquest.core.domain.repository

import com.jworks.kanjiquest.core.domain.model.Kana
import com.jworks.kanjiquest.core.domain.model.KanaType

interface KanaRepository {
    suspend fun getKanaById(id: Int): Kana?
    suspend fun getKanaByLiteral(literal: String): Kana?
    suspend fun getKanaByType(type: KanaType): List<Kana>
    suspend fun getKanaByTypeAndVariant(type: KanaType, variant: String): List<Kana>
    suspend fun getKanaByGroup(type: KanaType, group: String): List<Kana>
    suspend fun countByType(type: KanaType): Long
    suspend fun countAll(): Long
    suspend fun getRandomByType(type: KanaType, limit: Int): List<Kana>
    suspend fun getGroupDistractors(type: KanaType, group: String, excludeId: Int, limit: Int): List<Kana>
    suspend fun getUnseenKana(type: KanaType, limit: Int): List<Kana>
}
