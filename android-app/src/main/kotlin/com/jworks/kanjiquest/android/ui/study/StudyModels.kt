package com.jworks.kanjiquest.android.ui.study

enum class ContentTab(val label: String) {
    KANA("Kana"),
    RADICALS("Radicals"),
    KANJI("Kanji")
}

sealed class StudySource {
    data object All : StudySource()
    data class FromFlashcardDeck(val deckId: Long, val deckName: String) : StudySource()
    data object FromCollection : StudySource()
}

enum class KanaFilter(val label: String) {
    HIRAGANA_ONLY("Hiragana"),
    KATAKANA_ONLY("Katakana"),
    MIXED("Mixed")
}
