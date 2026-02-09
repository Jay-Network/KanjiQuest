package com.jworks.kanjiquest.japanese

data class JapaneseToken(
    val surface: String,
    val reading: String,
    val startIndex: Int,
    val containsKanji: Boolean
)
