package com.jworks.kanjiquest.android.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.UserSessionProvider
import com.jworks.kanjiquest.core.domain.model.LevelProgression
import com.jworks.kanjiquest.core.domain.model.UserLevel
import com.jworks.kanjiquest.core.domain.repository.DevChatRepository
import com.jworks.kanjiquest.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val vibrationsEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val dailyGoal: Int = 20,
    val sessionLength: Int = 10,
    val difficulty: DifficultyLevel = DifficultyLevel.MEDIUM,
    val autoPlayAudio: Boolean = true,
    val showHints: Boolean = true,
    val theme: AppTheme = AppTheme.SYSTEM,
    val isAdmin: Boolean = false,
    val isDeveloper: Boolean = false,
    val effectiveLevel: UserLevel = UserLevel.FREE,
    val adminOverrideLevel: UserLevel? = null,
    val adminPlayerLevelOverride: Int? = null,
    val currentPlayerLevel: Int = 1
)

enum class DifficultyLevel(val displayName: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard")
}

enum class AppTheme(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System Default")
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val userSessionProvider: UserSessionProvider,
    private val devChatRepository: DevChatRepository
) : ViewModel() {

    private val prefs = context.getSharedPreferences("kanjiquest_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val profile = try { userRepository.getProfile() } catch (_: Exception) { null }
            val email = userSessionProvider.getUserEmail()
            val isDev = if (email != null) {
                try { devChatRepository.isDeveloper(email) } catch (_: Exception) { false }
            } else false
            _uiState.value = SettingsUiState(
                soundEnabled = prefs.getBoolean("sound_enabled", true),
                musicEnabled = prefs.getBoolean("music_enabled", true),
                vibrationsEnabled = prefs.getBoolean("vibrations_enabled", true),
                notificationsEnabled = prefs.getBoolean("notifications_enabled", true),
                dailyGoal = prefs.getInt("daily_goal", 20),
                sessionLength = prefs.getInt("session_length", 10),
                difficulty = DifficultyLevel.valueOf(
                    prefs.getString("difficulty", DifficultyLevel.MEDIUM.name) ?: DifficultyLevel.MEDIUM.name
                ),
                autoPlayAudio = prefs.getBoolean("auto_play_audio", true),
                showHints = prefs.getBoolean("show_hints", true),
                theme = AppTheme.valueOf(
                    prefs.getString("theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
                ),
                isAdmin = userSessionProvider.isAdmin(),
                isDeveloper = isDev,
                effectiveLevel = userSessionProvider.getEffectiveLevel(),
                adminOverrideLevel = userSessionProvider.getAdminOverrideLevel(),
                adminPlayerLevelOverride = userSessionProvider.getAdminPlayerLevelOverride(),
                currentPlayerLevel = profile?.level ?: 1
            )
        }
    }

    fun setAdminOverrideLevel(level: UserLevel?) {
        userSessionProvider.setAdminOverrideLevel(level)
        _uiState.value = _uiState.value.copy(
            adminOverrideLevel = level,
            effectiveLevel = userSessionProvider.getEffectiveLevel()
        )
    }

    fun setAdminPlayerLevelOverride(level: Int?) {
        userSessionProvider.setAdminPlayerLevelOverride(level)
        _uiState.value = _uiState.value.copy(adminPlayerLevelOverride = level)
    }

    fun toggleSound() {
        val newValue = !_uiState.value.soundEnabled
        prefs.edit().putBoolean("sound_enabled", newValue).apply()
        _uiState.value = _uiState.value.copy(soundEnabled = newValue)
    }

    fun toggleMusic() {
        val newValue = !_uiState.value.musicEnabled
        prefs.edit().putBoolean("music_enabled", newValue).apply()
        _uiState.value = _uiState.value.copy(musicEnabled = newValue)
    }

    fun toggleVibrations() {
        val newValue = !_uiState.value.vibrationsEnabled
        prefs.edit().putBoolean("vibrations_enabled", newValue).apply()
        _uiState.value = _uiState.value.copy(vibrationsEnabled = newValue)
    }

    fun toggleNotifications() {
        val newValue = !_uiState.value.notificationsEnabled
        prefs.edit().putBoolean("notifications_enabled", newValue).apply()
        _uiState.value = _uiState.value.copy(notificationsEnabled = newValue)
    }

    fun toggleAutoPlayAudio() {
        val newValue = !_uiState.value.autoPlayAudio
        prefs.edit().putBoolean("auto_play_audio", newValue).apply()
        _uiState.value = _uiState.value.copy(autoPlayAudio = newValue)
    }

    fun toggleShowHints() {
        val newValue = !_uiState.value.showHints
        prefs.edit().putBoolean("show_hints", newValue).apply()
        _uiState.value = _uiState.value.copy(showHints = newValue)
    }

    fun setDailyGoal(goal: Int) {
        prefs.edit().putInt("daily_goal", goal).apply()
        _uiState.value = _uiState.value.copy(dailyGoal = goal)

        viewModelScope.launch {
            try {
                userRepository.updateDailyGoal(goal)
            } catch (e: Exception) {
                // Handle error silently, SharedPreferences still saved
            }
        }
    }

    fun setSessionLength(length: Int) {
        prefs.edit().putInt("session_length", length).apply()
        _uiState.value = _uiState.value.copy(sessionLength = length)
    }

    fun setDifficulty(difficulty: DifficultyLevel) {
        prefs.edit().putString("difficulty", difficulty.name).apply()
        _uiState.value = _uiState.value.copy(difficulty = difficulty)
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString("theme", theme.name).apply()
        _uiState.value = _uiState.value.copy(theme = theme)
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        loadSettings()
    }
}
