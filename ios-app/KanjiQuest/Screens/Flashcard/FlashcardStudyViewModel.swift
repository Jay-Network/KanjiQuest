import Foundation
import SharedCore

struct StudyCard: Identifiable {
    let id: String
    let kanji: Kanji
    let vocabulary: [Vocabulary]
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

    func load(container: AppContainer, deckId: String) {
        flashcardRepository = container.flashcardRepository
        kanjiRepository = container.kanjiRepository
        srsRepository = container.srsRepository

        Task {
            let entries = (try? await flashcardRepository?.getCardsInDeck(deckId: deckId)) ?? []
            var studyCards: [StudyCard] = []
            for entry in entries {
                if let kanji = try? await kanjiRepository?.getKanjiById(id: entry.kanjiId) {
                    let vocab = (try? await kanjiRepository?.getVocabularyForKanji(kanjiId: entry.kanjiId)) ?? []
                    studyCards.append(StudyCard(id: entry.id, kanji: kanji, vocabulary: Array(vocab.prefix(3))))
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

        // Update SRS card
        if currentIndex < cards.count {
            let card = cards[currentIndex]
            Task {
                try? await srsRepository?.updateReview(
                    kanjiId: card.kanji.id,
                    quality: Int32(grade.rawValue)
                )
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
