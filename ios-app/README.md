# KanjiQuest iPad

A calligraphy-focused kanji learning app for iPad with Apple Pencil support.

## Overview

KanjiQuest iPad is the iOS companion to KanjiQuest Android, featuring:

- **Recognition Mode** - Multiple choice kanji reading quizzes
- **Calligraphy Mode** - Apple Pencil writing practice with AI feedback
- **SRS Learning** - Spaced repetition for effective memorization
- **Progress Tracking** - XP, levels, streaks, and achievements

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    SwiftUI UI Layer                      │
│  HomeView │ RecognitionView │ CalligraphySessionView    │
└─────────────────────────┬───────────────────────────────┘
                          │
┌─────────────────────────┴───────────────────────────────┐
│                    ViewModels                            │
│  HomeViewModel │ RecognitionViewModel │ Calligraphy...  │
└─────────────────────────┬───────────────────────────────┘
                          │ SKIE (StateFlow → AsyncSequence)
┌─────────────────────────┴───────────────────────────────┐
│              SharedCore (KMP Framework)                  │
│  GameEngine │ SRS │ Repositories │ StrokeMatcher        │
└─────────────────────────────────────────────────────────┘
```

### Key Technologies

| Layer | Technology |
|-------|------------|
| UI | SwiftUI + UIKit (Canvas) |
| State | ObservableObject + @Published |
| Shared Logic | Kotlin Multiplatform (KMP) |
| KMP Bridge | SKIE 0.10.10 |
| Database | SQLDelight (NativeSqliteDriver) |
| Auth | Supabase |
| AI Feedback | Gemini 2.5 Flash |

## Project Structure

```
ios-app/
├── KanjiQuest/
│   ├── App/                    # @main entry point
│   ├── Core/                   # KMPBridge, Configuration
│   ├── DI/                     # AppContainer (manual DI)
│   ├── Navigation/             # NavigationStack + Routes
│   ├── Theme/                  # KanjiQuestTheme tokens
│   ├── Screens/
│   │   ├── Home/               # Main menu
│   │   ├── Recognition/        # Quiz mode
│   │   ├── Calligraphy/        # Writing mode
│   │   │   ├── Canvas/         # Apple Pencil input
│   │   │   └── Feedback/       # AI evaluation
│   │   ├── Auth/               # Login/Register
│   │   ├── Progress/           # Stats & achievements
│   │   ├── Settings/           # App settings
│   │   └── Subscription/       # StoreKit 2 IAP
│   └── Calligraphy/
│       ├── Canvas/             # CalligraphyCanvasUIView
│       └── Feedback/           # CalligraphyFeedbackService
├── KanjiQuestTests/            # Unit tests
├── KanjiQuestUITests/          # UI tests
├── project.yml                 # XcodeGen spec
└── KanjiQuest.xcconfig         # Build configuration
```

## Development Setup

### Prerequisites

- macOS with Xcode 15+
- iOS 16.0+ deployment target
- Apple Developer account (for device testing)

### Building

```bash
# 1. Build KMP framework (requires Gradle)
cd ~/path/to/KanjiQuest
./gradlew :shared-core:assembleXCFramework

# 2. Generate Xcode project
cd ios-app
xcodegen generate

# 3. Open in Xcode
open KanjiQuest.xcodeproj
```

### Configuration

Copy the template and fill in your values:

```bash
cp KanjiQuest.xcconfig.template KanjiQuest.xcconfig
```

Required values in `KanjiQuest.xcconfig`:
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_SERVICE_ROLE_KEY`
- `GEMINI_API_KEY`
- `DEVELOPMENT_TEAM`

### Running Tests

```bash
# Unit tests
xcodebuild test -scheme KanjiQuest -destination 'platform=iOS Simulator,name=iPad Pro 13-inch (M4)'

# UI tests
xcodebuild test -scheme KanjiQuestUITests -destination 'platform=iOS Simulator,name=iPad Pro 13-inch (M4)'
```

## CI/CD

GitHub Actions workflow builds and deploys to TestFlight automatically.

See [CI-SETUP.md](CI-SETUP.md) for:
- Required GitHub secrets (12 total)
- Apple Developer setup steps
- App Store Connect API key creation

See [CI-SECRETS-TEMPLATE.md](CI-SECRETS-TEMPLATE.md) for a fill-in template.

## Game Modes

### Recognition Mode

Multiple choice quiz asking for kanji readings:

1. Display kanji character
2. Show 4 reading choices
3. User selects answer
4. Show result + XP gained
5. Continue to next question
6. Session complete with stats

### Calligraphy Mode (iPad Exclusive)

Apple Pencil writing practice with real-time feedback:

1. Display target kanji with ghost strokes
2. User writes with Apple Pencil
3. Geometric stroke matching (StrokeMatcher)
4. AI calligraphy feedback (Gemini)
5. Five-aspect evaluation:
   - バランス (Balance)
   - 筆順 (Stroke order)
   - 止め/はね/はらい (Endings)
   - 筆圧 (Pressure)
   - 運筆 (Flow)

## Shared Code (KMP)

The app shares core logic with Android via Kotlin Multiplatform:

| Module | Purpose |
|--------|---------|
| `shared-core` | GameEngine, SRS, Repositories, StrokeMatcher |
| `shared-japanese` | Text utilities (no platform dependencies) |

SKIE provides Swift-friendly transforms:
- `StateFlow` → `AsyncSequence`
- `suspend fun` → `async func`
- Sealed classes → Pattern-matchable types

## Version History

| Version | Status | Features |
|---------|--------|----------|
| v0.1.0 | Planned | TestFlight MVP (Recognition only) |
| v0.2.0 | Planned | Calligraphy mode with Apple Pencil |
| v0.3.0 | Planned | All 4 game modes |
| v1.0.0 | Planned | App Store release |

## Related Projects

- [KanjiQuest Android](../android-app/) - Full-featured Android app
- [KanjiLens](../../KanjiLens/) - Camera OCR companion app
- [shared-core](../shared-core/) - KMP shared logic
- [shared-japanese](../shared-japanese/) - Japanese text utilities

## License

Proprietary - JWorks AI
