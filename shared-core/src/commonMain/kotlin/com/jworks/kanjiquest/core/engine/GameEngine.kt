package com.jworks.kanjiquest.core.engine

import com.jworks.kanjiquest.core.collection.EncounterEngine
import com.jworks.kanjiquest.core.collection.ItemLevelEngine
import com.jworks.kanjiquest.core.domain.UserSessionProvider
import com.jworks.kanjiquest.core.domain.model.CollectedItem
import com.jworks.kanjiquest.core.domain.model.CollectionItemType
import com.jworks.kanjiquest.core.domain.model.GameMode
import com.jworks.kanjiquest.core.domain.model.KanaType
import com.jworks.kanjiquest.core.domain.model.LevelProgression
import com.jworks.kanjiquest.core.domain.model.SrsState
import com.jworks.kanjiquest.core.domain.model.VocabSrsCard
import com.jworks.kanjiquest.core.domain.repository.CollectionRepository
import com.jworks.kanjiquest.core.domain.repository.KanaSrsRepository
import com.jworks.kanjiquest.core.domain.repository.RadicalSrsRepository
import com.jworks.kanjiquest.core.domain.repository.SrsRepository
import com.jworks.kanjiquest.core.domain.repository.UserRepository
import com.jworks.kanjiquest.core.domain.repository.VocabSrsRepository
import com.jworks.kanjiquest.core.scoring.ScoringEngine
import com.jworks.kanjiquest.core.srs.SrsAlgorithm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class GameEngine(
    private val questionGenerator: QuestionGenerator,
    private val srsAlgorithm: SrsAlgorithm,
    private val srsRepository: SrsRepository,
    private val scoringEngine: ScoringEngine,
    private val vocabSrsRepository: VocabSrsRepository? = null,
    private val userRepository: UserRepository? = null,
    private val wordOfTheDayVocabId: Long? = null,
    private val userSessionProvider: UserSessionProvider? = null,
    private val kanaQuestionGenerator: KanaQuestionGenerator? = null,
    private val kanaSrsRepository: KanaSrsRepository? = null,
    private val radicalQuestionGenerator: RadicalQuestionGenerator? = null,
    private val radicalSrsRepository: RadicalSrsRepository? = null,
    private val collectionRepository: CollectionRepository? = null,
    private val encounterEngine: EncounterEngine? = null,
    private val itemLevelEngine: ItemLevelEngine? = null,
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
    private val touchedKanjiIds = mutableListOf<Int>()
    private val touchedVocabIds = mutableListOf<Long>()

    suspend fun onEvent(event: GameEvent) {
        try {
            when (event) {
                is GameEvent.StartSession -> handleStartSession(event)
                is GameEvent.SubmitAnswer -> handleSubmitAnswer(event)
                is GameEvent.NextQuestion -> handleNextQuestion()
                is GameEvent.EndSession -> handleEndSession()
            }
        } catch (e: Exception) {
            _state.value = GameState.Error("Something went wrong: ${e.message}")
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
        touchedKanjiIds.clear()
        touchedVocabIds.clear()

        _state.value = GameState.Preparing(gameMode)

        // Run DB-heavy session prep off the main thread
        val ready = withContext(Dispatchers.IO) {
            // Admin override takes precedence, then real profile level
            playerLevel = userSessionProvider?.getAdminPlayerLevelOverride()
                ?: userRepository?.getProfile()?.level ?: 1

            when {
                gameMode.isKanaMode -> {
                    val kanaType = event.kanaType ?: KanaType.HIRAGANA
                    kanaQuestionGenerator?.prepareSession(totalQuestions, timeProvider(), kanaType) ?: false
                }
                gameMode.isRadicalMode -> {
                    radicalQuestionGenerator?.prepareSession(totalQuestions, timeProvider(), playerLevel) ?: false
                }
                event.targetKanjiId != null -> {
                    questionGenerator.prepareTargetedSession(event.targetKanjiId, totalQuestions)
                }
                gameMode == GameMode.VOCABULARY -> {
                    questionGenerator.prepareVocabSession(totalQuestions, timeProvider(), playerLevel)
                }
                else -> {
                    questionGenerator.prepareSession(totalQuestions, timeProvider(), playerLevel)
                }
            }
        }

        if (!ready) {
            val errorMsg = when {
                gameMode.isKanaMode -> "No kana available for study."
                gameMode.isRadicalMode -> "No radicals available for study."
                gameMode == GameMode.VOCABULARY -> "No vocabulary available. Study more kanji first!"
                else -> "No kanji available for study. Add kanji data first."
            }
            _state.value = GameState.Error(errorMsg)
            return
        }

        totalQuestions = when {
            gameMode.isKanaMode -> kanaQuestionGenerator?.remainingCount() ?: 0
            gameMode.isRadicalMode -> radicalQuestionGenerator?.remainingCount() ?: 0
            gameMode == GameMode.VOCABULARY -> questionGenerator.vocabRemainingCount()
            else -> questionGenerator.remainingCount()
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

        // Update SRS card (off main thread) and track touched IDs
        withContext(Dispatchers.IO) {
            when {
                gameMode.isKanaMode -> {
                    val kanaRepo = kanaSrsRepository
                    if (kanaRepo != null) {
                        val card = kanaRepo.getCard(question.kanjiId)
                        if (card != null) {
                            val updated = srsAlgorithm.review(card, quality, timeProvider())
                            kanaRepo.saveCard(updated)
                        }
                    }
                    touchedKanjiIds.add(question.kanjiId)
                }
                gameMode.isRadicalMode -> {
                    val radRepo = radicalSrsRepository
                    if (radRepo != null) {
                        val card = radRepo.getCard(question.kanjiId)
                        if (card != null) {
                            val updated = srsAlgorithm.review(card, quality, timeProvider())
                            radRepo.saveCard(updated)
                        }
                    }
                    touchedKanjiIds.add(question.kanjiId)
                }
                gameMode == GameMode.VOCABULARY && question.vocabId != null -> {
                    val vocabRepo = vocabSrsRepository
                    if (vocabRepo != null) {
                        val vocabCard = vocabRepo.getCard(question.vocabId)
                        if (vocabCard != null) {
                            val updated = reviewVocabCard(vocabCard, quality, timeProvider())
                            vocabRepo.saveCard(updated)
                        }
                    }
                    touchedVocabIds.add(question.vocabId)
                }
                else -> {
                    val card = srsRepository.getCard(question.kanjiId)
                    if (card != null) {
                        val updatedCard = srsAlgorithm.review(card, quality, timeProvider())
                        srsRepository.saveCard(updatedCard)
                    }
                    touchedKanjiIds.add(question.kanjiId)
                }
            }
            // Track per-mode stats (only for kanji modes)
            if (!gameMode.isKanaMode && !gameMode.isRadicalMode) {
                srsRepository.incrementModeStats(question.kanjiId, gameMode.name.lowercase(), isCorrect)
            }
        }

        // Collection system: item XP + encounter rolls
        var discoveredItem: CollectedItem? = null
        var itemLevelUp = false

        if (collectionRepository != null) {
            withContext(Dispatchers.IO) {
                val itemType = when {
                    gameMode.isKanaMode -> null // Kana encounters handled separately if needed
                    gameMode.isRadicalMode -> CollectionItemType.RADICAL
                    else -> CollectionItemType.KANJI
                }

                if (itemType != null && itemLevelEngine != null) {
                    // Add XP to collected items
                    val levelResult = itemLevelEngine.addXp(
                        question.kanjiId, itemType, isCorrect, currentCombo
                    )
                    if (levelResult != null) {
                        itemLevelUp = levelResult.leveledUp
                    }
                }

                // Roll for encounter on correct answer (kanji modes only)
                if (isCorrect && itemType == CollectionItemType.KANJI && encounterEngine != null) {
                    val isUncollected = !collectionRepository.isCollected(question.kanjiId, CollectionItemType.KANJI)
                    if (isUncollected) {
                        // The question itself was uncollected — auto-collect it on correct answer
                        val rarity = com.jworks.kanjiquest.core.collection.RarityCalculator.calculateKanjiRarity(
                            question.kanjiGrade, question.kanjiFrequency, question.kanjiStrokeCount
                        )
                        val newItem = CollectedItem(
                            itemId = question.kanjiId,
                            itemType = CollectionItemType.KANJI,
                            rarity = rarity,
                            itemLevel = 1,
                            itemXp = 0,
                            discoveredAt = timeProvider(),
                            source = "gameplay"
                        )
                        collectionRepository.collect(newItem)
                        discoveredItem = newItem
                    } else {
                        // Already collected — roll for a bonus encounter of a new kanji
                        val unlockedGrades = LevelProgression.getUnlockedGrades(playerLevel)
                        val encounter = encounterEngine.rollEncounter(unlockedGrades, timeProvider())
                        if (encounter != null) {
                            discoveredItem = encounter.collectedItem
                        }
                    }
                }
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
            sessionXp = sessionXp,
            discoveredItem = discoveredItem,
            itemLevelUp = itemLevelUp
        )
    }

    private suspend fun handleNextQuestion() {
        val hasNext = when {
            gameMode.isKanaMode -> kanaQuestionGenerator?.hasNextQuestion() ?: false
            gameMode.isRadicalMode -> radicalQuestionGenerator?.hasNextQuestion() ?: false
            gameMode == GameMode.VOCABULARY -> questionGenerator.vocabRemainingCount() > 0
            else -> questionGenerator.hasNextQuestion()
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
        val question = withContext(Dispatchers.IO) {
            when (gameMode) {
                GameMode.KANA_RECOGNITION -> kanaQuestionGenerator?.generateRecognitionQuestion()
                GameMode.KANA_WRITING -> kanaQuestionGenerator?.generateWritingQuestion()
                GameMode.RADICAL_RECOGNITION -> radicalQuestionGenerator?.generateRecognitionQuestion()
                GameMode.RADICAL_BUILDER -> radicalQuestionGenerator?.generateBuilderQuestion()
                GameMode.RECOGNITION -> questionGenerator.generateRecognitionQuestion()
                GameMode.WRITING -> questionGenerator.generateWritingQuestion()
                GameMode.VOCABULARY -> questionGenerator.generateVocabularyQuestion(playerLevel)
                else -> questionGenerator.generateRecognitionQuestion()
            }
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
                durationSec = elapsed,
                touchedKanjiIds = touchedKanjiIds.toList(),
                touchedVocabIds = touchedVocabIds.toList()
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
