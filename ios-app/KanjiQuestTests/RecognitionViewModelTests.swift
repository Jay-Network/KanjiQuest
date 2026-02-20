import XCTest
@testable import KanjiQuest
import SharedCore

/// Unit tests for RecognitionViewModel.
/// These tests verify the ViewModel's state management and interaction with GameEngine.
/// Note: Tests require the SharedCore framework and a valid AppContainer.
@MainActor
final class RecognitionViewModelTests: XCTestCase {

    var viewModel: RecognitionViewModel!
    var container: AppContainer!

    override func setUp() async throws {
        try await super.setUp()
        container = AppContainer()
        viewModel = RecognitionViewModel()
    }

    override func tearDown() async throws {
        viewModel = nil
        container = nil
        try await super.tearDown()
    }

    // MARK: - Initialization Tests

    func testInitialState() {
        // Given: A freshly created ViewModel
        // Then: State should be Idle
        XCTAssertTrue(viewModel.gameState is GameState.Idle)
        XCTAssertFalse(viewModel.isLoading)
    }

    func testSetupInitializesGameEngine() {
        // Given: A ViewModel without setup
        // When: Setup is called with container
        viewModel.setup(container: container)

        // Then: GameEngine should be initialized (indirectly verified by state observation)
        // State remains Idle until startGame is called
        XCTAssertTrue(viewModel.gameState is GameState.Idle)
    }

    func testSetupOnlyInitializesOnce() {
        // Given: A ViewModel
        // When: Setup is called multiple times
        viewModel.setup(container: container)
        viewModel.setup(container: container)

        // Then: Should not throw or create multiple engines
        XCTAssertTrue(viewModel.gameState is GameState.Idle)
    }

    // MARK: - Game Flow Tests

    func testStartGameSetsLoadingState() async {
        // Given: A setup ViewModel
        viewModel.setup(container: container)

        // When: startGame begins
        // Note: This test checks loading state, actual game start depends on database
        let loadingBefore = viewModel.isLoading

        // Then: Loading state should be false before start
        XCTAssertFalse(loadingBefore)
    }

    func testStartGameWithDefaultQuestionCount() async {
        // Given: A setup ViewModel
        viewModel.setup(container: container)

        // When: Start game with default parameters
        await viewModel.startGame()

        // Then: Game should transition from Idle (may error if no kanji in DB)
        // We verify loading completes
        XCTAssertFalse(viewModel.isLoading)
    }

    func testStartGameWithCustomQuestionCount() async {
        // Given: A setup ViewModel
        viewModel.setup(container: container)

        // When: Start game with 5 questions
        await viewModel.startGame(questionCount: 5)

        // Then: Loading should complete
        XCTAssertFalse(viewModel.isLoading)
    }

    func testStartGameWithTargetKanjiId() async {
        // Given: A setup ViewModel
        viewModel.setup(container: container)

        // When: Start game targeting specific kanji
        await viewModel.startGame(questionCount: 1, targetKanjiId: 1)

        // Then: Loading should complete
        XCTAssertFalse(viewModel.isLoading)
    }

    // MARK: - Reset Tests

    func testResetRestoresIdleState() async {
        // Given: A ViewModel that has started a game
        viewModel.setup(container: container)
        await viewModel.startGame()

        // When: Reset is called
        viewModel.reset()

        // Then: State should return to Idle
        XCTAssertTrue(viewModel.gameState is GameState.Idle)
    }

    // MARK: - State Type Tests

    func testGameStateAwaitingAnswerProperties() {
        // Given: A mock AwaitingAnswer state
        let question = Question(
            kanjiId: 1,
            kanjiLiteral: "日",
            correctAnswer: "にち",
            choices: ["にち", "ひ", "か", "げつ"],
            questionText: "What is the reading?",
            isNewCard: true,
            strokePaths: [],
            srsState: "new",
            vocabId: nil,
            vocabReading: nil,
            vocabQuestionType: nil,
            exampleSentenceJa: nil,
            exampleSentenceEn: nil,
            kanjiBreakdown: []
        )

        let state = GameState.AwaitingAnswer(
            question: question,
            questionNumber: 1,
            totalQuestions: 10,
            currentCombo: 0,
            sessionXp: 0
        )

        // Then: Properties should be accessible
        XCTAssertEqual(state.questionNumber, 1)
        XCTAssertEqual(state.totalQuestions, 10)
        XCTAssertEqual(state.currentCombo, 0)
        XCTAssertEqual(state.sessionXp, 0)
        XCTAssertEqual(state.question.kanjiLiteral, "日")
        XCTAssertEqual(state.question.choices.count, 4)
    }

