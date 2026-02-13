package com.jworks.kanjiquest.android.ui.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.model.FlashcardEntry
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.repository.FlashcardRepository
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FlashcardItem(
    val entry: FlashcardEntry,
    val kanji: Kanji
)

data class FlashcardUiState(
    val items: List<FlashcardItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class FlashcardViewModel @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    private val kanjiRepository: KanjiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardUiState())
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    init {
        loadDeck()
    }

    private fun loadDeck() {
        viewModelScope.launch {
            val entries = flashcardRepository.getAllFlashcards()
            val items = entries.mapNotNull { entry ->
                val kanji = kanjiRepository.getKanjiById(entry.kanjiId)
                if (kanji != null) FlashcardItem(entry, kanji) else null
            }
            _uiState.value = FlashcardUiState(items = items, isLoading = false)
        }
    }

    fun removeFromDeck(kanjiId: Int) {
        viewModelScope.launch {
            flashcardRepository.removeFromDeck(kanjiId)
            _uiState.value = _uiState.value.copy(
                items = _uiState.value.items.filter { it.entry.kanjiId != kanjiId }
            )
        }
    }

    fun refresh() {
        loadDeck()
    }
}
