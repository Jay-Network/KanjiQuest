package com.jworks.kanjiquest.core.domain.model

enum class GameMode(val value: String) {
    RECOGNITION("recognition"),
    WRITING("writing"),
    VOCABULARY("vocabulary"),
    CAMERA_CHALLENGE("camera_challenge");

    companion object {
        fun fromString(value: String): GameMode =
            entries.find { it.value == value } ?: RECOGNITION
    }
}
