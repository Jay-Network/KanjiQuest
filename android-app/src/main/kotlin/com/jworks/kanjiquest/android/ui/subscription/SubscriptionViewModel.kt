package com.jworks.kanjiquest.android.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.UserSessionProvider
import com.jworks.kanjiquest.core.domain.model.Subscription
import com.jworks.kanjiquest.core.domain.model.SubscriptionPlan
import com.jworks.kanjiquest.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionUiState(
    val isLoggedIn: Boolean = false,
    val isPremium: Boolean = false,
    val subscription: Subscription? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userSessionProvider: UserSessionProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        loadSubscription()
    }

    private fun loadSubscription() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            val subscription = authRepository.getSubscription()
            _uiState.value = SubscriptionUiState(
                isLoggedIn = userId != null,
                isPremium = subscription?.plan == SubscriptionPlan.PREMIUM,
                subscription = subscription,
                isLoading = false
            )
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadSubscription()
    }
}
