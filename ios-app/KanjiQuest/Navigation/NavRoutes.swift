import Foundation

/// All navigation routes matching the Android NavRoutes sealed class.
/// Uses associated values for parameterized routes.
enum NavRoute: Hashable {
    // Core
    case splash
    case login
    case home
    case placementTest

    // Kanji detail
    case kanjiDetail(kanjiId: Int32)

    // Game modes
    case recognition
    case recognitionTargeted(kanjiId: Int32)
    case writing
    case writingTargeted(kanjiId: Int32)
    case vocabulary
    case vocabularyTargeted(kanjiId: Int32)
    case camera
    case cameraTargeted(kanjiId: Int32)

    // Kana modes
    case kanaRecognition(kanaType: String)
    case kanaWriting(kanaType: String)

    // Radical modes
    case radicalRecognition
    case radicalBuilder
    case radicalDetail(radicalId: Int32)

    // Collection & Flashcards
    case collection
    case flashcards
    case flashcardStudy(deckId: Int64)

    // Progress & Achievements
    case progress
    case achievements

    // Shop & Subscription
    case shop
    case subscription

    // Settings
    case settings

    // Word detail
    case wordDetail(wordId: Int64)

    // Test Mode (Games hub)
    case testMode

    // Dev & Feedback
    case devChat
    case fieldJournal

    // iPad-only calligraphy
    #if IPAD_TARGET
    case calligraphySession(kanjiLiteral: String, strokePaths: [String])
    #endif
}
