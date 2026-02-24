package com.jworks.kanjiquest.android.ui.navigation

sealed class NavRoute(val route: String) {
    data object Splash : NavRoute("splash")
    data object Login : NavRoute("login")
    data object Home : NavRoute("home")
    data object KanjiDetail : NavRoute("kanji/{kanjiId}") {
        fun createRoute(kanjiId: Int) = "kanji/$kanjiId"
    }
    data object Recognition : NavRoute("game/recognition")
    data object Writing : NavRoute("game/writing")
    data object Vocabulary : NavRoute("game/vocabulary")
    data object Camera : NavRoute("game/camera")
    data object Shop : NavRoute("shop")
    data object Progress : NavRoute("progress")
    data object Settings : NavRoute("settings")
    data object Achievements : NavRoute("achievements")
    data object Subscription : NavRoute("subscription")
    data object WritingTargeted : NavRoute("game/writing/{kanjiId}") {
        fun createRoute(kanjiId: Int) = "game/writing/$kanjiId"
    }
    data object RecognitionTargeted : NavRoute("game/recognition/{kanjiId}") {
        fun createRoute(kanjiId: Int) = "game/recognition/$kanjiId"
    }
    data object VocabularyTargeted : NavRoute("game/vocabulary/{kanjiId}") {
        fun createRoute(kanjiId: Int) = "game/vocabulary/$kanjiId"
    }
    data object CameraTargeted : NavRoute("game/camera/{kanjiId}") {
        fun createRoute(kanjiId: Int) = "game/camera/$kanjiId"
    }
    data object Flashcards : NavRoute("flashcards")
    data object FlashcardStudy : NavRoute("flashcards/study/{deckId}") {
        fun createRoute(deckId: Long) = "flashcards/study/$deckId"
    }
    data object DevChat : NavRoute("devchat")
    data object PlacementTest : NavRoute("placement_test")
    data object WordDetail : NavRoute("word/{wordId}") {
        fun createRoute(wordId: Long) = "word/$wordId"
    }
    data object KanaRecognition : NavRoute("game/kana_recognition/{kanaType}") {
        fun createRoute(kanaType: String) = "game/kana_recognition/$kanaType"
    }
    data object KanaWriting : NavRoute("game/kana_writing/{kanaType}") {
        fun createRoute(kanaType: String) = "game/kana_writing/$kanaType"
    }
    data object RadicalDetail : NavRoute("radical/{radicalId}") {
        fun createRoute(radicalId: Int) = "radical/$radicalId"
    }
    data object RadicalRecognition : NavRoute("game/radical_recognition")
    data object RadicalBuilder : NavRoute("game/radical_builder")
    data object FieldJournal : NavRoute("field_journal")
    data object Collection : NavRoute("collection")
    data object Study : NavRoute("study")
    data object Games : NavRoute("games")
    data object CollectionHub : NavRoute("collection_hub")
    data object TestMode : NavRoute("game/test_mode")
}
