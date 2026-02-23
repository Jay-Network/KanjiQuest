import SwiftUI
import SharedCore

/// HomeView entry point — delegates to HomeViewContent for proper container-based init.
struct HomeView: View {
    @EnvironmentObject var container: AppContainer
    let navigateTo: (NavRoute) -> Void

    var body: some View {
        HomeViewContent(container: container, navigateTo: navigateTo)
    }
}



// MARK: - Subviews

private struct GameModeButtonView: View {
    let label: String
    var subtitle: String? = nil
    var modeColor: Color = KanjiQuestTheme.primary
    var imageAsset: String? = nil
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                if let imageAsset {
                    AssetImage(filename: imageAsset, contentDescription: label)
                        .frame(width: 48, height: 48)
                    Spacer().frame(width: 8)
                }
                VStack(alignment: .leading) {
                    Text(label)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.white)
                    if let subtitle {
                        Text(subtitle)
                            .font(.system(size: 10))
                            .foregroundColor(.white.opacity(0.7))
                    }
                }
                Spacer()
            }
            .padding(.horizontal, 12)
            .frame(height: 80)
            .frame(maxWidth: .infinity)
            .background(modeColor)
            .cornerRadius(KanjiQuestTheme.radiusM)
        }
    }
}

private struct PreviewableGameModeButtonView: View {
    let label: String
    let isPremium: Bool
    let trialInfo: PreviewTrialInfo?
    var modeColor: Color = KanjiQuestTheme.primary
    var imageAsset: String? = nil
    let onPremiumClick: () -> Void
    let onPreviewClick: () -> Void
    let onUpgradeClick: () -> Void

    var body: some View {
        if isPremium {
            GameModeButtonView(
                label: label, modeColor: modeColor, imageAsset: imageAsset,
                action: onPremiumClick
            )
        } else {
            let remaining = trialInfo?.remaining ?? 0
            let hasTrials = remaining > 0

            Button(action: { hasTrials ? onPreviewClick() : onUpgradeClick() }) {
                HStack {
                    if let imageAsset {
                        AssetImage(filename: imageAsset, contentDescription: label)
                            .frame(width: 48, height: 48)
                        Spacer().frame(width: 8)
                    }
                    VStack(alignment: .leading) {
                        Text(label)
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(hasTrials ? KanjiQuestTheme.onSurface : KanjiQuestTheme.onSurfaceVariant)
                        Text(hasTrials ? "Preview (\(remaining) left)" : "Upgrade to unlock")
                            .font(.system(size: 10, weight: hasTrials ? .regular : .bold))
                            .foregroundColor(hasTrials ? KanjiQuestTheme.onSurfaceVariant : Color(hex: 0xB8860B))
                    }
                    Spacer()
                }
                .padding(.horizontal, 12)
                .frame(height: 80)
                .frame(maxWidth: .infinity)
                .background(hasTrials ? KanjiQuestTheme.secondary.opacity(0.15) : KanjiQuestTheme.surfaceVariant)
                .cornerRadius(KanjiQuestTheme.radiusM)
            }
        }
    }
}

private struct LearningPathCardView: View {
    let title: String
    let subtitle: String
    let progress: Float
    let color: Color

    var body: some View {
        VStack(spacing: 4) {
            Text(title)
                .font(KanjiQuestTheme.labelMedium)
                .fontWeight(.bold)
            Text(subtitle)
                .font(.system(size: 10))
                .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
            ProgressView(value: Double(min(max(progress, 0), 1)))
                .tint(color)
            Text("\(Int(progress * 100))%")
                .font(.system(size: 9))
                .foregroundColor(color)
        }
        .padding(8)
        .frame(width: 100, height: 80)
        .background(KanjiQuestTheme.surface)
        .cornerRadius(KanjiQuestTheme.radiusM)
    }
}

private struct GradeMasteryBadgeView: View {
    let mastery: GradeMastery

