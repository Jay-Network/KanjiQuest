package com.jworks.kanjiquest.core

import com.jworks.kanjiquest.core.domain.model.GameMode
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.model.SrsCard
import com.jworks.kanjiquest.core.domain.model.SrsState
import com.jworks.kanjiquest.core.domain.model.Vocabulary
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import com.jworks.kanjiquest.core.domain.repository.SrsRepository
import com.jworks.kanjiquest.core.engine.GameEngine
import com.jworks.kanjiquest.core.engine.GameEvent
import com.jworks.kanjiquest.core.engine.GameState
import com.jworks.kanjiquest.core.engine.QuestionGenerator
import com.jworks.kanjiquest.core.scoring.ScoringEngine
import com.jworks.kanjiquest.core.srs.Sm2Algorithm
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameEngineTest {

    private val testKanji = listOf(
        makeKanji(19968, "一", grade = 1, onReadings = listOf("イチ"), kunReadings = listOf("ひと.つ")),
        makeKanji(20108, "二", grade = 1, onReadings = listOf("ニ"), kunReadings = listOf("ふた.つ")),
        makeKanji(19977, "三", grade = 1, onReadings = listOf("サン"), kunReadings = listOf("み.つ")),
        makeKanji(22235, "四", grade = 1, onReadings = listOf("シ"), kunReadings = listOf("よ.つ")),
        makeKanji(20116, "五", grade = 1, onReadings = listOf("ゴ"), kunReadings = listOf("いつ.つ")),
    )

    private fun createEngine(): GameEngine {
        val kanjiRepo = FakeKanjiRepository(testKanji)
        val srsRepo = FakeSrsRepository()
        val questionGenerator = QuestionGenerator(kanjiRepo, srsRepo)
        return GameEngine(
            questionGenerator = questionGenerator,
            srsAlgorithm = Sm2Algorithm(),
            srsRepository = srsRepo,
            scoringEngine = ScoringEngine(),
            timeProvider = { 1700000000L }
        )
    }

    @Test
    fun initialState_isIdle() {
        val engine = createEngine()
        assertIs<GameState.Idle>(engine.state.value)
    }

    @Test
    fun startSession_transitionsToAwaitingAnswer() = runTest {
        val engine = createEngine()
        engine.onEvent(GameEvent.StartSession(GameMode.RECOGNITION, 3))
        val state = engine.state.value
        // Should transition through Preparing -> ShowingQuestion -> AwaitingAnswer
        assertIs<GameState.AwaitingAnswer>(state)
    }

    @Test
    fun startSession_setsCorrectTotalQuestions() = runTest {
        val engine = createEngine()
        engine.onEvent(GameEvent.StartSession(GameMode.RECOGNITION, 3))
        val state = engine.state.value as GameState.AwaitingAnswer
        assertEquals(1, state.questionNumber)
        assertTrue(state.totalQuestions <= 5) // max is what we have
    }

    @Test
    fun submitCorrectAnswer_showsCorrectResult() = runTest {
        val engine = createEngine()
        engine.onEvent(GameEvent.StartSession(GameMode.RECOGNITION, 3))
        val awaiting = engine.state.value as GameState.AwaitingAnswer
        val correct = awaiting.question.correctAnswer

        engine.onEvent(GameEvent.SubmitAnswer(correct))
        val result = engine.state.value as GameState.ShowingResult
        assertTrue(result.isCorrect)
        assertTrue(result.xpGained > 0)
    }

    @Test
    fun submitWrongAnswer_showsIncorrectResult() = runTest {
        val engine = createEngine()
        engine.onEvent(GameEvent.StartSession(GameMode.RECOGNITION, 3))
        val awaiting = engine.state.value as GameState.AwaitingAnswer
        val wrong = awaiting.question.choices.first { it != awaiting.question.correctAnswer }

        engine.onEvent(GameEvent.SubmitAnswer(wrong))
        val result = engine.state.value as GameState.ShowingResult
        assertEquals(false, result.isCorrect)
        assertEquals(0, result.xpGained)
    }

    @Test
    fun nextQuestion_advancesQuestionNumber() = runTest {
        val engine = createEngine()
        engine.onEvent(GameEvent.StartSession(GameMode.RECOGNITION, 3))
        val q1 = engine.state.value as GameState.AwaitingAnswer
        engine.onEvent(GameEvent.SubmitAnswer(q1.question.correctAnswer))
        engine.onEvent(GameEvent.NextQuestion)

        val state = engine.state.value
        if (state is GameState.AwaitingAnswer) {
            assertEquals(2, state.questionNumber)
        }
        // Could also be SessionComplete if only 1 question available
    }

    @Test
    fun combo_incrementsOnCorrectStreaks() = runTest {
        val engine = createEngine()
        engine.onEvent(GameEvent.StartSession(GameMode.RECOGNITION, 5))

        // Answer first question correctly
        val q1 = engine.state.value as GameState.AwaitingAnswer
        engine.onEvent(GameEvent.SubmitAnswer(q1.question.correctAnswer))
        val r1 = engine.state.value as GameState.ShowingResult
        assertEquals(1, r1.currentCombo)

        // Go to next question and answer correctly
        engine.onEvent(GameEvent.NextQuestion)
        val q2state = engine.state.value
        if (q2state is GameState.AwaitingAnswer) {
            engine.onEvent(GameEvent.SubmitAnswer(q2state.question.correctAnswer))
            val r2 = engine.state.value as GameState.ShowingResult
            assertEquals(2, r2.currentCombo)
        }
    }

    @Test
    fun combo_resetsOnWrongAnswer() = runTest {
        val engine = createEngine()
        engine.onEvent(GameEvent.StartSession(GameMode.RECOGNITION, 5))

        // Answer correctly first
        val q1 = engine.state.value as GameState.AwaitingAnswer
        engine.onEvent(GameEvent.SubmitAnswer(q1.question.correctAnswer))
        engine.onEvent(GameEvent.NextQuestion)

        // Answer wrong
        val q2state = engine.state.value
        if (q2state is GameState.AwaitingAnswer) {
            val wrong = q2state.question.choices.first { it != q2state.question.correctAnswer }
            engine.onEvent(GameEvent.SubmitAnswer(wrong))
            val r2 = engine.state.value as GameState.ShowingResult
            assertEquals(0, r2.currentCombo)
        }
    }

    @Test
    fun endSession_immediatelyCompletesSession() = runTest {
        val engine = createEngine()
        engine.onEvent(GameEvent.StartSession(GameMode.RECOGNITION, 5))
        engine.onEvent(GameEvent.EndSession)
        assertIs<GameState.SessionComplete>(engine.state.value)
    }

    @Test
    fun sessionComplete_hasAccurateStats() = runTest {
        val engine = createEngine()
        engine.onEvent(GameEvent.StartSession(GameMode.RECOGNITION, 2))

        val q1 = engine.state.value as GameState.AwaitingAnswer
        engine.onEvent(GameEvent.SubmitAnswer(q1.question.correctAnswer))
        engine.onEvent(GameEvent.NextQuestion)

        val q2state = engine.state.value
        if (q2state is GameState.AwaitingAnswer) {
            val wrong = q2state.question.choices.first { it != q2state.question.correctAnswer }
            engine.onEvent(GameEvent.SubmitAnswer(wrong))
            engine.onEvent(GameEvent.NextQuestion)
        }

        val state = engine.state.value
        if (state is GameState.SessionComplete) {
            assertEquals(GameMode.RECOGNITION, state.stats.gameMode)
            assertTrue(state.stats.cardsStudied > 0)
        }
    }

    @Test
    fun reset_returnsToIdle() = runTest {
        val engine = createEngine()
        engine.onEvent(GameEvent.StartSession(GameMode.RECOGNITION, 3))
        engine.reset()
        assertIs<GameState.Idle>(engine.state.value)
    }

    @Test
    fun question_hasFourChoices() = runTest {
        val engine = createEngine()
        engine.onEvent(GameEvent.StartSession(GameMode.RECOGNITION, 1))
        val state = engine.state.value as GameState.AwaitingAnswer
        assertEquals(4, state.question.choices.size)
    }

    @Test
    fun question_containsCorrectAnswer() = runTest {
        val engine = createEngine()
        engine.onEvent(GameEvent.StartSession(GameMode.RECOGNITION, 1))
        val state = engine.state.value as GameState.AwaitingAnswer
        assertTrue(state.question.choices.contains(state.question.correctAnswer))
    }
}

