import Foundation
import SharedCore

struct FlashcardItem: Identifiable {
    let id: String
    let entry: FlashcardEntry
    let kanji: Kanji?
}

@MainActor
final class FlashcardViewModel: ObservableObject {
    @Published var items: [FlashcardItem] = []
    @Published var deckGroups: [FlashcardDeck] = []
    @Published var selectedDeckId: String? = nil
    @Published var isLoading = true
    @Published var showCreateDeckDialog = false
    @Published var editingDeck: FlashcardDeck? = nil

    private var flashcardRepository: FlashcardRepository?
    private var kanjiRepository: KanjiRepository?

    func load(container: AppContainer) {
        flashcardRepository = container.flashcardRepository
        kanjiRepository = container.kanjiRepository
        loadDecks()
    }

    func loadDecks() {
        Task {
            isLoading = true
            deckGroups = (try? await flashcardRepository?.getAllDecks()) ?? []

            // Select first deck if none selected
            if selectedDeckId == nil, let first = deckGroups.first {
                selectedDeckId = first.id
            }

            // Create default deck if none exist
            if deckGroups.isEmpty {
                try? await flashcardRepository?.createDeck(name: "My Kanji")
                deckGroups = (try? await flashcardRepository?.getAllDecks()) ?? []
                selectedDeckId = deckGroups.first?.id
            }

            await loadDeck()
            isLoading = false
        }
    }

    func loadDeck() async {
        guard let deckId = selectedDeckId else { items = []; return }
        let entries = (try? await flashcardRepository?.getCardsInDeck(deckId: deckId)) ?? []
        var newItems: [FlashcardItem] = []
        for entry in entries {
            let kanji = try? await kanjiRepository?.getKanjiById(id: entry.kanjiId)
            newItems.append(FlashcardItem(id: entry.id, entry: entry, kanji: kanji))
        }
        items = newItems
    }

    func selectDeck(_ deck: FlashcardDeck) {
        selectedDeckId = deck.id
        Task { await loadDeck() }
    }

    func createDeck(name: String) {
        Task {
            try? await flashcardRepository?.createDeck(name: name)
            loadDecks()
            showCreateDeckDialog = false
        }
    }

    func renameDeck(_ deck: FlashcardDeck, to name: String) {
        Task {
            try? await flashcardRepository?.renameDeck(deckId: deck.id, name: name)
            loadDecks()
            editingDeck = nil
        }
    }

    func deleteDeck(_ deck: FlashcardDeck) {
        Task {
            try? await flashcardRepository?.deleteDeck(deckId: deck.id)
            if selectedDeckId == deck.id { selectedDeckId = nil }
            loadDecks()
            editingDeck = nil
        }
    }

    func removeFromDeck(_ item: FlashcardItem) {
        Task {
            try? await flashcardRepository?.removeFromDeck(entryId: item.entry.id)
            await loadDeck()
        }
    }

    func refresh() { loadDecks() }
}
