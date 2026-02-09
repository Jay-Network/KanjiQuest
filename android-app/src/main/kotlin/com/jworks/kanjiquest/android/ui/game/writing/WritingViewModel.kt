package com.jworks.kanjiquest.android.ui.game.writing

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.model.GameMode
import com.jworks.kanjiquest.core.domain.usecase.CompleteSessionUseCase
import com.jworks.kanjiquest.core.domain.usecase.SessionResult
import com.jworks.kanjiquest.core.engine.GameEngine
import com.jworks.kanjiquest.core.engine.GameEvent
import com.jworks.kanjiquest.core.engine.GameState
import com.jworks.kanjiquest.core.writing.Point
import com.jworks.kanjiquest.core.writing.StrokeMatcher
import com.jworks.kanjiquest.core.writing.SvgPathParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WritingUiState(
    val gameState: GameState = GameState.Idle,
    val sessionResult: SessionResult? = null,
    val completedStrokes: List<List<Offset>> = emptyList(),
    val activeStroke: List<Offset> = emptyList(),
    val canvasSize: Float = 0f,
    val aiFeedback: HandwritingFeedback? = null,
    val aiLoading: Boolean = false,
    val aiEnabled: Boolean = true
)

@HiltViewModel
class WritingViewModel @Inject constructor(
    private val gameEngine: GameEngine,
    private val completeSessionUseCase: CompleteSessionUseCase,
    private val handwritingChecker: HandwritingChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(WritingUiState())
    val uiState: StateFlow<WritingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            gameEngine.state.collect { state ->
                _uiState.value = _uiState.value.copy(gameState = state)

                if (state is GameState.SessionComplete) {
                    val result = completeSessionUseCase.execute(state.stats)
                    _uiState.value = _uiState.value.copy(sessionResult = result)
                }

                // Clear strokes and AI feedback when a new question appears
                if (state is GameState.AwaitingAnswer) {
                    _uiState.value = _uiState.value.copy(
                        completedStrokes = emptyList(),
                        activeStroke = emptyList(),
                        aiFeedback = null,
                        aiLoading = false
                    )
                }
            }
        }
    }

    fun startGame(questionCount: Int = 10) {
        viewModelScope.launch {
            gameEngine.onEvent(GameEvent.StartSession(GameMode.WRITING, questionCount))
        }
    }

    fun onCanvasSizeChanged(size: Float) {
        _uiState.value = _uiState.value.copy(canvasSize = size)
    }

    fun onDragStart(offset: Offset) {
        _uiState.value = _uiState.value.copy(activeStroke = listOf(offset))
    }

    fun onDrag(offset: Offset) {
        val current = _uiState.value.activeStroke
        _uiState.value = _uiState.value.copy(activeStroke = current + offset)
    }

    fun onDragEnd() {
        val active = _uiState.value.activeStroke
        if (active.size >= 2) {
            _uiState.value = _uiState.value.copy(
                completedStrokes = _uiState.value.completedStrokes + listOf(active),
                activeStroke = emptyList()
            )
        } else {
            _uiState.value = _uiState.value.copy(activeStroke = emptyList())
        }
    }

    fun undoLastStroke() {
        val strokes = _uiState.value.completedStrokes
        if (strokes.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                completedStrokes = strokes.dropLast(1)
            )
        }
    }

    fun setAiEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(aiEnabled = enabled)
    }

    fun clearStrokes() {
        _uiState.value = _uiState.value.copy(
            completedStrokes = emptyList(),
            activeStroke = emptyList()
        )
    }

    fun submitDrawing() {
        val state = _uiState.value.gameState
        if (state !is GameState.AwaitingAnswer) return

        val strokePaths = state.question.strokePaths
        val canvasSize = _uiState.value.canvasSize

        // Convert drawn Offsets to Points (in canvas pixel coordinates)
        val drawnStrokes = _uiState.value.completedStrokes.map { stroke ->
            stroke.map { offset -> Point(offset.x, offset.y) }
        }

        // Parse reference strokes from SVG and convert to point lists
        // Scale reference from KanjiVG 109x109 to canvas coordinates for fair comparison
        val referenceStrokes = strokePaths.map { pathData ->
            val rawPoints = SvgPathParser.parseSvgPath(pathData)
            if (canvasSize > 0f) {
                val scale = canvasSize / 109f
                rawPoints.map { p -> Point(p.x * scale, p.y * scale) }
            } else {
                rawPoints
            }
        }

        // Run stroke matching with SRS-aware threshold
        val srsState = state.question.srsState
        val result = StrokeMatcher.validateWriting(drawnStrokes, referenceStrokes, srsState)

        // Encode result as "isCorrect|quality" for GameEngine
        val answer = "${result.isCorrect}|${result.quality}"

        viewModelScope.launch {
            gameEngine.onEvent(GameEvent.SubmitAnswer(answer))
        }

        // Fire-and-forget AI check (runs in parallel, updates UI when done)
        if (_uiState.value.aiEnabled) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(aiLoading = true)
                try {
                    val feedback = handwritingChecker.evaluate(
                        drawnStrokes = drawnStrokes,
                        targetKanji = state.question.kanjiLiteral,
                        strokeCount = state.question.strokePaths.size,
                        canvasSize = canvasSize
                    )
                    _uiState.value = _uiState.value.copy(aiFeedback = feedback, aiLoading = false)
                } catch (_: Exception) {
                    _uiState.value = _uiState.value.copy(
                        aiFeedback = HandwritingFeedback(
                            overallComment = "",
                            strokeFeedback = emptyList(),
                            qualityRating = 0,
                            isAvailable = false
                        ),
                        aiLoading = false
                    )
                }
            }
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
        _uiState.value = WritingUiState()
    }
}
