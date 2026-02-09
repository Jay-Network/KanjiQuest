package com.jworks.kanjiquest.android.ui.worddetail

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

data class WordDetailUiState(
    val vocabulary: Vocabulary? = null,
    val relatedKanji: List<Kanji> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class WordDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val kanjiRepository: KanjiRepository
) : ViewModel() {

    private val wordId: Long = checkNotNull(savedStateHandle["wordId"])

    private val _uiState = MutableStateFlow(WordDetailUiState())
    val uiState: StateFlow<WordDetailUiState> = _uiState.asStateFlow()

    init {
        loadWord()
    }

    private fun loadWord() {
        viewModelScope.launch {
            val vocab = kanjiRepository.getVocabularyById(wordId)
            val kanji = if (vocab != null) {
                val kanjiIds = kanjiRepository.getKanjiIdsForVocab(wordId)
                kanjiIds.mapNotNull { kanjiRepository.getKanjiById(it.toInt()) }
            } else emptyList()

            _uiState.value = WordDetailUiState(
                vocabulary = vocab,
                relatedKanji = kanji,
                isLoading = false
            )
        }
    }
}
