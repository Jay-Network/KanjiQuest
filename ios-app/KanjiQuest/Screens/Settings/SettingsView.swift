import SwiftUI

/// Full settings screen. Mirrors Android's SettingsScreen.kt.
/// Audio, Gameplay (session length, difficulty, daily goal, hints, retake assessment),
/// Notifications, Appearance, Developer (dev chat), Admin (level override).
struct SettingsView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = SettingsViewModel()
    var onBack: () -> Void = {}
    var onDevChat: () -> Void = {}
    var onRetakeAssessment: () -> Void = {}

    @State private var showDailyGoalDialog = false
    @State private var showDifficultyDialog = false
    @State private var showThemeDialog = false
    @State private var showResetDialog = false

    var body: some View {
        ZStack {
            KanjiQuestTheme.background.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 16) {
                    // Audio
                    settingsSection(title: "Audio", icon: "speaker.wave.3") {
                        switchSetting("Sound Effects", "Button clicks, correct/incorrect feedback", viewModel.soundEnabled) { viewModel.toggleSound() }
                        switchSetting("Background Music", "Ambient music during gameplay", viewModel.musicEnabled) { viewModel.toggleMusic() }
                        switchSetting("Auto-Play Audio", "Automatically play kanji pronunciation", viewModel.autoPlayAudio) { viewModel.toggleAutoPlayAudio() }
                    }

                    // Gameplay
                    settingsSection(title: "Gameplay", icon: "gamecontroller") {
                        // Session Length
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Session Length").font(KanjiQuestTheme.bodyLarge)
                            Text("Cards per session").font(KanjiQuestTheme.bodySmall).foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                            HStack(spacing: 4) {
                                ForEach([10, 15, 20, 30], id: \.self) { length in
                                    let isSelected = viewModel.sessionLength == length
                                    Text("\(length)")
                                        .font(KanjiQuestTheme.bodyLarge)
                                        .fontWeight(isSelected ? .bold : .regular)
                                        .foregroundColor(isSelected ? .white : KanjiQuestTheme.onSurfaceVariant)
                                        .frame(maxWidth: .infinity).padding(.vertical, 10)
                                        .background(isSelected ? KanjiQuestTheme.primary : Color.clear)
                                        .cornerRadius(6)
                                        .onTapGesture { viewModel.setSessionLength(length) }
                                }
                            }
                            .padding(4)
                            .background(KanjiQuestTheme.surfaceVariant)
                            .cornerRadius(8)
                        }
                        .padding(.vertical, 8)

                        Divider()

                        clickableSetting("Difficulty Level", viewModel.difficulty.rawValue) { showDifficultyDialog = true }
                        clickableSetting("Daily XP Goal", "\(viewModel.dailyGoal) XP per day") { showDailyGoalDialog = true }
                        switchSetting("Show Hints", "Display helpful hints during games", viewModel.showHints) { viewModel.toggleShowHints() }

                        Divider().padding(.vertical, 8)

                        clickableSetting("Retake Assessment", "Re-test your kanji proficiency level") { onRetakeAssessment() }
                    }

                    // Notifications
                    settingsSection(title: "Notifications", icon: "bell") {
                        switchSetting("Study Reminders", "Daily reminders to practice", viewModel.notificationsEnabled) { viewModel.toggleNotifications() }
                        switchSetting("Vibrations", "Haptic feedback for interactions", viewModel.vibrationsEnabled) { viewModel.toggleVibrations() }
                    }

                    // Appearance
                    settingsSection(title: "Appearance", icon: "paintbrush") {
                        clickableSetting("Theme", viewModel.theme.rawValue) { showThemeDialog = true }
                    }

                    // Developer
                    if viewModel.isDeveloper {
                        settingsSection(title: "Developer", icon: "wrench.and.screwdriver") {
                            clickableSetting("Dev Chat", "Chat with the KanjiQuest dev agent") { onDevChat() }
                        }
                    }

                    // About
                    settingsSection(title: "About", icon: "info.circle") {
                        HStack {
                            Text("Version").font(KanjiQuestTheme.bodyLarge)
                            Spacer()
                            Text("1.0.0 (\(KanjiQuestTheme.isPhone ? "iPhone" : "iPad"))").font(KanjiQuestTheme.bodyMedium).foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                        }
                        .padding(.vertical, 4)

                        Button(action: {
                            Task { try? await container.authRepository.signOut() }
                        }) {
                            Text("Sign Out").foregroundColor(KanjiQuestTheme.error).fontWeight(.bold)
                                .frame(maxWidth: .infinity).padding(.vertical, 12)
                        }
                    }

                    Spacer().frame(height: 8)

                    // Reset
                    Button(action: { showResetDialog = true }) {
                        Text("Reset to Defaults")
                            .foregroundColor(KanjiQuestTheme.error).fontWeight(.bold)
                            .frame(maxWidth: .infinity)
                    }

                    Spacer().frame(height: 16)
                }
                .padding(16)
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onBack) { Image(systemName: "chevron.left"); Text("Back") }.foregroundColor(.white)
            }
            ToolbarItem(placement: .principal) { Text("Settings").font(.headline).foregroundColor(.white) }
        }
        .toolbarBackground(KanjiQuestTheme.primary, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .task { viewModel.loadSettings(container: container) }
        .alert("Set Daily XP Goal", isPresented: $showDailyGoalDialog) {
            ForEach([10, 20, 30, 50, 75, 100], id: \.self) { goal in
                Button("\(goal) XP") { viewModel.setDailyGoal(goal) }
            }
            Button("Cancel", role: .cancel) {}
        }
        .confirmationDialog("Select Difficulty", isPresented: $showDifficultyDialog) {
            ForEach(DifficultyLevel.allCases, id: \.self) { diff in
                Button(diff.rawValue) { viewModel.setDifficulty(diff) }
            }
            Button("Cancel", role: .cancel) {}
        }
        .confirmationDialog("Select Theme", isPresented: $showThemeDialog) {
            ForEach(AppTheme.allCases, id: \.self) { theme in
                Button(theme.rawValue) { viewModel.setTheme(theme) }
            }
            Button("Cancel", role: .cancel) {}
        }
        .alert("Reset Settings?", isPresented: $showResetDialog) {
            Button("Reset", role: .destructive) { viewModel.resetToDefaults() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This will reset all settings to their default values.")
        }
    }

    // MARK: - Components

    private func settingsSection(title: String, icon: String, @ViewBuilder content: () -> some View) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 12) {
                Image(systemName: icon).foregroundColor(KanjiQuestTheme.primary)
                Text(title).font(KanjiQuestTheme.titleMedium).fontWeight(.bold)
            }
            .padding(.bottom, 12)

            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(KanjiQuestTheme.surface)
        .cornerRadius(KanjiQuestTheme.radiusM)
    }

    private func switchSetting(_ title: String, _ subtitle: String, _ isOn: Bool, action: @escaping () -> Void) -> some View {
        HStack {
            VStack(alignment: .leading) {
                Text(title).font(KanjiQuestTheme.bodyLarge)
                Text(subtitle).font(KanjiQuestTheme.bodySmall).foregroundColor(KanjiQuestTheme.onSurfaceVariant)
            }
            Spacer()
            Toggle("", isOn: Binding(get: { isOn }, set: { _ in action() })).labelsHidden()
        }
        .padding(.vertical, 8)
    }

    private func clickableSetting(_ title: String, _ subtitle: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(KanjiQuestTheme.bodyLarge).foregroundColor(KanjiQuestTheme.onSurface)
                Text(subtitle).font(KanjiQuestTheme.bodyMedium).foregroundColor(KanjiQuestTheme.primary).fontWeight(.bold)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.vertical, 12)
        }
    }
}
