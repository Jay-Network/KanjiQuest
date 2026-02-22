import Foundation
import SharedCore

@MainActor
class VocabularyViewModel: ObservableObject {
    @Published var currentQuestion: Question? = nil
    @Published var selectedAnswer: String? = nil
    @Published var isCorrect: Bool? = nil
    @Published var showResult: Bool = false
    @Published var sessionComplete: Bool = false
    @Published var errorMessage: String? = nil

    @Published var questionNumber: Int = 0
    @Published var totalQuestions: Int = 10
    @Published var correctCount: Int = 0
    @Published var currentCombo: Int = 0
    @Published var comboMax: Int = 0
    @Published var sessionXp: Int = 0
    @Published var xpGained: Int = 0
    @Published var durationSec: Int = 0

    @Published var sessionResult: SessionResultData? = nil

    @Published var discoveredItem: CollectedItem? = nil
    @Published var showDiscovery: Bool = false

    private var gameEngine: GameEngine?
    private var completeSessionUseCase: CompleteSessionUseCase?
    private var startTime: Date?

    func start(container: AppContainer, targetKanjiId: Int32? = nil, questionCount: Int? = nil) {
        let engine = container.makeGameEngine()
        self.gameEngine = engine
        self.completeSessionUseCase = container.makeCompleteSessionUseCase()

        let count = questionCount ?? UserDefaults.standard.integer(forKey: "session_length").clamped(to: 5...30, default: 10)
        totalQuestions = count
        startTime = Date()

        Task {
            do {
                if let targetId = targetKanjiId {
                    engine.startTargetedSession(mode: .vocabulary, targetKanjiId: targetId)
                } else {
                    engine.startSession(mode: .vocabulary, questionCount: Int32(count))
                }
                loadQuestion()
            } catch {
                errorMessage = "Failed to start session: \(error.localizedDescription)"
            }
        }
    }

    func submitAnswer(_ answer: String) {
        guard let engine = gameEngine, let question = currentQuestion else { return }
        selectedAnswer = answer
        let correct = engine.submitAnswer(answer: answer)
        isCorrect = correct
        showResult = true

        if correct {
            correctCount += 1
            currentCombo += 1
            if currentCombo > comboMax { comboMax = currentCombo }
            let baseXp = 10
            let comboMultiplier = min(currentCombo, 5)
            xpGained = baseXp + (comboMultiplier - 1) * 2
            sessionXp += xpGained
        } else {
            xpGained = 0
            currentCombo = 0
        }

        if let discovered = engine.lastDiscoveredItem {
            discoveredItem = discovered
            showDiscovery = true
        }
    }

    func next() {
        loadQuestion()
    }

    func reset() {
        gameEngine?.reset()
        currentQuestion = nil
        selectedAnswer = nil
        isCorrect = nil
        showResult = false
        sessionComplete = false
        errorMessage = nil
        questionNumber = 0
        correctCount = 0
        currentCombo = 0
        comboMax = 0
        sessionXp = 0
        xpGained = 0
        sessionResult = nil
        discoveredItem = nil
        showDiscovery = false
    }

    private func loadQuestion() {
        guard let engine = gameEngine else { return }
        selectedAnswer = nil
        isCorrect = nil
        showResult = false
        xpGained = 0

        if let question = engine.nextQuestion() {
            currentQuestion = question
            questionNumber += 1
        } else {
            endSession()
        }
    }

    private func endSession() {
        guard let engine = gameEngine else { return }
        let result = engine.endSession()
        sessionXp = Int(result?.xpEarned ?? Int32(sessionXp))
        durationSec = Int(Date().timeIntervalSince(startTime ?? Date()))
        sessionComplete = true

        Task {
            if let result = result {
                try? await completeSessionUseCase?.execute(result: result)
            }
        }
    }
}

private extension Int {
    func clamped(to range: ClosedRange<Int>, default defaultValue: Int) -> Int {
        self == 0 ? defaultValue : min(max(self, range.lowerBound), range.upperBound)
    }
}
