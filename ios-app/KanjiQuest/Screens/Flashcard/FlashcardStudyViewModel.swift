import Foundation
import SharedCore

struct StudyCard: Identifiable {
    var id: Int32 { kanji.id }
    let kanji: Kanji
    let vocabulary: [Vocabulary]
    let srsCard: SrsCard?
}

enum StudyGrade: Int, CaseIterable {
    case again = 1
    case hard = 2
    case good = 4
    case easy = 5

    var label: String {
        switch self {
        case .again: return "Again"
        case .hard: return "Hard"
        case .good: return "Good"
        case .easy: return "Easy"
        }
    }
}

@MainActor
final class FlashcardStudyViewModel: ObservableObject {
    @Published var cards: [StudyCard] = []
    @Published var currentIndex = 0
    @Published var isFlipped = false
    @Published var isComplete = false
    @Published var isLoading = true
    @Published var totalStudied = 0
    @Published var gradeResults: [StudyGrade: Int] = [:]

    private var flashcardRepository: FlashcardRepository?
    private var kanjiRepository: KanjiRepository?
    private var srsRepository: SrsRepository?
    private var srsAlgorithm: Sm2Algorithm?
    private var deckId: Int64 = 1

    func load(container: AppContainer, deckId: Int64) {
        flashcardRepository = container.flashcardRepository
        kanjiRepository = container.kanjiRepository
        srsRepository = container.srsRepository
        srsAlgorithm = container.srsAlgorithm
        self.deckId = deckId

        Task {
            let kanjiIds = (try? await flashcardRepository?.getKanjiIdsByDeck(deckId: deckId)) ?? []
            var studyCards: [StudyCard] = []
            for kanjiId in kanjiIds {
                if let kanji = try? await kanjiRepository?.getKanjiById(id: Int32(kanjiId)) {
                    let vocab = (try? await kanjiRepository?.getVocabularyForKanji(kanjiId: Int32(kanjiId))) ?? []
                    let srsCard = try? await srsRepository?.getCard(kanjiId: Int32(kanjiId))
                    studyCards.append(StudyCard(kanji: kanji, vocabulary: Array(vocab.prefix(3)), srsCard: srsCard))
                }
            }
            cards = studyCards.shuffled()
            isLoading = false
        }
    }

    func flip() {
        isFlipped = true
    }

    func grade(_ grade: StudyGrade) {
        gradeResults[grade, default: 0] += 1
        totalStudied += 1

        if currentIndex < cards.count {
            let card = cards[currentIndex]
            Task {
                // Update SRS card via algorithm
                if let existingCard = try? await srsRepository?.getCard(kanjiId: card.kanji.id) {
                    let currentTime = Int64(Date().timeIntervalSince1970)
                    if let updated = srsAlgorithm?.review(card: existingCard, quality: Int32(grade.rawValue), currentTime: currentTime) {
                        try? await srsRepository?.saveCard(card: updated)
                    }
                }

                // Mark studied in flashcard deck
                try? await flashcardRepository?.markStudied(deckId: deckId, kanjiId: card.kanji.id)
            }
        }

        // Advance to next card
        isFlipped = false
        if currentIndex + 1 < cards.count {
            currentIndex += 1
        } else {
            isComplete = true
        }
    }

    var currentCard: StudyCard? {
        guard currentIndex < cards.count else { return nil }
        return cards[currentIndex]
    }
}
