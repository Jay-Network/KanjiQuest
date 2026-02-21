package com.jworks.kanjiquest.android.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.model.CollectedItem
import com.jworks.kanjiquest.core.domain.model.CollectionItemType
import com.jworks.kanjiquest.core.domain.model.CollectionStats
import com.jworks.kanjiquest.core.domain.model.Rarity
import com.jworks.kanjiquest.core.domain.repository.CollectionRepository
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionUiState(
    val selectedTab: CollectionItemType = CollectionItemType.KANJI,
    val items: List<CollectedItem> = emptyList(),
    val filteredItems: List<CollectedItem> = emptyList(),
    val stats: CollectionStats = CollectionStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
    val selectedRarityFilter: Rarity? = null,
    val isLoading: Boolean = true,
    val kanjiLiterals: Map<Int, String> = emptyMap()
)

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val kanjiRepository: KanjiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    init {
        loadCollection()
    }

    private fun loadCollection() {
        viewModelScope.launch {
            val stats = collectionRepository.getCollectionStats()
            _uiState.value = _uiState.value.copy(stats = stats)
            selectTab(CollectionItemType.KANJI)
        }
    }

    fun selectTab(type: CollectionItemType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, selectedTab = type)

            val items = collectionRepository.getCollectedByType(type)

            // Load kanji literals for display
            val literals = if (type == CollectionItemType.KANJI) {
                items.associate { item ->
                    val kanji = kanjiRepository.getKanjiById(item.itemId)
                    item.itemId to (kanji?.literal ?: "?")
                }
            } else _uiState.value.kanjiLiterals

            val filtered = applyFilter(items, _uiState.value.selectedRarityFilter)

            _uiState.value = _uiState.value.copy(
                items = items,
                filteredItems = filtered,
                kanjiLiterals = literals,
                isLoading = false
            )
        }
    }

    fun filterByRarity(rarity: Rarity?) {
        val filtered = applyFilter(_uiState.value.items, rarity)
        _uiState.value = _uiState.value.copy(
            selectedRarityFilter = rarity,
            filteredItems = filtered
        )
    }

    private fun applyFilter(items: List<CollectedItem>, rarity: Rarity?): List<CollectedItem> {
        return if (rarity != null) {
            items.filter { it.rarity == rarity }
        } else {
            items
        }
    }
}
