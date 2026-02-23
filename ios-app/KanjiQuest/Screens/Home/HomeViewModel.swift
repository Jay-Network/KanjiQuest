import Foundation
import SharedCore
import Combine

// MARK: - Enums matching Android

enum MainTab: String, CaseIterable {
    case hiragana = "Hiragana"
    case katakana = "Katakana"
    case radicals = "部首"
    case kanji = "Kanji"
}

enum KanjiSortMode: String, CaseIterable {
    case schoolGrade = "School Grade"
    case jlptLevel = "JLPT Level"
    case strokes = "Strokes"
    case frequency = "Frequency"
}

struct PreviewTrialInfo {
    let remaining: Int
    let limit: Int
}

// MARK: - UI State matching Android HomeUiState (58 properties)

struct HomeUiState {
    var profile: UserProfile?
    var gradeOneKanji: [Kanji] = []
    var kanjiCount: Int64 = 0
    var coinBalance: CoinBalance?
    var wordOfTheDay: Vocabulary?
    var isLoading: Bool = true
    var isPremium: Bool = false
    var isAdmin: Bool = false
    var effectiveLevel: UserLevel = .free
    var previewTrials: [String: PreviewTrialInfo] = [:]
    var tierName: String = "Beginner"
    var tierNameJp: String = "入門"
    var tierProgress: Float = 0
    var nextTierName: String?
    var nextTierLevel: Int32?
    var highestUnlockedGrade: Int32 = 1
    var gradeMasteryList: [GradeMastery] = []
    var displayLevel: Int32 = 1
    var kanjiPracticeCounts: [Int32: Int32] = [:]
    var kanjiModeStats: [Int32: [String: Int32]] = [:]
    var flashcardDeckCount: Int64 = 0
    var unlockedGrades: [Int32] = [1]
    var allGrades: [Int32] = [1, 2, 3, 4, 5, 6, 8]
    var gradesWithCollection: Set<Int32> = []
    var selectedGrade: Int32 = 1
    var hiraganaProgress: Float = 0
    var katakanaProgress: Float = 0
    var radicalProgress: Float = 0
    var selectedMainTab: MainTab = .kanji
    var radicals: [Radical] = []
    var kanjiSortMode: KanjiSortMode = .schoolGrade
    var selectedJlptLevel: Int32 = 5
    var selectedStrokeCount: Int32 = 1
    var selectedFrequencyRange: Int = 0
    var availableStrokeCounts: [Int32] = []
    var collectedKanjiCount: Int = 0
    var totalKanjiInGrades: Int = 0
    var collectedItems: [Int32: CollectedItem] = [:]
    var hiraganaList: [Kana] = []
    var katakanaList: [Kana] = []
    var collectedHiraganaIds: Set<Int32> = []
    var collectedKatakanaIds: Set<Int32> = []
    var collectedRadicalIds: Set<Int32> = []
    var collectedHiraganaItems: [Int32: CollectedItem] = [:]
    var collectedKatakanaItems: [Int32: CollectedItem] = [:]
    var collectedRadicalItems: [Int32: CollectedItem] = [:]
    var perGradeCollectedCounts: [Int32: Int] = [:]
    var perGradeTotalCounts: [Int32: Int] = [:]
    var perJlptCollectedCounts: [Int32: Int] = [:]
    var perJlptTotalCounts: [Int32: Int] = [:]
}

// MARK: - ViewModel

@MainActor
class HomeViewModel: ObservableObject {
    @Published var uiState = HomeUiState()

    private let container: AppContainer
    private var isFirstLoad = true

    static let frequencyRanges: [(Int32, Int32)] = [
        (1, 500), (501, 1000), (1001, 2000), (2001, 5000)
    ]
    static let frequencyLabels = ["Top 500", "501-1K", "1K-2K", "2K-5K"]

    init(container: AppContainer) {
        self.container = container
        Task { await loadData() }
        Task { await observeCoinBalance() }
    }

    // MARK: - Main data load (mirrors Android loadData())

