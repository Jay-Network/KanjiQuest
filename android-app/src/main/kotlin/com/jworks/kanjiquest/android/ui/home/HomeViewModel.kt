package com.jworks.kanjiquest.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.model.CoinBalance
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.model.LOCAL_USER_ID
import com.jworks.kanjiquest.core.domain.model.UserProfile
import com.jworks.kanjiquest.core.domain.model.Vocabulary
import com.jworks.kanjiquest.core.domain.repository.JCoinRepository
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import com.jworks.kanjiquest.core.domain.repository.UserRepository
import com.jworks.kanjiquest.core.domain.usecase.WordOfTheDayUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val profile: UserProfile = UserProfile(),
    val gradeOneKanji: List<Kanji> = emptyList(),
    val kanjiCount: Long = 0,
    val coinBalance: CoinBalance = CoinBalance.empty(),
    val wordOfTheDay: Vocabulary? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val kanjiRepository: KanjiRepository,
    private val userRepository: UserRepository,
    private val jCoinRepository: JCoinRepository,
    private val wordOfTheDayUseCase: WordOfTheDayUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeCoinBalance()
    }

    private fun loadData() {
        viewModelScope.launch {
            val profile = userRepository.getProfile()
            val gradeOne = kanjiRepository.getKanjiByGrade(1)
            val totalCount = kanjiRepository.getKanjiCount()
            val coinBalance = jCoinRepository.getBalance(LOCAL_USER_ID)
            val wotd = wordOfTheDayUseCase.getWordOfTheDay()

            _uiState.value = HomeUiState(
                profile = profile,
                gradeOneKanji = gradeOne,
                kanjiCount = totalCount,
                coinBalance = coinBalance,
                wordOfTheDay = wotd,
                isLoading = false
            )
        }
    }

    private fun observeCoinBalance() {
        viewModelScope.launch {
            jCoinRepository.observeBalance(LOCAL_USER_ID).collect { balance ->
                _uiState.value = _uiState.value.copy(coinBalance = balance)
            }
        }
    }
}
