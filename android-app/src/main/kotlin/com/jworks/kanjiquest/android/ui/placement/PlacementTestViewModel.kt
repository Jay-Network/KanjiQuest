package com.jworks.kanjiquest.android.ui.placement

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.model.Kana
import com.jworks.kanjiquest.core.domain.model.KanaType
import com.jworks.kanjiquest.core.domain.model.KanaVariant
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.model.LevelProgression
import com.jworks.kanjiquest.core.domain.model.Radical
import com.jworks.kanjiquest.core.domain.repository.KanaRepository
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import com.jworks.kanjiquest.core.domain.repository.RadicalRepository
import com.jworks.kanjiquest.core.domain.repository.UserRepository
import com.jworks.kanjiquest.core.scoring.ScoringEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PlacementStage(val displayName: String) {
    HIRAGANA("Hiragana"),
    KATAKANA("Katakana"),
    RADICAL("Radicals"),
    GRADE_1("Grade 1"),
    GRADE_2("Grade 2"),
    GRADE_3("Grade 3"),
    GRADE_4("Grade 4"),
    GRADE_5("Grade 5"),
    GRADE_6("Grade 6")
}

data class PlacementQuestion(
    val displayCharacter: String,
    val questionPrompt: String,
    val options: List<String>,
    val correctIndex: Int
)

data class PlacementUiState(
    val showIntro: Boolean = true,
    val isLoading: Boolean = true,
    val currentStage: PlacementStage = PlacementStage.HIRAGANA,
    val questionIndex: Int = 0,
    val totalQuestionIndex: Int = 0,
    val question: PlacementQuestion? = null,
    val selectedAnswer: Int? = null,
    val isCorrect: Boolean? = null,
    val stageCorrectCount: Int = 0,
    val stageResults: Map<String, Pair<Int, Int>> = emptyMap(),
    val isComplete: Boolean = false,
    val assignedLevel: Int = 1,
    val assignedTierName: String = "Newcomer",
    val highestPassedGrade: Int? = null,
    val highestPassedStage: PlacementStage? = null,
    val remainingSeconds: Int = 180,
    val timedOut: Boolean = false
)

