import Foundation
import SharedCore

/// Progress & Stats state. Mirrors Android's ProgressViewModel.kt.
@MainActor
class ProgressViewModel: ObservableObject {
    @Published var profile: UserProfile?
    @Published var masteredCount: Int64 = 0
    @Published var totalKanjiInSrs: Int64 = 0
    @Published var recentSessions: [StudySession] = []
    @Published var totalGamesPlayed = 0
    @Published var totalCardsStudied = 0
    @Published var overallAccuracy: Float = 0
    @Published var gradeMasteryList: [GradeMastery] = []
    @Published var isLoading = true

    private var container: AppContainer?

    func load(container: AppContainer) {
        self.container = container
        loadStats()
    }

    func refresh(container: AppContainer) {
        self.container = container
        isLoading = true
        loadStats()
    }

    // MARK: - Computed Convenience Properties

    var level: Int { Int(profile?.level ?? 1) }
    var totalXp: Int64 { Int64(profile?.totalXp ?? 0) }
    var currentStreak: Int { Int(profile?.currentStreak ?? 0) }
    var longestStreak: Int { Int(profile?.longestStreak ?? 0) }
    var dailyGoal: Int {
        let stored = UserDefaults.standard.integer(forKey: "daily_goal_xp")
        return stored > 0 ? stored : 50
    }

    var xpForNextLevel: Int64 {
        let next = Int32(level + 1)
        return Int64(next) * Int64(next) * 50
    }

    var xpInCurrentLevel: Int64 {
        let currentLevelXp = Int64(level) * Int64(level) * 50
        return totalXp - currentLevelXp
    }

    var xpProgress: Float {
        let range = xpForNextLevel - Int64(level) * Int64(level) * 50
        guard range > 0 else { return 0 }
        return Float(xpInCurrentLevel) / Float(range)
    }

    // MARK: - Data Loading

    private func loadStats() {
        guard let container else { return }
        Task {
            do {
                let profile = try await container.userRepository.getProfile()
                let masteredRaw = try await container.srsRepository.getMasteredCount()
                let newCountRaw = try await container.srsRepository.getNewCount()
                let dueCountRaw = try await container.srsRepository.getDueCount(currentTime: Int64(Date().timeIntervalSince1970 * 1000))
                let mastered = (masteredRaw as? NSNumber)?.int64Value ?? 0
                let newCount = (newCountRaw as? NSNumber)?.int64Value ?? 0
                let dueCount = (dueCountRaw as? NSNumber)?.int64Value ?? 0
                let total = mastered + newCount + dueCount
                let sessions = try await container.sessionRepository.getRecentSessions(limit: 10)

                let gamesPlayed = sessions.count
                let cardsStudied = sessions.reduce(0) { $0 + Int($1.cardsStudied) }
                let totalCorrect = sessions.reduce(0) { $0 + Int($1.correctCount) }
                let accuracy: Float = cardsStudied > 0 ? Float(totalCorrect) / Float(cardsStudied) * 100 : 0

                // Compute per-grade mastery
                let playerLevel = container.userSessionProvider.getAdminPlayerLevelOverride()?.int32Value ?? profile.level
                let tier = LevelProgression.shared.getTierForLevel(level: playerLevel)
                let unlockedGrades: [Int32] = (tier.unlockedGrades as? [NSNumber])?.map { $0.int32Value } ?? [1]
                var masteries: [GradeMastery] = []
                for grade in unlockedGrades {
                    let totalInGradeRaw = try await container.kanjiRepository.getKanjiCountByGrade(grade: grade)
                    let totalInGrade = (totalInGradeRaw as? NSNumber)?.int64Value ?? 0
                    let mastery = try await container.srsRepository.getGradeMastery(grade: grade, totalKanjiInGrade: totalInGrade)
                    masteries.append(mastery)
                }

                self.profile = profile
                self.masteredCount = mastered
                self.totalKanjiInSrs = total
                self.recentSessions = sessions
                self.totalGamesPlayed = gamesPlayed
                self.totalCardsStudied = cardsStudied
                self.overallAccuracy = accuracy
                self.gradeMasteryList = masteries
                self.isLoading = false
            } catch {
                self.isLoading = false
            }
        }
    }
}