    func loadData() async {
        let prev = uiState
        let preserveMainTab = prev.selectedMainTab
        let preserveSortMode = prev.kanjiSortMode
        let preserveGrade = prev.selectedGrade
        let preserveJlpt = prev.selectedJlptLevel
        let preserveStroke = prev.selectedStrokeCount
        let preserveFreq = prev.selectedFrequencyRange
        let preserveStrokeCounts = prev.availableStrokeCounts

        // Sync email for admin detection
        if let email = try? await container.userSessionProvider.getUserEmail() {
            container.userSessionProvider.updateEmail(email: email)
        }

        let userId = (try? await container.userSessionProvider.getUserId()) ?? ""
        let profile = try? await container.userRepository.getProfile()
        let totalCount: Int64 = Int64(truncating: (try? await container.kanjiRepository.getKanjiCount()) ?? 0)
        let coinBalance = try? await container.jCoinRepository.getBalance(userId: userId)
        let wotd = try? await container.wordOfTheDayUseCase.getWordOfTheDay()
        let effectiveLevel = container.userSessionProvider.getEffectiveLevel()
        let isPremium = effectiveLevel == .premium || effectiveLevel == .admin

        // Tier progression
        let playerLevel = container.userSessionProvider.getAdminPlayerLevelOverride()?.int32Value ?? (profile?.level ?? 1)
        let tier = LevelProgression.shared.getTierForLevel(level: playerLevel)
        let nextTier = LevelProgression.shared.getNextTier(level: playerLevel)
        let unlockedGrades: [Int32] = (tier.unlockedGrades as? [NSNumber])?.map { $0.int32Value } ?? [1]
        let highestGrade = unlockedGrades.max() ?? 1

        let activeGrade = isFirstLoad ? highestGrade : preserveGrade
        let gradeKanji = (try? await container.kanjiRepository.getKanjiByGrade(grade: activeGrade)) ?? []

        // Grade mastery for all unlocked grades
        var gradeMasteryList: [GradeMastery] = []
        for grade in unlockedGrades {
            let totalRaw = try? await container.kanjiRepository.getKanjiCountByGrade(grade: grade)
            let total: Int64 = (totalRaw as? NSNumber)?.int64Value ?? 0
            if let mastery = try? await container.srsRepository.getGradeMastery(grade: grade, totalKanjiInGrade: total) {
                gradeMasteryList.append(mastery)
            }
        }

        // Practice counts for displayed kanji
        let kanjiIds = gradeKanji.map { Int64($0.id) }
        let practiceCounts = await loadPracticeCounts(kanjiIds: kanjiIds)
        let modeStats = await loadModeStats(kanjiIds: kanjiIds)

        let deckCount = (try? await container.flashcardRepository.getDeckCount()) ?? 0

        // Collection data — kanji
        let collectedKanjiItems = (try? await container.collectionRepository.getCollectedByType(type: .kanji)) ?? []
        let collectedItemsMap = Dictionary(uniqueKeysWithValues: collectedKanjiItems.map { ($0.itemId, $0) })
        let collectedKanjiIds = Set(collectedKanjiItems.map { $0.itemId })

        // Grades with collection
        var gradesWithCollection: Set<Int32> = []
        var perGradeCollected: [Int32: Int] = [:]
        var perGradeTotal: [Int32: Int] = [:]
        var totalKanjiInGrades = 0
        for grade: Int32 in [1, 2, 3, 4, 5, 6, 8] {
            let gradeList = (try? await container.kanjiRepository.getKanjiByGrade(grade: grade)) ?? []
            perGradeTotal[grade] = gradeList.count
            totalKanjiInGrades += gradeList.count
            let collected = gradeList.filter { collectedKanjiIds.contains($0.id) }.count
            perGradeCollected[grade] = collected
            if collected > 0 { gradesWithCollection.insert(grade) }
        }

        // Per-JLPT counts
        var perJlptCollected: [Int32: Int] = [:]
        var perJlptTotal: [Int32: Int] = [:]
        for level: Int32 in [5, 4, 3, 2, 1] {
            let jlptList = (try? await container.kanjiRepository.getKanjiByJlptLevel(level: level)) ?? []
            perJlptTotal[level] = jlptList.count
            perJlptCollected[level] = jlptList.filter { collectedKanjiIds.contains($0.id) }.count
        }

        // Collection data — kana + radicals
        let collectedHiragana = (try? await container.collectionRepository.getCollectedByType(type: .hiragana)) ?? []
        let collectedKatakana = (try? await container.collectionRepository.getCollectedByType(type: .katakana)) ?? []
        let collectedRadicals = (try? await container.collectionRepository.getCollectedByType(type: .radical)) ?? []

        // Kana lists
        let hiraganaList = (try? await container.kanaRepository.getKanaByType(type: .hiragana)) ?? []
        let katakanaList = (try? await container.kanaRepository.getKanaByType(type: .katakana)) ?? []

        // Radicals
        let radicals = (try? await container.radicalRepository.getAllRadicals()) ?? []

        // Progress
        let hiraganaTotal = Float(hiraganaList.count)
        let hiraganaStudied = Float((try? await container.kanaSrsRepository.getTypeStudiedCount(kanaType: "HIRAGANA")) ?? 0)
        let katakanaTotal = Float(katakanaList.count)
        let katakanaStudied = Float((try? await container.kanaSrsRepository.getTypeStudiedCount(kanaType: "KATAKANA")) ?? 0)
        let radicalTotal = Float(radicals.count)
        let radicalStudied = Float((try? await container.radicalSrsRepository.getStudiedCount()) ?? 0)

        // Stroke counts
        let strokeCounts: [Int32]
        if !isFirstLoad && !preserveStrokeCounts.isEmpty {
            strokeCounts = preserveStrokeCounts
        } else {
            strokeCounts = ((try? await container.kanjiRepository.getDistinctStrokeCounts()) ?? []).compactMap { ($0 as? NSNumber)?.int32Value }
        }

        uiState = HomeUiState(
            profile: profile,
            gradeOneKanji: gradeKanji,
            kanjiCount: totalCount,
            coinBalance: coinBalance,
            wordOfTheDay: wotd,
            isLoading: false,
            isPremium: isPremium,
            isAdmin: container.userSessionProvider.isAdmin().boolValue,
            effectiveLevel: effectiveLevel,
            previewTrials: loadPreviewTrials(),
            tierName: tier.nameEn,
            tierNameJp: tier.nameJp,
            tierProgress: LevelProgression.shared.getTierProgress(level: playerLevel),
            nextTierName: nextTier?.nameEn,
            nextTierLevel: (nextTier?.levelRange?.first as? NSNumber)?.int32Value,
            highestUnlockedGrade: highestGrade,
            gradeMasteryList: gradeMasteryList,
            displayLevel: playerLevel,
            kanjiPracticeCounts: practiceCounts,
            kanjiModeStats: modeStats,
            flashcardDeckCount: deckCount,
            unlockedGrades: unlockedGrades,
            allGrades: [1, 2, 3, 4, 5, 6, 8],
            gradesWithCollection: gradesWithCollection,
            selectedGrade: activeGrade,
            hiraganaProgress: hiraganaTotal > 0 ? hiraganaStudied / hiraganaTotal : 0,
            katakanaProgress: katakanaTotal > 0 ? katakanaStudied / katakanaTotal : 0,
            radicalProgress: radicalTotal > 0 ? radicalStudied / radicalTotal : 0,
            selectedMainTab: isFirstLoad ? .kanji : preserveMainTab,
            radicals: radicals,
            kanjiSortMode: isFirstLoad ? .schoolGrade : preserveSortMode,
            selectedJlptLevel: isFirstLoad ? 5 : preserveJlpt,
            selectedStrokeCount: isFirstLoad ? (strokeCounts.first ?? 1) : preserveStroke,
            selectedFrequencyRange: isFirstLoad ? 0 : preserveFreq,
            availableStrokeCounts: strokeCounts,
            collectedKanjiCount: collectedKanjiItems.count,
            totalKanjiInGrades: totalKanjiInGrades,
            collectedItems: collectedItemsMap,
            hiraganaList: hiraganaList,
            katakanaList: katakanaList,
            collectedHiraganaIds: Set(collectedHiragana.map { $0.itemId }),
            collectedKatakanaIds: Set(collectedKatakana.map { $0.itemId }),
            collectedRadicalIds: Set(collectedRadicals.map { $0.itemId }),
            collectedHiraganaItems: Dictionary(uniqueKeysWithValues: collectedHiragana.map { ($0.itemId, $0) }),
            collectedKatakanaItems: Dictionary(uniqueKeysWithValues: collectedKatakana.map { ($0.itemId, $0) }),
            collectedRadicalItems: Dictionary(uniqueKeysWithValues: collectedRadicals.map { ($0.itemId, $0) }),
            perGradeCollectedCounts: perGradeCollected,
            perGradeTotalCounts: perGradeTotal,
            perJlptCollectedCounts: perJlptCollected,
            perJlptTotalCounts: perJlptTotal
        )
        isFirstLoad = false
    }

