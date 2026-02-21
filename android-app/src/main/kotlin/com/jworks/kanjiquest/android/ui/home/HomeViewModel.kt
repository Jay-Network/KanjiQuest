package com.jworks.kanjiquest.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.kanjiquest.android.data.PreviewTrialManager
import com.jworks.kanjiquest.core.domain.UserSessionProvider
import com.jworks.kanjiquest.core.domain.UserSessionProviderImpl
import com.jworks.kanjiquest.core.domain.model.CoinBalance
import com.jworks.kanjiquest.core.domain.model.CollectedItem
import com.jworks.kanjiquest.core.domain.model.CollectionItemType
import com.jworks.kanjiquest.core.domain.model.GameMode
import com.jworks.kanjiquest.core.domain.model.GradeMastery
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.model.LevelProgression
import com.jworks.kanjiquest.core.domain.model.Radical
import com.jworks.kanjiquest.core.domain.model.UserLevel
import com.jworks.kanjiquest.core.domain.model.UserProfile
import com.jworks.kanjiquest.core.domain.model.Vocabulary
import com.jworks.kanjiquest.core.domain.repository.CollectionRepository
import com.jworks.kanjiquest.core.domain.repository.JCoinRepository
import com.jworks.kanjiquest.core.domain.repository.AuthRepository
import com.jworks.kanjiquest.core.domain.repository.FlashcardRepository
import com.jworks.kanjiquest.core.domain.repository.KanaRepository
import com.jworks.kanjiquest.core.domain.repository.KanaSrsRepository
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import com.jworks.kanjiquest.core.domain.repository.RadicalRepository
import com.jworks.kanjiquest.core.domain.repository.RadicalSrsRepository
import com.jworks.kanjiquest.core.domain.repository.SrsRepository
import com.jworks.kanjiquest.core.domain.repository.UserRepository
import com.jworks.kanjiquest.core.domain.usecase.WordOfTheDayUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MainTab(val label: String) {
    HIRAGANA("Hiragana"),
    KATAKANA("Katakana"),
    RADICALS("部首"),
    KANJI("Kanji")
}

enum class KanjiSortMode(val label: String) {
    SCHOOL_GRADE("School Grade"),
    JLPT_LEVEL("JLPT Level"),
    STROKES("Strokes"),
    FREQUENCY("Frequency")
}

data class PreviewTrialInfo(
    val remaining: Int,
    val limit: Int
)

