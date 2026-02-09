package com.jworks.kanjiquest.core.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class Kanji(
    val id: Int,
    val literal: String,
    val grade: Int?,
    val jlptLevel: Int?,
    val frequency: Int?,
    val strokeCount: Int,
    val meaningsEn: List<String>,
    val onReadings: List<String>,
    val kunReadings: List<String>,
    val strokeSvg: String?
) {
    val unicodeHex: String get() = "U+%04X".format(id)

    val primaryMeaning: String get() = meaningsEn.firstOrNull() ?: ""

    val primaryOnReading: String get() = onReadings.firstOrNull() ?: ""

    val primaryKunReading: String get() = kunReadings.firstOrNull() ?: ""

    val jlptLabel: String? get() = jlptLevel?.let { "N$it" }

    val gradeLabel: String?
        get() = when (grade) {
            in 1..6 -> "Grade $grade"
            8 -> "Junior High"
            else -> null
        }
}

internal val json = Json { ignoreUnknownKeys = true }

fun parseJsonStringArray(jsonStr: String): List<String> {
    return try {
        json.decodeFromString<List<String>>(jsonStr)
    } catch (_: Exception) {
        emptyList()
    }
}
