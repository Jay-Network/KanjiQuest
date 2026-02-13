package com.jworks.kanjiquest.android.ui.placement

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.model.LevelProgression
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import com.jworks.kanjiquest.core.domain.repository.UserRepository
import com.jworks.kanjiquest.core.scoring.ScoringEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlacementQuestion(
    val kanji: Kanji,
    val options: List<String>,
    val correctIndex: Int
)

data class PlacementUiState(
    val isLoading: Boolean = true,
    val currentGrade: Int = 1,
    val questionIndex: Int = 0,
    val totalQuestionIndex: Int = 0,
    val question: PlacementQuestion? = null,
    val selectedAnswer: Int? = null,
    val isCorrect: Boolean? = null,
    val gradeCorrectCount: Int = 0,
    val gradeResults: Map<Int, Pair<Int, Int>> = emptyMap(),
    val isComplete: Boolean = false,
    val assignedLevel: Int = 1,
    val assignedTierName: String = "Beginner",
    val highestPassedGrade: Int? = null
)

@HiltViewModel
class PlacementTestViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val kanjiRepository: KanjiRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlacementUiState())
    val uiState: StateFlow<PlacementUiState> = _uiState.asStateFlow()

    private val questionsPerGrade = 5
    private val passThreshold = 4
    private val maxGrade = 6

    private val gradeKanjiCache = mutableMapOf<Int, List<Kanji>>()
    private var currentGradeQuestions = listOf<PlacementQuestion>()

    init {
        startAssessment()
    }

    private fun startAssessment() {
        viewModelScope.launch {
            for (grade in 1..maxGrade) {
                gradeKanjiCache[grade] = kanjiRepository.getKanjiByGrade(grade)
            }
            prepareGrade(1)
        }
    }

    private suspend fun prepareGrade(grade: Int) {
        val gradeKanji = gradeKanjiCache[grade] ?: emptyList()
        if (gradeKanji.size < questionsPerGrade) {
            finishAssessment()
            return
        }

        val selected = gradeKanji.shuffled().take(questionsPerGrade)

        val allMeanings = gradeKanjiCache.values.flatten()
            .flatMap { it.meaningsEn }
            .distinct()

        currentGradeQuestions = selected.map { kanji ->
            val correctMeaning = kanji.meaningsEn.firstOrNull() ?: "unknown"

            val distractors = allMeanings
                .filter { it !in kanji.meaningsEn }
                .shuffled()
                .take(3)

            val options = (distractors + correctMeaning).shuffled()
            val correctIndex = options.indexOf(correctMeaning)

            PlacementQuestion(
                kanji = kanji,
                options = options,
                correctIndex = correctIndex
            )
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            currentGrade = grade,
            questionIndex = 0,
            question = currentGradeQuestions.first(),
            selectedAnswer = null,
            isCorrect = null,
            gradeCorrectCount = 0
        )
    }

    fun selectAnswer(index: Int) {
        val state = _uiState.value
        if (state.selectedAnswer != null) return

        val isCorrect = index == state.question?.correctIndex
        val newCorrectCount = state.gradeCorrectCount + if (isCorrect) 1 else 0

        _uiState.value = state.copy(
            selectedAnswer = index,
            isCorrect = isCorrect,
            gradeCorrectCount = newCorrectCount
        )
    }

    fun nextQuestion() {
        viewModelScope.launch {
            val state = _uiState.value
            val nextIndex = state.questionIndex + 1

            if (nextIndex >= questionsPerGrade) {
                val passed = state.gradeCorrectCount >= passThreshold
                val newResults = state.gradeResults +
                    (state.currentGrade to Pair(state.gradeCorrectCount, questionsPerGrade))

                _uiState.value = state.copy(gradeResults = newResults)

                if (!passed || state.currentGrade >= maxGrade) {
                    finishAssessment()
                } else {
                    prepareGrade(state.currentGrade + 1)
                }
            } else {
                _uiState.value = state.copy(
                    questionIndex = nextIndex,
                    totalQuestionIndex = state.totalQuestionIndex + 1,
                    question = currentGradeQuestions[nextIndex],
                    selectedAnswer = null,
                    isCorrect = null
                )
            }
        }
    }

    private suspend fun finishAssessment() {
        val state = _uiState.value

        val highestPassed = state.gradeResults.entries
            .filter { it.value.first >= passThreshold }
            .maxByOrNull { it.key }?.key

        val assignedLevel = when (highestPassed) {
            null -> 1
            1 -> 5
            2 -> 10
            3 -> 15
            4 -> 20
            5 -> 25
            6 -> 30
            else -> 1
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
            assignedLevel = assignedLevel,
            assignedTierName = tier.nameEn,
            highestPassedGrade = highestPassed
        )
    }
}
