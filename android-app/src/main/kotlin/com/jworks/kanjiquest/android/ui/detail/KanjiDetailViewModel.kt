package com.jworks.kanjiquest.android.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.android.data.PreviewTrialManager
import com.jworks.kanjiquest.core.domain.UserSessionProvider
import com.jworks.kanjiquest.core.domain.model.GameMode
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.model.UserLevel
import com.jworks.kanjiquest.core.domain.model.Vocabulary
import com.jworks.kanjiquest.core.domain.repository.FlashcardRepository
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import com.jworks.kanjiquest.core.domain.repository.SrsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModeTrialInfo(
    val canPractice: Boolean,
    val trialsRemaining: Int
)

data class KanjiDetailUiState(
    val kanji: Kanji? = null,
    val vocabulary: List<Vocabulary> = emptyList(),
    val isLoading: Boolean = true,
    val totalPracticeCount: Int = 0,
    val accuracy: Float? = null,
    val isInFlashcardDeck: Boolean = false,
    val canPracticeWriting: Boolean = false,
    val writingTrialsRemaining: Int = 0,
    val isPremium: Boolean = false,
    val isAdmin: Boolean = false,
    val modeTrials: Map<GameMode, ModeTrialInfo> = emptyMap(),
    val modeStats: Map<String, Int> = emptyMap()
)

@HiltViewModel
class KanjiDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val kanjiRepository: KanjiRepository,
    private val srsRepository: SrsRepository,
    private val flashcardRepository: FlashcardRepository,
    private val userSessionProvider: UserSessionProvider,
    private val previewTrialManager: PreviewTrialManager
) : ViewModel() {

    val kanjiId: Int = checkNotNull(savedStateHandle["kanjiId"])

    private val _uiState = MutableStateFlow(KanjiDetailUiState())
    val uiState: StateFlow<KanjiDetailUiState> = _uiState.asStateFlow()

    init {
        loadKanji()
    }

    private fun loadKanji() {
        viewModelScope.launch {
            val kanji = kanjiRepository.getKanjiById(kanjiId)
            val vocab = if (kanji != null) {
                kanjiRepository.getVocabularyForKanji(kanjiId)
            } else emptyList()

            val srsCard = srsRepository.getCard(kanjiId)
            val totalPractice = srsCard?.totalReviews ?: 0
            val accuracy = if (srsCard != null && srsCard.totalReviews > 0) srsCard.accuracy else null

            val inDeck = flashcardRepository.isInDeck(kanjiId)

            val effectiveLevel = userSessionProvider.getEffectiveLevel()
            val isPremium = effectiveLevel == UserLevel.PREMIUM || effectiveLevel == UserLevel.ADMIN
            val isAdmin = userSessionProvider.isAdmin()
            val writingTrials = previewTrialManager.getRemainingTrials(GameMode.WRITING)
            val canPractice = isPremium || isAdmin || writingTrials > 0

            // Per-mode trial info
            val modeTrials = mapOf(
                GameMode.RECOGNITION to ModeTrialInfo(canPractice = true, trialsRemaining = -1),
                GameMode.WRITING to ModeTrialInfo(
                    canPractice = isPremium || isAdmin || writingTrials > 0,
                    trialsRemaining = writingTrials
                ),
                GameMode.VOCABULARY to run {
                    val trials = previewTrialManager.getRemainingTrials(GameMode.VOCABULARY)
                    ModeTrialInfo(canPractice = isPremium || isAdmin || trials > 0, trialsRemaining = trials)
                },
                GameMode.CAMERA_CHALLENGE to run {
                    val trials = previewTrialManager.getRemainingTrials(GameMode.CAMERA_CHALLENGE)
                    ModeTrialInfo(canPractice = isPremium || isAdmin || trials > 0, trialsRemaining = trials)
                }
            )

            // Per-mode stats
            val modeStats = try {
                val statsMap = srsRepository.getModeStatsByIds(listOf(kanjiId.toLong()))
                statsMap[kanjiId] ?: emptyMap()
            } catch (_: Exception) { emptyMap() }

            _uiState.value = KanjiDetailUiState(
                kanji = kanji,
                vocabulary = vocab,
                isLoading = false,
                totalPracticeCount = totalPractice,
                accuracy = accuracy,
                isInFlashcardDeck = inDeck,
                canPracticeWriting = canPractice,
                writingTrialsRemaining = writingTrials,
                isPremium = isPremium,
                isAdmin = isAdmin,
                modeTrials = modeTrials,
                modeStats = modeStats
            )
        }
    }

    fun toggleFlashcard() {
        viewModelScope.launch {
            val nowInDeck = flashcardRepository.toggleInDeck(kanjiId)
            _uiState.value = _uiState.value.copy(isInFlashcardDeck = nowInDeck)
        }
    }

    fun useWritingTrial(): Boolean {
        return useModeTrial(GameMode.WRITING)
    }

    fun useModeTrial(mode: GameMode): Boolean {
        if (mode == GameMode.RECOGNITION) return true // Always free
        val success = previewTrialManager.usePreviewTrial(mode)
        if (success) {
            val remaining = previewTrialManager.getRemainingTrials(mode)
            val isPremium = _uiState.value.isPremium
            val isAdmin = _uiState.value.isAdmin
            val updatedTrials = _uiState.value.modeTrials.toMutableMap()
            updatedTrials[mode] = ModeTrialInfo(
                canPractice = isPremium || isAdmin || remaining > 0,
                trialsRemaining = remaining
            )
            _uiState.value = _uiState.value.copy(
                modeTrials = updatedTrials,
                writingTrialsRemaining = if (mode == GameMode.WRITING) remaining else _uiState.value.writingTrialsRemaining,
                canPracticeWriting = if (mode == GameMode.WRITING) (isPremium || isAdmin || remaining > 0) else _uiState.value.canPracticeWriting
            )
        }
        return success
    }
}