    // MARK: - Preview Trials

    private func loadPreviewTrials() -> [String: PreviewTrialInfo] {
        return [
            "WRITING": PreviewTrialInfo(
                remaining: container.previewTrialManager.getRemainingTrials(mode: "WRITING"),
                limit: container.previewTrialManager.getTrialLimit(mode: "WRITING")
            ),
            "VOCABULARY": PreviewTrialInfo(
                remaining: container.previewTrialManager.getRemainingTrials(mode: "VOCABULARY"),
                limit: container.previewTrialManager.getTrialLimit(mode: "VOCABULARY")
            ),
            "CAMERA_CHALLENGE": PreviewTrialInfo(
                remaining: container.previewTrialManager.getRemainingTrials(mode: "CAMERA_CHALLENGE"),
                limit: container.previewTrialManager.getTrialLimit(mode: "CAMERA_CHALLENGE")
            ),
            "RADICAL_BUILDER": PreviewTrialInfo(
                remaining: container.previewTrialManager.getRemainingTrials(mode: "RADICAL_BUILDER"),
                limit: container.previewTrialManager.getTrialLimit(mode: "RADICAL_BUILDER")
            )
        ]
    }

    func usePreviewTrial(mode: String) -> Bool {
        let success = container.previewTrialManager.usePreviewTrial(mode: mode)
        if success {
            uiState.previewTrials = loadPreviewTrials()
        }
        return success
    }

