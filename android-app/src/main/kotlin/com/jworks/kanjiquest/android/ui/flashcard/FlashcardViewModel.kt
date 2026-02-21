package com.jworks.kanjiquest.android.ui.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.model.FlashcardDeckGroup
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
    val deckGroups: List<FlashcardDeckGroup> = emptyList(),
    val selectedDeckId: Long = 1,
    val isLoading: Boolean = true,
    val showCreateDeckDialog: Boolean = false,
    val editingDeck: FlashcardDeckGroup? = null
)

@HiltViewModel
class FlashcardViewModel @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    private val kanjiRepository: KanjiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardUiState())
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    init {
        loadDecks()
    }

    private fun loadDecks() {
        viewModelScope.launch {
            flashcardRepository.ensureDefaultDeck()
            val groups = flashcardRepository.getAllDeckGroups()
            val selectedId = _uiState.value.selectedDeckId
            val effectiveId = if (groups.any { it.id == selectedId }) selectedId else groups.firstOrNull()?.id ?: 1
            _uiState.value = _uiState.value.copy(deckGroups = groups, selectedDeckId = effectiveId)
            loadDeck(effectiveId)
        }
    }

    private fun loadDeck(deckId: Long) {
        viewModelScope.launch {
            val entries = flashcardRepository.getFlashcardsByDeck(deckId)
            val items = entries.mapNotNull { entry ->
                val kanji = kanjiRepository.getKanjiById(entry.kanjiId)
                if (kanji != null) FlashcardItem(entry, kanji) else null
            }
            _uiState.value = _uiState.value.copy(
                items = items,
                selectedDeckId = deckId,
                isLoading = false
            )
        }
    }

    fun selectDeck(deckId: Long) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadDeck(deckId)
    }

    fun createDeck(name: String) {
        viewModelScope.launch {
            val newId = flashcardRepository.createDeckGroup(name)
            val groups = flashcardRepository.getAllDeckGroups()
            _uiState.value = _uiState.value.copy(
                deckGroups = groups,
                showCreateDeckDialog = false
            )
            selectDeck(newId)
        }
    }

    fun renameDeck(deckId: Long, newName: String) {
        viewModelScope.launch {
            flashcardRepository.renameDeckGroup(deckId, newName)
            val groups = flashcardRepository.getAllDeckGroups()
            _uiState.value = _uiState.value.copy(
                deckGroups = groups,
                editingDeck = null
            )
        }
    }

    fun deleteDeck(deckId: Long) {
        viewModelScope.launch {
            flashcardRepository.deleteDeckGroup(deckId)
            val groups = flashcardRepository.getAllDeckGroups()
            if (groups.isEmpty()) {
                flashcardRepository.ensureDefaultDeck()
                val refreshedGroups = flashcardRepository.getAllDeckGroups()
                _uiState.value = _uiState.value.copy(deckGroups = refreshedGroups, editingDeck = null)
                selectDeck(refreshedGroups.first().id)
            } else {
                _uiState.value = _uiState.value.copy(deckGroups = groups, editingDeck = null)
                if (deckId == _uiState.value.selectedDeckId) {
                    selectDeck(groups.first().id)
                }
            }
        }
    }

    fun showCreateDeckDialog() {
        _uiState.value = _uiState.value.copy(showCreateDeckDialog = true)
    }

    fun dismissCreateDeckDialog() {
        _uiState.value = _uiState.value.copy(showCreateDeckDialog = false)
    }

    fun showEditDeckDialog(deck: FlashcardDeckGroup) {
        _uiState.value = _uiState.value.copy(editingDeck = deck)
    }

    fun dismissEditDeckDialog() {
        _uiState.value = _uiState.value.copy(editingDeck = null)
    }

    fun removeFromDeck(kanjiId: Int) {
        viewModelScope.launch {
            flashcardRepository.removeFromDeck(_uiState.value.selectedDeckId, kanjiId)
            _uiState.value = _uiState.value.copy(
                items = _uiState.value.items.filter { it.entry.kanjiId != kanjiId }
            )
        }
    }

    fun refresh() {
        loadDecks()
    }
}
