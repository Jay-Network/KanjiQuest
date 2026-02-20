import Foundation
import SwiftUI
import SharedCore

/// UI state for the Recognition game mode.
@MainActor
final class RecognitionViewModel: ObservableObject {

    // MARK: - Published State

    @Published private(set) var gameState: GameState = IdleState()
    @Published private(set) var isLoading = false

    // MARK: - Dependencies

    private var gameEngine: GameEngine?
    private var stateObservationTask: Task<Void, Never>?

    // MARK: - Initialization

    func setup(container: AppContainer) {
        guard gameEngine == nil else { return }
        gameEngine = container.makeGameEngine()
        observeGameState()
    }

    deinit {
        stateObservationTask?.cancel()
    }

    // MARK: - State Observation (SKIE: StateFlow → AsyncSequence)

    private func observeGameState() {
        stateObservationTask?.cancel()
        stateObservationTask = Task { [weak self] in
            guard let gameEngine = self?.gameEngine else { return }
            for await state in gameEngine.state {
                guard !Task.isCancelled else { break }
                await MainActor.run {
                    self?.gameState = state
                }
            }
        }
    }

    // MARK: - Game Actions

    func startGame(questionCount: Int = 10, targetKanjiId: Int32? = nil) async {
        guard let gameEngine else { return }
        isLoading = true
        await gameEngine.onEvent(
            event: GameEvent.StartSession(
                gameMode: GameMode.recognition,
                questionCount: Int32(questionCount),
                targetKanjiId: targetKanjiId.map { KotlinInt(value: $0) }
            )
        )
        isLoading = false
    }

    func submitAnswer(_ answer: String) async {
        guard let gameEngine else { return }
        await gameEngine.onEvent(event: GameEvent.SubmitAnswer(answer: answer))
    }

    func nextQuestion() async {
        guard let gameEngine else { return }
        await gameEngine.onEvent(event: GameEvent.NextQuestion())
    }

    func endSession() async {
        guard let gameEngine else { return }
        await gameEngine.onEvent(event: GameEvent.EndSession())
    }

    func reset() {
        gameEngine?.reset()
        gameState = IdleState()
    }
}

// MARK: - Type alias for cleaner GameState init
// SKIE transforms Kotlin sealed class to Swift-accessible types

private typealias IdleState = GameState.Idle
