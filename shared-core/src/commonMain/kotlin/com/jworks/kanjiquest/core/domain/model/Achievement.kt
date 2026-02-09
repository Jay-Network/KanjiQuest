package com.jworks.kanjiquest.core.domain.model

data class Achievement(
    val id: String,
    val progress: Int,
    val target: Int,
    val unlockedAt: Long? = null
)
