# KanjiQuest Changelog

All notable changes to KanjiQuest are documented here.

---

## [0.1.0-beta5] - 2026-02-21

### Added
- **Collection Gameplay System**: "Gotta Catch 'Em All" mechanic — kanji, kana, and radicals are discovered through gameplay with probability-based encounters and a pity system
- **Rarity System**: 5 tiers (Common/Uncommon/Rare/Epic/Legendary) based on grade, frequency, and stroke count; color-coded borders on collected items
- **Encounter Engine**: Per-answer encounter rolls (40%→2% by rarity), pity counters guarantee discoveries after N correct answers
- **Item Level Engine**: Collected items earn XP per practice (+10 correct, +2 wrong); max level 10 with quadratic scaling
- **Discovery Overlay**: Pokémon-catch-style animation when a new item is discovered during gameplay (all game modes)
- **Collection Screen**: Full browser with tabs per type, rarity filters, stats, and grid with rarity borders + level badges
- **JWorks Splash Screen**: Branded splash matching KanjiLens (black bg, logo shimmer, teal title, fade out)
- **KanjiLens Deep Link Integration**: `kanjiquest://collect?kanji_id=XXX&source=kanjilens` adds kanji to collection from KanjiLens
- **Starter Pack**: New players receive 5 hiragana + 5 katakana + 3 Grade 1 kanji on first launch
- **Radical Detail Screen**: View radical info, related kanji, and practice buttons
- **Field Journal**: Camera challenge scan history with photo entries
- **Flashcard Deck Groups**: Organize flashcard decks into folders
- **Kana Game Modes**: Hiragana/Katakana recognition and writing practice screens
- **Radical Game Modes**: Radical recognition quiz and radical builder (compose kanji from radicals)

### Changed
- **Home Screen Panel Tabs**: 3-layer tab system — Layer 1: Hiragana/Katakana/部首/Kanji; Layer 2 (Kanji): School Grade/JLPT/Strokes/Frequency; Layer 3: level selectors (G1-G8+, N1-N5, etc.)
- **Home Screen Selection Persistence**: Tab, sort mode, and grade/level selections now persist when navigating back from detail pages
- **All Grades Visible**: School Grade shows G1-G8+ with grayed-out tabs for grades with no collected kanji
- **Hidden Uncollected Kanji**: Uncollected items are blank placeholders (no character, not clickable) maintaining sort order (虫食い pattern)
- **Home Grid**: Shows collection-aware items with rarity borders and level badges for kanji, kana, and radicals
- **Collection Counter**: Shows "N/Total Kanji Collected" on home screen
- **Game Engine**: Integrated encounter rolls and item XP on correct/wrong answers
- **Question Generator**: Collection-aware — mixes collected items for review with uncollected for exploration encounters
- **Navigation**: Added routes for Splash, Collection, Radical Detail, Field Journal, Kana Recognition/Writing, Radical Recognition/Builder
- **Database**: New `collection` and `field_journal` tables with migration for existing SRS data → collection
- **Data Pipeline**: Enhanced radical parser with image generation

### Fixed
- Smart cast errors across modules for `discoveredItem` in game result screens

---

## [0.1.0-beta4] - 2026-02-13

### Added
- **Placement Test**: First-time user assessment across Grade 1-6 kanji (5 questions per grade) with automatic level assignment
- **Flashcard & SRS System**: Spaced repetition flashcard decks with self-rating (Again/Hard/Good/Easy), interval scheduling, and SQLDelight persistence
- **Feedback System**: In-app feedback dialog with category chips (Bug, Feature Request, UI/UX, Performance, Content, Other), character counter, rate limiting (5/day), feedback history with status tracking, and 15s polling for updates
- **Feedback FAB**: Floating action button on all screens (except Login) for quick feedback submission
- **FCM Push Notifications**: Firebase Cloud Messaging service for feedback status updates (pending Firebase registration)
- **Developer Chat**: In-app chat for registered developers, routed through Supabase Edge Functions to Discord via n8n
- **AI Feedback Reporter**: Dedicated reporter for writing mode AI feedback results
- **KanjiText Theme Component**: Reusable Compose component for consistent kanji text rendering
- **Notification Icon**: Vector drawable for push notification display
- **KanjiModeStats SQLDelight**: Per-mode statistics tracking in local database

