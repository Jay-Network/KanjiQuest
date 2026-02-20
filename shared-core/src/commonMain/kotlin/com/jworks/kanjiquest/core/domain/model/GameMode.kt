package com.jworks.kanjiquest.core.domain.model

enum class GameMode(val value: String) {
    RECOGNITION("recognition"),
    WRITING("writing"),
    VOCABULARY("vocabulary"),
    CAMERA_CHALLENGE("camera_challenge"),
    KANA_RECOGNITION("kana_recognition"),
    KANA_WRITING("kana_writing"),
    RADICAL_RECOGNITION("radical_recognition"),
    RADICAL_BUILDER("radical_builder");

    val isKanaMode: Boolean get() = this == KANA_RECOGNITION || this == KANA_WRITING
    val isRadicalMode: Boolean get() = this == RADICAL_RECOGNITION || this == RADICAL_BUILDER

    companion object {
        fun fromString(value: String): GameMode =
            entries.find { it.value == value } ?: RECOGNITION
    }
}