    private var badgeAsset: String {
        switch mastery.masteryLevel {
        case .beginning: return "grade-beginning.png"
        case .developing: return "grade-developing.png"
        case .proficient: return "grade-proficient.png"
        case .advanced: return "grade-advanced.png"
        default: return "grade-beginning.png"
        }
    }

    private var ringColor: Color {
        switch mastery.masteryLevel {
        case .beginning: return Color(hex: 0xE57373)
        case .developing: return Color(hex: 0xFFB74D)
        case .proficient: return Color(hex: 0x81C784)
        case .advanced: return Color(hex: 0xFFD700)
        default: return Color(hex: 0xE57373)
        }
    }

    var body: some View {
        VStack(spacing: 2) {
            ZStack {
                AssetImage(filename: badgeAsset, contentDescription: "\(mastery.masteryLevel.label) badge")
                    .frame(width: 56, height: 56)
                Text("G\(mastery.grade)")
                    .font(KanjiQuestTheme.labelLarge)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            }
            Text(mastery.masteryLevel.label)
                .font(.system(size: 9, weight: .bold))
                .foregroundColor(ringColor)
        }
        .padding(4)
    }
}

// MARK: - Grid Items

private struct KanjiGridItemView: View {
    let kanji: Kanji
    var practiceCount: Int32 = 0
    var modeStats: [String: Int32] = [:]
    var collectedItem: CollectedItem? = nil
    let onClick: () -> Void

    var body: some View {
        let isCollected = collectedItem != nil
        let borderColor = collectedItem.map { Color(hex: UInt($0.rarity.colorValue)) }

        ZStack {
            if isCollected {
                KanjiText(text: kanji.literal, font: .system(size: 28))

                // Level badge
                if let item = collectedItem, item.itemLevel > 1 {
                    VStack {
                        HStack {
                            Spacer()
                            Text("Lv.\(item.itemLevel)")
                                .font(.system(size: 7, weight: .bold))
                                .foregroundColor(Color(hex: UInt(item.rarity.colorValue)))
                                .padding(2)
                        }
                        Spacer()
                    }
                }

                // 4-corner mode badges
                let recCount = modeStats["recognition"] ?? 0
                let vocCount = modeStats["vocabulary"] ?? 0
                let wrtCount = modeStats["writing"] ?? 0
                let camCount = modeStats["camera_challenge"] ?? 0

                if recCount > 0 {
                    VStack { HStack { Text("\(recCount)").font(.system(size: 7, weight: .bold)).foregroundColor(Color(hex: 0x2196F3)).padding(3); Spacer() }; Spacer() }
                }
                if vocCount > 0 && (collectedItem?.itemLevel ?? 0) <= 1 {
                    VStack { HStack { Spacer(); Text("\(vocCount)").font(.system(size: 7, weight: .bold)).foregroundColor(Color(hex: 0xFF9800)).padding(3) }; Spacer() }
                }
                if wrtCount > 0 {
                    VStack { Spacer(); HStack { Text("\(wrtCount)").font(.system(size: 7, weight: .bold)).foregroundColor(Color(hex: 0x4CAF50)).padding(3); Spacer() } }
                }
                if camCount > 0 {
                    VStack { Spacer(); HStack { Spacer(); Text("\(camCount)").font(.system(size: 7, weight: .bold)).foregroundColor(Color(hex: 0x9C27B0)).padding(3) } }
                }
            }
        }
        .frame(height: 64)
        .frame(maxWidth: .infinity)
        .background(isCollected ? KanjiQuestTheme.surface : KanjiQuestTheme.surface.opacity(0.3))
        .cornerRadius(KanjiQuestTheme.radiusM)
        .overlay(
            RoundedRectangle(cornerRadius: KanjiQuestTheme.radiusM)
                .stroke(borderColor ?? .clear, lineWidth: borderColor != nil ? 2 : 0)
        )
        .onTapGesture { if isCollected { onClick() } }
    }
}

