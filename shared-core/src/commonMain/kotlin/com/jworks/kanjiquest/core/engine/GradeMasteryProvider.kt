package com.jworks.kanjiquest.core.engine

import com.jworks.kanjiquest.core.domain.model.GradeMastery

fun interface GradeMasteryProvider {
    suspend fun getGradeMastery(grade: Int): GradeMastery
}
