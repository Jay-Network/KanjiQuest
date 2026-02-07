package com.jworks.kanjiquest.core.engine

import com.jworks.kanjiquest.core.domain.model.GameMode
import com.jworks.kanjiquest.core.domain.repository.SrsRepository
import com.jworks.kanjiquest.core.scoring.ScoringEngine
import com.jworks.kanjiquest.core.srs.SrsAlgorithm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameEngine(
    private val questionGenerator: QuestionGenerator,
    private val srsAlgorithm: SrsAlgorithm,
    private val srsRepository: SrsRepository,
    private val scoringEngine: ScoringEngine,
    private val timeProvider: () -> Long = { kotlinx.datetime.Clock.System.now().epochSeconds }
) {
    private val _state = MutableStateFlow<GameState>(GameState.Idle)
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var gameMode: GameMode = GameMode.RECOGNITION
    private var totalQuestions: Int = 0
    private var questionNumber: Int = 0
    private var correctCount: Int = 0
    private var currentCombo: Int = 0
    private var maxCombo: Int = 0
    private var sessionXp: Int = 0
    private var sessionStartTime: Long = 0L

    suspend fun onEvent(event: GameEvent) {
        when (event) {
            is GameEvent.StartSession -> handleStartSession(event)
            is GameEvent.SubmitAnswer -> handleSubmitAnswer(event)
            is GameEvent.NextQuestion -> handleNextQuestion()
            is GameEvent.EndSession -> handleEndSession()
        }
    }

    private suspend fun handleStartSession(event: GameEvent.StartSession) {
        gameMode = event.gameMode
        totalQuestions = event.questionCount
        questionNumber = 0
        correctCount = 0
        currentCombo = 0
        maxCombo = 0
        sessionXp = 0
        sessionStartTime = timeProvider()

        _state.value = GameState.Preparing(gameMode)

        val ready = questionGenerator.prepareSession(totalQuestions, timeProvider())
        if (!ready) {
            _state.value = GameState.Error("No kanji available for study. Add kanji data first.")
            return
        }

        // Adjust total to what's actually available
        totalQuestions = questionGenerator.remainingCount()
        showNextQuestion()
    }

    private suspend fun handleSubmitAnswer(event: GameEvent.SubmitAnswer) {
        val current = _state.value
        if (current !is GameState.AwaitingAnswer) return

        val question = current.question

        // Parse answer based on game mode
        val isCorrect: Boolean
        val quality: Int

        if (gameMode == GameMode.WRITING) {
            // Writing mode: answer is "true|4" or "false|1" (isCorrect|quality)
            val parts = event.answer.split("|")
            isCorrect = parts.getOrNull(0)?.toBooleanStrictOrNull() ?: false
            quality = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 5) ?: if (isCorrect) 4 else 1
        } else {
            // Recognition mode: direct string comparison
            isCorrect = event.answer == question.correctAnswer
            quality = if (isCorrect) 4 else 1
        }

        // Update combo
        if (isCorrect) {
            currentCombo++
            if (currentCombo > maxCombo) maxCombo = currentCombo
            correctCount++
        } else {
            currentCombo = 0
        }

        // Calculate XP
        val scoreResult = scoringEngine.calculateScore(
            quality = quality,
            comboCount = currentCombo,
            isNewCard = question.isNewCard,
            gameMode = gameMode
        )
        sessionXp += scoreResult.totalXp

        // Update SRS card
        val card = srsRepository.getCard(question.kanjiId)
        if (card != null) {
            val updatedCard = srsAlgorithm.review(card, quality, timeProvider())
            srsRepository.saveCard(updatedCard)
        }

        _state.value = GameState.ShowingResult(
            question = question,
            selectedAnswer = event.answer,
            isCorrect = isCorrect,
            quality = quality,
            xpGained = scoreResult.totalXp,
            currentCombo = currentCombo,
            questionNumber = current.questionNumber,
            totalQuestions = current.totalQuestions,
            sessionXp = sessionXp
        )
    }

    private fun handleNextQuestion() {
        if (questionGenerator.hasNextQuestion()) {
            showNextQuestion()
        } else {
            completeSession()
        }
    }

    private fun handleEndSession() {
        completeSession()
    }

    private fun showNextQuestion() {
        val question = when (gameMode) {
            GameMode.RECOGNITION -> questionGenerator.generateRecognitionQuestion()
            GameMode.WRITING -> questionGenerator.generateWritingQuestion()
            else -> questionGenerator.generateRecognitionQuestion()
        }

        if (question == null) {
            completeSession()
            return
        }

        questionNumber++

        _state.value = GameState.AwaitingAnswer(
            question = question,
            questionNumber = questionNumber,
            totalQuestions = totalQuestions,
            currentCombo = currentCombo,
            sessionXp = sessionXp
        )
    }

    private fun completeSession() {
        val elapsed = (timeProvider() - sessionStartTime).toInt()

        _state.value = GameState.SessionComplete(
            stats = SessionStats(
                gameMode = gameMode,
                cardsStudied = questionNumber,
                correctCount = correctCount,
                comboMax = maxCombo,
                xpEarned = sessionXp,
                durationSec = elapsed
            )
        )
    }

    fun reset() {
        _state.value = GameState.Idle
    }
}