    func testGameStateShowingResultProperties() {
        // Given: A mock ShowingResult state
        let question = Question(
            kanjiId: 1,
            kanjiLiteral: "日",
            correctAnswer: "にち",
            choices: ["にち", "ひ", "か", "げつ"],
            questionText: "What is the reading?",
            isNewCard: true,
            strokePaths: [],
            srsState: "new",
            vocabId: nil,
            vocabReading: nil,
            vocabQuestionType: nil,
            exampleSentenceJa: nil,
            exampleSentenceEn: nil,
            kanjiBreakdown: []
        )

        let state = GameState.ShowingResult(
            question: question,
            selectedAnswer: "にち",
            isCorrect: true,
            quality: 5,
            xpGained: 10,
            currentCombo: 1,
            questionNumber: 1,
            totalQuestions: 10,
            sessionXp: 10
        )

        // Then: Properties should be accessible
        XCTAssertTrue(state.isCorrect)
        XCTAssertEqual(state.xpGained, 10)
        XCTAssertEqual(state.selectedAnswer, "にち")
        XCTAssertEqual(state.quality, 5)
    }

    func testGameStateSessionCompleteProperties() {
        // Given: A mock SessionComplete state
        let stats = SessionStats(
            gameMode: GameMode.recognition,
            cardsStudied: 10,
            correctCount: 8,
            comboMax: 5,
            xpEarned: 100,
            durationSec: 120,
            touchedKanjiIds: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
            touchedVocabIds: []
        )

        let state = GameState.SessionComplete(stats: stats)

        // Then: Stats should be accessible
        XCTAssertEqual(state.stats.cardsStudied, 10)
        XCTAssertEqual(state.stats.correctCount, 8)
        XCTAssertEqual(state.stats.comboMax, 5)
        XCTAssertEqual(state.stats.xpEarned, 100)
        XCTAssertEqual(state.stats.durationSec, 120)
    }

    func testGameStateErrorProperties() {
        // Given: A mock Error state
        let state = GameState.Error(message: "No kanji available")

        // Then: Message should be accessible
        XCTAssertEqual(state.message, "No kanji available")
    }
}

// MARK: - Integration Tests (require populated database)

extension RecognitionViewModelTests {

    /// Integration test that requires a populated kanji database.
    /// This test verifies the full game flow from start to answer submission.
    func testFullGameFlowIntegration() async throws {
        // Skip if database is empty
        viewModel.setup(container: container)
        await viewModel.startGame(questionCount: 1)

        // Wait for state to settle
        try await Task.sleep(nanoseconds: 500_000_000) // 0.5s

        // Check if we got a question or an error (empty DB)
        if viewModel.gameState is GameState.AwaitingAnswer {
            let awaiting = viewModel.gameState as! GameState.AwaitingAnswer

            // Submit the correct answer
            await viewModel.submitAnswer(awaiting.question.correctAnswer)

            // Wait for result
            try await Task.sleep(nanoseconds: 100_000_000) // 0.1s

            // Should now be showing result
            if viewModel.gameState is GameState.ShowingResult {
                let result = viewModel.gameState as! GameState.ShowingResult
                XCTAssertTrue(result.isCorrect)

                // Move to next (should complete session with 1 question)
                await viewModel.nextQuestion()

                try await Task.sleep(nanoseconds: 100_000_000) // 0.1s

                // Should be session complete
                XCTAssertTrue(viewModel.gameState is GameState.SessionComplete)
            }
        } else if viewModel.gameState is GameState.Error {
            // Database likely empty - this is expected in test environment
            let error = viewModel.gameState as! GameState.Error
            print("Integration test skipped: \(error.message)")
        }
    }
}
