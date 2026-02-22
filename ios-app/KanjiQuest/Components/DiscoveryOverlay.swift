import SwiftUI
import SharedCore

/// Full-screen overlay shown when a new kanji/kana/radical is discovered (collected).
/// Mirrors Android's DiscoveryOverlay.kt with rarity glow, stars, and source info.
struct DiscoveryOverlay: View {
    let discoveredItem: CollectedItem
    let kanjiLiteral: String?
    let kanjiMeaning: String?
    let onDismiss: () -> Void

    @State private var animateIn = false
    @State private var glowAlpha: Double = 0.3

    private var rarityColor: Color {
        Color(hex: UInt(discoveredItem.rarity.colorValue))
    }

    private var rarityStars: String {
        switch discoveredItem.rarity {
        case .common: return ""
        case .uncommon: return "\u{2605}"
        case .rare: return "\u{2605}\u{2605}"
        case .epic: return "\u{2605}\u{2605}\u{2605}"
        case .legendary: return "\u{2605}\u{2605}\u{2605}\u{2605}"
        default: return ""
        }
    }

    private var sourceText: String {
        switch discoveredItem.source {
        case "kanjilens": return "Discovered via KanjiLens!"
        case "starter": return "Starter Collection"
        default: return "Added to Collection"
        }
    }

    var body: some View {
        ZStack {
            Color.black.opacity(0.7).ignoresSafeArea()
                .onTapGesture(perform: onDismiss)

            VStack(spacing: KanjiQuestTheme.spacingM) {
                // "NEW!" header
                Text("NEW!")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(rarityColor)
                    .tracking(4)

                // Kanji card with rarity glow
                ZStack {
                    Circle()
                        .fill(
                            RadialGradient(
                                gradient: Gradient(colors: [rarityColor.opacity(glowAlpha), .clear]),
                                center: .center,
                                startRadius: 0,
                                endRadius: 120
                            )
                        )
                        .frame(width: 240, height: 240)

                    RoundedRectangle(cornerRadius: 20)
                        .fill(KanjiQuestTheme.surface)
                        .shadow(color: .black.opacity(0.2), radius: 8, y: 4)
                        .frame(width: 180, height: 180)
                        .overlay(
                            Group {
                                if let literal = kanjiLiteral {
                                    KanjiText(
                                        text: literal,
                                        font: .system(size: 96, weight: .regular, design: .serif)
                                    )
                                } else {
                                    Text("\(discoveredItem.itemId)")
                                        .font(.system(size: 48))
                                }
                            }
                        )
                }
                .scaleEffect(animateIn ? 1.0 : 0.5)
                .opacity(animateIn ? 1.0 : 0.0)

                // Meaning
                if let meaning = kanjiMeaning {
                    Text(meaning)
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.white)
                }

                // Rarity badge with stars
                Text("\(rarityStars) \(discoveredItem.rarity.label)")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(rarityColor)

                // Source info
                Text(sourceText)
                    .font(.system(size: 14))
                    .foregroundColor(.white.opacity(0.7))

                Spacer().frame(height: KanjiQuestTheme.spacingL)

                Text("Tap to continue")
                    .font(.system(size: 12))
                    .foregroundColor(.white.opacity(0.5))
            }
            .padding(32)
        }
        .onAppear {
            withAnimation(.spring(response: 0.5, dampingFraction: 0.7)) {
                animateIn = true
            }
            // Pulsing glow animation
            withAnimation(.easeInOut(duration: 1.0).repeatForever(autoreverses: true)) {
                glowAlpha = 0.7
            }
        }
    }
}
