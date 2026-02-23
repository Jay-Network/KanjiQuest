import Foundation
import SharedCore

struct ModeTrialInfo {
    let canPractice: Bool
    let trialsRemaining: Int
}

@MainActor
final class KanjiDetailViewModel: ObservableObject {
    @Published var kanji: Kanji? = nil
    @Published var vocabulary: [Vocabulary] = []
    @Published var vocabSentences: [Int64: ExampleSentence] = [:]
    @Published var isLoading = true
    @Published var totalPracticeCount = 0
    @Published var accuracy: Float? = nil
    @Published var isInFlashcardDeck = false
    @Published var isPremium = false
    @Published var isAdmin = false
    @Published var modeTrials: [String: ModeTrialInfo] = [:]
    @Published var showDeckChooser = false
    @Published var deckGroups: [FlashcardDeckGroup] = []
    @Published var kanjiInDecks: [Int64] = []

    private var kanjiRepository: KanjiRepository?
    private var srsRepository: SrsRepository?
    private var flashcardRepository: FlashcardRepository?
    private var userRepository: UserRepository?
    private var previewTrialManager: PreviewTrialManager?

    func load(container: AppContainer, kanjiId: Int32) {
        kanjiRepository = container.kanjiRepository
        srsRepository = container.srsRepository
        flashcardRepository = container.flashcardRepository
        userRepository = container.userRepository
        previewTrialManager = container.previewTrialManager

        Task {
            let k = try? await kanjiRepository?.getKanjiById(id: kanjiId)
            let vocab: [Vocabulary] = (k != nil) ? ((try? await kanjiRepository?.getVocabularyForKanji(kanjiId: kanjiId)) ?? []) : []

            // Load example sentences
            var sentences: [Int64: ExampleSentence] = [:]
            for v in vocab.prefix(10) {
                if let sentence = try? await kanjiRepository?.getExampleSentence(vocabId: v.id) {
                    sentences[v.id] = sentence
                }
            }

            // SRS stats
            let srsCard = try? await srsRepository?.getCard(kanjiId: kanjiId)
            let totalPractice = Int(srsCard?.totalReviews ?? 0)
            let acc: Float? = (srsCard != nil && srsCard!.totalReviews > 0) ? srsCard!.accuracy : nil

            // Flashcard state
            let inDeckRaw = try? await flashcardRepository?.isInDeck(kanjiId: kanjiId)
            let inDeck = (inDeckRaw as? NSNumber)?.boolValue ?? false

            // Premium/admin check
            let premium = container.userSessionProvider.isPremium()
            let admin = container.userSessionProvider.isAdmin()

            // Mode trials
            let writingTrials = previewTrialManager?.getRemainingTrials(mode: "WRITING") ?? 0
            let vocabTrials = previewTrialManager?.getRemainingTrials(mode: "VOCABULARY") ?? 0
            let cameraTrials = previewTrialManager?.getRemainingTrials(mode: "CAMERA_CHALLENGE") ?? 0

            let trials: [String: ModeTrialInfo] = [
                "recognition": ModeTrialInfo(canPractice: true, trialsRemaining: -1),
                "writing": ModeTrialInfo(canPractice: premium || admin || writingTrials > 0, trialsRemaining: writingTrials),
                "vocabulary": ModeTrialInfo(canPractice: premium || admin || vocabTrials > 0, trialsRemaining: vocabTrials),
                "camera_challenge": ModeTrialInfo(canPractice: premium || admin || cameraTrials > 0, trialsRemaining: cameraTrials)
            ]

            self.kanji = k
            self.vocabulary = vocab
            self.vocabSentences = sentences
            self.totalPracticeCount = totalPractice
            self.accuracy = acc
            self.isInFlashcardDeck = inDeck
            self.isPremium = premium
            self.isAdmin = admin
            self.modeTrials = trials
            self.isLoading = false
        }
    }

    func toggleFlashcard() {
        Task {
            try? await flashcardRepository?.ensureDefaultDeck()
            let groups = (try? await flashcardRepository?.getAllDeckGroups()) ?? []
            if groups.count <= 1 {
                let toggleRaw = try? await flashcardRepository?.toggleInDeck(kanjiId: kanji?.id ?? 0)
                let nowInDeck = (toggleRaw as? NSNumber)?.boolValue ?? false
                isInFlashcardDeck = nowInDeck
            } else {
                let decksForKanji = (try? await flashcardRepository?.getDecksForKanji(kanjiId: kanji?.id ?? 0)) ?? []
                deckGroups = groups
                kanjiInDecks = decksForKanji.map { Int64(truncating: $0) }
                showDeckChooser = true
            }
        }
    }

    func addToDeck(_ deckId: Int64) {
        guard let kanjiId = kanji?.id else { return }
        Task {
            try? await flashcardRepository?.addToDeck(deckId: deckId, kanjiId: kanjiId)
            let decks = (try? await flashcardRepository?.getDecksForKanji(kanjiId: kanjiId)) ?? []
            kanjiInDecks = decks.map { Int64(truncating: $0) }
            isInFlashcardDeck = true
        }
    }

    func removeFromDeck(_ deckId: Int64) {
        guard let kanjiId = kanji?.id else { return }
        Task {
            try? await flashcardRepository?.removeFromDeck(deckId: deckId, kanjiId: kanjiId)
            let decks = (try? await flashcardRepository?.getDecksForKanji(kanjiId: kanjiId)) ?? []
            kanjiInDecks = decks.map { Int64(truncating: $0) }
            isInFlashcardDeck = !decks.isEmpty
        }
    }

    func dismissDeckChooser() {
        showDeckChooser = false
    }

    func useModeTrial(mode: String) -> Bool {
        if mode == "recognition" { return true }
        guard let manager = previewTrialManager else { return false }
        let success = manager.usePreviewTrial(mode: mode)
        if success {
            let remaining = manager.getRemainingTrials(mode: mode)
            var updatedTrials = modeTrials
            updatedTrials[mode] = ModeTrialInfo(
                canPractice: isPremium || isAdmin || remaining > 0,
                trialsRemaining: remaining
            )
            modeTrials = updatedTrials
        }
        return success
    }
}
