import Foundation
import SharedCore
import UIKit

@MainActor
class RadicalRecognitionViewModel: ObservableObject {
    @Published var gameState: GameStateWrapper = .idle
    @Published var sessionResult: SessionResultData? = nil

    private var gameEngine: GameEngine?
    private var completeSessionUseCase: CompleteSessionUseCase?

    func start(container: AppContainer) {
        let engine = container.makeGameEngine()
        self.gameEngine = engine
        self.completeSessionUseCase = container.makeCompleteSessionUseCase()

        var sessionLength = UserDefaults.standard.integer(forKey: "session_length")
        if sessionLength == 0 { sessionLength = 10 }
        gameState = .preparing

        Task {
            engine.onEvent(event: GameEvent.StartSession(
                gameMode: GameMode.radicalRecognition,
                questionCount: Int32(sessionLength)
            ))
            observeState(engine)
        }
    }

    private func observeState(_ engine: GameEngine) {
        Task {
            for await state in engine.state {
                await MainActor.run { self.updateState(state) }
            }
        }
    }

    private func updateState(_ state: GameState) {
        switch state {
        case let awaiting as GameState.AwaitingAnswer:
            gameState = .awaitingAnswer(GameQuestionState(from: awaiting))
        case let result as GameState.ShowingResult:
            gameState = .showingResult(GameResultState(from: result))
            if result.isCorrect {
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
            } else {
                UINotificationFeedbackGenerator().notificationOccurred(.error)
            }
        case let complete as GameState.SessionComplete:
            gameState = .sessionComplete(GameSessionStats(from: complete.stats))
            UINotificationFeedbackGenerator().notificationOccurred(.success)
            Task {
                if let useCase = completeSessionUseCase {
                    let result = try? await useCase.execute(stats: complete.stats)
                    await MainActor.run {
                        self.sessionResult = result.map { SessionResultData(from: $0) }
                    }
                }
            }
        case let error as GameState.Error:
            gameState = .error(error.message)
        default: break
        }
    }

    func submitAnswer(_ answer: String) {
        Task { gameEngine?.onEvent(event: GameEvent.SubmitAnswer(answer: answer)) }
    }

    func nextQuestion() {
        Task { gameEngine?.onEvent(event: GameEvent.NextQuestion()) }
    }

    func reset() {
        gameEngine?.reset()
        gameState = .idle
        sessionResult = nil
    }
}