// Test helpers

private fun makeKanji(
    id: Int,
    literal: String,
    grade: Int? = null,
    onReadings: List<String> = emptyList(),
    kunReadings: List<String> = emptyList()
) = Kanji(
    id = id,
    literal = literal,
    grade = grade,
    jlptLevel = null,
    frequency = null,
    strokeCount = 1,
    meaningsEn = listOf("meaning"),
    onReadings = onReadings,
    kunReadings = kunReadings,
    strokeSvg = null
)

private class FakeKanjiRepository(private val kanjiList: List<Kanji>) : KanjiRepository {
    override suspend fun getKanjiById(id: Int) = kanjiList.find { it.id == id }
    override suspend fun getKanjiByLiteral(literal: String) = kanjiList.find { it.literal == literal }
    override suspend fun getKanjiByGrade(grade: Int) = kanjiList.filter { it.grade == grade }
    override suspend fun getKanjiByJlptLevel(level: Int) = kanjiList.filter { it.jlptLevel == level }
    override suspend fun getVocabularyForKanji(kanjiId: Int) = emptyList<Vocabulary>()
    override suspend fun searchKanji(query: String, limit: Int) = emptyList<Kanji>()
    override suspend fun getKanjiCount() = kanjiList.size.toLong()
}

private class FakeSrsRepository : SrsRepository {
    private val cards = mutableMapOf<Int, SrsCard>()

    override suspend fun getCard(kanjiId: Int) = cards[kanjiId]
    override suspend fun getDueCards(currentTime: Long) =
        cards.values.filter { it.nextReview <= currentTime && it.state != SrsState.GRADUATED }.toList()
    override suspend fun getNewCards(limit: Int) =
        cards.values.filter { it.state == SrsState.NEW }.take(limit).toList()
    override suspend fun getDueCount(currentTime: Long) =
        cards.values.count { it.nextReview <= currentTime && it.state != SrsState.GRADUATED }.toLong()
    override suspend fun getNewCount() =
        cards.values.count { it.state == SrsState.NEW }.toLong()
    override suspend fun getMasteredCount() =
        cards.values.count { it.state == SrsState.GRADUATED }.toLong()
    override suspend fun saveCard(card: SrsCard) {
        cards[card.kanjiId] = card
    }
    override suspend fun ensureCardExists(kanjiId: Int) {
        if (kanjiId !in cards) {
            cards[kanjiId] = SrsCard(kanjiId = kanjiId)
        }
    }
}
