package com.jworks.kanjiquest.android.ui.game.kana

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.android.audio.HapticManager
import com.jworks.kanjiquest.android.audio.SoundManager
import com.jworks.kanjiquest.core.domain.model.GameMode
import com.jworks.kanjiquest.core.domain.model.KanaType
import com.jworks.kanjiquest.core.domain.usecase.CompleteSessionUseCase
import com.jworks.kanjiquest.core.domain.usecase.SessionResult
import com.jworks.kanjiquest.core.engine.GameEngine
import com.jworks.kanjiquest.core.engine.GameEvent
import com.jworks.kanjiquest.core.engine.GameState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KanaRecognitionUiState(
    val gameState: GameState = GameState.Idle,
    val sessionResult: SessionResult? = null
)

@HiltViewModel
class KanaRecognitionViewModel @Inject constructor(
    private val gameEngine: GameEngine,
    private val completeSessionUseCase: CompleteSessionUseCase,
    private val soundManager: SoundManager,
    private val hapticManager: HapticManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(KanaRecognitionUiState())
    val uiState: StateFlow<KanaRecognitionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            gameEngine.state.collect { state ->
                _uiState.value = _uiState.value.copy(gameState = state)

                when (state) {
                    is GameState.ShowingResult -> {
                        if (state.isCorrect) {
                            soundManager.play(SoundManager.SoundEffect.CORRECT)
                            hapticManager.vibrate(HapticManager.HapticType.SUCCESS)
                        } else {
                            soundManager.play(SoundManager.SoundEffect.INCORRECT)
                            hapticManager.vibrate(HapticManager.HapticType.ERROR)
                        }
                    }
                    is GameState.SessionComplete -> {
                        soundManager.play(SoundManager.SoundEffect.SESSION_COMPLETE)
                        val result = completeSessionUseCase.execute(state.stats)
                        _uiState.value = _uiState.value.copy(sessionResult = result)
                        if (result.leveledUp) {
                            soundManager.play(SoundManager.SoundEffect.LEVEL_UP)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun startGame(kanaType: KanaType, questionCount: Int = 10) {
        viewModelScope.launch {
            gameEngine.onEvent(GameEvent.StartSession(
                gameMode = GameMode.KANA_RECOGNITION,
                questionCount = questionCount,
                kanaType = kanaType
            ))
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

    fun reset() {
        gameEngine.reset()
        _uiState.value = KanaRecognitionUiState()
    }
}
