import Foundation
import SharedCore

// MARK: - GameStateWrapper
// Swift enum bridging KMP's GameState sealed class for ViewModels that observe state flow.

enum GameStateWrapper {
    case idle
    case preparing
    case awaitingAnswer(GameQuestionState)
    case showingResult(GameResultState)
    case sessionComplete(GameSessionStats)
    case error(String)
}

// MARK: - GameQuestionState

struct GameQuestionState {
    let question: Question
    let questionNumber: Int32
    let totalQuestions: Int32
    let currentCombo: Int32
    let sessionXp: Int32

    // Convenience accessors
    var kanjiLiteral: String { question.kanjiLiteral }
    var kanjiMeaning: String? { question.kanjiMeaning }
    var correctAnswer: String { question.correctAnswer }
    var questionText: String { question.questionText }
    var choices: [String] { question.choices as? [String] ?? [] }
    var strokePaths: [String] { question.strokePaths as? [String] ?? [] }
    var srsState: String? { question.srsState }

    init(from state: GameState.AwaitingAnswer) {
        self.question = state.question
        self.questionNumber = state.questionNumber
        self.totalQuestions = state.totalQuestions
        self.currentCombo = state.currentCombo
        self.sessionXp = state.sessionXp
    }
}

// MARK: - GameResultState

struct GameResultState {
    let question: Question
    let isCorrect: Bool
    let xpGained: Int32
    let sessionXp: Int32
    let discoveredItems: [CollectedItem]
    let comboCount: Int32

    /// Convenience for code that only needs the first discovered item
    var discoveredItem: CollectedItem? { discoveredItems.first }

    var kanjiLiteral: String { question.kanjiLiteral }
    var correctAnswer: String { question.correctAnswer }

    init(from state: GameState.ShowingResult) {
        self.question = state.question
        self.isCorrect = state.isCorrect
        self.xpGained = state.xpGained
        self.sessionXp = state.sessionXp
        self.discoveredItems = state.discoveredItems as? [CollectedItem] ?? []
        self.comboCount = state.currentCombo
    }
}

// MARK: - GameSessionStats

struct GameSessionStats {
    let stats: SessionStats

    var cardsStudied: Int32 { stats.cardsStudied }
    var correctCount: Int32 { stats.correctCount }
    var comboMax: Int32 { stats.comboMax }
    var xpEarned: Int32 { stats.xpEarned }
    var durationSec: Int32 { stats.durationSec }

    var accuracy: String {
        guard cardsStudied > 0 else { return "0" }
        let pct = Int(Float(correctCount) / Float(cardsStudied) * 100)
        return "\(pct)"
    }

    var formattedDuration: String {
        let m = durationSec / 60
        let s = durationSec % 60
        return String(format: "%d:%02d", m, s)
    }

    init(from stats: SessionStats) {
        self.stats = stats
    }
}

// MARK: - SessionResultData bridging

extension SessionResultData {
    init(from result: SessionResult) {
        self.coinsEarned = Int(result.coinsEarned)
        self.leveledUp = result.leveledUp
        self.newLevel = Int(result.newLevel)
        self.streakIncreased = result.streakIncreased
        self.currentStreak = Int(result.currentStreak)
        self.adaptiveMessage = result.adaptiveMessage
    }
}
