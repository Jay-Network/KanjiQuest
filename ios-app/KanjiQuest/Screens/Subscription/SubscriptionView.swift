import SwiftUI

struct SubscriptionView: View {
    @State private var isSubscribed = false
    @State private var isLoading = false

    var body: some View {
        ScrollView {
            VStack(spacing: KanjiQuestTheme.spacingL) {
                // Header
                VStack(spacing: KanjiQuestTheme.spacingS) {
                    Image(systemName: "star.circle.fill")
                        .font(.system(size: 64))
                        .foregroundColor(KanjiQuestTheme.coinGold)

                    Text("KanjiQuest Premium")
                        .font(KanjiQuestTheme.titleLarge)

                    Text(KanjiQuestTheme.isPhone ? "漢字 Learning Edition" : "書道 Calligraphy Edition")
                        .font(KanjiQuestTheme.bodyLarge)
                        .foregroundColor(.secondary)
                }
                .padding(.top, KanjiQuestTheme.spacingXL)

                // Price
                VStack(spacing: KanjiQuestTheme.spacingXS) {
                    Text("$9.99/month")
                        .font(KanjiQuestTheme.titleMedium)
                        .foregroundColor(KanjiQuestTheme.primary)

                    Text(KanjiQuestTheme.isPhone
                         ? "Premium recognition & calligraphy features"
                         : "Premium calligraphy features with Apple Pencil")
                        .font(KanjiQuestTheme.bodyMedium)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }

                // Features list
                VStack(alignment: .leading, spacing: KanjiQuestTheme.spacingM) {
                    FeatureRow(icon: "pencil.tip", text: "Full pressure-sensitive calligraphy")
                    FeatureRow(icon: "brain.head.profile", text: "AI 書道 feedback (5 aspects)")
                    FeatureRow(icon: "arrow.triangle.2.circlepath", text: "SRS scheduling for mastery")
                    FeatureRow(icon: "chart.bar.fill", text: "Detailed progress analytics")
                    FeatureRow(icon: "j.circle.fill", text: "Earn J Coins for rewards")
                    FeatureRow(icon: "icloud.fill", text: "Cloud sync across devices")
                }
                .padding()
                .background(KanjiQuestTheme.surface)
                .cornerRadius(KanjiQuestTheme.radiusL)

                // Subscribe button
                Button {
                    // StoreKit 2 purchase will be implemented in Phase 3
                    isLoading = true
                } label: {
                    if isLoading {
                        SwiftUI.ProgressView()
                            .progressViewStyle(.circular)
                            .tint(.white)
                    } else {
                        Text(isSubscribed ? "Subscribed" : "Subscribe Now")
                    }
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(isSubscribed ? KanjiQuestTheme.success : KanjiQuestTheme.primary)
                .foregroundColor(.white)
                .cornerRadius(KanjiQuestTheme.radiusM)
                .disabled(isSubscribed)

                Text("Cancel anytime. Subscription auto-renews monthly.")
                    .font(KanjiQuestTheme.labelSmall)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding()
        }
        .background(KanjiQuestTheme.background)
        .navigationTitle("Premium")
    }
}

private struct FeatureRow: View {
    let icon: String
    let text: String

    var body: some View {
        HStack(spacing: KanjiQuestTheme.spacingM) {
            Image(systemName: icon)
                .foregroundColor(KanjiQuestTheme.primary)
                .frame(width: 24)

            Text(text)
                .font(KanjiQuestTheme.bodyLarge)
        }
    }
}
