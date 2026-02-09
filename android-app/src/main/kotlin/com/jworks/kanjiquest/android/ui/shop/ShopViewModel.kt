package com.jworks.kanjiquest.android.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.UserSessionProvider
import com.jworks.kanjiquest.core.domain.model.CoinBalance
import com.jworks.kanjiquest.core.domain.model.PurchaseResult
import com.jworks.kanjiquest.core.domain.model.ShopCategory
import com.jworks.kanjiquest.core.domain.model.ShopItem
import com.jworks.kanjiquest.core.domain.repository.JCoinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShopUiState(
    val catalog: List<ShopItem> = emptyList(),
    val selectedCategory: ShopCategory? = null,
    val balance: CoinBalance = CoinBalance.empty(),
    val ownedContentIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val purchaseDialogItem: ShopItem? = null,
    val purchaseResult: PurchaseResult? = null,
    val featuredItem: ShopItem? = null,
    val hasRedeemedTrial: Boolean = false
) {
    val categories: List<ShopCategory>
        get() = catalog.map { it.category }.distinct()

    val filteredItems: List<ShopItem>
        get() = if (selectedCategory == null) catalog
                else catalog.filter { it.category == selectedCategory }
}

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val jCoinRepository: JCoinRepository,
    private val userSessionProvider: UserSessionProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopUiState())
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    init {
        loadShop()
        observeBalance()
    }

    private fun loadShop() {
        viewModelScope.launch {
            val userId = userSessionProvider.getUserId()
            val catalog = jCoinRepository.getShopCatalog()
            val owned = mutableSetOf<String>()
            for (category in ShopCategory.entries) {
                val unlocked = jCoinRepository.getUnlockedContent(userId, category.name.lowercase())
                owned.addAll(unlocked)
            }
            val featuredItem = catalog.find { it.id == "tutoringjay_trial" }
            val hasRedeemedTrial = featuredItem?.contentId != null && featuredItem.contentId in owned
            _uiState.value = _uiState.value.copy(
                catalog = catalog,
                ownedContentIds = owned,
                featuredItem = featuredItem,
                hasRedeemedTrial = hasRedeemedTrial,
                isLoading = false
            )
        }
    }

    private fun observeBalance() {
        viewModelScope.launch {
            val userId = userSessionProvider.getUserId()
            jCoinRepository.observeBalance(userId).collect { balance ->
                _uiState.value = _uiState.value.copy(balance = balance)
            }
        }
    }

    fun selectCategory(category: ShopCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun showPurchaseDialog(item: ShopItem) {
        _uiState.value = _uiState.value.copy(purchaseDialogItem = item, purchaseResult = null)
    }

    fun dismissPurchaseDialog() {
        _uiState.value = _uiState.value.copy(purchaseDialogItem = null, purchaseResult = null)
    }

    fun confirmPurchase(item: ShopItem) {
        viewModelScope.launch {
            val userId = userSessionProvider.getUserId()
            val result = jCoinRepository.purchaseItem(userId, item)
            _uiState.value = _uiState.value.copy(purchaseResult = result)

            if (result is PurchaseResult.Success) {
                refreshOwnedContent()
            }
        }
    }

    fun purchaseFeatured() {
        val featured = _uiState.value.featuredItem ?: return
        showPurchaseDialog(featured)
    }

    private suspend fun refreshOwnedContent() {
        val userId = userSessionProvider.getUserId()
        val owned = mutableSetOf<String>()
        for (category in ShopCategory.entries) {
            val unlocked = jCoinRepository.getUnlockedContent(userId, category.name.lowercase())
            owned.addAll(unlocked)
        }
        val featuredItem = _uiState.value.featuredItem
        val hasRedeemedTrial = featuredItem?.contentId != null && featuredItem.contentId in owned
        _uiState.value = _uiState.value.copy(
            ownedContentIds = owned,
            hasRedeemedTrial = hasRedeemedTrial
        )
    }
}
