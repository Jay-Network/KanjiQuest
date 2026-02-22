import Foundation
import SharedCore

enum DifficultyLevel: String, CaseIterable {
    case easy = "Easy"
    case medium = "Medium"
    case hard = "Hard"
}

enum AppTheme: String, CaseIterable {
    case light = "Light"
    case dark = "Dark"
    case system = "System Default"
}

/// Settings view model. Mirrors Android's SettingsViewModel.kt.
@MainActor
class SettingsViewModel: ObservableObject {
    @Published var soundEnabled = true
    @Published var musicEnabled = true
    @Published var vibrationsEnabled = true
    @Published var notificationsEnabled = true
    @Published var dailyGoal = 20
    @Published var sessionLength = 10
    @Published var difficulty: DifficultyLevel = .medium
    @Published var autoPlayAudio = true
    @Published var showHints = true
    @Published var theme: AppTheme = .system
    @Published var isAdmin = false
    @Published var isDeveloper = false
    @Published var currentPlayerLevel = 1
    @Published var adminPlayerLevelOverride: Int?

    private let defaults = UserDefaults.standard

    func load(container: AppContainer) {
        soundEnabled = defaults.object(forKey: "sound_enabled") as? Bool ?? true
        musicEnabled = defaults.object(forKey: "music_enabled") as? Bool ?? true
        vibrationsEnabled = defaults.object(forKey: "vibrations_enabled") as? Bool ?? true
        notificationsEnabled = defaults.object(forKey: "notifications_enabled") as? Bool ?? true
        dailyGoal = defaults.integer(forKey: "daily_goal").nonZero ?? 20
        sessionLength = defaults.integer(forKey: "session_length").nonZero ?? 10
        autoPlayAudio = defaults.object(forKey: "auto_play_audio") as? Bool ?? true
        showHints = defaults.object(forKey: "show_hints") as? Bool ?? true

        if let diffStr = defaults.string(forKey: "difficulty"),
           let diff = DifficultyLevel(rawValue: diffStr) {
            difficulty = diff
        }
        if let themeStr = defaults.string(forKey: "theme"),
           let t = AppTheme(rawValue: themeStr) {
            theme = t
        }

        Task {
            let profile = try? await container.userRepository.getProfile()
            currentPlayerLevel = Int(profile?.level ?? 1)
        }
    }

    func toggleSound() {
        soundEnabled.toggle()
        defaults.set(soundEnabled, forKey: "sound_enabled")
    }

    func toggleMusic() {
        musicEnabled.toggle()
        defaults.set(musicEnabled, forKey: "music_enabled")
    }

    func toggleVibrations() {
        vibrationsEnabled.toggle()
        defaults.set(vibrationsEnabled, forKey: "vibrations_enabled")
    }

    func toggleNotifications() {
        notificationsEnabled.toggle()
        defaults.set(notificationsEnabled, forKey: "notifications_enabled")
    }

    func toggleAutoPlayAudio() {
        autoPlayAudio.toggle()
        defaults.set(autoPlayAudio, forKey: "auto_play_audio")
    }

    func toggleShowHints() {
        showHints.toggle()
        defaults.set(showHints, forKey: "show_hints")
    }

    func setDailyGoal(_ goal: Int) {
        dailyGoal = goal
        defaults.set(goal, forKey: "daily_goal")
    }

    func setSessionLength(_ length: Int) {
        sessionLength = length
        defaults.set(length, forKey: "session_length")
    }

    func setDifficulty(_ diff: DifficultyLevel) {
        difficulty = diff
        defaults.set(diff.rawValue, forKey: "difficulty")
    }

    func setTheme(_ t: AppTheme) {
        theme = t
        defaults.set(t.rawValue, forKey: "theme")
    }

    func resetToDefaults() {
        let keys = ["sound_enabled", "music_enabled", "vibrations_enabled", "notifications_enabled",
                    "daily_goal", "session_length", "difficulty", "auto_play_audio", "show_hints", "theme"]
        keys.forEach { defaults.removeObject(forKey: $0) }
        soundEnabled = true; musicEnabled = true; vibrationsEnabled = true; notificationsEnabled = true
        dailyGoal = 20; sessionLength = 10; difficulty = .medium; autoPlayAudio = true
        showHints = true; theme = .system
    }
}

private extension Int {
    var nonZero: Int? { self == 0 ? nil : self }
}
