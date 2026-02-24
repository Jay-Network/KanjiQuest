import Foundation
import SharedCore

/// UI state matching Android's StudyUiState.
struct StudyUiState {
    var selectedTab: ContentTab = .kanji
    var selectedMode: GameModeEnum? = nil
    var selectedSource: StudySource = .all
    var kanaFilter: KanaFilter = .hiraganaOnly
    var isPremium: Bool = false
    var isAdmin: Bool = false
    var previewTrialsRemaining: [GameModeEnum: Int] = [:]
    var decks: [(id: Int64, name: String)] = []
}

/// ViewModel matching Android's StudyViewModel.
@MainActor
class StudyViewModel: ObservableObject {
    @Published var uiState = StudyUiState()

    private let container: AppContainer

    init(container: AppContainer) {
        self.container = container
        Task { await loadData() }
    }

    func loadData() async {
        let effectiveLevel = container.userSessionProvider.getEffectiveLevel()
        let isPremium = effectiveLevel == .premium || effectiveLevel == .admin
        let isAdmin = container.userSessionProvider.isAdmin()

        // Load flashcard decks
        let decksList: [(id: Int64, name: String)]
        if let decks = try? await container.flashcardRepository.getAllDeckGroups() {
            decksList = decks.map { (id: $0.id, name: $0.name) }
        } else {
            decksList = []
        }

        // Load preview trial counts
        var trials: [GameModeEnum: Int] = [:]
        for mode in GameModeEnum.allCases {
            if let key = mode.previewTrialKey {
                trials[mode] = container.previewTrialManager.getRemainingTrials(mode: key)
            }
        }

        uiState.isPremium = isPremium
        uiState.isAdmin = isAdmin
        uiState.decks = decksList
        uiState.previewTrialsRemaining = trials
    }

    func selectTab(_ tab: ContentTab) {
        uiState.selectedTab = tab
        uiState.selectedMode = nil
        uiState.selectedSource = .all
    }

    func selectMode(_ mode: GameModeEnum) {
        uiState.selectedMode = mode
    }

    func selectSource(_ source: StudySource) {
        uiState.selectedSource = source
    }

    func selectKanaFilter(_ filter: KanaFilter) {
        uiState.kanaFilter = filter
    }

    func isModeAccessible(_ mode: GameModeEnum) -> Bool {
        if uiState.isPremium || uiState.isAdmin { return true }
        if !mode.isPremiumGated { return true }
        // Check preview trials
        if let key = mode.previewTrialKey {
            return container.previewTrialManager.getRemainingTrials(mode: key) > 0
        }
        return false
    }

    func usePreviewTrial(_ mode: GameModeEnum) {
        if let key = mode.previewTrialKey {
            _ = container.previewTrialManager.usePreviewTrial(mode: key)
            uiState.previewTrialsRemaining[mode] = container.previewTrialManager.getRemainingTrials(mode: key)
        }
    }
}
