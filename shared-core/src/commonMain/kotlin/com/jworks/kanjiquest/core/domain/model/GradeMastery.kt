package com.jworks.kanjiquest.core.domain.model

data class GradeMastery(
    val grade: Int,
    val totalKanjiInGrade: Long,
    val studiedCount: Long,
    val masteredCount: Long,
    val averageAccuracy: Float,
    val coverage: Float
) {
    val masteryScore: Float get() = coverage * 0.4f + averageAccuracy * 0.6f
    val masteryLevel: MasteryLevel get() = when {
        masteryScore >= 0.85f -> MasteryLevel.ADVANCED
        masteryScore >= 0.70f -> MasteryLevel.PROFICIENT
        masteryScore >= 0.50f -> MasteryLevel.DEVELOPING
        else -> MasteryLevel.BEGINNING
    }
    val newGradeRatio: Float get() = when (masteryLevel) {
        MasteryLevel.ADVANCED -> 0.40f
        MasteryLevel.PROFICIENT -> 0.20f
        MasteryLevel.DEVELOPING -> 0.10f
        MasteryLevel.BEGINNING -> 0.0f
    }
}

enum class MasteryLevel(val label: String, val labelJp: String) {
    BEGINNING("Beginning", "初歩"),
    DEVELOPING("Developing", "発展中"),
    PROFICIENT("Proficient", "習熟"),
    ADVANCED("Advanced", "上級")
}
