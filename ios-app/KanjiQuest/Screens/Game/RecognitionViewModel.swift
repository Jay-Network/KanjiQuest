import Foundation
import SharedCore

/// Session result data tracked locally since KMP bridge may not expose all fields.
struct SessionResultData {
    var coinsEarned: Int = 0
    var leveledUp: Bool = false
    var newLevel: Int = 0
    var streakIncreased: Bool = false
    var currentStreak: Int = 0
    var adaptiveMessage: String? = nil
}

@MainActor
class RecognitionViewModel: ObservableObject {
    // Question state
    @Published var currentQuestion: Question? = nil
    @Published var selectedAnswer: String? = nil
    @Published var isCorrect: Bool? = nil
    @Published var showResult: Bool = false
    @Published var sessionComplete: Bool = false
    @Published var errorMessage: String? = nil

    // Progress & stats
    @Published var questionNumber: Int = 0
    @Published var totalQuestions: Int = 10
    @Published var correctCount: Int = 0
    @Published var currentCombo: Int = 0
    @Published var comboMax: Int = 0
    @Published var sessionXp: Int = 0
    @Published var xpGained: Int = 0
    @Published var durationSec: Int = 0

    // Session result
    @Published var sessionResult: SessionResultData? = nil
    @Published var newDiscoveries: [(literal: String, meaning: String)] = []

    // Discovery overlay
    @Published var discoveredItem: CollectedItem? = nil
    @Published var discoveredKanjiLiteral: String? = nil
    @Published var showDiscovery: Bool = false

    private var gameEngine: GameEngine?
    private var completeSessionUseCase: CompleteSessionUseCase?
    private var startTime: Date?
    private var allDiscoveries: [(literal: String, meaning: String)] = []

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
                    try await engine.startTargetedSession(mode: .recognition, targetKanjiId: targetId)
                } else {
                    try await engine.startSession(mode: .recognition, questionCount: Int32(count))
                }
                await loadQuestion()
            } catch {
                errorMessage = "Failed to start session: \(error.localizedDescription)"
            }
        }
    }

    func submitAnswer(_ answer: String) {
        guard let engine = gameEngine, let question = currentQuestion else { return }
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
                let earned = baseXp + (comboMultiplier - 1) * 2
                xpGained = earned
                sessionXp += earned
            } else {
                xpGained = 0
                currentCombo = 0
            }

            // Check for discovered collection item
            if let discovered = engine.lastDiscoveredItem {
                discoveredItem = discovered
                discoveredKanjiLiteral = question.kanjiLiteral
                showDiscovery = true
                allDiscoveries.append((literal: question.kanjiLiteral, meaning: question.kanjiMeaning ?? question.kanjiLiteral))
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
        newDiscoveries = []
        allDiscoveries = []
        discoveredItem = nil
        showDiscovery = false
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
        durationSec = Int(Date().timeIntervalSince(startTime ?? Date()))
        newDiscoveries = allDiscoveries
        sessionComplete = true

        if let stats = stats {
            let result = try? await completeSessionUseCase?.execute(stats: stats)
            if let result = result {
                sessionResult = SessionResultData(from: result)
            }
        }
    }
}

private extension Int {
    func clamped(to range: ClosedRange<Int>, default defaultValue: Int) -> Int {
        self == 0 ? defaultValue : Swift.min(Swift.max(self, range.lowerBound), range.upperBound)
    }
}