private struct KanaGridItemView: View {
    let kana: Kana
    var collectedItem: CollectedItem? = nil
    let onClick: () -> Void

    var body: some View {
        let isCollected = collectedItem != nil
        let borderColor = collectedItem.map { Color(hex: UInt($0.rarity.colorValue)) }

        ZStack {
            if isCollected {
                VStack(spacing: 0) {
                    Text(kana.literal)
                        .font(.system(size: 26, weight: .bold))
                    Text(kana.romanization)
                        .font(.system(size: 8))
                        .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                }
            }
        }
        .frame(height: 64)
        .frame(maxWidth: .infinity)
        .background(isCollected ? KanjiQuestTheme.surface : KanjiQuestTheme.surface.opacity(0.3))
        .cornerRadius(KanjiQuestTheme.radiusM)
        .overlay(
            RoundedRectangle(cornerRadius: KanjiQuestTheme.radiusM)
                .stroke(borderColor ?? .clear, lineWidth: borderColor != nil ? 2 : 0)
        )
        .onTapGesture { if isCollected { onClick() } }
    }
}

private struct RadicalGridItemView: View {
    let radical: Radical
    var collectedItem: CollectedItem? = nil
    let onClick: () -> Void

    var body: some View {
        let isCollected = collectedItem != nil
        let borderColor = collectedItem.map { Color(hex: UInt($0.rarity.colorValue)) }

        ZStack {
            if isCollected {
                VStack(spacing: 2) {
                    RadicalImage(radicalId: radical.id, contentDescription: radical.literal)
                        .frame(width: 36, height: 36)
                    if let meaningJp = radical.meaningJp, !meaningJp.isEmpty {
                        Text(meaningJp)
                            .font(.system(size: 9))
                            .foregroundColor(.white)
                            .lineLimit(1)
                    }
                }
            }
        }
        .frame(height: 72)
        .frame(maxWidth: .infinity)
        .background(isCollected ? KanjiQuestTheme.surface : KanjiQuestTheme.surface.opacity(0.3))
        .cornerRadius(KanjiQuestTheme.radiusM)
        .overlay(
            RoundedRectangle(cornerRadius: KanjiQuestTheme.radiusM)
                .stroke(borderColor ?? .clear, lineWidth: borderColor != nil ? 2 : 0)
        )
        .onTapGesture { if isCollected { onClick() } }
    }
}

/// Inner view that owns the ViewModel with a proper container reference.
private struct HomeViewContent: View {
    @StateObject private var viewModel: HomeViewModel
    @Environment(\.scenePhase) private var scenePhase
    let navigateTo: (NavRoute) -> Void

    init(container: AppContainer, navigateTo: @escaping (NavRoute) -> Void) {
        _viewModel = StateObject(wrappedValue: HomeViewModel(container: container))
        self.navigateTo = navigateTo
    }

    var body: some View {
        HomeViewBody(viewModel: viewModel, navigateTo: navigateTo)
            .onChange(of: scenePhase) { newPhase in
                if newPhase == .active {
                    viewModel.refresh()
                }
            }
    }
}

/// The actual HomeView body extracted to avoid the init problem.
private struct HomeViewBody: View {
    @ObservedObject var viewModel: HomeViewModel
    let navigateTo: (NavRoute) -> Void

