package com.jworks.kanjiquest.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.android.data.PreviewTrialManager
import com.jworks.kanjiquest.core.domain.UserSessionProvider
import com.jworks.kanjiquest.core.domain.UserSessionProviderImpl
import com.jworks.kanjiquest.core.domain.model.CoinBalance
import com.jworks.kanjiquest.core.domain.model.GameMode
import com.jworks.kanjiquest.core.domain.model.GradeMastery
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.model.LevelProgression
import com.jworks.kanjiquest.core.domain.model.UserLevel
import com.jworks.kanjiquest.core.domain.model.UserProfile
import com.jworks.kanjiquest.core.domain.model.Vocabulary
import com.jworks.kanjiquest.core.domain.repository.JCoinRepository
import com.jworks.kanjiquest.core.domain.repository.AuthRepository
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import com.jworks.kanjiquest.core.domain.repository.SrsRepository
import com.jworks.kanjiquest.core.domain.repository.UserRepository
import com.jworks.kanjiquest.core.domain.usecase.WordOfTheDayUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PreviewTrialInfo(
    val remaining: Int,
    val limit: Int
)

data class HomeUiState(
    val profile: UserProfile = UserProfile(),
    val gradeOneKanji: List<Kanji> = emptyList(),
    val kanjiCount: Long = 0,
    val coinBalance: CoinBalance = CoinBalance.empty(),
    val wordOfTheDay: Vocabulary? = null,
    val isLoading: Boolean = true,
    val isPremium: Boolean = false,
    val isAdmin: Boolean = false,
    val effectiveLevel: UserLevel = UserLevel.FREE,
    val previewTrials: Map<GameMode, PreviewTrialInfo> = emptyMap(),
    val tierName: String = "Beginner",
    val tierNameJp: String = "入門",
    val tierProgress: Float = 0f,
    val nextTierName: String? = "Novice",
    val nextTierLevel: Int? = 5,
    val highestUnlockedGrade: Int = 1,
    val gradeMasteryList: List<GradeMastery> = emptyList(),
    val displayLevel: Int = 1
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val kanjiRepository: KanjiRepository,
    private val userRepository: UserRepository,
    private val srsRepository: SrsRepository,
    private val jCoinRepository: JCoinRepository,
    private val wordOfTheDayUseCase: WordOfTheDayUseCase,
    private val userSessionProvider: UserSessionProvider,
    private val authRepository: AuthRepository,
    private val previewTrialManager: PreviewTrialManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeCoinBalance()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Sync email from auth state so admin detection works
            val authState = authRepository.observeAuthState().first()
            if (authState.email != null) {
                (userSessionProvider as? UserSessionProviderImpl)?.updateEmail(authState.email)
            }

            val userId = userSessionProvider.getUserId()
            val profile = userRepository.getProfile()
            val totalCount = kanjiRepository.getKanjiCount()
            val coinBalance = jCoinRepository.getBalance(userId)
            val wotd = wordOfTheDayUseCase.getWordOfTheDay()
            val effectiveLevel = userSessionProvider.getEffectiveLevel()
            val isPremium = effectiveLevel == UserLevel.PREMIUM || effectiveLevel == UserLevel.ADMIN

            // Tier progression info
            val playerLevel = userSessionProvider.getAdminPlayerLevelOverride() ?: profile.level
            val tier = LevelProgression.getTierForLevel(playerLevel)
            val nextTier = LevelProgression.getNextTier(playerLevel)
            val highestGrade = tier.unlockedGrades.maxOrNull() ?: 1
            val gradeKanji = kanjiRepository.getKanjiByGrade(highestGrade)

            // Compute grade mastery for all unlocked grades
            val gradeMasteryList = tier.unlockedGrades.map { grade ->
                val total = kanjiRepository.getKanjiCountByGrade(grade)
                srsRepository.getGradeMastery(grade, total)
            }

            _uiState.value = HomeUiState(
                profile = profile,
                gradeOneKanji = gradeKanji,
                kanjiCount = totalCount,
                coinBalance = coinBalance,
                wordOfTheDay = wotd,
                isLoading = false,
                isPremium = isPremium,
                isAdmin = userSessionProvider.isAdmin(),
                effectiveLevel = effectiveLevel,
                previewTrials = loadPreviewTrials(),
                tierName = tier.nameEn,
                tierNameJp = tier.nameJp,
                tierProgress = LevelProgression.getTierProgress(playerLevel),
                nextTierName = nextTier?.nameEn,
                nextTierLevel = nextTier?.levelRange?.first,
                highestUnlockedGrade = highestGrade,
                gradeMasteryList = gradeMasteryList,
                displayLevel = playerLevel
            )
        }
    }

    private fun loadPreviewTrials(): Map<GameMode, PreviewTrialInfo> {
        return mapOf(
            GameMode.WRITING to PreviewTrialInfo(
                remaining = previewTrialManager.getRemainingTrials(GameMode.WRITING),
                limit = previewTrialManager.getTrialLimit(GameMode.WRITING)
            ),
            GameMode.VOCABULARY to PreviewTrialInfo(
                remaining = previewTrialManager.getRemainingTrials(GameMode.VOCABULARY),
                limit = previewTrialManager.getTrialLimit(GameMode.VOCABULARY)
            ),
            GameMode.CAMERA_CHALLENGE to PreviewTrialInfo(
                remaining = previewTrialManager.getRemainingTrials(GameMode.CAMERA_CHALLENGE),
                limit = previewTrialManager.getTrialLimit(GameMode.CAMERA_CHALLENGE)
            )
        )
    }

    /** Use a preview trial and return true if successful */
    fun usePreviewTrial(mode: GameMode): Boolean {
        val success = previewTrialManager.usePreviewTrial(mode)
        if (success) {
            _uiState.value = _uiState.value.copy(previewTrials = loadPreviewTrials())
        }
        return success
    }

    fun refreshTrials() {
        _uiState.value = _uiState.value.copy(previewTrials = loadPreviewTrials())
    }

    fun refresh() {
        loadData()
    }

    private fun observeCoinBalance() {
        viewModelScope.launch {
            val userId = userSessionProvider.getUserId()
            jCoinRepository.observeBalance(userId).collect { balance ->
                _uiState.value = _uiState.value.copy(coinBalance = balance)
            }
        }
    }
}
