package com.jworks.kanjiquest.android.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.model.Achievement
import com.jworks.kanjiquest.core.domain.repository.AchievementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AchievementCategory(
    val name: String,
    val achievements: List<AchievementDefinition>
)

data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val category: String,
    val target: Int,
    val progress: Int = 0,
    val unlockedAt: Long? = null
) {
    val isUnlocked: Boolean get() = unlockedAt != null
    val progressPercent: Float get() = if (target > 0) (progress.toFloat() / target.toFloat()) * 100f else 0f
}

data class AchievementsUiState(
    val categories: List<AchievementCategory> = emptyList(),
    val totalAchievements: Int = 0,
    val unlockedCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val achievementRepository: AchievementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        loadAchievements()
    }

    private fun loadAchievements() {
        viewModelScope.launch {
            try {
                // Get all achievements from DB
                val dbAchievements = achievementRepository.getAllAchievements()

                // Merge with predefined definitions
                val definitions = getPredefinedAchievements()
                val merged = definitions.map { def ->
                    val dbAch = dbAchievements.find { it.id == def.id }
                    def.copy(
                        progress = dbAch?.progress ?: 0,
                        unlockedAt = dbAch?.unlockedAt
                    )
                }

                // Group by category
                val categories = merged.groupBy { it.category }.map { (categoryName, achievements) ->
                    AchievementCategory(
                        name = categoryName,
                        achievements = achievements.sortedBy { it.id }
                    )
                }.sortedBy { it.name }

                val unlockedCount = merged.count { it.isUnlocked }

                _uiState.value = AchievementsUiState(
                    categories = categories,
                    totalAchievements = merged.size,
                    unlockedCount = unlockedCount,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadAchievements()
    }

    // Predefined achievement definitions
    private fun getPredefinedAchievements(): List<AchievementDefinition> {
        return listOf(
            // XP Achievements
            AchievementDefinition(
                id = "xp_100",
                title = "Getting Started",
                description = "Earn 100 total XP",
                icon = "⭐",
                category = "Progress",
                target = 100
            ),
            AchievementDefinition(
                id = "xp_500",
                title = "Dedicated Learner",
                description = "Earn 500 total XP",
                icon = "🌟",
                category = "Progress",
                target = 500
            ),
            AchievementDefinition(
                id = "xp_1000",
                title = "XP Master",
                description = "Earn 1,000 total XP",
                icon = "✨",
                category = "Progress",
                target = 1000
            ),
            AchievementDefinition(
                id = "xp_5000",
                title = "XP Legend",
                description = "Earn 5,000 total XP",
                icon = "💫",
                category = "Progress",
                target = 5000
            ),

            // Level Achievements
            AchievementDefinition(
                id = "level_5",
                title = "Level 5",
                description = "Reach level 5",
                icon = "5️⃣",
                category = "Progress",
                target = 5
            ),
            AchievementDefinition(
                id = "level_10",
                title = "Level 10",
                description = "Reach level 10",
                icon = "🔟",
                category = "Progress",
                target = 10
            ),
            AchievementDefinition(
                id = "level_20",
                title = "Level 20",
                description = "Reach level 20",
                icon = "💪",
                category = "Progress",
                target = 20
            ),

            // Kanji Mastery Achievements
            AchievementDefinition(
                id = "kanji_10",
                title = "First Ten",
                description = "Master 10 kanji",
                icon = "漢",
                category = "Mastery",
                target = 10
            ),
            AchievementDefinition(
                id = "kanji_50",
                title = "Half Century",
                description = "Master 50 kanji",
                icon = "📚",
                category = "Mastery",
                target = 50
            ),
            AchievementDefinition(
                id = "kanji_100",
                title = "Centurion",
                description = "Master 100 kanji",
                icon = "💯",
                category = "Mastery",
                target = 100
            ),
            AchievementDefinition(
                id = "kanji_500",
                title = "Kanji Expert",
                description = "Master 500 kanji",
                icon = "🎓",
                category = "Mastery",
                target = 500
            ),
            AchievementDefinition(
                id = "kanji_1000",
                title = "Kanji Master",
                description = "Master 1,000 kanji",
                icon = "👑",
                category = "Mastery",
                target = 1000
            ),

            // Streak Achievements
            AchievementDefinition(
                id = "streak_3",
                title = "Three Day Streak",
                description = "Study for 3 days in a row",
                icon = "🔥",
                category = "Consistency",
                target = 3
            ),
            AchievementDefinition(
                id = "streak_7",
                title = "Week Warrior",
                description = "Study for 7 days in a row",
                icon = "📅",
                category = "Consistency",
                target = 7
            ),
            AchievementDefinition(
                id = "streak_30",
                title = "Month Master",
                description = "Study for 30 days in a row",
                icon = "🏆",
                category = "Consistency",
                target = 30
            ),
            AchievementDefinition(
                id = "streak_100",
                title = "Unstoppable",
                description = "Study for 100 days in a row",
                icon = "⚡",
                category = "Consistency",
                target = 100
            ),

            // Game Mode Achievements
            AchievementDefinition(
                id = "games_10",
                title = "Getting Started",
                description = "Complete 10 game sessions",
                icon = "🎮",
                category = "Games",
                target = 10
            ),
            AchievementDefinition(
                id = "games_50",
                title = "Frequent Player",
                description = "Complete 50 game sessions",
                icon = "🕹️",
                category = "Games",
                target = 50
            ),
            AchievementDefinition(
                id = "games_100",
                title = "Game Master",
                description = "Complete 100 game sessions",
                icon = "🎯",
                category = "Games",
                target = 100
            ),

            // Perfect Score Achievements
            AchievementDefinition(
                id = "perfect_score",
                title = "Perfect Score",
                description = "Get 100% accuracy in a session",
                icon = "💯",
                category = "Accuracy",
                target = 1
            ),
            AchievementDefinition(
                id = "perfect_5",
                title = "Perfectionist",
                description = "Get 100% accuracy in 5 sessions",
                icon = "✨",
                category = "Accuracy",
                target = 5
            ),

            // J Coin Achievements
            AchievementDefinition(
                id = "coins_100",
                title = "Coin Collector",
                description = "Earn 100 J Coins",
                icon = "🪙",
                category = "Rewards",
                target = 100
            ),
            AchievementDefinition(
                id = "coins_500",
                title = "Coin Hoarder",
                description = "Earn 500 J Coins",
                icon = "💰",
                category = "Rewards",
                target = 500
            ),
        )
    }
}