### Changed
- **Home Screen**: Major UI overhaul — game mode cards, tier card with XP progress, J Coin balance, Word of the Day, premium upgrade banner, free user trial indicators
- **Writing Mode**: Enhanced drawing canvas with improved touch handling, upgraded stroke renderer with better visual feedback, expanded handwriting checker with AI integration, session management improvements
- **Kanji Detail Screen**: Added practice launch buttons (Recognition, Writing, Camera), example words section, enhanced layout (+152 lines)
- **Navigation**: Added routes for Placement Test, Flashcards, Flashcard Study, Dev Chat, Kanji Detail, Word Detail; updated nav host with full route handling
- **Settings Screen**: Added Developer section (visible only for registered devs), enhanced admin controls
- **Recognition Mode**: Refined screen layout and view model session handling
- **Camera Challenge**: Enhanced view model with improved scan result processing
- **Vocabulary Mode**: Refined screen and view model
- **AppModule (DI)**: Registered DevChatRepository, FeedbackRepository, FlashcardRepository, and associated ViewModels
- **AndroidManifest**: Added FCM service, notification permissions, internet permission declarations
- **build.gradle.kts**: Added Firebase Messaging dependency
- **Game Engine**: Improved session flow and state transitions
- **Question Generator**: Enhanced question selection with adaptive difficulty support
- **GameState**: Updated state model for new game modes
- **CompleteSessionUseCase**: Enhanced with J Coin earning integration, XP accuracy bonuses, and earning cap awareness
- **WordOfTheDayUseCase**: Improved word selection logic
- **SrsRepository**: Added mode-specific stats tracking methods
- **DatabaseDriverFactory**: Added Android-specific schema migration support

### Fixed
- **getUserEmail() always returning null**: `UserSessionProvider` now correctly collects from auth state flow via `firstOrNull()` instead of creating a flow and never collecting
- **Feedback history race condition**: `FeedbackViewModel.openDialog()` now resolves email before loading history (sequential in same coroutine)
- **"Note: null" in feedback history**: `FeedbackRepositoryImpl` now handles `JsonNull` properly with `contentOrNull`

---

## [0.1.0-beta3] - 2026-02-08

### Added
- Adaptive difficulty system with grade mastery badges (Beginning/Developing/Proficient/Advanced)
- Adaptive grade mixing based on mastery level
- XP accuracy bonus (+25% for 90%+ with 10+ cards, +15% for 85%+ with 5+ cards)
- Full project structure reorganization

### Fixed
- XP display overflow (level-1 to level format)
- Tier card effective level with admin override

---

## [0.1.0-beta2] - 2026-02-07

### Added
- J Coin Shop with TutoringJay Featured Banner
- Settings screen moved to top banner navigation
- Word of the Day randomization on launch

### Fixed
- Home screen bleed-through in game mode screens

---

## [0.1.0-beta1] - 2026-02-06

### Added
- Ollama AI handwriting feedback in Writing Mode
- Vocabulary Mode (Game Mode 2) with 4 question types
- SRS-aware difficulty scaling in Writing Mode
- Word of the Day feature

---

## [0.1.0-alpha] - 2026-02-05

### Added
- Recognition Mode (Game Mode 1) — core kanji quiz gameplay
- Writing Mode (Game Mode 1b) — stroke-based kanji writing practice
- Camera Challenge Mode — ML Kit OCR kanji detection
- TutoringJay authentication (dual Supabase)
- User progression system (levels, XP, streaks)
- Achievement system
- Progress & Stats tracking
- Stripe subscription integration ($4.99/mo premium)
- Free user preview mode (daily trial limits)
- Admin detection for Jay's emails
- iOS (iPad 書道) Phase 1 foundation
- GitHub Actions CI/CD for iOS builds
