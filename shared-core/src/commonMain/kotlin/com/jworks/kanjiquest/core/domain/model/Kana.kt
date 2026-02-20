package com.jworks.kanjiquest.core.domain.model

data class Kana(
    val id: Int,
    val literal: String,
    val type: KanaType,
    val romanization: String,
    val group: String,
    val strokeCount: Int,
    val strokeSvg: String? = null,
    val variant: KanaVariant = KanaVariant.BASIC,
    val baseKanaId: Int? = null
)

enum class KanaType(val value: String) {
    HIRAGANA("hiragana"),
    KATAKANA("katakana");

    companion object {
        fun fromString(value: String): KanaType =
            entries.find { it.value == value } ?: HIRAGANA
    }
}

enum class KanaVariant(val value: String) {
    BASIC("basic"),
    DAKUTEN("dakuten"),
    HANDAKUTEN("handakuten"),
    COMBINATION("combination");

    companion object {
        fun fromString(value: String): KanaVariant =
            entries.find { it.value == value } ?: BASIC
    }
}
