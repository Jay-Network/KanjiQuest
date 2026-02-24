import SwiftUI

/// Games hub screen matching Android's GamesScreen.kt.
/// Shows game mode cards: Radical Builder, Test Mode, Speed Challenge, Battle.
struct GamesTabView: View {
    @EnvironmentObject var container: AppContainer
    let navigateTo: (NavRoute) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("Game Modes")
                    .font(KanjiQuestTheme.titleLarge)
                    .fontWeight(.bold)

                // Radical Builder - playable
                GameCardView(
                    title: "Radical Builder",
                    description: "Build kanji from radical parts",
                    color: Color(hex: 0x795548),
                    imageAsset: "mode-radical-builder.png",
                    isPlayable: true,
                    action: { navigateTo(.radicalBuilder) }
                )

                // Test Mode - playable
                GameCardView(
                    title: "Test Mode",
                    description: "Quiz yourself on grades, JLPT, kana, or radicals",
                    color: Color(hex: 0x2196F3),
                    isPlayable: true,
                    action: { navigateTo(.testMode) }
                )

                // Speed Challenge - coming soon
                GameCardView(
                    title: "Speed Challenge",
                    description: "Answer as fast as you can before time runs out",
                    color: Color(hex: 0xFF5722),
                    isPlayable: false,
                    comingSoonLabel: "Coming Soon"
                )

                // Battle - coming soon
                GameCardView(
                    title: "Battle",
                    description: "Challenge friends online",
                    color: Color(hex: 0x673AB7),
                    isPlayable: false,
                    comingSoonLabel: "Coming Soon"
                )
            }
            .padding(16)
        }
        .background(KanjiQuestTheme.background)
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Text("Play")
                    .font(KanjiQuestTheme.titleSmall)
                    .foregroundColor(.white)
            }
        }
        .toolbarBackground(KanjiQuestTheme.primary, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
    }
}

/// Game mode card matching Android's GameCard composable.
private struct GameCardView: View {
    let title: String
    let description: String
    let color: Color
    var imageAsset: String? = nil
    let isPlayable: Bool
    var comingSoonLabel: String? = nil
    var action: () -> Void = {}

    var body: some View {
        Button(action: { if isPlayable { action() } }) {
            HStack {
                if let imageAsset {
                    AssetImage(filename: imageAsset, contentDescription: title)
                        .frame(width: 56, height: 56)
                    Spacer().frame(width: 12)
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(isPlayable ? .white : .white.opacity(0.6))
                    Text(description)
                        .font(.system(size: 12))
                        .foregroundColor(isPlayable ? .white.opacity(0.8) : .white.opacity(0.4))
                }
                Spacer()
                if let comingSoonLabel {
                    Text(comingSoonLabel)
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(.white.opacity(0.5))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(.white.opacity(0.15))
                        .cornerRadius(6)
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, minHeight: 100, alignment: .leading)
            .background(isPlayable ? color : color.opacity(0.3))
            .cornerRadius(16)
        }
        .disabled(!isPlayable)
    }
}
