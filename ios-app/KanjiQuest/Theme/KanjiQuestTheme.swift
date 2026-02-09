import SwiftUI

/// Design tokens matching the Android KanjiQuest Material3 theme.
enum KanjiQuestTheme {
    // Primary colors
    static let primary = Color(hex: 0xC62828)        // Deep red (漢字 ink)
    static let onPrimary = Color.white
    static let primaryContainer = Color(hex: 0xFFCDD2)

    // Secondary
    static let secondary = Color(hex: 0x424242)
    static let onSecondary = Color.white

    // Background
    static let background = Color(hex: 0xFFFBFE)
    static let surface = Color.white
    static let surfaceVariant = Color(hex: 0xF5F5F5)

    // Text
    static let onBackground = Color(hex: 0x1C1B1F)
    static let onSurface = Color(hex: 0x1C1B1F)

    // Status
    static let success = Color(hex: 0x2E7D32)
    static let error = Color(hex: 0xB00020)

    // XP / Gold
    static let xpGold = Color(hex: 0xFFB300)
    static let coinGold = Color(hex: 0xFFC107)

    // Typography
    static let titleLarge = Font.system(size: 28, weight: .bold, design: .rounded)
    static let titleMedium = Font.system(size: 22, weight: .semibold, design: .rounded)
    static let bodyLarge = Font.system(size: 17, weight: .regular)
    static let bodyMedium = Font.system(size: 15, weight: .regular)
    static let labelLarge = Font.system(size: 15, weight: .semibold)
    static let labelSmall = Font.system(size: 12, weight: .medium)

    // Kanji display (large center kanji)
    static let kanjiDisplay = Font.system(size: 96, weight: .regular, design: .serif)
    static let kanjiMedium = Font.system(size: 48, weight: .regular, design: .serif)

    // Spacing
    static let spacingXS: CGFloat = 4
    static let spacingS: CGFloat = 8
    static let spacingM: CGFloat = 16
    static let spacingL: CGFloat = 24
    static let spacingXL: CGFloat = 32

    // Corner radius
    static let radiusS: CGFloat = 8
    static let radiusM: CGFloat = 12
    static let radiusL: CGFloat = 16
}

extension Color {
    init(hex: UInt, alpha: Double = 1.0) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0,
            opacity: alpha
        )
    }
}
