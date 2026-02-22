import Foundation
import SharedCore

struct FlashcardItem: Identifiable {
    var id: Int32 { entry.kanjiId }
    let entry: FlashcardEntry
    let kanji: Kanji?
}

@MainActor
final class FlashcardViewModel: ObservableObject {
    @Published var items: [FlashcardItem] = []
    @Published var deckGroups: [FlashcardDeckGroup] = []
    @Published var selectedDeckId: Int64 = 1
    @Published var isLoading = true
    @Published var showCreateDeckDialog = false
    @Published var editingDeck: FlashcardDeckGroup? = nil

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
            try? await flashcardRepository?.ensureDefaultDeck()
            deckGroups = (try? await flashcardRepository?.getAllDeckGroups()) ?? []

            // Select first deck if current not in list
            if !deckGroups.contains(where: { $0.id == selectedDeckId }), let first = deckGroups.first {
                selectedDeckId = first.id
            }

            await loadDeck(selectedDeckId)
            isLoading = false
        }
    }

    func loadDeck(_ deckId: Int64) async {
        let entries = (try? await flashcardRepository?.getFlashcardsByDeck(deckId: deckId)) ?? []
        var newItems: [FlashcardItem] = []
        for entry in entries {
            let kanji = try? await kanjiRepository?.getKanjiById(id: entry.kanjiId)
            newItems.append(FlashcardItem(entry: entry, kanji: kanji))
        }
        items = newItems
    }

    func selectDeck(_ deck: FlashcardDeckGroup) {
        selectedDeckId = deck.id
        isLoading = true
        Task {
            await loadDeck(deck.id)
            isLoading = false
        }
    }

    func createDeck(name: String) {
        Task {
            let _ = try? await flashcardRepository?.createDeckGroup(name: name)
            loadDecks()
            showCreateDeckDialog = false
        }
    }

    func renameDeck(_ deck: FlashcardDeckGroup, to name: String) {
        Task {
            try? await flashcardRepository?.renameDeckGroup(id: deck.id, name: name)
            loadDecks()
            editingDeck = nil
        }
    }

    func deleteDeck(_ deck: FlashcardDeckGroup) {
        Task {
            try? await flashcardRepository?.deleteDeckGroup(id: deck.id)
            if selectedDeckId == deck.id, let first = deckGroups.first(where: { $0.id != deck.id }) {
                selectedDeckId = first.id
            }
            loadDecks()
            editingDeck = nil
        }
    }

    func removeFromDeck(_ item: FlashcardItem) {
        Task {
            try? await flashcardRepository?.removeFromDeck(deckId: selectedDeckId, kanjiId: item.entry.kanjiId)
            await loadDeck(selectedDeckId)
        }
    }

    func refresh() { loadDecks() }
}
