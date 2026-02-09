package com.jworks.kanjiquest.android.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.UserSessionProvider
import com.jworks.kanjiquest.core.domain.model.GradeMastery
import com.jworks.kanjiquest.core.domain.model.LevelProgression
import com.jworks.kanjiquest.core.domain.model.UserProfile
import com.jworks.kanjiquest.core.domain.model.StudySession
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import com.jworks.kanjiquest.core.domain.repository.SessionRepository
import com.jworks.kanjiquest.core.domain.repository.SrsRepository
import com.jworks.kanjiquest.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgressUiState(
    val profile: UserProfile = UserProfile(),
    val masteredCount: Long = 0,
    val totalKanjiInSrs: Long = 0,
    val recentSessions: List<StudySession> = emptyList(),
    val totalGamesPlayed: Int = 0,
    val totalCardsStudied: Int = 0,
    val overallAccuracy: Float = 0f,
    val gradeMasteryList: List<GradeMastery> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val srsRepository: SrsRepository,
    private val sessionRepository: SessionRepository,
    private val kanjiRepository: KanjiRepository,
    private val userSessionProvider: UserSessionProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                // Get user profile (XP, level, streaks)
                val profile = userRepository.getProfile()

                // Get kanji mastery stats
                val masteredCount = srsRepository.getMasteredCount()
                val newCount = srsRepository.getNewCount()
                val dueCount = srsRepository.getDueCount(System.currentTimeMillis())
                val totalInSrs = masteredCount + newCount + dueCount

                // Get recent study sessions
                val recentSessions = sessionRepository.getRecentSessions(limit = 10)

                // Calculate overall stats from sessions
                val totalGamesPlayed = recentSessions.size
                val totalCardsStudied = recentSessions.sumOf { it.cardsStudied }
                val totalCorrect = recentSessions.sumOf { it.correctCount }
                val overallAccuracy = if (totalCardsStudied > 0) {
                    (totalCorrect.toFloat() / totalCardsStudied.toFloat()) * 100f
                } else {
                    0f
                }

                // Compute per-grade mastery
                val playerLevel = userSessionProvider.getAdminPlayerLevelOverride() ?: profile.level
                val unlockedGrades = LevelProgression.getUnlockedGrades(playerLevel)
                val gradeMasteryList = unlockedGrades.map { grade ->
                    val total = kanjiRepository.getKanjiCountByGrade(grade)
                    srsRepository.getGradeMastery(grade, total)
                }

                _uiState.value = ProgressUiState(
                    profile = profile,
                    masteredCount = masteredCount,
                    totalKanjiInSrs = totalInSrs,
                    recentSessions = recentSessions,
                    totalGamesPlayed = totalGamesPlayed,
                    totalCardsStudied = totalCardsStudied,
                    overallAccuracy = overallAccuracy,
                    gradeMasteryList = gradeMasteryList,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadStats()
    }
}
