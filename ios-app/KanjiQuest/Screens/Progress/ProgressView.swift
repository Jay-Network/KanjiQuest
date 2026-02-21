import SwiftUI
import SharedCore

struct ProgressView: View {
    @EnvironmentObject var container: AppContainer
    @State private var totalKanji = 0
    @State private var masteredKanji = 0
    @State private var currentStreak = 0
    @State private var totalXP = 0
    @State private var gradeMasteries: [GradeMastery] = []

    var body: some View {
        ScrollView {
            VStack(spacing: KanjiQuestTheme.spacingL) {
                Text("Your Progress")
                    .font(KanjiQuestTheme.titleMedium)
                    .frame(maxWidth: .infinity, alignment: .leading)

                LazyVGrid(columns: [
                    GridItem(.flexible()),
                    GridItem(.flexible())
                ], spacing: KanjiQuestTheme.spacingM) {
                    StatCard(title: "Total XP", value: "\(totalXP)", icon: "star.fill", color: KanjiQuestTheme.xpGold)
                    StatCard(title: "Streak", value: "\(currentStreak)", icon: "flame.fill", color: KanjiQuestTheme.success)
                    StatCard(title: "Kanji Seen", value: "\(totalKanji)", icon: "character.ja", color: KanjiQuestTheme.primary)
                    StatCard(title: "Mastered", value: "\(masteredKanji)", icon: "trophy.fill", color: KanjiQuestTheme.coinGold)
                }

                // Grade Mastery Badges
                if !gradeMasteries.isEmpty {
                    VStack(alignment: .leading, spacing: KanjiQuestTheme.spacingS) {
                        Text("Grade Mastery")
                            .font(KanjiQuestTheme.titleMedium)
                            .frame(maxWidth: .infinity, alignment: .leading)

                        LazyVGrid(columns: [
                            GridItem(.flexible()),
                            GridItem(.flexible()),
                            GridItem(.flexible())
                        ], spacing: KanjiQuestTheme.spacingM) {
                            ForEach(gradeMasteries, id: \.grade) { mastery in
                                VStack(spacing: 6) {
                                    MasteryBadgeView(level: mastery.masteryLevel, size: 56)
                                    Text("Grade \(mastery.grade)")
                                        .font(KanjiQuestTheme.labelSmall)
                                    Text("\(Int(mastery.masteryScore * 100))%")
                                        .font(KanjiQuestTheme.labelSmall)
                                        .foregroundColor(.secondary)
                                }
                                .padding(.vertical, 8)
                                .frame(maxWidth: .infinity)
                                .background(KanjiQuestTheme.surface)
                                .cornerRadius(KanjiQuestTheme.radiusM)
                                .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
                            }
                        }
                    }
                }
            }
            .padding()
        }
        .background(KanjiQuestTheme.background)
        .navigationTitle("Progress")
        .task {
            await loadStats()
            await loadGradeMasteries()
        }
    }

    private func loadStats() async {
        do {
            let profile = try await container.userRepository.getProfile()
            totalXP = Int(profile.totalXp)
            currentStreak = Int(profile.currentStreak)
        } catch {}
    }

    private func loadGradeMasteries() async {
        var masteries: [GradeMastery] = []
        for grade: Int32 in 1...6 {
            do {
                let total = try await container.kanjiRepository.getKanjiCountByGrade(grade: grade)
                let mastery = try await container.srsRepository.getGradeMastery(grade: grade, totalKanjiInGrade: total.int64Value)
                if mastery.studiedCount > 0 {
                    masteries.append(mastery)
                }
            } catch {}
        }
        gradeMasteries = masteries
    }
}

private struct StatCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color

    var body: some View {
        VStack(spacing: KanjiQuestTheme.spacingS) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(color)

            Text(value)
                .font(KanjiQuestTheme.titleMedium)

            Text(title)
                .font(KanjiQuestTheme.labelSmall)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(KanjiQuestTheme.surface)
        .cornerRadius(KanjiQuestTheme.radiusM)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
}
