package com.jworks.kanjiquest.core.domain.model

enum class VocabQuestionType(val requiredLevel: Int) {
    MEANING(10),
    READING(12),
    KANJI_FILL(15),
    SENTENCE(20);

    companion object {
        fun availableForLevel(level: Int): List<VocabQuestionType> =
            entries.filter { level >= it.requiredLevel }
    }
}
