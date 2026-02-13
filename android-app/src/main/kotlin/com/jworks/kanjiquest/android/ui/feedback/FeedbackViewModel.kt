package com.jworks.kanjiquest.android.ui.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.UserSessionProvider
import com.jworks.kanjiquest.core.domain.model.Feedback
import com.jworks.kanjiquest.core.domain.model.FeedbackCategory
import com.jworks.kanjiquest.core.domain.model.FeedbackWithHistory
import com.jworks.kanjiquest.core.domain.model.SubmitFeedbackResult
import com.jworks.kanjiquest.core.domain.repository.FeedbackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedbackUiState(
    val feedbackList: List<FeedbackWithHistory> = emptyList(),
    val isDialogOpen: Boolean = false,
    val isSubmitting: Boolean = false,
    val selectedCategory: FeedbackCategory = FeedbackCategory.OTHER,
    val feedbackText: String = "",
    val error: String? = null,
    val successMessage: String? = null,
    val isLoadingHistory: Boolean = false
)

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val feedbackRepository: FeedbackRepository,
    private val userSessionProvider: UserSessionProvider
) : ViewModel() {

    companion object {
        private const val APP_ID = "kanjiquests"
        private const val POLL_INTERVAL_MS = 15_000L // 15 seconds
    }

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var cachedEmail: String? = null
    private var lastFeedbackId: Long? = null

    init {
        viewModelScope.launch {
            cachedEmail = userSessionProvider.getUserEmail()
        }
    }

    fun openDialog() {
        _uiState.value = _uiState.value.copy(
            isDialogOpen = true,
            feedbackText = "",
            selectedCategory = FeedbackCategory.OTHER,
            error = null,
            successMessage = null
        )

        // Refresh cached email then load history (sequential to avoid race condition)
        viewModelScope.launch {
            cachedEmail = userSessionProvider.getUserEmail()
            loadFeedbackHistory()
        }

        // Start polling for updates
        startPolling()
    }

    fun closeDialog() {
        _uiState.value = _uiState.value.copy(isDialogOpen = false)
        stopPolling()
    }

    fun selectCategory(category: FeedbackCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun updateFeedbackText(text: String) {
        _uiState.value = _uiState.value.copy(feedbackText = text)
    }

    fun submitFeedback() {
        val userEmail = cachedEmail
        if (userEmail == null) {
            _uiState.value = _uiState.value.copy(error = "User not authenticated")
            return
        }

        val text = _uiState.value.feedbackText.trim()
        if (text.length < 10) {
            _uiState.value = _uiState.value.copy(error = "Please provide at least 10 characters")
            return
        }
        if (text.length > 1000) {
            _uiState.value = _uiState.value.copy(error = "Maximum 1000 characters allowed")
            return
        }

        _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)

        viewModelScope.launch {
            // Collect device info
            val deviceInfo = mapOf(
                "os" to "Android",
                "osVersion" to android.os.Build.VERSION.RELEASE,
                "device" to android.os.Build.MODEL,
                "manufacturer" to android.os.Build.MANUFACTURER
            )

            when (val result = feedbackRepository.submitFeedback(
                email = userEmail,
                appId = APP_ID,
                category = _uiState.value.selectedCategory,
                feedbackText = text,
                deviceInfo = deviceInfo
            )) {
                is SubmitFeedbackResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        successMessage = "Thank you for your feedback! We'll keep you updated on progress.",
                        feedbackText = "",
                        selectedCategory = FeedbackCategory.OTHER
                    )
                    // Refresh history to show new submission
                    loadFeedbackHistory()
                }
                is SubmitFeedbackResult.RateLimited -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = result.message
                    )
                }
                is SubmitFeedbackResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    private fun loadFeedbackHistory() {
        val userEmail = cachedEmail ?: return

        _uiState.value = _uiState.value.copy(isLoadingHistory = true)

        viewModelScope.launch {
            try {
                val feedback = feedbackRepository.getFeedbackUpdates(
                    email = userEmail,
                    appId = APP_ID,
                    sinceId = null // Load all on first load
                )

                // Track the latest feedback ID for incremental polling
                lastFeedbackId = feedback.maxOfOrNull { it.feedback.id }

                _uiState.value = _uiState.value.copy(
                    feedbackList = feedback.sortedByDescending { it.feedback.createdAt },
                    isLoadingHistory = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingHistory = false,
                    error = "Failed to load feedback history: ${e.message}"
                )
            }
        }
    }

    private fun startPolling() {
        // Cancel existing job if any
        stopPolling()

        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                val userEmail = cachedEmail ?: continue

                try {
                    // Incremental update: only fetch feedback newer than what we have
                    val newFeedback = feedbackRepository.getFeedbackUpdates(
                        email = userEmail,
                        appId = APP_ID,
                        sinceId = lastFeedbackId
                    )

                    if (newFeedback.isNotEmpty()) {
                        // Merge new feedback with existing
                        val updatedList = (_uiState.value.feedbackList + newFeedback)
                            .distinctBy { it.feedback.id }
                            .sortedByDescending { it.feedback.createdAt }

                        lastFeedbackId = updatedList.maxOfOrNull { it.feedback.id }

                        _uiState.value = _uiState.value.copy(feedbackList = updatedList)
                    }
                } catch (_: Exception) {
                    // Silently ignore poll failures (network may be down)
                }
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun registerFcmToken(token: String) {
        val userEmail = cachedEmail ?: return

        viewModelScope.launch {
            val deviceInfo = mapOf(
                "os" to "Android",
                "osVersion" to android.os.Build.VERSION.RELEASE,
                "device" to android.os.Build.MODEL
            )

            feedbackRepository.registerFcmToken(
                email = userEmail,
                appId = APP_ID,
                fcmToken = token,
                deviceInfo = deviceInfo
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
