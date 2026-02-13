package com.jworks.kanjiquest.core.domain.model

data class FlashcardEntry(
    val kanjiId: Int,
    val addedAt: String,
    val lastStudiedAt: String?,
    val studyCount: Int,
    val notes: String?
)
