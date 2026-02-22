import Foundation
import SharedCore

struct AchievementDefinition: Identifiable {
    let id: String
    let title: String
    let description: String
    let icon: String
    let category: String
    let target: Int
    var progress: Int = 0
    var unlockedAt: Int64?

    var isUnlocked: Bool { unlockedAt != nil }
    var progressPercent: Float { target > 0 ? Float(progress) / Float(target) * 100 : 0 }
}

struct AchievementCategory: Identifiable {
    let id: String
    let name: String
    let achievements: [AchievementDefinition]
}

/// Achievements view model. Mirrors Android's AchievementsViewModel.kt.
@MainActor
class AchievementsViewModel: ObservableObject {
    @Published var categories: [AchievementCategory] = []
    @Published var totalAchievements = 0
    @Published var unlockedCount = 0
    @Published var isLoading = true

    func load(container: AppContainer) {
        Task {
            do {
                let dbAchievements = try await container.achievementRepository.getAllAchievements()
                var definitions = Self.predefinedAchievements
                for i in definitions.indices {
                    if let db = dbAchievements.first(where: { ($0 as AnyObject).id as? String == definitions[i].id }) {
                        definitions[i].progress = Int((db as AnyObject).progress as? Int32 ?? 0)
                        definitions[i].unlockedAt = (db as AnyObject).unlockedAt as? Int64
                    }
                }

                let grouped = Dictionary(grouping: definitions, by: \.category)
                categories = grouped.map { name, achs in
                    AchievementCategory(id: name, name: name, achievements: achs.sorted { $0.id < $1.id })
                }.sorted { $0.name < $1.name }

                totalAchievements = definitions.count
                unlockedCount = definitions.filter(\.isUnlocked).count
                isLoading = false
            } catch {
                isLoading = false
            }
        }
    }

    func refresh(container: AppContainer) {
        isLoading = true
        load(container: container)
    }

    static let predefinedAchievements: [AchievementDefinition] = [
        // Progress
        .init(id: "xp_100", title: "Getting Started", description: "Earn 100 total XP", icon: "\u{2B50}", category: "Progress", target: 100),
        .init(id: "xp_500", title: "Dedicated Learner", description: "Earn 500 total XP", icon: "\u{1F31F}", category: "Progress", target: 500),
        .init(id: "xp_1000", title: "XP Master", description: "Earn 1,000 total XP", icon: "\u{2728}", category: "Progress", target: 1000),
        .init(id: "xp_5000", title: "XP Legend", description: "Earn 5,000 total XP", icon: "\u{1F4AB}", category: "Progress", target: 5000),
        .init(id: "level_5", title: "Level 5", description: "Reach level 5", icon: "5\u{FE0F}\u{20E3}", category: "Progress", target: 5),
        .init(id: "level_10", title: "Level 10", description: "Reach level 10", icon: "\u{1F51F}", category: "Progress", target: 10),
        .init(id: "level_20", title: "Level 20", description: "Reach level 20", icon: "\u{1F4AA}", category: "Progress", target: 20),
        // Mastery
        .init(id: "kanji_10", title: "First Ten", description: "Master 10 kanji", icon: "\u{6F22}", category: "Mastery", target: 10),
        .init(id: "kanji_50", title: "Half Century", description: "Master 50 kanji", icon: "\u{1F4DA}", category: "Mastery", target: 50),
        .init(id: "kanji_100", title: "Centurion", description: "Master 100 kanji", icon: "\u{1F4AF}", category: "Mastery", target: 100),
        .init(id: "kanji_500", title: "Kanji Expert", description: "Master 500 kanji", icon: "\u{1F393}", category: "Mastery", target: 500),
        .init(id: "kanji_1000", title: "Kanji Master", description: "Master 1,000 kanji", icon: "\u{1F451}", category: "Mastery", target: 1000),
        // Consistency
        .init(id: "streak_3", title: "Three Day Streak", description: "Study for 3 days in a row", icon: "\u{1F525}", category: "Consistency", target: 3),
        .init(id: "streak_7", title: "Week Warrior", description: "Study for 7 days in a row", icon: "\u{1F4C5}", category: "Consistency", target: 7),
        .init(id: "streak_30", title: "Month Master", description: "Study for 30 days in a row", icon: "\u{1F3C6}", category: "Consistency", target: 30),
        .init(id: "streak_100", title: "Unstoppable", description: "Study for 100 days in a row", icon: "\u{26A1}", category: "Consistency", target: 100),
        // Games
        .init(id: "games_10", title: "Getting Started", description: "Complete 10 game sessions", icon: "\u{1F3AE}", category: "Games", target: 10),
        .init(id: "games_50", title: "Frequent Player", description: "Complete 50 game sessions", icon: "\u{1F579}", category: "Games", target: 50),
        .init(id: "games_100", title: "Game Master", description: "Complete 100 game sessions", icon: "\u{1F3AF}", category: "Games", target: 100),
        // Accuracy
        .init(id: "perfect_score", title: "Perfect Score", description: "Get 100% accuracy in a session", icon: "\u{1F4AF}", category: "Accuracy", target: 1),
        .init(id: "perfect_5", title: "Perfectionist", description: "Get 100% accuracy in 5 sessions", icon: "\u{2728}", category: "Accuracy", target: 5),
        // Rewards
        .init(id: "coins_100", title: "Coin Collector", description: "Earn 100 J Coins", icon: "\u{1FA99}", category: "Rewards", target: 100),
        .init(id: "coins_500", title: "Coin Hoarder", description: "Earn 500 J Coins", icon: "\u{1F4B0}", category: "Rewards", target: 500),
    ]
}
