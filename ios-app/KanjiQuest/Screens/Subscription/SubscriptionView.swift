import SwiftUI

/// Subscription screen. Mirrors Android's SubscriptionScreen.kt.
/// Current plan badge, feature comparison table, upgrade button ($4.99/mo).
struct SubscriptionView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var viewModel = SubscriptionViewModel()
    var onBack: () -> Void = {}

    var body: some View {
        ZStack {
            KanjiQuestTheme.background.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 0) {
                    // Current plan badge
                    let isPremium = viewModel.isPremium
                    VStack(spacing: 4) {
                        Text("Current Plan")
                            .font(KanjiQuestTheme.labelLarge)
                            .foregroundColor(isPremium ? .black : KanjiQuestTheme.onSurfaceVariant)

                        Text(isPremium ? "Premium" : "Free")
                            .font(KanjiQuestTheme.headlineMedium).fontWeight(.bold)
                            .foregroundColor(isPremium ? .black : KanjiQuestTheme.onSurface)

                        if isPremium {
                            Text("$4.99/month")
                                .font(KanjiQuestTheme.bodyMedium)
                                .foregroundColor(.black.opacity(0.7))
                        }
                    }
                    .padding(24)
                    .frame(maxWidth: .infinity)
                    .background(isPremium ? Color(hex: 0xFFD700) : KanjiQuestTheme.surfaceVariant)
                    .cornerRadius(KanjiQuestTheme.radiusM)
                    .padding(.horizontal, 16)

                    Spacer().frame(height: 24)

                    // Feature comparison
                    Text("Feature Comparison")
                        .font(KanjiQuestTheme.titleMedium).fontWeight(.bold)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 16)

                    Spacer().frame(height: 12)

                    VStack(spacing: 0) {
                        featureRow("Recognition Mode", free: true, premium: true)
                        featureRow("Writing Mode", free: false, premium: true)
                        featureRow("Vocabulary Mode", free: false, premium: true)
                        featureRow("Camera Challenge", free: false, premium: true)
                        featureRow("Daily Questions", freeText: "15/day", premiumText: "Unlimited")
                        featureRow("Kanji Grades", freeText: "1-2", premiumText: "All")
                        featureRow("J Coin Earning", free: false, premium: true)
                        featureRow("AI Handwriting", free: false, premium: true)
                        featureRow("Shop Purchases", freeText: "View only", premiumText: "Full")
                    }
                    .padding(.horizontal, 16)

                    Spacer().frame(height: 24)

                    if !isPremium {
                        Button(action: {
                            if let url = URL(string: "https://portal.tutoringjay.com/subscribe/kanjiquest") {
                                UIApplication.shared.open(url)
                            }
                        }) {
                            Text("Upgrade to Premium - $4.99/mo")
                                .fontWeight(.bold).font(.system(size: 16))
                                .foregroundColor(.black)
                                .frame(maxWidth: .infinity).frame(height: 56)
                                .background(Color(hex: 0xFFD700))
                                .cornerRadius(12)
                        }
                        .padding(.horizontal, 16)

                        Spacer().frame(height: 8)

                        Text("Managed through portal.tutoringjay.com")
                            .font(KanjiQuestTheme.labelSmall)
                            .foregroundColor(KanjiQuestTheme.onSurfaceVariant)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 16)
                    } else {
                        Button(action: {
                            if let url = URL(string: "https://portal.tutoringjay.com/subscription") {
                                UIApplication.shared.open(url)
                            }
                        }) {
                            Text("Manage Subscription")
                                .fontWeight(.bold).font(.system(size: 16))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity).frame(height: 56)
                                .background(KanjiQuestTheme.primary)
                                .cornerRadius(12)
                        }
                        .padding(.horizontal, 16)
                    }

                    Spacer().frame(height: 32)
                }
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onBack) { Image(systemName: "chevron.left"); Text("Back") }.foregroundColor(.white)
            }
            ToolbarItem(placement: .principal) { Text("Subscription").font(.headline).foregroundColor(.white) }
        }
        .toolbarBackground(KanjiQuestTheme.primary, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .task { viewModel.loadSubscription(container: container) }
    }

    private func featureRow(_ feature: String, free: Bool = false, premium: Bool = false, freeText: String? = nil, premiumText: String? = nil) -> some View {
        HStack {
            Text(feature).font(KanjiQuestTheme.bodyMedium).frame(maxWidth: .infinity, alignment: .leading)
            HStack(spacing: 0) {
                Text(freeText ?? (free ? "Yes" : "---"))
                    .font(KanjiQuestTheme.bodySmall)
                    .foregroundColor(free || freeText != nil ? KanjiQuestTheme.onSurface : KanjiQuestTheme.onSurfaceVariant.opacity(0.5))
                    .frame(width: 70, alignment: .center)

                Text(premiumText ?? (premium ? "Yes" : "---"))
                    .font(KanjiQuestTheme.bodySmall).fontWeight(.bold)
                    .foregroundColor(Color(hex: 0xFFD700))
                    .frame(width: 70, alignment: .center)
            }
        }
        .padding(.vertical, 6)
    }
}
