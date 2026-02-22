import Foundation
import SharedCore

// MARK: - GameEngine convenience methods
// The KMP GameEngine uses event-driven pattern: onEvent() + state Flow.
// These extensions provide the convenience API that ViewModels expect.

extension GameEngine {

    /// Start a standard session with a game mode and question count.
    func startSession(mode: GameMode, questionCount: Int32) async throws {
        try await onEvent(event: GameEvent.StartSession(
            gameMode: mode,
            questionCount: questionCount,
            targetKanjiId: nil,
            kanaType: nil
        ))
    }

    /// Start a targeted session for a specific kanji.
    func startTargetedSession(mode: GameMode, targetKanjiId: Int32) async throws {
        try await onEvent(event: GameEvent.StartSession(
            gameMode: mode,
            questionCount: 10,
            targetKanjiId: KotlinInt(int: targetKanjiId),
            kanaType: nil
        ))
    }

    /// Start a kana session with a specific kana type.
    func startKanaSession(kanaType: KanaType, questionCount: Int32) async throws {
        try await onEvent(event: GameEvent.StartSession(
            gameMode: GameMode.kanaRecognition,
            questionCount: questionCount,
            targetKanjiId: nil,
            kanaType: kanaType
        ))
    }

    /// Submit an answer string. Returns true if the state transitions to ShowingResult with isCorrect=true.
    @discardableResult
    func submitAnswer(answer: String) async throws -> Bool {
        try await onEvent(event: GameEvent.SubmitAnswer(answer: answer))
        if let result = state.value as? GameState.ShowingResult {
            return result.isCorrect
        }
        return false
    }

    /// Advance to the next question.
    func nextQuestion() async throws -> Question? {
        try await onEvent(event: GameEvent.NextQuestion())
        if let awaiting = state.value as? GameState.AwaitingAnswer {
            return awaiting.question
        }
        return nil
    }

    /// End the session and return stats.
    func endSession() async throws -> SessionStats? {
        try await onEvent(event: GameEvent.EndSession())
        if let complete = state.value as? GameState.SessionComplete {
            return complete.stats
        }
        return nil
    }

    /// Get the current question from the state, if available.
    var currentQuestion: Question? {
        if let awaiting = state.value as? GameState.AwaitingAnswer {
            return awaiting.question
        }
        return nil
    }

    /// Get the last discovered item from the most recent result state.
    var lastDiscoveredItem: CollectedItem? {
        if let result = state.value as? GameState.ShowingResult {
            return result.discoveredItem
        }
        return nil
    }

    /// Get the XP gained from the most recent result.
    var lastXpGained: Int32 {
        if let result = state.value as? GameState.ShowingResult {
            return result.xpGained
        }
        return 0
    }

    /// Get current session XP from the state.
    var currentSessionXp: Int32 {
        if let result = state.value as? GameState.ShowingResult {
            return result.sessionXp
        }
        if let awaiting = state.value as? GameState.AwaitingAnswer {
            return awaiting.sessionXp
        }
        return 0
    }
}

// MARK: - Rarity typealias (KMP uses `Rarity`, not `CollectedItemRarity`)
// If CollectedItemRarity doesn't exist in SharedCore, use this:
typealias CollectedItemRarity = Rarity

// MARK: - KotlinInt convenience
extension KotlinInt {
    var intValue: Int { Int(int32Value) }
}

// MARK: - Int64 / KotlinLong bridging
extension Int64 {
    var kotlinLong: KotlinLong { KotlinLong(value: self) }
}

extension KotlinLong {
    var int64Value: Int64 { Int64(truncating: self) }
}
