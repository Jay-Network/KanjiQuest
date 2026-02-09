import SwiftUI
import SharedCore

struct ProgressView: View {
    @EnvironmentObject var container: AppContainer
    @State private var totalKanji = 0
    @State private var masteredKanji = 0
    @State private var sessionsCompleted = 0
    @State private var totalXP = 0

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
                    StatCard(title: "Sessions", value: "\(sessionsCompleted)", icon: "checkmark.circle.fill", color: KanjiQuestTheme.success)
                    StatCard(title: "Kanji Seen", value: "\(totalKanji)", icon: "character.ja", color: KanjiQuestTheme.primary)
                    StatCard(title: "Mastered", value: "\(masteredKanji)", icon: "trophy.fill", color: KanjiQuestTheme.coinGold)
                }
            }
            .padding()
        }
        .background(KanjiQuestTheme.background)
        .navigationTitle("Progress")
        .task {
            await loadStats()
        }
    }

    private func loadStats() async {
        // Load from shared-core repositories
        do {
            let profile = try await container.userRepository.getUserProfile()
            totalXP = Int(profile?.totalXp ?? 0)
            sessionsCompleted = Int(profile?.sessionsCompleted ?? 0)
        } catch {}
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
