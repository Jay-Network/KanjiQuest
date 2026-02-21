package com.jworks.kanjiquest.android.ui.flashcard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.model.SrsCard
import com.jworks.kanjiquest.core.domain.model.Vocabulary
import com.jworks.kanjiquest.core.domain.repository.FlashcardRepository
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import com.jworks.kanjiquest.core.domain.repository.SrsRepository
import com.jworks.kanjiquest.core.srs.SrsAlgorithm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudyCard(
    val kanji: Kanji,
    val vocabulary: List<Vocabulary>,
    val srsCard: SrsCard?
)

data class FlashcardStudyUiState(
    val cards: List<StudyCard> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val isComplete: Boolean = false,
    val isLoading: Boolean = true,
    val totalStudied: Int = 0,
    val gradeResults: Map<Int, Int> = emptyMap()
) {
    val currentCard: StudyCard? get() = cards.getOrNull(currentIndex)
    val progress: String get() = "${currentIndex + 1} of ${cards.size}"
}

enum class StudyGrade(val quality: Int, val label: String) {
    AGAIN(1, "Again"),
    HARD(2, "Hard"),
    GOOD(4, "Good"),
    EASY(5, "Easy")
}

@HiltViewModel
class FlashcardStudyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val flashcardRepository: FlashcardRepository,
    private val kanjiRepository: KanjiRepository,
    private val srsRepository: SrsRepository,
    private val srsAlgorithm: SrsAlgorithm
) : ViewModel() {

    private val deckId: Long = checkNotNull(savedStateHandle["deckId"])

    private val _uiState = MutableStateFlow(FlashcardStudyUiState())
    val uiState: StateFlow<FlashcardStudyUiState> = _uiState.asStateFlow()

    private val gradeResults = mutableMapOf<Int, Int>()

    init {
        loadCards()
    }

    private fun loadCards() {
        viewModelScope.launch {
            val kanjiIds = flashcardRepository.getKanjiIdsByDeck(deckId)
            val cards = kanjiIds.mapNotNull { id ->
                val kanji = kanjiRepository.getKanjiById(id) ?: return@mapNotNull null
                val vocab = kanjiRepository.getVocabularyForKanji(id).take(3)
                val srsCard = srsRepository.getCard(id)
                StudyCard(kanji, vocab, srsCard)
            }.shuffled()

            _uiState.value = FlashcardStudyUiState(
                cards = cards,
                isLoading = false
            )
        }
    }

    fun flip() {
        _uiState.value = _uiState.value.copy(isFlipped = true)
    }

    fun grade(grade: StudyGrade) {
        val current = _uiState.value.currentCard ?: return
        val kanjiId = current.kanji.id

        gradeResults[kanjiId] = grade.quality

        viewModelScope.launch {
            // Update SRS card
            val card = srsRepository.getCard(kanjiId)
            if (card != null) {
                val currentTime = kotlinx.datetime.Clock.System.now().epochSeconds
                val updated = srsAlgorithm.review(card, grade.quality, currentTime)
                srsRepository.saveCard(updated)
            }

            // Update flashcard study count
            flashcardRepository.markStudied(deckId, kanjiId)

            // Advance to next card or complete
            val nextIndex = _uiState.value.currentIndex + 1
            if (nextIndex >= _uiState.value.cards.size) {
                _uiState.value = _uiState.value.copy(
                    isComplete = true,
                    totalStudied = _uiState.value.cards.size,
                    gradeResults = gradeResults.toMap()
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    currentIndex = nextIndex,
                    isFlipped = false
                )
            }
        }
    }
}
