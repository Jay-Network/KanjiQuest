package com.jworks.kanjiquest.android.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.UserSessionProvider
import com.jworks.kanjiquest.core.domain.model.UserLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GamesUiState(
    val isPremium: Boolean = false
)

@HiltViewModel
class GamesViewModel @Inject constructor(
    private val userSessionProvider: UserSessionProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(GamesUiState())
    val uiState: StateFlow<GamesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val effectiveLevel = userSessionProvider.getEffectiveLevel()
            _uiState.value = GamesUiState(
                isPremium = effectiveLevel == UserLevel.PREMIUM || effectiveLevel == UserLevel.ADMIN
            )
        }
    }
}
