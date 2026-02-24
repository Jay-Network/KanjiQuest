package com.jworks.kanjiquest.android.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TestPhase {
    SCOPE_SELECTION,
    IN_PROGRESS,
    RESULTS
}

data class TestModeUiState(
    val phase: TestPhase = TestPhase.SCOPE_SELECTION,
    val isLoading: Boolean = false,
    val selectedScope: TestScope? = null,
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswer: Int? = null,
    val isCorrect: Boolean? = null,
    val correctCount: Int = 0,
    val elapsedSeconds: Long = 0,
    val answers: List<Boolean> = emptyList()
) {
    val currentQuestion: QuizQuestion? get() = questions.getOrNull(currentIndex)
    val totalQuestions: Int get() = questions.size
    val accuracy: Float get() = if (answers.isEmpty()) 0f else correctCount.toFloat() / answers.size
}

@HiltViewModel
class TestModeViewModel @Inject constructor(
    private val questionGenerator: QuizQuestionGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(TestModeUiState())
    val uiState: StateFlow<TestModeUiState> = _uiState.asStateFlow()

    private var timerRunning = false
    private var startTimeMs = 0L

    fun selectScope(scope: TestScope) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedScope = scope,
                isLoading = true
            )
            val questions = questionGenerator.generateQuestions(scope, count = 10)
            if (questions.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }
            startTimeMs = System.currentTimeMillis()
            timerRunning = true
            startTimer()
            _uiState.value = _uiState.value.copy(
                phase = TestPhase.IN_PROGRESS,
                isLoading = false,
                questions = questions,
                currentIndex = 0,
                selectedAnswer = null,
                isCorrect = null,
                correctCount = 0,
                answers = emptyList()
            )
        }
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (timerRunning) {
                val elapsed = (System.currentTimeMillis() - startTimeMs) / 1000
                _uiState.value = _uiState.value.copy(elapsedSeconds = elapsed)
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    fun selectAnswer(index: Int) {
        val state = _uiState.value
        if (state.selectedAnswer != null) return
        val question = state.currentQuestion ?: return

        val isCorrect = index == question.correctIndex
        val newCorrect = state.correctCount + if (isCorrect) 1 else 0

        _uiState.value = state.copy(
            selectedAnswer = index,
            isCorrect = isCorrect,
            correctCount = newCorrect,
            answers = state.answers + isCorrect
        )
    }

    fun nextQuestion() {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1

        if (nextIndex >= state.totalQuestions) {
            timerRunning = false
            _uiState.value = state.copy(phase = TestPhase.RESULTS)
        } else {
            _uiState.value = state.copy(
                currentIndex = nextIndex,
                selectedAnswer = null,
                isCorrect = null
            )
        }
    }

    fun resetToScopeSelection() {
        timerRunning = false
        _uiState.value = TestModeUiState()
    }

    fun retakeTest() {
        val scope = _uiState.value.selectedScope ?: return
        selectScope(scope)
    }
}
