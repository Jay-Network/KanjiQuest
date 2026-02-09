package com.jworks.kanjiquest.android.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.model.GameMode
import com.jworks.kanjiquest.core.domain.usecase.CompleteSessionUseCase
import com.jworks.kanjiquest.core.domain.usecase.SessionResult
import com.jworks.kanjiquest.core.engine.GameEngine
import com.jworks.kanjiquest.core.engine.GameEvent
import com.jworks.kanjiquest.core.engine.GameState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecognitionUiState(
    val gameState: GameState = GameState.Idle,
    val sessionResult: SessionResult? = null
)

@HiltViewModel
class RecognitionViewModel @Inject constructor(
    private val gameEngine: GameEngine,
    private val completeSessionUseCase: CompleteSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecognitionUiState())
    val uiState: StateFlow<RecognitionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            gameEngine.state.collect { state ->
                _uiState.value = _uiState.value.copy(gameState = state)

                // When session completes, record stats
                if (state is GameState.SessionComplete) {
                    val result = completeSessionUseCase.execute(state.stats)
                    _uiState.value = _uiState.value.copy(sessionResult = result)
                }
            }
        }
    }

    fun startGame(questionCount: Int = 10) {
        viewModelScope.launch {
            gameEngine.onEvent(GameEvent.StartSession(GameMode.RECOGNITION, questionCount))
        }
    }

    fun submitAnswer(answer: String) {
        viewModelScope.launch {
            gameEngine.onEvent(GameEvent.SubmitAnswer(answer))
        }
    }

    fun nextQuestion() {
        viewModelScope.launch {
            gameEngine.onEvent(GameEvent.NextQuestion)
        }
    }

    fun endSession() {
        viewModelScope.launch {
            gameEngine.onEvent(GameEvent.EndSession)
        }
    }

    fun reset() {
        gameEngine.reset()
        _uiState.value = RecognitionUiState()
    }
}