data class HomeUiState(
    val profile: UserProfile = UserProfile(),
    val gradeOneKanji: List<Kanji> = emptyList(),
    val kanjiCount: Long = 0,
    val coinBalance: CoinBalance = CoinBalance.empty(),
    val wordOfTheDay: Vocabulary? = null,
    val isLoading: Boolean = true,
    val isPremium: Boolean = false,
    val isAdmin: Boolean = false,
    val effectiveLevel: UserLevel = UserLevel.FREE,
    val previewTrials: Map<GameMode, PreviewTrialInfo> = emptyMap(),
    val tierName: String = "Beginner",
    val tierNameJp: String = "入門",
    val tierProgress: Float = 0f,
    val nextTierName: String? = "Novice",
    val nextTierLevel: Int? = 5,
    val highestUnlockedGrade: Int = 1,
    val gradeMasteryList: List<GradeMastery> = emptyList(),
    val displayLevel: Int = 1,
    val kanjiPracticeCounts: Map<Int, Int> = emptyMap(),
    val kanjiModeStats: Map<Int, Map<String, Int>> = emptyMap(),
    val flashcardDeckCount: Long = 0,
    val unlockedGrades: List<Int> = listOf(1),
    val allGrades: List<Int> = listOf(1, 2, 3, 4, 5, 6, 8),
    val gradesWithCollection: Set<Int> = emptySet(),
    val selectedGrade: Int = 1,
    val hiraganaProgress: Float = 0f,
    val katakanaProgress: Float = 0f,
    val radicalProgress: Float = 0f,
    val selectedMainTab: MainTab = MainTab.KANJI,
    val radicals: List<Radical> = emptyList(),
    val kanjiSortMode: KanjiSortMode = KanjiSortMode.SCHOOL_GRADE,
    val selectedJlptLevel: Int = 5,
    val selectedStrokeCount: Int = 1,
    val selectedFrequencyRange: Int = 0,
    val availableStrokeCounts: List<Int> = emptyList(),
    val collectedKanjiCount: Int = 0,
    val totalKanjiInGrades: Int = 0,
    val collectedItems: Map<Int, CollectedItem> = emptyMap(),
    val hiraganaList: List<com.jworks.kanjiquest.core.domain.model.Kana> = emptyList(),
    val katakanaList: List<com.jworks.kanjiquest.core.domain.model.Kana> = emptyList(),
    val collectedHiraganaIds: Set<Int> = emptySet(),
    val collectedKatakanaIds: Set<Int> = emptySet(),
    val collectedRadicalIds: Set<Int> = emptySet(),
    val collectedHiraganaItems: Map<Int, CollectedItem> = emptyMap(),
    val collectedKatakanaItems: Map<Int, CollectedItem> = emptyMap(),
    val collectedRadicalItems: Map<Int, CollectedItem> = emptyMap()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val kanjiRepository: KanjiRepository,
    private val userRepository: UserRepository,
    private val srsRepository: SrsRepository,
    private val jCoinRepository: JCoinRepository,
    private val wordOfTheDayUseCase: WordOfTheDayUseCase,
    private val userSessionProvider: UserSessionProvider,
    private val authRepository: AuthRepository,
    private val previewTrialManager: PreviewTrialManager,
    private val flashcardRepository: FlashcardRepository,
    private val kanaRepository: KanaRepository,
    private val kanaSrsRepository: KanaSrsRepository,
    private val radicalRepository: RadicalRepository,
    private val radicalSrsRepository: RadicalSrsRepository,
    private val collectionRepository: CollectionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeCoinBalance()
    }

    private var isFirstLoad = true

    private fun loadData() {
        viewModelScope.launch {
            // Preserve current selections across refresh
            val prev = _uiState.value
            val preserveMainTab = prev.selectedMainTab
            val preserveSortMode = prev.kanjiSortMode
            val preserveGrade = prev.selectedGrade
            val preserveJlpt = prev.selectedJlptLevel
            val preserveStroke = prev.selectedStrokeCount
            val preserveFreq = prev.selectedFrequencyRange
            val preserveStrokeCounts = prev.availableStrokeCounts

            // Sync email from auth state so admin detection works
            val authState = authRepository.observeAuthState().first()
            if (authState.email != null) {
                (userSessionProvider as? UserSessionProviderImpl)?.updateEmail(authState.email)
            }

            val userId = userSessionProvider.getUserId()
            val profile = userRepository.getProfile()
            val totalCount = kanjiRepository.getKanjiCount()
            val coinBalance = jCoinRepository.getBalance(userId)
            val wotd = wordOfTheDayUseCase.getWordOfTheDay()
            val effectiveLevel = userSessionProvider.getEffectiveLevel()
            val isPremium = effectiveLevel == UserLevel.PREMIUM || effectiveLevel == UserLevel.ADMIN

            // Tier progression info
            val playerLevel = userSessionProvider.getAdminPlayerLevelOverride() ?: profile.level
            val tier = LevelProgression.getTierForLevel(playerLevel)
            val nextTier = LevelProgression.getNextTier(playerLevel)
            val highestGrade = tier.unlockedGrades.maxOrNull() ?: 1

            // On first load use highest grade; on refresh preserve selection
            val activeGrade = if (isFirstLoad) highestGrade else preserveGrade
            val gradeKanji = kanjiRepository.getKanjiByGrade(activeGrade)

            // Compute grade mastery for all unlocked grades
            val gradeMasteryList = tier.unlockedGrades.map { grade ->
                val total = kanjiRepository.getKanjiCountByGrade(grade)
                srsRepository.getGradeMastery(grade, total)
            }

            // Load practice counts for displayed kanji
            val kanjiIds = gradeKanji.map { it.id.toLong() }
            val practiceCounts = if (kanjiIds.isNotEmpty()) {
                try {
                    val cards = srsRepository.getCardsByIds(kanjiIds)
                    cards.associate { it.kanjiId to it.totalReviews }
                } catch (_: Exception) { emptyMap() }
            } else emptyMap()

            // Load per-mode stats
            val modeStats = if (kanjiIds.isNotEmpty()) {
                try { srsRepository.getModeStatsByIds(kanjiIds) } catch (_: Exception) { emptyMap() }
            } else emptyMap()

            val deckCount = flashcardRepository.getDeckCount()

            // Collection data — kanji
            val collectedKanjiItems = try {
                collectionRepository.getCollectedByType(CollectionItemType.KANJI)
            } catch (_: Exception) { emptyList() }
            val collectedItemsMap = collectedKanjiItems.associateBy { it.itemId }
            val collectedKanjiCount = collectedKanjiItems.size

            // Determine which grades have collected kanji
            val collectedKanjiIds = collectedKanjiItems.map { it.itemId }.toSet()
            val gradesWithCollection = mutableSetOf<Int>()
            for (grade in listOf(1, 2, 3, 4, 5, 6, 8)) {
                val gradeKanjiList = kanjiRepository.getKanjiByGrade(grade)
                if (gradeKanjiList.any { it.id in collectedKanjiIds }) {
                    gradesWithCollection.add(grade)
                }
            }

            val totalKanjiInGrades = listOf(1, 2, 3, 4, 5, 6, 8).sumOf { g ->
                kanjiRepository.getKanjiCountByGrade(g).toInt()
            }

            // Collection data — hiragana, katakana, radicals
            val collectedHiraganaItems = try {
                collectionRepository.getCollectedByType(CollectionItemType.HIRAGANA)
            } catch (_: Exception) { emptyList() }
            val collectedKatakanaItems = try {
                collectionRepository.getCollectedByType(CollectionItemType.KATAKANA)
            } catch (_: Exception) { emptyList() }
            val collectedRadicalItems = try {
                collectionRepository.getCollectedByType(CollectionItemType.RADICAL)
            } catch (_: Exception) { emptyList() }

            // Load kana lists for grid display
            val hiraganaList = try { kanaRepository.getKanaByType(com.jworks.kanjiquest.core.domain.model.KanaType.HIRAGANA) } catch (_: Exception) { emptyList() }
            val katakanaList = try { kanaRepository.getKanaByType(com.jworks.kanjiquest.core.domain.model.KanaType.KATAKANA) } catch (_: Exception) { emptyList() }

            // Radicals
            val radicals = try { radicalRepository.getAllRadicals() } catch (_: Exception) { emptyList() }

            // Kana & radical progress
            val hiraganaTotal = hiraganaList.size.toLong()
            val hiraganaStudied = try { kanaSrsRepository.getTypeStudiedCount("HIRAGANA") } catch (_: Exception) { 0L }
            val katakanaTotal = katakanaList.size.toLong()
            val katakanaStudied = try { kanaSrsRepository.getTypeStudiedCount("KATAKANA") } catch (_: Exception) { 0L }
            val radicalTotal = radicals.size.toLong()
            val radicalStudied = try { radicalSrsRepository.getStudiedCount() } catch (_: Exception) { 0L }

            // Stroke counts (preserve on refresh)
            val strokeCounts = if (!isFirstLoad && preserveStrokeCounts.isNotEmpty()) {
                preserveStrokeCounts
            } else {
                try { kanjiRepository.getDistinctStrokeCounts() } catch (_: Exception) { emptyList() }
            }

            _uiState.value = HomeUiState(
                profile = profile,
                gradeOneKanji = gradeKanji,
                kanjiCount = totalCount,
                coinBalance = coinBalance,
                wordOfTheDay = wotd,
                isLoading = false,
                isPremium = isPremium,
                isAdmin = userSessionProvider.isAdmin(),
                effectiveLevel = effectiveLevel,
                previewTrials = loadPreviewTrials(),
                tierName = tier.nameEn,
                tierNameJp = tier.nameJp,
                tierProgress = LevelProgression.getTierProgress(playerLevel),
                nextTierName = nextTier?.nameEn,
                nextTierLevel = nextTier?.levelRange?.first,
                highestUnlockedGrade = highestGrade,
                gradeMasteryList = gradeMasteryList,
                displayLevel = playerLevel,
                kanjiPracticeCounts = practiceCounts,
                kanjiModeStats = modeStats,
                flashcardDeckCount = deckCount,
                unlockedGrades = tier.unlockedGrades,
                allGrades = listOf(1, 2, 3, 4, 5, 6, 8),
                gradesWithCollection = gradesWithCollection,
                selectedGrade = activeGrade,
                hiraganaProgress = if (hiraganaTotal > 0) hiraganaStudied.toFloat() / hiraganaTotal else 0f,
                katakanaProgress = if (katakanaTotal > 0) katakanaStudied.toFloat() / katakanaTotal else 0f,
                radicalProgress = if (radicalTotal > 0) radicalStudied.toFloat() / radicalTotal else 0f,
                selectedMainTab = if (isFirstLoad) MainTab.KANJI else preserveMainTab,
                collectedKanjiCount = collectedKanjiCount,
                totalKanjiInGrades = totalKanjiInGrades,
                collectedItems = collectedItemsMap,
                hiraganaList = hiraganaList,
                katakanaList = katakanaList,
                radicals = radicals,
                collectedHiraganaIds = collectedHiraganaItems.map { it.itemId }.toSet(),
                collectedKatakanaIds = collectedKatakanaItems.map { it.itemId }.toSet(),
                collectedRadicalIds = collectedRadicalItems.map { it.itemId }.toSet(),
                collectedHiraganaItems = collectedHiraganaItems.associateBy { it.itemId },
                collectedKatakanaItems = collectedKatakanaItems.associateBy { it.itemId },
                collectedRadicalItems = collectedRadicalItems.associateBy { it.itemId },
                kanjiSortMode = if (isFirstLoad) KanjiSortMode.SCHOOL_GRADE else preserveSortMode,
                selectedJlptLevel = if (isFirstLoad) 5 else preserveJlpt,
                selectedStrokeCount = if (isFirstLoad) (strokeCounts.firstOrNull() ?: 1) else preserveStroke,
                selectedFrequencyRange = if (isFirstLoad) 0 else preserveFreq,
                availableStrokeCounts = strokeCounts
            )
            isFirstLoad = false
        }
    }

    private fun loadPreviewTrials(): Map<GameMode, PreviewTrialInfo> {
        return mapOf(
            GameMode.WRITING to PreviewTrialInfo(
                remaining = previewTrialManager.getRemainingTrials(GameMode.WRITING),
                limit = previewTrialManager.getTrialLimit(GameMode.WRITING)
            ),
            GameMode.VOCABULARY to PreviewTrialInfo(
                remaining = previewTrialManager.getRemainingTrials(GameMode.VOCABULARY),
                limit = previewTrialManager.getTrialLimit(GameMode.VOCABULARY)
            ),
            GameMode.CAMERA_CHALLENGE to PreviewTrialInfo(
                remaining = previewTrialManager.getRemainingTrials(GameMode.CAMERA_CHALLENGE),
                limit = previewTrialManager.getTrialLimit(GameMode.CAMERA_CHALLENGE)
            )
        )
    }

    /** Use a preview trial and return true if successful */
    fun usePreviewTrial(mode: GameMode): Boolean {
        val success = previewTrialManager.usePreviewTrial(mode)
        if (success) {
            _uiState.value = _uiState.value.copy(previewTrials = loadPreviewTrials())
        }
        return success
    }

    fun refreshTrials() {
        _uiState.value = _uiState.value.copy(previewTrials = loadPreviewTrials())
    }

    fun selectGrade(grade: Int) {
        viewModelScope.launch {
            val gradeKanji = kanjiRepository.getKanjiByGrade(grade)
            val kanjiIds = gradeKanji.map { it.id.toLong() }
            val practiceCounts = if (kanjiIds.isNotEmpty()) {
                try {
                    val cards = srsRepository.getCardsByIds(kanjiIds)
                    cards.associate { it.kanjiId to it.totalReviews }
                } catch (_: Exception) { emptyMap() }
            } else emptyMap()

            val modeStats = if (kanjiIds.isNotEmpty()) {
                try { srsRepository.getModeStatsByIds(kanjiIds) } catch (_: Exception) { emptyMap() }
            } else emptyMap()

            _uiState.value = _uiState.value.copy(
                gradeOneKanji = gradeKanji,
                selectedGrade = grade,
                kanjiPracticeCounts = practiceCounts,
                kanjiModeStats = modeStats,

                kanjiSortMode = KanjiSortMode.SCHOOL_GRADE
            )
        }
    }

    fun selectMainTab(tab: MainTab) {
        _uiState.value = _uiState.value.copy(selectedMainTab = tab)
    }

    fun selectSortMode(mode: KanjiSortMode) {
        viewModelScope.launch {
            when (mode) {
                KanjiSortMode.SCHOOL_GRADE -> {
                    val grade = _uiState.value.selectedGrade
                    val gradeKanji = kanjiRepository.getKanjiByGrade(grade)
                    val kanjiIds = gradeKanji.map { it.id.toLong() }
                    val practiceCounts = loadPracticeCounts(kanjiIds)
                    val modeStats = loadModeStats(kanjiIds)
                    _uiState.value = _uiState.value.copy(
                        kanjiSortMode = mode,
                        gradeOneKanji = gradeKanji,
                        kanjiPracticeCounts = practiceCounts,
                        kanjiModeStats = modeStats,
                        selectedMainTab = MainTab.KANJI
                    )
                }
                KanjiSortMode.JLPT_LEVEL -> {
                    val level = _uiState.value.selectedJlptLevel
                    val kanji = kanjiRepository.getKanjiByJlptLevel(level)
                    val kanjiIds = kanji.map { it.id.toLong() }
                    val practiceCounts = loadPracticeCounts(kanjiIds)
                    val modeStats = loadModeStats(kanjiIds)
                    _uiState.value = _uiState.value.copy(
                        kanjiSortMode = mode,
                        gradeOneKanji = kanji,
                        kanjiPracticeCounts = practiceCounts,
                        kanjiModeStats = modeStats,
                        selectedMainTab = MainTab.KANJI
                    )
                }
                KanjiSortMode.STROKES -> {
                    val strokeCounts = kanjiRepository.getDistinctStrokeCounts()
                    val firstStroke = strokeCounts.firstOrNull() ?: 1
                    val kanji = kanjiRepository.getKanjiByStrokeCount(firstStroke)
                    val kanjiIds = kanji.map { it.id.toLong() }
                    val practiceCounts = loadPracticeCounts(kanjiIds)
                    val modeStats = loadModeStats(kanjiIds)
                    _uiState.value = _uiState.value.copy(
                        kanjiSortMode = mode,
                        gradeOneKanji = kanji,
                        availableStrokeCounts = strokeCounts,
                        selectedStrokeCount = firstStroke,
                        kanjiPracticeCounts = practiceCounts,
                        kanjiModeStats = modeStats,
                        selectedMainTab = MainTab.KANJI
                    )
                }
                KanjiSortMode.FREQUENCY -> {
                    val rangeIndex = _uiState.value.selectedFrequencyRange
                    val (from, to) = frequencyRanges[rangeIndex]
                    val kanji = kanjiRepository.getKanjiByFrequencyRange(from, to)
                    val kanjiIds = kanji.map { it.id.toLong() }
                    val practiceCounts = loadPracticeCounts(kanjiIds)
                    val modeStats = loadModeStats(kanjiIds)
                    _uiState.value = _uiState.value.copy(
                        kanjiSortMode = mode,
                        gradeOneKanji = kanji,
                        kanjiPracticeCounts = practiceCounts,
                        kanjiModeStats = modeStats,
                        selectedMainTab = MainTab.KANJI
                    )
                }
            }
        }
    }

    fun selectJlptLevel(level: Int) {
        viewModelScope.launch {
            val kanji = kanjiRepository.getKanjiByJlptLevel(level)
            val kanjiIds = kanji.map { it.id.toLong() }
            val practiceCounts = loadPracticeCounts(kanjiIds)
            val modeStats = loadModeStats(kanjiIds)
            _uiState.value = _uiState.value.copy(
                gradeOneKanji = kanji,
                selectedJlptLevel = level,
                kanjiPracticeCounts = practiceCounts,
                kanjiModeStats = modeStats
            )
        }
    }

    fun selectStrokeCount(count: Int) {
        viewModelScope.launch {
            val kanji = kanjiRepository.getKanjiByStrokeCount(count)
            val kanjiIds = kanji.map { it.id.toLong() }
            val practiceCounts = loadPracticeCounts(kanjiIds)
            val modeStats = loadModeStats(kanjiIds)
            _uiState.value = _uiState.value.copy(
                gradeOneKanji = kanji,
                selectedStrokeCount = count,
                kanjiPracticeCounts = practiceCounts,
                kanjiModeStats = modeStats
            )
        }
    }

    fun selectFrequencyRange(rangeIndex: Int) {
        viewModelScope.launch {
            val (from, to) = frequencyRanges[rangeIndex]
            val kanji = kanjiRepository.getKanjiByFrequencyRange(from, to)
            val kanjiIds = kanji.map { it.id.toLong() }
            val practiceCounts = loadPracticeCounts(kanjiIds)
            val modeStats = loadModeStats(kanjiIds)
            _uiState.value = _uiState.value.copy(
                gradeOneKanji = kanji,
                selectedFrequencyRange = rangeIndex,
                kanjiPracticeCounts = practiceCounts,
                kanjiModeStats = modeStats
            )
        }
    }

    private suspend fun loadPracticeCounts(kanjiIds: List<Long>): Map<Int, Int> {
        return if (kanjiIds.isNotEmpty()) {
            try {
                val cards = srsRepository.getCardsByIds(kanjiIds)
                cards.associate { it.kanjiId to it.totalReviews }
            } catch (_: Exception) { emptyMap() }
        } else emptyMap()
    }

    private suspend fun loadModeStats(kanjiIds: List<Long>): Map<Int, Map<String, Int>> {
        return if (kanjiIds.isNotEmpty()) {
            try { srsRepository.getModeStatsByIds(kanjiIds) } catch (_: Exception) { emptyMap() }
        } else emptyMap()
    }

    companion object {
        val frequencyRanges = listOf(
            1 to 500,
            501 to 1000,
            1001 to 2000,
            2001 to 5000
        )
        val frequencyLabels = listOf("Top 500", "501-1K", "1K-2K", "2K-5K")
    }

    fun refresh() {
        loadData()
    }

    private fun observeCoinBalance() {
        viewModelScope.launch {
            val userId = userSessionProvider.getUserId()
            jCoinRepository.observeBalance(userId).collect { balance ->
                _uiState.value = _uiState.value.copy(coinBalance = balance)
            }
        }
    }
}
