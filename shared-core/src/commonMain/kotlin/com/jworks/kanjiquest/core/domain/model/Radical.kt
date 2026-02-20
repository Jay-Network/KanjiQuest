package com.jworks.kanjiquest.core.domain.model

data class Radical(
    val id: Int,
    val literal: String,
    val meaningEn: String,
    val meaningJp: String? = null,
    val strokeCount: Int,
    val strokeSvg: String? = null,
    val frequency: Int = 0,
    val exampleKanji: String = "[]",
    val position: String? = null
)