    // MARK: - Tab / Sort / Filter Selection

    func selectMainTab(_ tab: MainTab) {
        uiState.selectedMainTab = tab
    }

    func selectGrade(_ grade: Int32) {
        Task {
            let gradeKanji = (try? await container.kanjiRepository.getKanjiByGrade(grade: grade)) ?? []
            let kanjiIds = gradeKanji.map { Int64($0.id) }
            let practiceCounts = await loadPracticeCounts(kanjiIds: kanjiIds)
            let modeStats = await loadModeStats(kanjiIds: kanjiIds)
            uiState.gradeOneKanji = gradeKanji
            uiState.selectedGrade = grade
            uiState.kanjiPracticeCounts = practiceCounts
            uiState.kanjiModeStats = modeStats
            uiState.kanjiSortMode = .schoolGrade
        }
    }

    func selectSortMode(_ mode: KanjiSortMode) {
        Task {
            switch mode {
            case .schoolGrade:
                let grade = uiState.selectedGrade
                let kanji = (try? await container.kanjiRepository.getKanjiByGrade(grade: grade)) ?? []
                let ids = kanji.map { Int64($0.id) }
                uiState.kanjiSortMode = mode
                uiState.gradeOneKanji = kanji
                uiState.kanjiPracticeCounts = await loadPracticeCounts(kanjiIds: ids)
                uiState.kanjiModeStats = await loadModeStats(kanjiIds: ids)
                uiState.selectedMainTab = .kanji

            case .jlptLevel:
                let level = uiState.selectedJlptLevel
                let kanji = (try? await container.kanjiRepository.getKanjiByJlptLevel(level: level)) ?? []
                let ids = kanji.map { Int64($0.id) }
                uiState.kanjiSortMode = mode
                uiState.gradeOneKanji = kanji
                uiState.kanjiPracticeCounts = await loadPracticeCounts(kanjiIds: ids)
                uiState.kanjiModeStats = await loadModeStats(kanjiIds: ids)
                uiState.selectedMainTab = .kanji

            case .strokes:
                let counts = ((try? await container.kanjiRepository.getDistinctStrokeCounts()) ?? []).compactMap { ($0 as? NSNumber)?.int32Value }
                let first = counts.first ?? 1
                let kanji = (try? await container.kanjiRepository.getKanjiByStrokeCount(strokeCount: first)) ?? []
                let ids = kanji.map { Int64($0.id) }
                uiState.kanjiSortMode = mode
                uiState.gradeOneKanji = kanji
                uiState.availableStrokeCounts = counts
                uiState.selectedStrokeCount = first
                uiState.kanjiPracticeCounts = await loadPracticeCounts(kanjiIds: ids)
                uiState.kanjiModeStats = await loadModeStats(kanjiIds: ids)
                uiState.selectedMainTab = .kanji

            case .frequency:
                let rangeIndex = uiState.selectedFrequencyRange
                let (from, to) = Self.frequencyRanges[rangeIndex]
                let kanji = (try? await container.kanjiRepository.getKanjiByFrequencyRange(from: from, to: to)) ?? []
                let ids = kanji.map { Int64($0.id) }
                uiState.kanjiSortMode = mode
                uiState.gradeOneKanji = kanji
                uiState.kanjiPracticeCounts = await loadPracticeCounts(kanjiIds: ids)
                uiState.kanjiModeStats = await loadModeStats(kanjiIds: ids)
                uiState.selectedMainTab = .kanji
            }
        }
    }

