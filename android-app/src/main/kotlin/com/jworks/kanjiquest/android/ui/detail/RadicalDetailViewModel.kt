package com.jworks.kanjiquest.android.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.model.Radical
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import com.jworks.kanjiquest.core.domain.repository.RadicalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RadicalDetailUiState(
    val radical: Radical? = null,
    val exampleKanji: List<Kanji> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class RadicalDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val radicalRepository: RadicalRepository,
    private val kanjiRepository: KanjiRepository
) : ViewModel() {

    val radicalId: Int = checkNotNull(savedStateHandle["radicalId"])

    private val _uiState = MutableStateFlow(RadicalDetailUiState())
    val uiState: StateFlow<RadicalDetailUiState> = _uiState.asStateFlow()

    init {
        loadRadical()
    }

    private fun loadRadical() {
        viewModelScope.launch {
            try {
                val radical = radicalRepository.getRadicalById(radicalId)
                val kanjiIds = if (radical != null) {
                    radicalRepository.getKanjiIdsForRadical(radicalId)
                } else emptyList()

                val kanji = kanjiIds.mapNotNull { id ->
                    kanjiRepository.getKanjiById(id.toInt())
                }.sortedWith(compareBy<Kanji> { it.grade ?: 99 }.thenBy { it.frequency ?: 9999 })

                _uiState.value = RadicalDetailUiState(
                    radical = radical,
                    exampleKanji = kanji,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = RadicalDetailUiState(
                    radical = null,
                    exampleKanji = emptyList(),
                    isLoading = false
                )
            }
        }
    }
}
