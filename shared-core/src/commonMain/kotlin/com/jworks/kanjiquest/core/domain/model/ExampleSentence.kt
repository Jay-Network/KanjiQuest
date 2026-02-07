package com.jworks.kanjiquest.core.domain.model

data class ExampleSentence(
    val id: Long,
    val vocabId: Long,
    val japanese: String,
    val english: String
)