    func selectJlptLevel(_ level: Int32) {
        Task {
            let kanji = (try? await container.kanjiRepository.getKanjiByJlptLevel(level: level)) ?? []
            let ids = kanji.map { Int64($0.id) }
            uiState.gradeOneKanji = kanji
            uiState.selectedJlptLevel = level
            uiState.kanjiPracticeCounts = await loadPracticeCounts(kanjiIds: ids)
            uiState.kanjiModeStats = await loadModeStats(kanjiIds: ids)
        }
    }

    func selectStrokeCount(_ count: Int32) {
        Task {
            let kanji = (try? await container.kanjiRepository.getKanjiByStrokeCount(strokeCount: count)) ?? []
            let ids = kanji.map { Int64($0.id) }
            uiState.gradeOneKanji = kanji
            uiState.selectedStrokeCount = count
            uiState.kanjiPracticeCounts = await loadPracticeCounts(kanjiIds: ids)
            uiState.kanjiModeStats = await loadModeStats(kanjiIds: ids)
        }
    }

    func selectFrequencyRange(_ rangeIndex: Int) {
        Task {
            let (from, to) = Self.frequencyRanges[rangeIndex]
            let kanji = (try? await container.kanjiRepository.getKanjiByFrequencyRange(from: from, to: to)) ?? []
            let ids = kanji.map { Int64($0.id) }
            uiState.gradeOneKanji = kanji
            uiState.selectedFrequencyRange = rangeIndex
            uiState.kanjiPracticeCounts = await loadPracticeCounts(kanjiIds: ids)
            uiState.kanjiModeStats = await loadModeStats(kanjiIds: ids)
        }
    }

    // MARK: - Helpers

    private func loadPracticeCounts(kanjiIds: [Int64]) async -> [Int32: Int32] {
        guard !kanjiIds.isEmpty else { return [:] }
        let kotlinIds = kanjiIds.map { KotlinLong(value: $0) }
        guard let cards = try? await container.srsRepository.getCardsByIds(kanjiIds: kotlinIds) else { return [:] }
        var result: [Int32: Int32] = [:]
        for card in cards { result[card.kanjiId] = card.totalReviews }
        return result
    }

    private func loadModeStats(kanjiIds: [Int64]) async -> [Int32: [String: Int32]] {
        guard !kanjiIds.isEmpty else { return [:] }
        let kotlinIds = kanjiIds.map { KotlinLong(value: $0) }
        guard let stats = try? await container.srsRepository.getModeStatsByIds(kanjiIds: kotlinIds) else { return [:] }
        // Convert from KMP type to Swift dictionary
        var result: [Int32: [String: Int32]] = [:]
        for (key, value) in stats {
            if let k = key as? Int32, let v = value as? [String: Int32] {
                result[k] = v
            }
        }
        return result
    }

    func refresh() {
        Task { await loadData() }
    }

    private func observeCoinBalance() async {
        let userId = (try? await container.userSessionProvider.getUserId()) ?? ""
        // Observe coin balance flow — SKIE converts Flow to AsyncSequence
        do {
            for try await balance in container.jCoinRepository.observeBalance(userId: userId) {
                uiState.coinBalance = balance
            }
        } catch {
            // Flow collection ended
        }
    }
}
