package com.jworks.kanjiquest.android.ui.game.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import com.jworks.kanjiquest.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CameraChallengeState {
    data object Loading : CameraChallengeState()
    data class ShowTarget(
        val targetKanji: Kanji,
        val challengeNumber: Int,
        val totalChallenges: Int,
        val successCount: Int,
        val sessionXp: Int
    ) : CameraChallengeState()
    data class Success(
        val targetKanji: Kanji,
        val challengeNumber: Int,
        val totalChallenges: Int,
        val successCount: Int,
        val sessionXp: Int,
        val xpGained: Int
    ) : CameraChallengeState()
    data class SessionComplete(
        val totalChallenges: Int,
        val successCount: Int,
        val accuracy: Int,
        val totalXp: Int
    ) : CameraChallengeState()
    data class Error(val message: String) : CameraChallengeState()
}

data class CameraChallengeUiState(
    val state: CameraChallengeState = CameraChallengeState.Loading,
    val isScanning: Boolean = false
)

@HiltViewModel
class CameraChallengeViewModel @Inject constructor(
    private val kanjiRepository: KanjiRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraChallengeUiState())
    val uiState: StateFlow<CameraChallengeUiState> = _uiState.asStateFlow()

    private var availableKanji: List<Kanji> = emptyList()
    private var usedKanjiIds = mutableSetOf<Int>()
    private var challengeNumber = 0
    private val totalChallenges = 5
    private var successCount = 0
    private var sessionXp = 0
    private var targetKanjiId: Int? = null

    fun startSession(targetKanjiId: Int? = null) {
        this.targetKanjiId = targetKanjiId
        viewModelScope.launch {
            try {
                if (targetKanjiId != null) {
                    // Targeted session: use the specific kanji
                    val kanji = kanjiRepository.getKanjiById(targetKanjiId)
                    if (kanji != null) {
                        availableKanji = listOf(kanji)
                    } else {
                        _uiState.value = CameraChallengeUiState(
                            state = CameraChallengeState.Error("Kanji not found")
                        )
                        return@launch
                    }
                } else {
                    // Load easier kanji (grades 1-3) for camera challenges
                    val grade1 = kanjiRepository.getKanjiByGrade(1)
                    val grade2 = kanjiRepository.getKanjiByGrade(2)
                    val grade3 = kanjiRepository.getKanjiByGrade(3)
                    availableKanji = (grade1 + grade2 + grade3).shuffled()
                }

                if (availableKanji.isEmpty()) {
                    _uiState.value = CameraChallengeUiState(
                        state = CameraChallengeState.Error("No kanji available for challenges")
                    )
                    return@launch
                }

                nextChallenge()
            } catch (e: Exception) {
                _uiState.value = CameraChallengeUiState(
                    state = CameraChallengeState.Error("Failed to load kanji: ${e.message}")
                )
            }
        }
    }

    fun nextChallenge() {
        if (challengeNumber >= totalChallenges) {
            completeSession()
            return
        }

        challengeNumber++

        // Select a kanji that hasn't been used yet
        val targetKanji = availableKanji
            .filterNot { it.id in usedKanjiIds }
            .randomOrNull() ?: availableKanji.random()

        usedKanjiIds.add(targetKanji.id)

        _uiState.value = CameraChallengeUiState(
            state = CameraChallengeState.ShowTarget(
                targetKanji = targetKanji,
                challengeNumber = challengeNumber,
                totalChallenges = totalChallenges,
                successCount = successCount,
                sessionXp = sessionXp
            ),
            isScanning = false
        )
    }

    fun onTextRecognized(recognizedText: String) {
        val currentState = _uiState.value.state
        if (currentState !is CameraChallengeState.ShowTarget || _uiState.value.isScanning) {
            return
        }

        // Set scanning flag to prevent duplicate processing
        _uiState.value = _uiState.value.copy(isScanning = true)

        // Check if the recognized text contains the target kanji
        if (recognizedText.contains(currentState.targetKanji.literal)) {
            onSuccess(currentState.targetKanji)
        } else {
            // Not a match, allow continued scanning
            _uiState.value = _uiState.value.copy(isScanning = false)
        }
    }

    private fun onSuccess(targetKanji: Kanji) {
        successCount++
        val xpGained = 50 // Base XP for successful camera scan

        sessionXp += xpGained

        _uiState.value = CameraChallengeUiState(
            state = CameraChallengeState.Success(
                targetKanji = targetKanji,
                challengeNumber = challengeNumber,
                totalChallenges = totalChallenges,
                successCount = successCount,
                sessionXp = sessionXp,
                xpGained = xpGained
            ),
            isScanning = false
        )
    }

    private fun completeSession() {
        viewModelScope.launch {
            try {
                // Award XP to user
                if (sessionXp > 0) {
                    val profile = userRepository.getProfile()
                    val newXp = profile.totalXp + sessionXp
                    userRepository.updateXpAndLevel(newXp, profile.level)
                }

                val accuracy = (successCount.toFloat() / totalChallenges * 100).toInt()

                _uiState.value = CameraChallengeUiState(
                    state = CameraChallengeState.SessionComplete(
                        totalChallenges = totalChallenges,
                        successCount = successCount,
                        accuracy = accuracy,
                        totalXp = sessionXp
                    )
                )
            } catch (e: Exception) {
                _uiState.value = CameraChallengeUiState(
                    state = CameraChallengeState.Error("Failed to save progress: ${e.message}")
                )
            }
        }
    }

    fun reset() {
        challengeNumber = 0
        successCount = 0
        sessionXp = 0
        usedKanjiIds.clear()
        _uiState.value = CameraChallengeUiState()
    }
}
