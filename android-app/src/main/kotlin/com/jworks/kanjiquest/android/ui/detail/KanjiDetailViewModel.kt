package com.jworks.kanjiquest.android.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.model.Vocabulary
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KanjiDetailUiState(
    val kanji: Kanji? = null,
    val vocabulary: List<Vocabulary> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class KanjiDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val kanjiRepository: KanjiRepository
) : ViewModel() {

    private val kanjiId: Int = checkNotNull(savedStateHandle["kanjiId"])

    private val _uiState = MutableStateFlow(KanjiDetailUiState())
    val uiState: StateFlow<KanjiDetailUiState> = _uiState.asStateFlow()

    init {
        loadKanji()
    }

    private fun loadKanji() {
        viewModelScope.launch {
            val kanji = kanjiRepository.getKanjiById(kanjiId)
            val vocab = if (kanji != null) {
                kanjiRepository.getVocabularyForKanji(kanjiId)
            } else emptyList()

            _uiState.value = KanjiDetailUiState(
                kanji = kanji,
                vocabulary = vocab,
                isLoading = false
            )
        }
    }
}
