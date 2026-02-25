import SwiftUI

/// Study screen matching Android's StudyScreen.kt.
/// Content tabs, mode selector, source selector, kana filter, start session button.
struct StudyTabView: View {
    @EnvironmentObject var container: AppContainer
    let navigateTo: (NavRoute) -> Void

    var body: some View {
        StudyTabViewContent(container: container, navigateTo: navigateTo)
    }
}

private struct StudyTabViewContent: View {
    @StateObject private var viewModel: StudyViewModel
    let navigateTo: (NavRoute) -> Void

    init(container: AppContainer, navigateTo: @escaping (NavRoute) -> Void) {
        _viewModel = StateObject(wrappedValue: StudyViewModel(container: container))
        self.navigateTo = navigateTo
    }

    var body: some View {
        let state = viewModel.uiState

        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Content tabs: Kana | Radicals | Kanji
                HStack(spacing: 8) {
                    ForEach(ContentTab.allCases, id: \.self) { tab in
                        let isSelected = tab == state.selectedTab
                        Text(tab.rawValue)
                            .font(.system(size: 14, weight: isSelected ? .bold : .regular))
                            .foregroundColor(isSelected ? .white : KanjiQuestTheme.primary)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(isSelected ? KanjiQuestTheme.primary : KanjiQuestTheme.surfaceVariant)
                            .cornerRadius(8)
                            .onTapGesture { viewModel.selectTab(tab) }
                    }
                }

                // Study Mode selector
                Text("Study Mode")
                    .font(KanjiQuestTheme.titleMedium)
                    .fontWeight(.bold)

                let availableModes = GameModeEnum.modesForTab(state.selectedTab)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(availableModes, id: \.self) { mode in
                            let isSelected = mode == state.selectedMode
                            let isAccessible = viewModel.isModeAccessible(mode)
                            let modeInfo = mode.info

                            Button(action: {
                                if isAccessible {
                                    viewModel.selectMode(mode)
                                } else {
                                    navigateTo(.subscription)
                                }
                            }) {
                                VStack(spacing: 4) {
                                    if let imageAsset = modeInfo.imageAsset {
                                        AssetImage(filename: imageAsset, contentDescription: modeInfo.label)
                                            .frame(width: 36, height: 36)
                                    }
                                    Text(modeInfo.label)
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundColor(isSelected ? .white : KanjiQuestTheme.onSurfaceVariant)
                                    if !isAccessible && !state.isPremium {
                                        let trials = state.previewTrialsRemaining[mode] ?? 0
                                        Text(trials > 0 ? "Preview (\(trials))" : "Premium")
                                            .font(.system(size: 9))
                                            .foregroundColor(Color(hex: 0xB8860B))
                                    }
                                }
                                .frame(width: 120, height: 100)
                                .background(
                                    isSelected ? Color(hex: UInt(modeInfo.color)) :
                                    isAccessible ? KanjiQuestTheme.surfaceVariant :
                                    KanjiQuestTheme.surfaceVariant.opacity(0.5)
                                )
                                .cornerRadius(12)
                            }
                        }
                    }
                }

                // Source selector
                Text("Source")
                    .font(KanjiQuestTheme.titleMedium)
                    .fontWeight(.bold)

                HStack(spacing: 8) {
                    SourceChipView(
                        label: "All",
                        isSelected: state.selectedSource == .all,
                        action: { viewModel.selectSource(.all) }
                    )

                    if !state.decks.isEmpty {
                        Menu {
                            ForEach(state.decks, id: \.id) { deck in
                                Button(deck.name) {
                                    viewModel.selectSource(.fromFlashcardDeck(deckId: deck.id, deckName: deck.name))
                                }
                            }
                        } label: {
                            let currentDeck: String? = {
                                if case .fromFlashcardDeck(_, let name) = state.selectedSource { return name }
                                return nil
                            }()
                            SourceChipLabel(
                                label: currentDeck.map { "Deck: \($0)" } ?? "Flashcard Deck",
                                isSelected: {
                                    if case .fromFlashcardDeck = state.selectedSource { return true }
                                    return false
                                }()
                            )
                        }
                    }

                    if state.selectedTab == .kanji {
                        SourceChipView(
                            label: "Collection",
                            isSelected: state.selectedSource == .fromCollection,
                            action: { viewModel.selectSource(.fromCollection) }
                        )
                    }
                }

                // Kana sub-filter (kana tab only)
                if state.selectedTab == .kana {
                    Text("Kana Type")
                        .font(KanjiQuestTheme.titleMedium)
                        .fontWeight(.bold)

                    HStack(spacing: 8) {
                        ForEach(KanaFilter.allCases, id: \.self) { filter in
                            let isSelected = filter == state.kanaFilter
                            Text(filter.rawValue)
                                .font(.system(size: 13, weight: isSelected ? .bold : .regular))
                                .foregroundColor(isSelected ? .white : KanjiQuestTheme.onSurfaceVariant)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(isSelected ? Color(hex: 0xE91E63) : KanjiQuestTheme.surfaceVariant)
                                .cornerRadius(8)
                                .onTapGesture { viewModel.selectKanaFilter(filter) }
                        }
                    }
                }

                Spacer().frame(height: 8)

                // Start Session button
                let canStart = state.selectedMode != nil && viewModel.isModeAccessible(state.selectedMode!)

                Button(action: {
                    guard let selectedMode = state.selectedMode else { return }

                    // Use preview trial if needed
                    if !state.isPremium && !state.isAdmin && selectedMode.isPremiumGated {
                        viewModel.usePreviewTrial(selectedMode)
                    }

                    // Navigate to the game mode
                    let route = routeForMode(selectedMode, kanaFilter: state.kanaFilter)
                    navigateTo(route)
                }) {
                    Text("Start Session")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(canStart ? KanjiQuestTheme.primary : KanjiQuestTheme.primary.opacity(0.4))
                        .cornerRadius(16)
                }
                .disabled(!canStart)

                if !canStart, let mode = state.selectedMode, !state.isPremium {
                    Button(action: { navigateTo(.subscription) }) {
                        Text("Upgrade to Premium to unlock this mode")
                            .font(.system(size: 12))
                            .foregroundColor(Color(hex: 0xB8860B))
                    }
                    .padding(.top, -8)
                }
            }
            .padding(16)
        }
        .background(KanjiQuestTheme.background)
    }

    private func routeForMode(_ mode: GameModeEnum, kanaFilter: KanaFilter) -> NavRoute {
        switch mode {
        case .recognition: return .recognition
        case .writing: return .writing
        case .vocabulary: return .vocabulary
        case .cameraChallenge: return .camera
        case .kanaRecognition:
            let type = kanaFilter == .katakanaOnly ? "KATAKANA" : "HIRAGANA"
            return .kanaRecognition(kanaType: type)
        case .kanaWriting:
            let type = kanaFilter == .katakanaOnly ? "KATAKANA" : "HIRAGANA"
            return .kanaWriting(kanaType: type)
        case .radicalRecognition: return .radicalRecognition
        case .radicalBuilder: return .radicalBuilder
        }
    }
}

/// Reusable source chip.
private struct SourceChipView: View {
    let label: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            SourceChipLabel(label: label, isSelected: isSelected)
        }
    }
}

private struct SourceChipLabel: View {
    let label: String
    let isSelected: Bool

    var body: some View {
        Text(label)
            .font(.system(size: 12, weight: isSelected ? .bold : .regular))
            .foregroundColor(isSelected ? .white : KanjiQuestTheme.primary)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(isSelected ? KanjiQuestTheme.primary : KanjiQuestTheme.surfaceVariant)
            .cornerRadius(8)
    }
}
