package com.jworks.kanjiquest.core.domain.model

enum class VocabQuestionType(val requiredLevel: Int) {
    MEANING(1),
    READING(5),
    KANJI_FILL(10),
    SENTENCE(15);

    companion object {
        fun availableForLevel(level: Int): List<VocabQuestionType> =
            entries.filter { level >= it.requiredLevel }
    }
}
