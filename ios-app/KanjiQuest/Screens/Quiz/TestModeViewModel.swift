import Foundation

/// Test phase matching Android's TestPhase enum.
enum TestPhase {
    case scopeSelection
    case inProgress
    case results
}

/// UI state matching Android's TestModeUiState.
struct TestModeUiState {
    var phase: TestPhase = .scopeSelection
    var isLoading: Bool = false
    var selectedScope: TestScope? = nil
    var questions: [QuizQuestion] = []
    var currentIndex: Int = 0
    var selectedAnswer: Int? = nil
    var isCorrect: Bool? = nil
    var correctCount: Int = 0
    var elapsedSeconds: Int = 0
    var answers: [Bool] = []

    var currentQuestion: QuizQuestion? {
        guard currentIndex < questions.count else { return nil }
        return questions[currentIndex]
    }
    var totalQuestions: Int { questions.count }
    var accuracy: Float {
        answers.isEmpty ? 0 : Float(correctCount) / Float(answers.count)
    }
}

/// ViewModel matching Android's TestModeViewModel.
@MainActor
class TestModeViewModel: ObservableObject {
    @Published var uiState = TestModeUiState()

    private let questionGenerator: QuizQuestionGenerator
    private var timerTask: Task<Void, Never>?
    private var startTime: Date?

    init(container: AppContainer) {
        self.questionGenerator = QuizQuestionGenerator(container: container)
    }

    func selectScope(_ scope: TestScope) {
        uiState.selectedScope = scope
        uiState.isLoading = true

        Task {
            let questions = await questionGenerator.generateQuestions(scope: scope, count: 10)
            if questions.isEmpty {
                uiState.isLoading = false
                return
            }
            startTime = Date()
            startTimer()
            uiState = TestModeUiState(
                phase: .inProgress,
                selectedScope: scope,
                questions: questions
            )
        }
    }

    func selectAnswer(_ index: Int) {
        guard uiState.selectedAnswer == nil else { return }
        guard let question = uiState.currentQuestion else { return }

        let isCorrect = index == question.correctIndex
        uiState.selectedAnswer = index
        uiState.isCorrect = isCorrect
        uiState.correctCount += isCorrect ? 1 : 0
        uiState.answers.append(isCorrect)
    }

    func nextQuestion() {
        let nextIndex = uiState.currentIndex + 1
        if nextIndex >= uiState.totalQuestions {
            timerTask?.cancel()
            uiState.phase = .results
        } else {
            uiState.currentIndex = nextIndex
            uiState.selectedAnswer = nil
            uiState.isCorrect = nil
        }
    }

    func resetToScopeSelection() {
        timerTask?.cancel()
        uiState = TestModeUiState()
    }

    func retakeTest() {
        guard let scope = uiState.selectedScope else { return }
        selectScope(scope)
    }

    private func startTimer() {
        timerTask?.cancel()
        timerTask = Task {
            while !Task.isCancelled {
                if let start = startTime {
                    uiState.elapsedSeconds = Int(Date().timeIntervalSince(start))
                }
                try? await Task.sleep(nanoseconds: 1_000_000_000)
            }
        }
    }
}
