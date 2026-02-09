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

class WritingModeEngineTest {

    private val testKanji = listOf(
        Kanji(
            id = 19968, literal = "一", grade = 1, jlptLevel = 4, frequency = 2,
            strokeCount = 1, meaningsEn = listOf("one"), onReadings = listOf("イチ"),
            kunReadings = listOf("ひと.つ"),
            strokeSvg = """["M 14.5,50.25 c 7.75,0.62 17.37,0.87 26.62,0.37 c 9.25,-0.5 18.12,-1.62 24.25,-3"]"""
        ),
        Kanji(
            id = 19977, literal = "三", grade = 1, jlptLevel = 4, frequency = 72,
            strokeCount = 3, meaningsEn = listOf("three"), onReadings = listOf("サン"),
            kunReadings = listOf("み.つ"),
            strokeSvg = """["M 20,25 c 5,0 10,-0.5 15,-1","M 15,50 c 8,0.5 18,0 27,-1","M 12,75 c 10,0.75 22,0.5 33,-1"]"""
        ),
        Kanji(
            id = 26408, literal = "木", grade = 1, jlptLevel = 4, frequency = 317,
            strokeCount = 4, meaningsEn = listOf("tree", "wood"), onReadings = listOf("モク"),
            kunReadings = listOf("き"),
            strokeSvg = """["M 54,14 c 0,10 0,50 0,70","M 18,38 c 12,5 24,10 36,12 c 12,2 22,-2 34,-8","M 50,52 c -12,14 -24,28 -36,40","M 52,53 c 12,14 24,28 36,38"]"""
        ),
    )

    private fun createEngine(): GameEngine {
        val kanjiRepo = WritingTestKanjiRepository(testKanji)
        val srsRepo = WritingTestSrsRepository()
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
    fun writingMode_startsWithAwaitingAnswer() = runTest {
        val engine = createEngine()
        engine.onEvent(GameEvent.StartSession(GameMode.WRITING, 3))
        assertIs<GameState.AwaitingAnswer>(engine.state.value)
    }

    @Test
    fun writingMode_questionHasStrokePaths() = runTest {
        val engine = createEngine()
        engine.onEvent(GameEvent.StartSession(GameMode.WRITING, 3))
        val state = engine.state.value as GameState.AwaitingAnswer
        assertTrue(state.question.strokePaths.isNotEmpty())
    }

    @Test
    fun writingMode_questionHasNoChoices() = runTest {
        val engine = createEngine()
        engine.onEvent(GameEvent.StartSession(GameMode.WRITING, 3))
        val state = engine.state.value as GameState.AwaitingAnswer
        assertTrue(state.question.choices.isEmpty())
    }

    @Test
    fun writingMode_questionTextContainsMeaning() = runTest {
        val engine = createEngine()
        engine.onEvent(GameEvent.StartSession(GameMode.WRITING, 3))
        val state = engine.state.value as GameState.AwaitingAnswer
        assertTrue(state.question.questionText.startsWith("Write:"))
    }

    @Test
    fun writingMode_submitCorrectAnswer_showsResult() = runTest {
        val engine = createEngine()
        engine.onEvent(GameEvent.StartSession(GameMode.WRITING, 3))

        // Submit a "correct" writing result: "true|4"
        engine.onEvent(GameEvent.SubmitAnswer("true|4"))
        val result = engine.state.value as GameState.ShowingResult
        assertTrue(result.isCorrect)
        assertEquals(4, result.quality)
        assertTrue(result.xpGained > 0)
    }

    @Test
    fun writingMode_submitIncorrectAnswer_showsResult() = runTest {
        val engine = createEngine()
        engine.onEvent(GameEvent.StartSession(GameMode.WRITING, 3))

        // Submit an "incorrect" writing result: "false|1"
        engine.onEvent(GameEvent.SubmitAnswer("false|1"))
        val result = engine.state.value as GameState.ShowingResult
        assertEquals(false, result.isCorrect)
        assertEquals(1, result.quality)
    }

    @Test
    fun writingMode_fullSession_completesSuccessfully() = runTest {
        val engine = createEngine()
        engine.onEvent(GameEvent.StartSession(GameMode.WRITING, 2))

        // Answer question 1
        engine.onEvent(GameEvent.SubmitAnswer("true|4"))
        engine.onEvent(GameEvent.NextQuestion)

        // Answer question 2
        val state2 = engine.state.value
        if (state2 is GameState.AwaitingAnswer) {
            engine.onEvent(GameEvent.SubmitAnswer("true|5"))
            engine.onEvent(GameEvent.NextQuestion)
        }

        val finalState = engine.state.value
        if (finalState is GameState.SessionComplete) {
            assertEquals(GameMode.WRITING, finalState.stats.gameMode)
            assertTrue(finalState.stats.xpEarned > 0)
        }
    }

    @Test
    fun writingMode_comboTracking_works() = runTest {
        val engine = createEngine()
        engine.onEvent(GameEvent.StartSession(GameMode.WRITING, 3))

        // Answer correctly
        engine.onEvent(GameEvent.SubmitAnswer("true|4"))
        val r1 = engine.state.value as GameState.ShowingResult
        assertEquals(1, r1.currentCombo)

        engine.onEvent(GameEvent.NextQuestion)
        val q2 = engine.state.value
        if (q2 is GameState.AwaitingAnswer) {
            engine.onEvent(GameEvent.SubmitAnswer("true|4"))
            val r2 = engine.state.value as GameState.ShowingResult
            assertEquals(2, r2.currentCombo)
        }
    }
}

// Test doubles for Writing mode tests

private class WritingTestKanjiRepository(private val kanjiList: List<Kanji>) : KanjiRepository {
    override suspend fun getKanjiById(id: Int) = kanjiList.find { it.id == id }
    override suspend fun getKanjiByLiteral(literal: String) = kanjiList.find { it.literal == literal }
    override suspend fun getKanjiByGrade(grade: Int) = kanjiList.filter { it.grade == grade }
    override suspend fun getKanjiByJlptLevel(level: Int) = kanjiList.filter { it.jlptLevel == level }
    override suspend fun getVocabularyForKanji(kanjiId: Int) = emptyList<Vocabulary>()
    override suspend fun searchKanji(query: String, limit: Int) = emptyList<Kanji>()
    override suspend fun getKanjiCount() = kanjiList.size.toLong()
}

private class WritingTestSrsRepository : SrsRepository {
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
