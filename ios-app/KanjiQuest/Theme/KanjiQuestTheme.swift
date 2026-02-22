import SwiftUI
import UIKit

/// Design tokens matching the Android KanjiQuest Material3 theme.
/// Warm, kid-friendly palette: Orange primary, Teal secondary, Gold accents.
enum KanjiQuestTheme {
    /// True on iPhone, false on iPad
    static let isPhone = UIDevice.current.userInterfaceIdiom == .phone

    // MARK: - Light palette (matches Android LightColors)
    static let primary = Color(hex: 0xFF8C42)           // Orange
    static let primaryDark = Color(hex: 0xE07030)        // OrangeDark
    static let onPrimary = Color.white

    static let secondary = Color(hex: 0x26A69A)          // Teal
    static let secondaryDark = Color(hex: 0x00897B)       // TealDark
    static let onSecondary = Color.white

    static let tertiary = Color(hex: 0xFFD54F)            // Gold
    static let tertiaryDark = Color(hex: 0xFFC107)        // GoldDark
    static let onTertiary = Color.black

    static let background = Color(hex: 0xFFF8E1)          // Cream
    static let backgroundDark = Color(hex: 0x2C2C2C)      // CreamDark
    static let onBackground = Color(hex: 0x1C1B1F)

    static let surface = Color.white
    static let surfaceDark = Color(hex: 0x1C1B1F)
    static let surfaceVariant = Color(hex: 0xF5F5F5)
    static let onSurface = Color(hex: 0x1C1B1F)
    static let onSurfaceVariant = Color(hex: 0x49454F)

    // MARK: - Status
    static let success = Color(hex: 0x2E7D32)
    static let error = Color(hex: 0xB00020)

    // MARK: - XP / Gold / Coins
    static let xpGold = Color(hex: 0xFFB300)
    static let coinGold = Color(hex: 0xFFC107)

    // MARK: - Tier colors
    static let tierBronze = Color(hex: 0xCD7F32)
    static let tierSilver = Color(hex: 0xC0C0C0)
    static let tierGold = Color(hex: 0xFFD700)
    static let tierPlatinum = Color(hex: 0xE5E4E2)
    static let tierDiamond = Color(hex: 0xB9F2FF)

    // MARK: - Rarity colors (collection)
    static let rarityCommon = Color(hex: 0x9E9E9E)
    static let rarityUncommon = Color(hex: 0x4CAF50)
    static let rarityRare = Color(hex: 0x2196F3)
    static let rarityEpic = Color(hex: 0x9C27B0)
    static let rarityLegendary = Color(hex: 0xFFD700)

    // MARK: - Grade mastery colors
    static let masteryBeginning = Color(hex: 0x9E9E9E)
    static let masteryDeveloping = Color(hex: 0x42A5F5)
    static let masteryProficient = Color(hex: 0x66BB6A)
    static let masteryAdvanced = Color(hex: 0xFFD54F)

    // MARK: - Typography
    static let headlineLarge = Font.system(size: 36, weight: .bold, design: .rounded)
    static let headlineMedium = Font.system(size: 32, weight: .bold, design: .rounded)
    static let titleLarge = Font.system(size: 28, weight: .bold, design: .rounded)
    static let titleMedium = Font.system(size: 22, weight: .semibold, design: .rounded)
    static let titleSmall = Font.system(size: 18, weight: .semibold, design: .rounded)
    static let bodyLarge = Font.system(size: 17, weight: .regular)
    static let bodyMedium = Font.system(size: 15, weight: .regular)
    static let bodySmall = Font.system(size: 13, weight: .regular)
    static let labelLarge = Font.system(size: 15, weight: .semibold)
    static let labelMedium = Font.system(size: 13, weight: .medium)
    static let labelSmall = Font.system(size: 12, weight: .medium)

    // Kanji display — scaled for iPad vs iPhone
    static var kanjiDisplay: Font {
        Font.system(size: isPhone ? 64 : 96, weight: .regular, design: .serif)
    }
    static var kanjiLarge: Font {
        Font.system(size: isPhone ? 48 : 72, weight: .regular, design: .serif)
    }
    static var kanjiMedium: Font {
        Font.system(size: isPhone ? 36 : 48, weight: .regular, design: .serif)
    }
    static var kanjiSmall: Font {
        Font.system(size: isPhone ? 24 : 32, weight: .regular, design: .serif)
    }
    static var kanjiGrid: Font {
        Font.system(size: isPhone ? 28 : 36, weight: .regular, design: .serif)
    }

    // MARK: - Spacing
    static let spacingXS: CGFloat = 4
    static let spacingS: CGFloat = 8
    static let spacingM: CGFloat = 16
    static let spacingL: CGFloat = 24
    static let spacingXL: CGFloat = 32

    // MARK: - Corner radius
    static let radiusS: CGFloat = 8
    static let radiusM: CGFloat = 12
    static let radiusL: CGFloat = 16
    static let radiusXL: CGFloat = 24
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
