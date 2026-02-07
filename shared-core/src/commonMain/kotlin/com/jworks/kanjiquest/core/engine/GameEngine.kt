package com.jworks.kanjiquest.core.engine

import com.jworks.kanjiquest.core.domain.model.GameMode
import com.jworks.kanjiquest.core.domain.model.SrsState
import com.jworks.kanjiquest.core.domain.model.VocabSrsCard
import com.jworks.kanjiquest.core.domain.repository.SrsRepository
import com.jworks.kanjiquest.core.domain.repository.UserRepository
import com.jworks.kanjiquest.core.domain.repository.VocabSrsRepository
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
    private val vocabSrsRepository: VocabSrsRepository? = null,
    private val userRepository: UserRepository? = null,
    private val wordOfTheDayVocabId: Long? = null,
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
    private var playerLevel: Int = 1

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

        // Cache player level for vocabulary question type gating
        playerLevel = userRepository?.getProfile()?.level ?: 1

        val ready = if (gameMode == GameMode.VOCABULARY) {
            questionGenerator.prepareVocabSession(totalQuestions, timeProvider(), playerLevel)
        } else {
            questionGenerator.prepareSession(totalQuestions, timeProvider())
        }

        if (!ready) {
            val errorMsg = if (gameMode == GameMode.VOCABULARY) {
                "No vocabulary available. Study more kanji first!"
            } else {
                "No kanji available for study. Add kanji data first."
            }
            _state.value = GameState.Error(errorMsg)
            return
        }

        totalQuestions = if (gameMode == GameMode.VOCABULARY) {
            questionGenerator.vocabRemainingCount()
        } else {
            questionGenerator.remainingCount()
        }
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
        var xpGained = scoreResult.totalXp

        // Word of the Day 1.5x bonus
        if (question.vocabId != null && wordOfTheDayVocabId != null && question.vocabId == wordOfTheDayVocabId) {
            xpGained = (xpGained * 1.5f).toInt()
        }
        sessionXp += xpGained

        // Update SRS card
        if (gameMode == GameMode.VOCABULARY && question.vocabId != null) {
            val vocabRepo = vocabSrsRepository
            if (vocabRepo != null) {
                val vocabCard = vocabRepo.getCard(question.vocabId)
                if (vocabCard != null) {
                    val updated = reviewVocabCard(vocabCard, quality, timeProvider())
                    vocabRepo.saveCard(updated)
                }
            }
        } else {
            val card = srsRepository.getCard(question.kanjiId)
            if (card != null) {
                val updatedCard = srsAlgorithm.review(card, quality, timeProvider())
                srsRepository.saveCard(updatedCard)
            }
        }

        _state.value = GameState.ShowingResult(
            question = question,
            selectedAnswer = event.answer,
            isCorrect = isCorrect,
            quality = quality,
            xpGained = xpGained,
            currentCombo = currentCombo,
            questionNumber = current.questionNumber,
            totalQuestions = current.totalQuestions,
            sessionXp = sessionXp
        )
    }

    private suspend fun handleNextQuestion() {
        val hasNext = if (gameMode == GameMode.VOCABULARY) {
            questionGenerator.vocabRemainingCount() > 0
        } else {
            questionGenerator.hasNextQuestion()
        }
        if (hasNext) {
            showNextQuestion()
        } else {
            completeSession()
        }
    }

    private fun handleEndSession() {
        completeSession()
    }

    private suspend fun showNextQuestion() {
        val question = when (gameMode) {
            GameMode.RECOGNITION -> questionGenerator.generateRecognitionQuestion()
            GameMode.WRITING -> questionGenerator.generateWritingQuestion()
            GameMode.VOCABULARY -> questionGenerator.generateVocabularyQuestion(playerLevel)
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

    private fun reviewVocabCard(card: VocabSrsCard, quality: Int, currentTime: Long): VocabSrsCard {
        val newTotalReviews = card.totalReviews + 1
        val newCorrectCount = if (quality >= 3) card.correctCount + 1 else card.correctCount

        return if (quality >= 3) {
            val newRepetitions = card.repetitions + 1
            val newInterval = when (newRepetitions) {
                1 -> 1
                2 -> 6
                else -> (card.interval * card.easeFactor).toInt()
            }
            val newEase = (card.easeFactor + 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)).coerceAtLeast(1.3)
            val newState = if (newInterval >= 21) SrsState.GRADUATED else SrsState.REVIEW

            card.copy(
                easeFactor = newEase,
                interval = newInterval,
                repetitions = newRepetitions,
                nextReview = currentTime + newInterval * 86400L,
                state = newState,
                totalReviews = newTotalReviews,
                correctCount = newCorrectCount
            )
        } else {
            card.copy(
                repetitions = 0,
                interval = 0,
                nextReview = currentTime + 60,
                state = SrsState.LEARNING,
                totalReviews = newTotalReviews,
                correctCount = newCorrectCount
            )
        }
    }

    fun reset() {
        _state.value = GameState.Idle
    }
}
