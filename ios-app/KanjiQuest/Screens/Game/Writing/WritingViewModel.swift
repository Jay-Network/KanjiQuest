import Foundation
import SharedCore
import UIKit

enum WritingDifficulty: String, CaseIterable {
    case guided = "Guided"
    case noOrder = "Shape Only"
    case blank = "From Memory"
}

@MainActor
class WritingViewModel: ObservableObject {
    // Game state
    @Published var gameState: GameStateWrapper = .idle
    @Published var sessionResult: SessionResultData? = nil

    // Drawing state
    @Published var completedStrokes: [[CGPoint]] = []
    @Published var activeStroke: [CGPoint] = []
    @Published var canvasSize: CGFloat = 0

    // AI feedback
    @Published var aiFeedback: HandwritingFeedback? = nil
    @Published var aiLoading: Bool = false
    @Published var aiEnabled: Bool = true
    @Published var aiFeedbackLanguage: String = "en"
    @Published var aiReportSubmitted: Bool = false
    @Published var analyzedImageBase64: String? = nil

    // Difficulty
    @Published var writingDifficulty: WritingDifficulty = .guided
    @Published var isAdmin: Bool = false
    @Published var adminDifficultyOverride: WritingDifficulty? = nil

    var effectiveDifficulty: WritingDifficulty {
        adminDifficultyOverride ?? writingDifficulty
    }

    private var gameEngine: GameEngine?
    private var completeSessionUseCase: CompleteSessionUseCase?
    private var handwritingChecker: HandwritingChecker?
    private var aiFeedbackReporter: AiFeedbackReporter?
    private var userSessionProvider: UserSessionProviderImpl?

    func start(container: AppContainer, targetKanjiId: Int32? = nil) {
        let engine = container.makeGameEngine()
        self.gameEngine = engine
        self.completeSessionUseCase = container.makeCompleteSessionUseCase()
        self.handwritingChecker = container.makeHandwritingChecker()
        self.aiFeedbackReporter = AiFeedbackReporter()
        self.userSessionProvider = container.userSessionProvider
        self.isAdmin = container.userSessionProvider.isAdmin()

        // Auto-start for targeted kanji
        if let targetId = targetKanjiId {
            gameState = .preparing
            Task {
                do {
                    try await engine.onEvent(event: GameEvent.StartSession(
                        gameMode: GameMode.writing,
                        questionCount: 5,
                        targetKanjiId: KotlinInt(int: targetId),
                        kanaType: nil
                    ))
                    observeState(engine)
                } catch {
                    gameState = .error(error.localizedDescription)
                }
            }
        }
    }

