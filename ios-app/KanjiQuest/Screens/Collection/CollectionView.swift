import SwiftUI
import SharedCore

/// Collection screen. Mirrors Android's CollectionScreen.kt.
/// Tabs for item types, rarity filters, grid display with stats.
struct CollectionView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = CollectionViewModel()
    var onBack: () -> Void = {}
    var onKanjiClick: ((Int32) -> Void)? = nil

    var body: some View {
        ZStack {
            KanjiQuestTheme.background.ignoresSafeArea()

            if viewModel.isLoading {
                VStack(spacing: 16) {
                    ProgressView()
                    Text("Loading collection...").font(KanjiQuestTheme.bodyLarge)
                }
            } else {
                VStack(spacing: 0) {
                    // Stats summary
                    HStack {
                        Text("\(viewModel.totalCollected) Collected")
                            .font(KanjiQuestTheme.titleMedium).fontWeight(.bold)
                        Spacer()
                    }
                    .padding(.horizontal, 16).padding(.vertical, 8)

                    // Tabs
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(CollectionTab.allCases, id: \.self) { tab in
                                let selected = viewModel.selectedTab == tab
                                Button { viewModel.selectTab(tab) } label: {
                                    Text(tab.rawValue)
                                        .font(KanjiQuestTheme.bodyMedium)
                                        .fontWeight(selected ? .bold : .regular)
                                        .foregroundColor(selected ? .white : KanjiQuestTheme.onSurfaceVariant)
                                        .padding(.horizontal, 16).padding(.vertical, 8)
                                        .background(selected ? KanjiQuestTheme.primary : KanjiQuestTheme.surfaceVariant)
                                        .cornerRadius(20)
                                }
                            }
                        }
                        .padding(.horizontal, 16)
                    }

                    // Rarity filter
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 6) {
                            rarityChip(nil, label: "All")
                            ForEach([CollectedItemRarity.common, .uncommon, .rare, .epic, .legendary], id: \.self) { rarity in
                                rarityChip(rarity, label: rarity.label)
                            }
                        }
                        .padding(.horizontal, 16).padding(.vertical, 6)
                    }

                    // Grid
                    ScrollView {
                        if viewModel.filteredItems.isEmpty {
                            VStack(spacing: 16) {
                                Spacer().frame(height: 60)
                                Image(systemName: "tray")
                                    .font(.system(size: 48))
                                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant.opacity(0.3))
                                Text("No items collected yet")
                                    .font(KanjiQuestTheme.bodyLarge)
                                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                            }
                            .frame(maxWidth: .infinity)
                        } else {
                            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 5), spacing: 8) {
                                ForEach(viewModel.filteredItems, id: \.itemId) { item in
                                    collectionItemCard(item)
                                }
                            }
                            .padding(12)
                        }
                    }
                }
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left"); Text("Back")
                }.foregroundColor(.white)
            }
            ToolbarItem(placement: .principal) {
                Text("Collection").font(.headline).foregroundColor(.white)
            }
        }
        .toolbarBackground(KanjiQuestTheme.primary, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .task { viewModel.load(container: container) }
    }

    private func rarityChip(_ rarity: CollectedItemRarity?, label: String) -> some View {
        let selected = viewModel.selectedRarityFilter == rarity
        return Button { viewModel.filterByRarity(rarity) } label: {
            Text(label)
                .font(KanjiQuestTheme.labelSmall)
                .foregroundColor(selected ? .white : KanjiQuestTheme.onSurfaceVariant)
                .padding(.horizontal, 10).padding(.vertical, 4)
                .background(selected ? KanjiQuestTheme.primary : KanjiQuestTheme.surfaceVariant)
                .cornerRadius(12)
        }
    }

    private func collectionItemCard(_ item: CollectedItem) -> some View {
        let rarityColor = Color(hex: UInt(item.rarity.colorValue))
        let literal = viewModel.kanjiLiterals["\(item.itemId)"] ?? "\(item.itemId)"

        return Button {
            if viewModel.selectedTab == .kanji {
                onKanjiClick?(item.itemId)
            }
        } label: {
            VStack(spacing: 2) {
                Text(literal)
                    .font(.system(size: 24))
                    .frame(width: 48, height: 48)
                    .background(KanjiQuestTheme.surface)
                    .cornerRadius(8)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(rarityColor, lineWidth: 2)
                    )
            }
        }
        .buttonStyle(.plain)
    }
}
