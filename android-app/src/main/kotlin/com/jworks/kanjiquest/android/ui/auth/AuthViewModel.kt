package com.jworks.kanjiquest.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.repository.AuthRepository
import com.jworks.kanjiquest.core.domain.repository.AuthState
import com.jworks.kanjiquest.core.domain.usecase.DataRestorationUseCase
import com.jworks.kanjiquest.core.domain.usecase.MigrateLocalDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val isSignUpMode: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val migrateLocalDataUseCase: MigrateLocalDataUseCase,
    private val dataRestorationUseCase: DataRestorationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _authState = MutableStateFlow(AuthState(false, null, null, false))
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        observeAuth()
    }

    private fun observeAuth() {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { state ->
                _authState.value = state
                _uiState.value = _uiState.value.copy(isLoggedIn = state.isLoggedIn)
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.signIn(email, password)
            result.onSuccess { userId ->
                // Migrate local data to the authenticated user
                migrateLocalDataUseCase.execute(userId)
                // Restore/sync learning data with cloud
                dataRestorationUseCase.execute(userId)
                _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = sanitizeError(e, "Sign in failed")
                )
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.signUp(email, password)
            result.onSuccess { userId ->
                migrateLocalDataUseCase.execute(userId)
                // Restore/sync learning data with cloud
                dataRestorationUseCase.execute(userId)
                _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = sanitizeError(e, "Sign up failed")
                )
            }
        }
    }

    private fun sanitizeError(e: Throwable, fallback: String): String {
        val msg = e.message ?: return fallback
        // Extract only the user-facing message, strip HTTP details (headers, URLs, keys)
        return when {
            "Invalid login credentials" in msg -> "Invalid email or password"
            "Email not confirmed" in msg -> "Please confirm your email first"
            "User already registered" in msg -> "An account with this email already exists"
            "Auth not configured" in msg -> "Authentication is not available"
            "rate limit" in msg.lowercase() -> "Too many attempts. Please try again later."
            else -> fallback
        }
    }

    fun toggleSignUpMode() {
        _uiState.value = _uiState.value.copy(
            isSignUpMode = !_uiState.value.isSignUpMode,
            error = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
