package com.jworks.kanjiquest.android.ui.devchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.UserSessionProvider
import com.jworks.kanjiquest.core.domain.model.DevChatMessage
import com.jworks.kanjiquest.core.domain.model.MessageCategory
import com.jworks.kanjiquest.core.domain.model.MessageDirection
import com.jworks.kanjiquest.core.domain.model.SendMessageResult
import com.jworks.kanjiquest.core.domain.repository.DevChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DevChatUiState(
    val messages: List<DevChatMessage> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val error: String? = null,
    val selectedCategory: MessageCategory? = null
)

@HiltViewModel
class DevChatViewModel @Inject constructor(
    private val devChatRepository: DevChatRepository,
    private val userSessionProvider: UserSessionProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevChatUiState())
    val uiState: StateFlow<DevChatUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var cachedEmail: String? = null

    init {
        viewModelScope.launch {
            cachedEmail = userSessionProvider.getUserEmail()
            loadMessages()
            startPolling()
        }
    }

    private suspend fun loadMessages() {
        val userEmail = cachedEmail ?: return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        try {
            val messages = devChatRepository.getMessageHistory(userEmail)
            // Reverse so oldest is first (API returns DESC)
            _uiState.value = _uiState.value.copy(
                messages = messages.reversed(),
                isLoading = false
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "Failed to load messages"
            )
        }
    }

    fun sendMessage(text: String) {
        val userEmail = cachedEmail ?: return
        if (text.isBlank()) return

        val category = _uiState.value.selectedCategory

        // Optimistic add
        val optimisticMsg = DevChatMessage(
            id = -System.currentTimeMillis(),
            messageText = text.trim(),
            direction = MessageDirection.TO_AGENT,
            category = category,
            sentAt = "",
            readAt = null
        )

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + optimisticMsg,
            isSending = true,
            error = null,
            selectedCategory = null
        )

        viewModelScope.launch {
            when (val result = devChatRepository.sendMessage(userEmail, text.trim(), category)) {
                is SendMessageResult.Success -> {
                    val updated = _uiState.value.messages.map { msg ->
                        if (msg.id == optimisticMsg.id) {
                            msg.copy(id = result.messageId, sentAt = result.sentAt)
                        } else msg
                    }
                    _uiState.value = _uiState.value.copy(messages = updated, isSending = false)
                }
                is SendMessageResult.Error -> {
                    val filtered = _uiState.value.messages.filter { it.id != optimisticMsg.id }
                    _uiState.value = _uiState.value.copy(
                        messages = filtered,
                        isSending = false,
                        error = result.message
                    )
                }
                is SendMessageResult.NotDeveloper -> {
                    val filtered = _uiState.value.messages.filter { it.id != optimisticMsg.id }
                    _uiState.value = _uiState.value.copy(
                        messages = filtered,
                        isSending = false,
                        error = "Not registered as a developer"
                    )
                }
            }
        }
    }

    fun selectCategory(category: MessageCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun startPolling() {
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000)
                val userEmail = cachedEmail ?: continue
                try {
                    val messages = devChatRepository.getMessageHistory(userEmail)
                    _uiState.value = _uiState.value.copy(messages = messages.reversed())
                } catch (_: Exception) {
                    // Silently ignore poll failures
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
