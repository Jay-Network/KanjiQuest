package com.jworks.kanjiquest.core.domain.model

data class FieldJournalEntry(
    val id: Long,
    val imagePath: String,
    val locationLabel: String,
    val kanjiFound: List<String>,
    val kanjiCount: Int,
    val capturedAt: Long
)
