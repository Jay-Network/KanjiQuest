import Foundation
import SharedCore

enum CollectionTab: String, CaseIterable {
    case kanji = "Kanji"
    case hiragana = "Hiragana"
    case katakana = "Katakana"
    case radical = "Radicals"
}

@MainActor
final class CollectionViewModel: ObservableObject {
    @Published var selectedTab: CollectionTab = .kanji
    @Published var items: [CollectedItem] = []
    @Published var filteredItems: [CollectedItem] = []
    @Published var totalCollected = 0
    @Published var selectedRarityFilter: CollectedItemRarity? = nil
    @Published var kanjiLiterals: [String: String] = [:]
    @Published var isLoading = true

    private var collectionRepository: CollectionRepository?
    private var kanjiRepository: KanjiRepository?

    func load(container: AppContainer) {
        collectionRepository = container.collectionRepository
        kanjiRepository = container.kanjiRepository
        loadItems()
    }

    func selectTab(_ tab: CollectionTab) {
        selectedTab = tab
        loadItems()
    }

    func filterByRarity(_ rarity: CollectedItemRarity?) {
        selectedRarityFilter = rarity
        applyFilter()
    }

    private func loadItems() {
        Task {
            isLoading = true
            let type = selectedTab.rawValue.lowercased()
            items = (try? await collectionRepository?.getItemsByType(type: type)) ?? []
            totalCollected = items.count

            // Load kanji literals for kanji tab
            if selectedTab == .kanji {
                var literals: [String: String] = [:]
                for item in items {
                    if let kanji = try? await kanjiRepository?.getKanjiById(id: item.itemId) {
                        literals["\(item.itemId)"] = kanji.literal
                    }
                }
                kanjiLiterals = literals
            }

            applyFilter()
            isLoading = false
        }
    }

    private func applyFilter() {
        if let rarity = selectedRarityFilter {
            filteredItems = items.filter { $0.rarity == rarity }
        } else {
            filteredItems = items
        }
    }
}