@HiltViewModel
class PlacementTestViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val kanjiRepository: KanjiRepository,
    private val userRepository: UserRepository,
    private val kanaRepository: KanaRepository,
    private val radicalRepository: RadicalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlacementUiState())
    val uiState: StateFlow<PlacementUiState> = _uiState.asStateFlow()

    private val questionsPerStage = 5
    private val passThreshold = 4
    private val timeLimitSeconds = 180

    private val gradeKanjiCache = mutableMapOf<Int, List<Kanji>>()
    private var hiraganaCache: List<Kana> = emptyList()
    private var katakanaCache: List<Kana> = emptyList()
    private var radicalCache: List<Radical> = emptyList()
    private var currentStageQuestions = listOf<PlacementQuestion>()
    private var timerJob: Job? = null

    private val stageOrder = PlacementStage.entries.toList()

    init {
        preloadData()
    }

    private fun preloadData() {
        viewModelScope.launch {
            // Load kana
            hiraganaCache = try { kanaRepository.getKanaByType(KanaType.HIRAGANA) } catch (_: Exception) { emptyList() }
            katakanaCache = try { kanaRepository.getKanaByType(KanaType.KATAKANA) } catch (_: Exception) { emptyList() }
            radicalCache = try { radicalRepository.getAllRadicals() } catch (_: Exception) { emptyList() }
            // Load kanji by grade
            for (grade in 1..6) {
                gradeKanjiCache[grade] = kanjiRepository.getKanjiByGrade(grade)
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun beginAssessment() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showIntro = false, isLoading = true)
            prepareStage(PlacementStage.HIRAGANA)
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            var remaining = timeLimitSeconds
            while (remaining > 0) {
                _uiState.value = _uiState.value.copy(remainingSeconds = remaining)
                delay(1000L)
                remaining--
                if (_uiState.value.isComplete) return@launch
            }
            _uiState.value = _uiState.value.copy(remainingSeconds = 0, timedOut = true)
            finishAssessment()
        }
    }

    private suspend fun prepareStage(stage: PlacementStage) {
        currentStageQuestions = when (stage) {
            PlacementStage.HIRAGANA -> generateKanaQuestions(hiraganaCache, "What is the reading?")
            PlacementStage.KATAKANA -> generateKanaQuestions(katakanaCache, "What is the reading?")
            PlacementStage.RADICAL -> generateRadicalQuestions()
            else -> generateKanjiQuestions(stage)
        }

        if (currentStageQuestions.isEmpty()) {
            // Skip this stage if no data available
            val nextStage = getNextStage(stage)
            if (nextStage != null) {
                prepareStage(nextStage)
            } else {
                finishAssessment()
            }
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            currentStage = stage,
            questionIndex = 0,
            question = currentStageQuestions.first(),
            selectedAnswer = null,
            isCorrect = null,
            stageCorrectCount = 0
        )
    }

    private fun generateKanaQuestions(kanaList: List<Kana>, prompt: String): List<PlacementQuestion> {
        if (kanaList.size < questionsPerStage) return emptyList()
        val basicKana = kanaList.filter { it.variant == KanaVariant.BASIC }
        val pool = if (basicKana.size >= questionsPerStage) basicKana else kanaList
        val selected = pool.shuffled().take(questionsPerStage)
        val allRomanizations = pool.map { it.romanization }.distinct()

        return selected.map { kana ->
            val correct = kana.romanization
            val distractors = allRomanizations
                .filter { it != correct }
                .shuffled()
                .take(3)
            val options = (distractors + correct).shuffled()

            PlacementQuestion(
                displayCharacter = kana.literal,
                questionPrompt = prompt,
                options = options,
                correctIndex = options.indexOf(correct)
            )
        }
    }

    private fun generateRadicalQuestions(): List<PlacementQuestion> {
        if (radicalCache.size < questionsPerStage) return emptyList()
        val selected = radicalCache.shuffled().take(questionsPerStage)
        val allMeanings = radicalCache.map { it.meaningEn }.distinct()

        return selected.map { radical ->
            val correct = radical.meaningEn
            val distractors = allMeanings
                .filter { it != correct }
                .shuffled()
                .take(3)
            val options = (distractors + correct).shuffled()

            PlacementQuestion(
                displayCharacter = radical.literal,
                questionPrompt = "What does this radical mean?",
                options = options,
                correctIndex = options.indexOf(correct)
            )
        }
    }

    private fun generateKanjiQuestions(stage: PlacementStage): List<PlacementQuestion> {
        val grade = when (stage) {
            PlacementStage.GRADE_1 -> 1
            PlacementStage.GRADE_2 -> 2
            PlacementStage.GRADE_3 -> 3
            PlacementStage.GRADE_4 -> 4
            PlacementStage.GRADE_5 -> 5
            PlacementStage.GRADE_6 -> 6
            else -> return emptyList()
        }

        val gradeKanji = gradeKanjiCache[grade] ?: emptyList()
        if (gradeKanji.size < questionsPerStage) return emptyList()

        val selected = gradeKanji.shuffled().take(questionsPerStage)
        val allMeanings = gradeKanjiCache.values.flatten()
            .flatMap { it.meaningsEn }
            .distinct()

        return selected.map { kanji ->
            val correctMeaning = kanji.meaningsEn.firstOrNull() ?: "unknown"
            val distractors = allMeanings
                .filter { it !in kanji.meaningsEn }
                .shuffled()
                .take(3)
            val options = (distractors + correctMeaning).shuffled()

            PlacementQuestion(
                displayCharacter = kanji.literal,
                questionPrompt = "What does this kanji mean?",
                options = options,
                correctIndex = options.indexOf(correctMeaning)
            )
        }
    }

    private fun getNextStage(current: PlacementStage): PlacementStage? {
        val idx = stageOrder.indexOf(current)
        return if (idx >= 0 && idx < stageOrder.size - 1) stageOrder[idx + 1] else null
    }

    fun selectAnswer(index: Int) {
        val state = _uiState.value
        if (state.selectedAnswer != null) return

        val isCorrect = index == state.question?.correctIndex
        val newCorrectCount = state.stageCorrectCount + if (isCorrect) 1 else 0

        _uiState.value = state.copy(
            selectedAnswer = index,
            isCorrect = isCorrect,
            stageCorrectCount = newCorrectCount
        )
    }

    fun nextQuestion() {
        viewModelScope.launch {
            val state = _uiState.value
            val nextIndex = state.questionIndex + 1

            if (nextIndex >= questionsPerStage) {
                val passed = state.stageCorrectCount >= passThreshold
                val newResults = state.stageResults +
                    (state.currentStage.displayName to Pair(state.stageCorrectCount, questionsPerStage))

                _uiState.value = state.copy(stageResults = newResults)

                if (!passed) {
                    finishAssessment()
                } else {
                    val nextStage = getNextStage(state.currentStage)
                    if (nextStage != null) {
                        prepareStage(nextStage)
                    } else {
                        finishAssessment()
                    }
                }
            } else {
                _uiState.value = state.copy(
                    questionIndex = nextIndex,
                    totalQuestionIndex = state.totalQuestionIndex + 1,
                    question = currentStageQuestions[nextIndex],
                    selectedAnswer = null,
                    isCorrect = null
                )
            }
        }
    }

    private suspend fun finishAssessment() {
        timerJob?.cancel()
        val state = _uiState.value

        // Include partial stage results if timed out mid-stage
        val finalResults = if (state.timedOut && state.questionIndex > 0 &&
            !state.stageResults.containsKey(state.currentStage.displayName)) {
            state.stageResults + (state.currentStage.displayName to Pair(state.stageCorrectCount, state.questionIndex))
        } else {
            state.stageResults
        }

        // Determine highest passed stage
        val passedStages = stageOrder.filter { stage ->
            val result = finalResults[stage.displayName]
            result != null && result.first >= passThreshold
        }
        val highestPassed = passedStages.lastOrNull()

        // Determine highest passed kanji grade
        val highestPassedGrade = passedStages
            .filter { it.ordinal >= PlacementStage.GRADE_1.ordinal }
            .lastOrNull()
            ?.let { stage ->
                when (stage) {
                    PlacementStage.GRADE_1 -> 1
                    PlacementStage.GRADE_2 -> 2
                    PlacementStage.GRADE_3 -> 3
                    PlacementStage.GRADE_4 -> 4
                    PlacementStage.GRADE_5 -> 5
                    PlacementStage.GRADE_6 -> 6
                    else -> null
                }
            }

        val assignedLevel = when (highestPassed) {
            null -> 1 // Newcomer
            PlacementStage.HIRAGANA -> 2 // Script Learner
            PlacementStage.KATAKANA -> 3 // Foundation
            PlacementStage.RADICAL -> 4 // Beginner
            PlacementStage.GRADE_1 -> 5
            PlacementStage.GRADE_2 -> 10
            PlacementStage.GRADE_3 -> 15
            PlacementStage.GRADE_4 -> 20
            PlacementStage.GRADE_5 -> 25
            PlacementStage.GRADE_6 -> 30
        }

        val tier = LevelProgression.getTierForLevel(assignedLevel)

        val xpForLevel = ScoringEngine.xpForLevel(assignedLevel)
        userRepository.updateXpAndLevel(xpForLevel, assignedLevel)

        context.getSharedPreferences("kanjiquest_settings", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("assessment_completed", true)
            .apply()

        _uiState.value = state.copy(
            isComplete = true,
            stageResults = finalResults,
            assignedLevel = assignedLevel,
            assignedTierName = tier.nameEn,
            highestPassedGrade = highestPassedGrade,
            highestPassedStage = highestPassed
        )
    }
}
