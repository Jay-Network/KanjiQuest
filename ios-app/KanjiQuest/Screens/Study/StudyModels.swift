import Foundation
import SharedCore

/// Content tabs matching Android's ContentTab enum.
enum ContentTab: String, CaseIterable {
    case kana = "Kana"
    case radicals = "Radicals"
    case kanji = "Kanji"
}

/// Study source matching Android's StudySource sealed class.
enum StudySource: Equatable {
    case all
    case fromFlashcardDeck(deckId: Int64, deckName: String)
    case fromCollection

    static func == (lhs: StudySource, rhs: StudySource) -> Bool {
        switch (lhs, rhs) {
        case (.all, .all): return true
        case (.fromCollection, .fromCollection): return true
        case (.fromFlashcardDeck(let a, _), .fromFlashcardDeck(let b, _)): return a == b
        default: return false
        }
    }
}

/// Kana filter matching Android's KanaFilter enum.
enum KanaFilter: String, CaseIterable {
    case hiraganaOnly = "Hiragana Only"
    case katakanaOnly = "Katakana Only"
    case mixed = "Mixed"
}

/// Study mode info for display.
struct StudyModeInfo {
    let mode: GameModeEnum
    let label: String
    let color: UInt
    let imageAsset: String?
}

/// Maps GameMode to display info (matches Android getModeInfo).
enum GameModeEnum: String, CaseIterable {
    case recognition
    case writing
    case vocabulary
    case cameraChallenge
    case kanaRecognition
    case kanaWriting
    case radicalRecognition
    case radicalBuilder

    var info: StudyModeInfo {
        switch self {
        case .recognition: return StudyModeInfo(mode: self, label: "Recognition", color: 0x2196F3, imageAsset: "mode-recognition.png")
        case .writing: return StudyModeInfo(mode: self, label: "Writing", color: 0x4CAF50, imageAsset: "mode-writing.png")
        case .vocabulary: return StudyModeInfo(mode: self, label: "Vocabulary", color: 0xFF9800, imageAsset: "mode-vocabulary.png")
        case .cameraChallenge: return StudyModeInfo(mode: self, label: "Camera", color: 0x9C27B0, imageAsset: "mode-camera.png")
        case .kanaRecognition: return StudyModeInfo(mode: self, label: "Recognition", color: 0xE91E63, imageAsset: "mode-kana-recognition.png")
        case .kanaWriting: return StudyModeInfo(mode: self, label: "Writing", color: 0x00BCD4, imageAsset: "mode-kana-writing.png")
        case .radicalRecognition: return StudyModeInfo(mode: self, label: "Recognition", color: 0x795548, imageAsset: "mode-radical-recognition.png")
        case .radicalBuilder: return StudyModeInfo(mode: self, label: "Builder", color: 0x795548, imageAsset: "mode-radical-builder.png")
        }
    }

    /// Returns the modes available for a given content tab.
    static func modesForTab(_ tab: ContentTab) -> [GameModeEnum] {
        switch tab {
        case .kana: return [.kanaRecognition, .kanaWriting]
        case .radicals: return [.radicalRecognition, .radicalBuilder]
        case .kanji: return [.recognition, .writing, .vocabulary, .cameraChallenge]
        }
    }

    /// Whether this mode is premium-gated.
    var isPremiumGated: Bool {
        switch self {
        case .writing, .vocabulary, .cameraChallenge, .radicalBuilder: return true
        default: return false
        }
    }

    /// Preview trial key for this mode.
    var previewTrialKey: String? {
        switch self {
        case .writing: return "WRITING"
        case .vocabulary: return "VOCABULARY"
        case .cameraChallenge: return "CAMERA_CHALLENGE"
        case .radicalBuilder: return "RADICAL_BUILDER"
        default: return nil
        }
    }
}