    var body: some View {
        let state = viewModel.uiState

        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                profileCard(state: state)

                if !state.isPremium && !state.isAdmin {
                    upgradeBanner
                }

                Spacer().frame(height: 12)
                actionButtonsRow

                if !state.gradeMasteryList.isEmpty {
                    gradeMasterySection(mastery: state.gradeMasteryList)
                }

                Spacer().frame(height: 12)

                if let wotd = state.wordOfTheDay {
                    wordOfTheDayCard(wotd: wotd)
                    Spacer().frame(height: 12)
                }

                learningPathSection(state: state)
                Spacer().frame(height: 12)
                kanaPracticeSection
                Spacer().frame(height: 8)
                radicalModesSection(state: state)
                Spacer().frame(height: 12)
                kanjiStudyModesSection(state: state)
                Spacer().frame(height: 8)
                flashcardCollectionRow(state: state)
                Spacer().frame(height: 16)
                mainTabSelector(state: state)

                if state.selectedMainTab == .kanji {
                    Spacer().frame(height: 4)
                    sortModeTabs(state: state)
                    Spacer().frame(height: 4)
                    filterTabs(state: state)
                }

                sectionTitle(state: state).padding(.top, 4)
                Spacer().frame(height: 8)
                contentGrid(state: state)
                Spacer().frame(height: 16)
            }
            .padding(16)
        }
        .background(KanjiQuestTheme.background)
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                HStack(spacing: 4) {
                    Text("KanjiQuest")
                        .font(KanjiQuestTheme.titleSmall)
                        .foregroundColor(.white)
                    if state.isAdmin {
                        Text(state.effectiveLevel.displayName)
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(adminBadgeColor(level: state.effectiveLevel))
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.white.opacity(0.2))
                            .cornerRadius(4)
                    }
                }
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                HStack(spacing: 4) {
                    Button(action: { navigateTo(.shop) }) {
                        Text("J")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(KanjiQuestTheme.coinGold)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.white.opacity(0.2))
                            .cornerRadius(6)
                    }
                    Button(action: { navigateTo(.settings) }) {
                        Image(systemName: "gearshape.fill")
                            .foregroundColor(.white)
                    }
                }
            }
        }
        .toolbarBackground(KanjiQuestTheme.primary, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
    }

    // All the same section builders as HomeView above, extracted into this body struct.
    // They reference `viewModel` and `navigateTo` from the struct context.

    private func profileCard(state: HomeUiState) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(state.tierNameJp)
                        .font(KanjiQuestTheme.labelMedium)
                        .foregroundColor(KanjiQuestTheme.primary)
                        .fontWeight(.bold)
                    Text("\(state.tierName) - Lv.\(state.displayLevel)")
                        .font(KanjiQuestTheme.titleLarge)
                        .fontWeight(.bold)
                    Text("\((state.profile?.totalXp ?? 0)) XP")
                        .font(KanjiQuestTheme.bodyMedium)
                        .foregroundColor(KanjiQuestTheme.tertiary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("\((state.coinBalance?.displayBalance ?? 0)) J Coins")
                        .font(KanjiQuestTheme.bodyMedium)
                        .fontWeight(.bold)
                        .foregroundColor(KanjiQuestTheme.coinGold)
                        .onTapGesture { navigateTo(.shop) }
                    if (state.coinBalance?.needsSync ?? false) {
                        Text("Pending sync...")
                            .font(KanjiQuestTheme.labelSmall)
                            .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                    }
                    Text("\(state.kanjiCount) kanji loaded")
                        .font(KanjiQuestTheme.bodySmall)
                        .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                }
            }
            ProgressView(value: Double((state.profile?.xpProgress ?? 0)))
                .tint(KanjiQuestTheme.primary)
            if let nextName = state.nextTierName, let nextLevel = state.nextTierLevel {
                Text("Next: \(nextName) at Lv.\(nextLevel)")
                    .font(KanjiQuestTheme.bodySmall)
                    .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
            }
        }
        .padding(16)
        .background(KanjiQuestTheme.surface)
        .cornerRadius(KanjiQuestTheme.radiusM)
    }

    private var upgradeBanner: some View {
        Button(action: { navigateTo(.subscription) }) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Upgrade to Premium").font(KanjiQuestTheme.labelLarge).fontWeight(.bold).foregroundColor(Color(hex: 0xB8860B))
                    Text("Unlock all modes, J Coins & more").font(KanjiQuestTheme.labelSmall).foregroundColor(Color(hex: 0xB8860B).opacity(0.8))
                }
                Spacer()
                Text("$4.99/mo").font(KanjiQuestTheme.titleMedium).fontWeight(.bold).foregroundColor(Color(hex: 0xB8860B))
            }
            .padding(12)
            .background(KanjiQuestTheme.coinGold.opacity(0.15))
            .cornerRadius(KanjiQuestTheme.radiusM)
        }
        .padding(.top, 8)
    }

    private var actionButtonsRow: some View {
        HStack(spacing: 8) {
            Button(action: { navigateTo(.progress) }) {
                Text("Progress").font(.system(size: 14, weight: .bold)).frame(maxWidth: .infinity).frame(height: 48)
            }
            .buttonStyle(.borderedProminent).tint(KanjiQuestTheme.secondary.opacity(0.2)).foregroundColor(KanjiQuestTheme.secondary)

            Button(action: { navigateTo(.achievements) }) {
                Text("Achievements").font(.system(size: 14, weight: .bold)).frame(maxWidth: .infinity).frame(height: 48)
            }
            .buttonStyle(.borderedProminent).tint(KanjiQuestTheme.tertiary.opacity(0.2)).foregroundColor(KanjiQuestTheme.onTertiary)
        }
    }

    private func gradeMasterySection(mastery: [GradeMastery]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Spacer().frame(height: 12)
            Text("Grade Mastery").font(KanjiQuestTheme.titleMedium).fontWeight(.bold)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(mastery, id: \.grade) { m in GradeMasteryBadgeView(mastery: m) }
                }
            }
        }
    }

    private func wordOfTheDayCard(wotd: Vocabulary) -> some View {
        Button(action: { navigateTo(.wordDetail(wordId: wotd.id)) }) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Word of the Day").font(KanjiQuestTheme.labelMedium)
                    Text(wotd.reading).font(KanjiQuestTheme.bodySmall)
                    Text(wotd.primaryMeaning).font(KanjiQuestTheme.bodyMedium)
                }
                Spacer()
                KanjiText(text: wotd.kanjiForm, font: .system(size: 40, weight: .bold))
            }
            .padding(16)
            .background(KanjiQuestTheme.tertiary.opacity(0.2))
            .cornerRadius(KanjiQuestTheme.radiusM)
        }
        .buttonStyle(.plain)
    }

    private func learningPathSection(state: HomeUiState) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Learning Path").font(KanjiQuestTheme.titleMedium).fontWeight(.bold)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    LearningPathCardView(title: "Hiragana", subtitle: "ひらがな", progress: state.hiraganaProgress, color: Color(hex: 0xE91E63))
                    LearningPathCardView(title: "Katakana", subtitle: "カタカナ", progress: state.katakanaProgress, color: Color(hex: 0x00BCD4))
                    LearningPathCardView(title: "Radicals", subtitle: "部首", progress: state.radicalProgress, color: Color(hex: 0x795548))
                    ForEach(state.gradeMasteryList, id: \.grade) { m in
                        LearningPathCardView(title: "Grade \(m.grade)", subtitle: "漢字", progress: m.masteryScore, color: KanjiQuestTheme.primary)
                    }
                }
            }
        }
    }

    private var kanaPracticeSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Kana Practice").font(KanjiQuestTheme.titleMedium).fontWeight(.bold)
            HStack(spacing: 12) {
                GameModeButtonView(label: "Hiragana", subtitle: "Recognition", modeColor: Color(hex: 0xE91E63), imageAsset: "mode-kana-recognition.png", action: { navigateTo(.kanaRecognition(kanaType: "HIRAGANA")) })
                GameModeButtonView(label: "Katakana", subtitle: "Recognition", modeColor: Color(hex: 0x00BCD4), imageAsset: "mode-kana-writing.png", action: { navigateTo(.kanaRecognition(kanaType: "KATAKANA")) })
            }
        }
    }

    private func radicalModesSection(state: HomeUiState) -> some View {
        HStack(spacing: 12) {
            GameModeButtonView(label: "Radicals", subtitle: "Free", modeColor: Color(hex: 0x795548), imageAsset: "mode-radical-recognition.png", action: { navigateTo(.radicalRecognition) })
            PreviewableGameModeButtonView(label: "Radical Builder", isPremium: state.isPremium, trialInfo: state.previewTrials["RADICAL_BUILDER"], modeColor: Color(hex: 0x795548), imageAsset: "mode-radical-builder.png", onPremiumClick: { navigateTo(.radicalBuilder) }, onPreviewClick: { if viewModel.usePreviewTrial(mode: "RADICAL_BUILDER") { navigateTo(.radicalBuilder) } }, onUpgradeClick: { navigateTo(.subscription) })
        }
    }

    private func kanjiStudyModesSection(state: HomeUiState) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Kanji Study Modes").font(KanjiQuestTheme.titleMedium).fontWeight(.bold)
            HStack(spacing: 12) {
                GameModeButtonView(label: "Recognition", subtitle: "Free", modeColor: Color(hex: 0x2196F3), imageAsset: "mode-recognition.png", action: { navigateTo(.recognition) })
                PreviewableGameModeButtonView(label: "Writing", isPremium: state.isPremium, trialInfo: state.previewTrials["WRITING"], modeColor: Color(hex: 0x4CAF50), imageAsset: "mode-writing.png", onPremiumClick: { navigateTo(.writing) }, onPreviewClick: { if viewModel.usePreviewTrial(mode: "WRITING") { navigateTo(.writing) } }, onUpgradeClick: { navigateTo(.subscription) })
            }
            HStack(spacing: 12) {
                PreviewableGameModeButtonView(label: "Vocabulary", isPremium: state.isPremium, trialInfo: state.previewTrials["VOCABULARY"], modeColor: Color(hex: 0xFF9800), imageAsset: "mode-vocabulary.png", onPremiumClick: { navigateTo(.vocabulary) }, onPreviewClick: { if viewModel.usePreviewTrial(mode: "VOCABULARY") { navigateTo(.vocabulary) } }, onUpgradeClick: { navigateTo(.subscription) })
                PreviewableGameModeButtonView(label: "Camera", isPremium: state.isPremium, trialInfo: state.previewTrials["CAMERA_CHALLENGE"], modeColor: Color(hex: 0x9C27B0), imageAsset: "mode-camera.png", onPremiumClick: { navigateTo(.camera) }, onPreviewClick: { if viewModel.usePreviewTrial(mode: "CAMERA_CHALLENGE") { navigateTo(.camera) } }, onUpgradeClick: { navigateTo(.subscription) })
            }
        }
    }

    private func flashcardCollectionRow(state: HomeUiState) -> some View {
        HStack(spacing: 8) {
            Button(action: { navigateTo(.flashcards) }) {
                Text(state.flashcardDeckCount > 0 ? "Flashcards (\(state.flashcardDeckCount))" : "Flashcards").font(.system(size: 14, weight: .bold)).frame(maxWidth: .infinity).frame(height: 48)
            }.buttonStyle(.borderedProminent).tint(KanjiQuestTheme.tertiary.opacity(0.2)).foregroundColor(KanjiQuestTheme.onTertiary)

            Button(action: { navigateTo(.collection) }) {
                Text("Collection \(state.collectedKanjiCount)/\(state.totalKanjiInGrades)").font(.system(size: 14, weight: .bold)).foregroundColor(.white).frame(maxWidth: .infinity).frame(height: 48)
            }.buttonStyle(.borderedProminent).tint(Color(hex: 0x9C27B0).opacity(0.8))
        }
    }

    private func mainTabSelector(state: HomeUiState) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 4) {
                ForEach(MainTab.allCases, id: \.self) { tab in
                    let isSelected = tab == state.selectedMainTab
                    Text(tab.rawValue).font(.system(size: 12, weight: isSelected ? .bold : .regular))
                        .foregroundColor(isSelected ? .white : KanjiQuestTheme.primary)
                        .padding(.horizontal, 10).padding(.vertical, 5)
                        .background(isSelected ? KanjiQuestTheme.primary : KanjiQuestTheme.surfaceVariant)
                        .cornerRadius(6)
                        .onTapGesture { viewModel.selectMainTab(tab) }
                }
            }
        }
    }

    private func sortModeTabs(state: HomeUiState) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 4) {
                ForEach(KanjiSortMode.allCases, id: \.self) { mode in
                    let isSelected = mode == state.kanjiSortMode
                    Text(mode.rawValue).font(.system(size: 11, weight: isSelected ? .bold : .regular))
                        .foregroundColor(isSelected ? .white : KanjiQuestTheme.onSurfaceVariant)
                        .padding(.horizontal, 8).padding(.vertical, 4)
                        .background(isSelected ? KanjiQuestTheme.tertiary : KanjiQuestTheme.surfaceVariant)
                        .cornerRadius(6)
                        .onTapGesture { viewModel.selectSortMode(mode) }
                }
            }
        }
    }

    private func filterTabs(state: HomeUiState) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 4) {
                switch state.kanjiSortMode {
                case .schoolGrade:
                    ForEach(state.allGrades, id: \.self) { grade in
                        let isSelected = grade == state.selectedGrade
                        let hasCollection = state.gradesWithCollection.contains(grade)
                        let collected = state.perGradeCollectedCounts[grade] ?? 0
                        let total = state.perGradeTotalCounts[grade] ?? 0
                        let gradeLabel = grade == 8 ? "G8+" : "G\(grade)"
                        let labelText = total > 0 ? "\(gradeLabel)\n\(collected)/\(total)" : gradeLabel
                        Text(labelText).font(.system(size: 11, weight: isSelected ? .bold : .regular)).multilineTextAlignment(.center)
                            .foregroundColor(isSelected ? .white : (hasCollection ? KanjiQuestTheme.primary : KanjiQuestTheme.onSurfaceVariant.opacity(0.38)))
                            .padding(.horizontal, 8).padding(.vertical, 4)
                            .background(isSelected ? KanjiQuestTheme.primary : (hasCollection ? KanjiQuestTheme.surfaceVariant : KanjiQuestTheme.surfaceVariant.opacity(0.5)))
                            .cornerRadius(6)
                            .onTapGesture { if hasCollection { viewModel.selectGrade(grade) } }
                    }
                case .jlptLevel:
                    ForEach([5, 4, 3, 2, 1] as [Int32], id: \.self) { level in
                        let isSelected = level == state.selectedJlptLevel
                        let collected = state.perJlptCollectedCounts[level] ?? 0
                        let total = state.perJlptTotalCounts[level] ?? 0
                        let labelText = total > 0 ? "N\(level)\n\(collected)/\(total)" : "N\(level)"
                        Text(labelText).font(.system(size: 11, weight: isSelected ? .bold : .regular)).multilineTextAlignment(.center)
                            .foregroundColor(isSelected ? .white : KanjiQuestTheme.primary)
                            .padding(.horizontal, 8).padding(.vertical, 4)
                            .background(isSelected ? KanjiQuestTheme.primary : KanjiQuestTheme.surfaceVariant)
                            .cornerRadius(6)
                            .onTapGesture { viewModel.selectJlptLevel(level) }
                    }
                case .strokes:
                    ForEach(state.availableStrokeCounts, id: \.self) { count in
                        let isSelected = count == state.selectedStrokeCount
                        Text("\(count)画").font(.system(size: 12, weight: isSelected ? .bold : .regular))
                            .foregroundColor(isSelected ? .white : KanjiQuestTheme.primary)
                            .padding(.horizontal, 8).padding(.vertical, 4)
                            .background(isSelected ? KanjiQuestTheme.primary : KanjiQuestTheme.surfaceVariant)
                            .cornerRadius(6)
                            .onTapGesture { viewModel.selectStrokeCount(count) }
                    }
                case .frequency:
                    ForEach(Array(HomeViewModel.frequencyLabels.enumerated()), id: \.offset) { index, label in
                        let isSelected = index == state.selectedFrequencyRange
                        Text(label).font(.system(size: 12, weight: isSelected ? .bold : .regular))
                            .foregroundColor(isSelected ? .white : KanjiQuestTheme.primary)
                            .padding(.horizontal, 8).padding(.vertical, 4)
                            .background(isSelected ? KanjiQuestTheme.primary : KanjiQuestTheme.surfaceVariant)
                            .cornerRadius(6)
                            .onTapGesture { viewModel.selectFrequencyRange(index) }
                    }
                }
            }
        }
    }

    private func sectionTitle(state: HomeUiState) -> some View {
        let title: String
        switch state.selectedMainTab {
        case .hiragana: title = "ひらがな Hiragana"
        case .katakana: title = "カタカナ Katakana"
        case .radicals: title = "部首 Radicals"
        case .kanji:
            switch state.kanjiSortMode {
            case .schoolGrade: title = "Grade \(state.selectedGrade) Kanji"
            case .jlptLevel: title = "JLPT N\(state.selectedJlptLevel) Kanji"
            case .strokes: title = "\(state.selectedStrokeCount)-Stroke Kanji"
            case .frequency: title = "\(HomeViewModel.frequencyLabels[state.selectedFrequencyRange]) Kanji"
            }
        }
        return Text(title).font(KanjiQuestTheme.titleMedium).fontWeight(.bold)
    }

    @ViewBuilder
    private func contentGrid(state: HomeUiState) -> some View {
        let columns5 = Array(repeating: GridItem(.flexible(), spacing: 8), count: 5)
        let columns4 = Array(repeating: GridItem(.flexible(), spacing: 8), count: 4)

        switch state.selectedMainTab {
        case .hiragana:
            LazyVGrid(columns: columns5, spacing: 8) {
                ForEach(state.hiraganaList, id: \.id) { kana in
                    KanaGridItemView(kana: kana, collectedItem: state.collectedHiraganaItems[kana.id], onClick: { navigateTo(.kanaRecognition(kanaType: "HIRAGANA")) })
                }
            }
        case .katakana:
            LazyVGrid(columns: columns5, spacing: 8) {
                ForEach(state.katakanaList, id: \.id) { kana in
                    KanaGridItemView(kana: kana, collectedItem: state.collectedKatakanaItems[kana.id], onClick: { navigateTo(.kanaRecognition(kanaType: "KATAKANA")) })
                }
            }
        case .radicals:
            LazyVGrid(columns: columns4, spacing: 8) {
                ForEach(state.radicals, id: \.id) { radical in
                    RadicalGridItemView(radical: radical, collectedItem: state.collectedRadicalItems[radical.id], onClick: { navigateTo(.radicalDetail(radicalId: radical.id)) })
                }
            }
        case .kanji:
            LazyVGrid(columns: columns5, spacing: 8) {
                ForEach(state.gradeOneKanji, id: \.id) { kanji in
                    KanjiGridItemView(kanji: kanji, practiceCount: state.kanjiPracticeCounts[kanji.id] ?? 0, modeStats: state.kanjiModeStats[kanji.id] ?? [:], collectedItem: state.collectedItems[kanji.id], onClick: { navigateTo(.kanjiDetail(kanjiId: kanji.id)) })
                }
            }
        }
    }

    private func adminBadgeColor(level: UserLevel) -> Color {
        switch level {
        case .admin: return Color(hex: 0xFF6B6B)
        case .premium: return KanjiQuestTheme.coinGold
        case .free: return Color.white.opacity(0.7)
        default: return Color.white.opacity(0.7)
        }
    }
}
