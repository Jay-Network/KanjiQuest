import Foundation
import SharedCore

@MainActor
final class CalligraphySessionViewModel: ObservableObject {

    // MARK: - Published State

    @Published var showResult = false
    @Published var isCorrect = false
    @Published var quality = 0
    @Published var xpGained: Int?
    @Published var isAILoading = false
    @Published var calligraphyFeedback: CalligraphyFeedbackService.CalligraphyFeedback?
    @Published var sessionComplete = false
    @Published var sessionStats: SessionStats?
    @Published var currentKanji: String = ""
    @Published var currentStrokePaths: [String] = []
    @Published var currentGradeMastery: GradeMastery?

    // MARK: - Dependencies

    private var container: AppContainer?
    private var gameEngine: GameEngine?
    private var feedbackService: CalligraphyFeedbackService?
    private var stateObservationTask: Task<Void, Never>?

    private var canvasSize: CGSize = CGSize(width: 400, height: 400)

    func setup(container: AppContainer, kanji: String, strokePaths: [String]) {
        self.container = container
        self.currentKanji = kanji
        self.currentStrokePaths = strokePaths
        self.gameEngine = container.makeGameEngine()
        self.feedbackService = container.makeCalligraphyFeedbackService()
        observeGameState()
    }

    // MARK: - Start Session

    func startSession(questionCount: Int = 10) async {
        guard let gameEngine else { return }
        await gameEngine.onEvent(
            event: GameEvent.StartSession(
                gameMode: GameMode.writing,
                questionCount: Int32(questionCount)
            )
        )
    }

    // MARK: - Submit Drawing

    func submitDrawing(strokes: [[CalligraphyPointData]]) async {
        guard let gameEngine else { return }

        // Path A: Geometric matching via shared-core StrokeMatcher
        let drawnPointLists = strokes.map { stroke in
            stroke.map { SharedCore.Point(x: Float($0.x), y: Float($0.y)) }
        }

        let referencePointLists = currentStrokePaths.map { path in
            SvgPathParser.shared.parseSvgPath(pathData: path)
        }

        let scale = Float(canvasSize.width) / 109.0
        let scaledReference = referencePointLists.map { points in
            points.map { SharedCore.Point(x: $0.x * scale, y: $0.y * scale) }
        }

        let result = StrokeMatcher.shared.validateWriting(
            drawnStrokes: drawnPointLists,
            referenceStrokes: scaledReference
        )

        isCorrect = result.isCorrect
        quality = Int(result.quality)
        showResult = true

        // Submit to GameEngine for SRS update + XP calculation
        let answer = "\(result.isCorrect)|\(result.quality)"
        await gameEngine.onEvent(event: GameEvent.SubmitAnswer(answer: answer))

        // Path B: AI Calligraphy Feedback (async, non-blocking)
        isAILoading = true
        let feedback = await feedbackService?.evaluate(
            strokes: strokes,
            targetKanji: currentKanji,
            strokeCount: currentStrokePaths.count,
            canvasSize: canvasSize
        )
        isAILoading = false

        if let feedback, feedback.isAvailable {
            calligraphyFeedback = feedback
        }
    }

    // MARK: - Next Kanji

    func nextKanji() async {
        reset()
        guard let gameEngine else { return }
        await gameEngine.onEvent(event: GameEvent.NextQuestion())
    }

    // MARK: - End Session

    func endSession() async {
        guard let gameEngine else { return }
        await gameEngine.onEvent(event: GameEvent.EndSession())
    }

    // MARK: - State Observation (SKIE: StateFlow → AsyncSequence)

    private func observeGameState() {
        stateObservationTask?.cancel()
        stateObservationTask = Task { [weak self] in
            guard let gameEngine = self?.gameEngine else { return }
            for await state in gameEngine.state {
                guard !Task.isCancelled else { break }
                await self?.handleGameState(state)
            }
        }
    }

    private func handleGameState(_ state: GameState) async {
        switch state {
        case let showing as GameState.ShowingResult:
            xpGained = Int(showing.xpGained)
        case let awaiting as GameState.AwaitingAnswer:
            currentKanji = awaiting.question.kanjiLiteral
            currentStrokePaths = awaiting.question.strokePaths as? [String] ?? []
        case let complete as GameState.SessionComplete:
            sessionComplete = true
            sessionStats = complete.stats
            await loadGradeMastery()
        case is GameState.Error:
            break
        default:
            break
        }
    }

    /// Load current grade mastery for the kanji being practiced
    private func loadGradeMastery() async {
        guard let container else { return }
        do {
            let total = try await container.kanjiRepository.getKanjiCountByGrade(grade: 1)
            let mastery = try await container.srsRepository.getGradeMastery(grade: 1, totalKanjiInGrade: total)
            currentGradeMastery = mastery
        } catch {}
    }

    func reset() {
        showResult = false
        isCorrect = false
        quality = 0
        xpGained = nil
        calligraphyFeedback = nil
        isAILoading = false
    }

    func updateCanvasSize(_ size: CGSize) {
        canvasSize = size
    }

    deinit {
        stateObservationTask?.cancel()
    }
}
