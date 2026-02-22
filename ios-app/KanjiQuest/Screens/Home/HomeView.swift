import SwiftUI
import SharedCore

struct HomeView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = HomeViewModel()
    #if IPAD_TARGET
    @State private var showCalligraphyError = false
    #endif
    let navigateTo: (Route) -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: KanjiQuestTheme.spacingL) {
                // Header
                VStack(spacing: KanjiQuestTheme.spacingS) {
                    Text("KanjiQuest")
                        .font(KanjiQuestTheme.titleLarge)
                        .foregroundColor(KanjiQuestTheme.primary)

                    #if IPAD_TARGET
                    Text("書道 Calligraphy")
                        .font(KanjiQuestTheme.bodyLarge)
                        .foregroundColor(KanjiQuestTheme.secondary)
                    #else
                    Text("漢字 Recognition")
                        .font(KanjiQuestTheme.bodyLarge)
                        .foregroundColor(KanjiQuestTheme.secondary)
                    #endif
                }
                .padding(.top, KanjiQuestTheme.spacingXL)

                // Level / XP card
                if let profile = viewModel.userProfile {
                    LevelCard(profile: profile)
                }

                // Game Mode Buttons
                VStack(spacing: KanjiQuestTheme.spacingM) {
                    #if IPAD_TARGET
                    calligraphyButton
                    recognitionButton
                    #else
                    recognitionButton
                    #endif
                }

                // Navigation links
                VStack(spacing: KanjiQuestTheme.spacingS) {
                    NavigationButton(title: "Progress", icon: "chart.bar.fill") {
                        navigateTo(.progress)
                    }
                    NavigationButton(title: "Settings", icon: "gearshape.fill") {
                        navigateTo(.settings)
                    }
                    NavigationButton(title: "Subscription", icon: "star.fill") {
                        navigateTo(.subscription)
                    }
                }
            }
            .padding(.horizontal)
        }
        .background(KanjiQuestTheme.background)
        #if IPAD_TARGET
        .alert("Calligraphy Unavailable", isPresented: $showCalligraphyError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(viewModel.kanjiLoadError ?? "Kanji data could not be loaded. Please restart the app.")
        }
        #endif
        .task {
            await viewModel.load(container: container)
        }
    }

    // MARK: - Game Mode Buttons

    private var recognitionButton: some View {
        Button {
            navigateTo(.recognition())
        } label: {
            HStack {
                Image(systemName: "eye.fill")
                    .font(.title2)
                VStack(alignment: .leading) {
                    Text("Recognition Mode")
                        .font(KanjiQuestTheme.labelLarge)
                    Text("認識モード")
                        .font(KanjiQuestTheme.labelSmall)
                        .foregroundColor(.white.opacity(0.8))
                }
                Spacer()
                Image(systemName: "chevron.right")
            }
            .padding()
            #if IPAD_TARGET
            .background(KanjiQuestTheme.secondary)
            #else
            .background(KanjiQuestTheme.primary)
            #endif
            .foregroundColor(.white)
            .cornerRadius(KanjiQuestTheme.radiusM)
        }
    }

    #if IPAD_TARGET
    private var calligraphyButton: some View {
        Button {
            if let kanji = viewModel.nextKanji {
                navigateTo(.calligraphySession(
                    kanjiLiteral: kanji.literal,
                    strokePaths: kanji.strokePaths
                ))
            } else if !viewModel.isLoadingKanji {
                showCalligraphyError = true
            }
        } label: {
            HStack {
                Image(systemName: "pencil.tip")
                    .font(.title2)
                VStack(alignment: .leading) {
                    Text("Calligraphy Practice")
                        .font(KanjiQuestTheme.labelLarge)
                    Text("書道モード")
                        .font(KanjiQuestTheme.labelSmall)
                        .foregroundColor(.white.opacity(0.8))
                }
                Spacer()
                if viewModel.isLoadingKanji {
                    SwiftUI.ProgressView()
                        .progressViewStyle(.circular)
                        .tint(.white)
                } else {
                    Image(systemName: "chevron.right")
                }
            }
            .padding()
            .background(viewModel.isLoadingKanji
                ? KanjiQuestTheme.primary.opacity(0.6)
                : KanjiQuestTheme.primary)
            .foregroundColor(.white)
            .cornerRadius(KanjiQuestTheme.radiusM)
        }
        .disabled(viewModel.isLoadingKanji)
    }
    #endif
}

private struct LevelCard: View {
    let profile: HomeViewModel.UserProfileData

    var body: some View {
        VStack(spacing: KanjiQuestTheme.spacingS) {
            HStack {
                Text("Level \(profile.level)")
                    .font(KanjiQuestTheme.titleMedium)
                Spacer()
                Text("\(profile.xp) XP")
                    .font(KanjiQuestTheme.labelLarge)
                    .foregroundColor(KanjiQuestTheme.xpGold)
            }

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(KanjiQuestTheme.surfaceVariant)
                        .frame(height: 8)

                    RoundedRectangle(cornerRadius: 4)
                        .fill(KanjiQuestTheme.xpGold)
                        .frame(width: geo.size.width * profile.xpProgress, height: 8)
                }
            }
            .frame(height: 8)

            Text("\(profile.xpToNext) XP to next level")
                .font(KanjiQuestTheme.labelSmall)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(KanjiQuestTheme.surface)
        .cornerRadius(KanjiQuestTheme.radiusM)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
}

private struct NavigationButton: View {
    let title: String
    let icon: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(KanjiQuestTheme.primary)
                Text(title)
                    .font(KanjiQuestTheme.bodyLarge)
                    .foregroundColor(KanjiQuestTheme.onSurface)
                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundColor(.secondary)
            }
            .padding()
            .background(KanjiQuestTheme.surface)
            .cornerRadius(KanjiQuestTheme.radiusM)
        }
    }
}
