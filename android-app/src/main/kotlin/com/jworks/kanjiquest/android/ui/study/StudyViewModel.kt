package com.jworks.kanjiquest.android.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.android.data.PreviewTrialManager
import com.jworks.kanjiquest.core.domain.UserSessionProvider
import com.jworks.kanjiquest.core.domain.model.FlashcardDeckGroup
import com.jworks.kanjiquest.core.domain.model.GameMode
import com.jworks.kanjiquest.core.domain.model.UserLevel
import com.jworks.kanjiquest.core.domain.repository.FlashcardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudyUiState(
    val selectedTab: ContentTab = ContentTab.KANJI,
    val selectedMode: GameMode? = null,
    val selectedSource: StudySource = StudySource.All,
    val kanaFilter: KanaFilter = KanaFilter.HIRAGANA_ONLY,
    val isPremium: Boolean = false,
    val isAdmin: Boolean = false,
    val decks: List<FlashcardDeckGroup> = emptyList(),
    val previewTrialsRemaining: Map<GameMode, Int> = emptyMap()
)

@HiltViewModel
class StudyViewModel @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    private val userSessionProvider: UserSessionProvider,
    private val previewTrialManager: PreviewTrialManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val effectiveLevel = userSessionProvider.getEffectiveLevel()
            val isPremium = effectiveLevel == UserLevel.PREMIUM || effectiveLevel == UserLevel.ADMIN
            val decks = try { flashcardRepository.getAllDeckGroups() } catch (_: Exception) { emptyList() }

            val trials = mapOf(
                GameMode.WRITING to previewTrialManager.getRemainingTrials(GameMode.WRITING),
                GameMode.VOCABULARY to previewTrialManager.getRemainingTrials(GameMode.VOCABULARY),
                GameMode.CAMERA_CHALLENGE to previewTrialManager.getRemainingTrials(GameMode.CAMERA_CHALLENGE)
            )

            _uiState.value = _uiState.value.copy(
                isPremium = isPremium,
                isAdmin = userSessionProvider.isAdmin(),
                decks = decks,
                previewTrialsRemaining = trials,
                selectedMode = getDefaultModeForTab(_uiState.value.selectedTab)
            )
        }
    }

    fun selectTab(tab: ContentTab) {
        _uiState.value = _uiState.value.copy(
            selectedTab = tab,
            selectedMode = getDefaultModeForTab(tab),
            selectedSource = StudySource.All
        )
    }

    fun selectMode(mode: GameMode) {
        _uiState.value = _uiState.value.copy(selectedMode = mode)
    }

    fun selectSource(source: StudySource) {
        _uiState.value = _uiState.value.copy(selectedSource = source)
    }

    fun selectKanaFilter(filter: KanaFilter) {
        _uiState.value = _uiState.value.copy(kanaFilter = filter)
    }

    fun usePreviewTrial(mode: GameMode): Boolean {
        val success = previewTrialManager.usePreviewTrial(mode)
        if (success) {
            _uiState.value = _uiState.value.copy(
                previewTrialsRemaining = _uiState.value.previewTrialsRemaining.toMutableMap().apply {
                    this[mode] = previewTrialManager.getRemainingTrials(mode)
                }
            )
        }
        return success
    }

    private fun getDefaultModeForTab(tab: ContentTab): GameMode {
        return when (tab) {
            ContentTab.KANA -> GameMode.KANA_RECOGNITION
            ContentTab.RADICALS -> GameMode.RADICAL_RECOGNITION
            ContentTab.KANJI -> GameMode.RECOGNITION
        }
    }

    fun getAvailableModes(tab: ContentTab): List<GameMode> {
        return when (tab) {
            ContentTab.KANA -> listOf(GameMode.KANA_RECOGNITION, GameMode.KANA_WRITING)
            ContentTab.RADICALS -> listOf(GameMode.RADICAL_RECOGNITION)
            ContentTab.KANJI -> listOf(GameMode.RECOGNITION, GameMode.WRITING, GameMode.VOCABULARY, GameMode.CAMERA_CHALLENGE)
        }
    }

    fun isModeAccessible(mode: GameMode): Boolean {
        val state = _uiState.value
        if (state.isPremium || state.isAdmin) return true
        return when (mode) {
            GameMode.RECOGNITION, GameMode.KANA_RECOGNITION, GameMode.KANA_WRITING,
            GameMode.RADICAL_RECOGNITION -> true
            GameMode.WRITING -> (state.previewTrialsRemaining[GameMode.WRITING] ?: 0) > 0
            GameMode.VOCABULARY -> (state.previewTrialsRemaining[GameMode.VOCABULARY] ?: 0) > 0
            GameMode.CAMERA_CHALLENGE -> (state.previewTrialsRemaining[GameMode.CAMERA_CHALLENGE] ?: 0) > 0
            else -> false
        }
    }
}
