import Foundation
import SharedCore
import SwiftUI

// NOTE: GameStateWrapper, GameQuestionState, GameResultState, GameSessionStats,
// and SessionResultData extension are defined in GameTypes.swift.
// HandwritingFeedback is in HandwritingChecker.swift.
// WritingStrokeRenderer is in Writing/WritingStrokeRenderer.swift.
// AiFeedbackReporter is in Writing/AiFeedbackReporter.swift.
// WritingDrawingCanvas is in Writing/DrawingCanvas.swift.

// NOTE: RadicalImage is defined in Components/AssetImage.swift

// MARK: - UserProfile convenience extensions

extension UserProfile {
    var xpProgress: Float {
        let currentLevelXp = level * level * 50
        let nextLevelXp = (level + 1) * (level + 1) * 50
        let range = nextLevelXp - currentLevelXp
        guard range > 0 else { return 0 }
        let progress = totalXp - currentLevelXp
        return Float(progress) / Float(range)
    }
}

// MARK: - CoinBalance convenience extensions

extension CoinBalance {
    var displayBalance: Int64 { balance }
    var needsSync: Bool { false }
}

// MARK: - Rarity color extension

extension Rarity {
    var colorValue: UInt {
        switch self {
        case .common: return 0x9E9E9E
        case .uncommon: return 0x4CAF50
        case .rare: return 0x2196F3
        case .epic: return 0x9C27B0
        case .legendary: return 0xFFD700
        default: return 0x9E9E9E
        }
    }
}

// MARK: - GradeMastery convenience

extension GradeMastery {
    var masteryScore: Float { Float(score) }
}

// MARK: - MasteryLevel label

extension MasteryLevel {
    var label: String {
        switch self {
        case .beginning: return "Beginning"
        case .developing: return "Developing"
        case .proficient: return "Proficient"
        case .advanced: return "Advanced"
        default: return "Beginning"
        }
    }
}

// MARK: - UserLevel display

extension UserLevel {
    var displayName: String {
        switch self {
        case .free: return "FREE"
        case .premium: return "PREMIUM"
        case .admin: return "ADMIN"
        default: return "FREE"
        }
    }
}

// MARK: - PlacementTestStage display

extension PlacementTestStage {
    var displayName: String {
        switch self {
        case .hiragana: return "Hiragana"
        case .katakana: return "Katakana"
        case .radicals: return "Radicals"
        case .grade1: return "Grade 1"
        case .grade2: return "Grade 2"
        case .grade3: return "Grade 3"
        default: return "\(self)"
        }
    }
}

// MARK: - GameEngine.reset (no-op, engine resets on new session)

extension GameEngine {
    func reset() {
        // GameEngine has no explicit reset method.
        // State returns to idle when a new session is started.
    }
}
