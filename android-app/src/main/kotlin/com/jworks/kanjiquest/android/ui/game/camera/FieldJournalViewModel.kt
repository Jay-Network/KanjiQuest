package com.jworks.kanjiquest.android.ui.game.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.model.FieldJournalEntry
import com.jworks.kanjiquest.core.domain.repository.FieldJournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FieldJournalUiState(
    val entries: List<FieldJournalEntry> = emptyList(),
    val totalPhotos: Long = 0,
    val totalKanjiCaught: Long = 0,
    val isLoading: Boolean = true,
    val selectedEntry: FieldJournalEntry? = null
)

@HiltViewModel
class FieldJournalViewModel @Inject constructor(
    private val repository: FieldJournalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FieldJournalUiState())
    val uiState: StateFlow<FieldJournalUiState> = _uiState.asStateFlow()

    init {
        loadEntries()
    }

    private fun loadEntries() {
        viewModelScope.launch {
            val entries = repository.getAll()
            val total = repository.countAll()
            val kanjiCaught = repository.totalKanjiCaught()
            _uiState.value = FieldJournalUiState(
                entries = entries,
                totalPhotos = total,
                totalKanjiCaught = kanjiCaught,
                isLoading = false
            )
        }
    }

    fun selectEntry(entry: FieldJournalEntry) {
        _uiState.value = _uiState.value.copy(selectedEntry = entry)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedEntry = null)
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
            loadEntries()
            _uiState.value = _uiState.value.copy(selectedEntry = null)
        }
    }
}
