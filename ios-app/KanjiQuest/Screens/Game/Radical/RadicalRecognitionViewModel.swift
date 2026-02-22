import Foundation
import SharedCore

@MainActor
class RadicalRecognitionViewModel: ObservableObject {
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

    @Published var sessionResult: SessionResultData? = nil

    private var gameEngine: GameEngine?
    private var completeSessionUseCase: CompleteSessionUseCase?

    func start(container: AppContainer) {
        let engine = container.makeGameEngine()
        self.gameEngine = engine
        self.completeSessionUseCase = container.makeCompleteSessionUseCase()

        var sessionLength = UserDefaults.standard.integer(forKey: "session_length")
        if sessionLength == 0 { sessionLength = 10 }
        totalQuestions = sessionLength

        Task {
            do {
                try await engine.startSession(mode: .radicalRecognition, questionCount: Int32(sessionLength))
                await loadQuestion()
            } catch {
                errorMessage = "Failed to start session: \(error.localizedDescription)"
            }
        }
    }

    func submitAnswer(_ answer: String) {
        guard let engine = gameEngine else { return }
        selectedAnswer = answer

        Task {
            let correct = try await engine.submitAnswer(answer: answer)
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
        }
    }

    func next() {
        Task { await loadQuestion() }
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
    }

    private func loadQuestion() async {
        guard let engine = gameEngine else { return }
        selectedAnswer = nil
        isCorrect = nil
        showResult = false
        xpGained = 0

        if let question = try? await engine.nextQuestion() {
            currentQuestion = question
            questionNumber += 1
        } else {
            await endSession()
        }
    }

    private func endSession() async {
        guard let engine = gameEngine else { return }
        let stats = try? await engine.endSession()
        sessionXp = Int(stats?.xpEarned ?? Int32(sessionXp))
        sessionComplete = true

        if let stats = stats {
            let result = try? await completeSessionUseCase?.execute(stats: stats)
            if let result = result {
                sessionResult = SessionResultData(from: result)
            }
        }
    }
}