    func startGame(questionCount: Int = 10) {
        guard let engine = gameEngine else { return }
        gameState = .preparing

        Task {
            do {
                try await engine.onEvent(event: GameEvent.StartSession(
                    gameMode: GameMode.writing,
                    questionCount: Int32(questionCount),
                    targetKanjiId: nil,
                    kanaType: nil
                ))
                observeState(engine)
            } catch {
                gameState = .error(error.localizedDescription)
            }
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
        if let awaiting = state as? GameState.AwaitingAnswer {
            gameState = .awaitingAnswer(GameQuestionState(from: awaiting))
            // Clear drawing state for new question
            completedStrokes = []
            activeStroke = []
            aiFeedback = nil
            aiLoading = false
            aiReportSubmitted = false
            analyzedImageBase64 = nil

            // Auto-select difficulty based on SRS
            let srsState = awaiting.question.srsState
            let playerLevel = userSessionProvider?.getAdminPlayerLevelOverride()?.int32Value ?? 1
            if srsState == "graduated" {
                writingDifficulty = .blank
            } else if srsState == "review" {
                writingDifficulty = .noOrder
            } else if playerLevel >= 10 && srsState == "learning" {
                writingDifficulty = .noOrder
            } else {
                writingDifficulty = .guided
            }

        } else if let result = state as? GameState.ShowingResult {
            gameState = .showingResult(GameResultState(from: result))
            if result.isCorrect {
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
            } else {
                UINotificationFeedbackGenerator().notificationOccurred(.error)
            }

        } else if let complete = state as? GameState.SessionComplete {
            gameState = .sessionComplete(GameSessionStats(from: complete.stats))
            UINotificationFeedbackGenerator().notificationOccurred(.success)
            Task {
                if let useCase = completeSessionUseCase {
                    let result = try? await useCase.execute(stats: complete.stats)
                    await MainActor.run {
                        if let result = result {
                            self.sessionResult = SessionResultData(from: result)
                        }
                    }
                }
            }

        } else if let error = state as? GameState.Error {
            gameState = .error(error.message)
        }
    }

    // MARK: - Drawing

    func onCanvasSizeChanged(_ size: CGFloat) {
        canvasSize = size
    }

    func onDragStart(_ point: CGPoint) {
        activeStroke = [point]
    }

    func onDrag(_ point: CGPoint) {
        activeStroke.append(point)
    }

    func onDragEnd() {
        if activeStroke.count >= 2 {
            completedStrokes.append(activeStroke)
        }
        activeStroke = []
    }

    func undoLastStroke() {
        if !completedStrokes.isEmpty {
            completedStrokes.removeLast()
        }
    }

    func clearStrokes() {
        completedStrokes = []
        activeStroke = []
    }

    // MARK: - AI Settings

    func setAiEnabled(_ enabled: Bool) { aiEnabled = enabled }
    func setAiFeedbackLanguage(_ lang: String) { aiFeedbackLanguage = lang }
    func setAdminDifficultyOverride(_ diff: WritingDifficulty?) { adminDifficultyOverride = diff }

    func reportAiFeedback() {
        guard let feedback = aiFeedback else { return }
        let kanji: String
        if case .showingResult(let result) = gameState {
            kanji = result.question.kanjiLiteral
        } else {
            kanji = "unknown"
        }
        aiFeedbackReporter?.reportFeedback(
            kanjiLiteral: kanji,
            isCorrect: feedback.isAvailable,
            confidence: Double(feedback.qualityRating) / 5.0,
            feedback: feedback.overallComment
        )
        aiReportSubmitted = true
    }

    // MARK: - Submit Drawing

    func submitDrawing() {
        guard case .awaitingAnswer(let q) = gameState else { return }

        let drawnStrokes = completedStrokes
        let strokePaths = q.strokePaths
        let canvasSize = self.canvasSize

        // Convert and match strokes using shared-core
        let drawnPoints = drawnStrokes.map { stroke in
            stroke.map { p in SharedCore.Point(x: Float(p.x), y: Float(p.y)) }
        }
        let referenceStrokes = strokePaths.map { pathData -> [SharedCore.Point] in
            let rawPoints = SvgPathParser.shared.parseSvgPath(pathData: pathData)
            if canvasSize > 0 {
                let scale = Float(canvasSize) / 109.0
                return rawPoints.map { p in SharedCore.Point(x: p.x * scale, y: p.y * scale) }
            }
            return rawPoints
        }

        let srsState = q.srsState ?? ""
        let result = StrokeMatcher.shared.validateWriting(
            drawnStrokes: drawnPoints,
            referenceStrokes: referenceStrokes,
            srsState: srsState
        )

        let answer = "\(result.isCorrect)|\(result.quality)"

        // Render color-coded image immediately
        analyzedImageBase64 = WritingStrokeRenderer.renderToBase64(
            drawnStrokes: drawnStrokes,
            canvasSize: canvasSize
        )

        Task {
            try? await gameEngine?.onEvent(event: GameEvent.SubmitAnswer(answer: answer))
        }

        // Fire-and-forget AI check
        if aiEnabled {
            aiLoading = true
            Task {
                let feedback = await handwritingChecker?.evaluate(
                    drawnStrokes: drawnStrokes,
                    targetKanji: q.kanjiLiteral,
                    strokeCount: strokePaths.count,
                    canvasSize: canvasSize,
                    language: aiFeedbackLanguage
                ) ?? .unavailable()
                await MainActor.run {
                    self.aiFeedback = feedback
                    self.aiLoading = false
                }
            }
        }
    }

    func nextQuestion() {
        Task { try? await gameEngine?.onEvent(event: GameEvent.NextQuestion()) }
    }

    func reset() {
        gameEngine?.reset()
        gameState = .idle
        sessionResult = nil
        completedStrokes = []
        activeStroke = []
        aiFeedback = nil
        aiLoading = false
        analyzedImageBase64 = nil
    }
}
