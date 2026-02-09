package com.jworks.kanjiquest.core.domain.model

data class Vocabulary(
    val id: Long,
    val kanjiForm: String,
    val reading: String,
    val meaningsEn: List<String>,
    val jlptLevel: Int?,
    val frequency: Int?
) {
    val primaryMeaning: String get() = meaningsEn.firstOrNull() ?: ""
}
