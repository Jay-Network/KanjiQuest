# KanjiQuest: Android vs iOS Differences Tracker

> This document tracks every difference between the Android and iOS implementations.
> Updated as files are ported. Use this to verify feature parity.

## Overview

| Metric | Android | iOS (Before Rebuild) | iOS (After Rebuild) |
|--------|---------|---------------------|---------------------|
| Total UI files | 64 Kotlin | 26 Swift | ~70 Swift (target) |
| Total lines (UI) | ~16,500 | ~3,800 | ~15,000 (target) |
| Routes | 23+ | 7 | 23+ (target) |
| Game modes | 8 | 2 | 8 (target) |
| ViewModels | 20+ | 6 | 20+ (target) |

---

## Phase 1: Foundation

### Theme (KanjiQuestTheme)

| Property | Android | iOS (Old) | iOS (New) | Notes |
|----------|---------|-----------|-----------|-------|
| **Primary** | `0xFFFF8C42` (Orange) | `0xC62828` (Deep Red) | `0xFF8C42` (Orange) | iOS was completely wrong color |
| **Secondary** | `0xFF26A69A` (Teal) | `0x424242` (Gray) | `0x26A69A` (Teal) | iOS had gray instead of teal |
| **Tertiary** | `0xFFFFD54F` (Gold) | `0xFF8C42` (Orange) | `0xFFD54F` (Gold) | iOS had orange as tertiary |
| **Background** | `0xFFFFF8E1` (Cream) | `0xFFFBFE` (Near-white) | `0xFFF8E1` (Cream) | iOS lacked warm cream tone |
| Dark mode | Full dark color scheme | Not implemented | Light/Dark tokens defined | iOS: tokens exist, dark mode ViewModifier TBD |
| Tier colors | Not in theme (inline) | Not present | Added (bronze/silver/gold/platinum/diamond) | Centralized for reuse |
| Rarity colors | Not in theme (inline) | Not present | Added (common/uncommon/rare/epic/legendary) | Centralized for reuse |
| Mastery colors | Not in theme (inline) | Not present | Added (beginning/developing/proficient/advanced) | Centralized for reuse |
| Typography scale | Material3 defaults | 8 font tokens | 13 font tokens + 5 kanji sizes | iOS now matches Android typography depth |

**Platform Differences (Intentional):**
- Android uses `MaterialTheme.colorScheme.*` (Material3 system). iOS uses `KanjiQuestTheme.*` static tokens.
- Android has `isSystemInDarkTheme()` toggle. iOS defines dark tokens but needs `@Environment(\.colorScheme)` per-view.
- iOS adds `isPhone` flag (not needed on Android since it's phone-only).

### KanjiText

| Aspect | Android | iOS | Notes |
|--------|---------|-----|-------|
| Purpose | Force `ja` locale for CJK glyphs | Force `ja` locale for CJK glyphs | Same purpose |
| Implementation | `Text(style = style.copy(localeList = LocaleList("ja")))` | `Text().environment(\.locale, Locale("ja"))` | Platform-idiomatic approach |
| Parameters | modifier, fontSize, fontWeight, color, textAlign, style | font, fontWeight, color, alignment | iOS uses SwiftUI Font instead of TextUnit |

**Platform Differences (Intentional):**
- Android modifies `TextStyle.localeList`. iOS uses `environment(\.locale)`.
- iOS version is a `View` struct (SwiftUI pattern), not a function wrapper.

### AssetImage

| Aspect | Android | iOS | Notes |
|--------|---------|-----|-------|
| Image loading | `context.assets.open("images/$filename")` → BitmapFactory | `Bundle.main.path(forResource:inDirectory:"images")` | Platform asset access |
| RadicalImage | Same file, loads from `radicals/` | Separate `RadicalImage` struct | Same behavior, Swift struct pattern |
| Fallback | Box with Text "?" | `Image(systemName: "photo")` + Text fallback | iOS uses SF Symbols fallback |

**Platform Differences (Intentional):**
- Android uses `BitmapFactory.decodeStream()`. iOS uses `UIImage(contentsOfFile:)`.
- Android `remember {}` memoization → iOS implicit SwiftUI view identity caching.

### NavRoutes

| Route | Android | iOS (Old) | iOS (New) | Notes |
|-------|---------|-----------|-----------|-------|
| splash | ✅ | ❌ | ✅ | Was missing |
| login | ✅ | ✅ | ✅ | |
| home | ✅ | ✅ | ✅ | |
| placementTest | ✅ | ❌ | ✅ | Was missing |
| kanjiDetail(id) | ✅ | ❌ | ✅ | Was missing |
| recognition | ✅ | ✅ | ✅ | |
| recognitionTargeted(id) | ✅ | ✅ (combined) | ✅ | Was combined with recognition |
| writing | ✅ | ❌ | ✅ | Was missing |
| writingTargeted(id) | ✅ | ❌ | ✅ | Was missing |
| vocabulary | ✅ | ❌ | ✅ | Was missing |
| vocabularyTargeted(id) | ✅ | ❌ | ✅ | Was missing |
| camera | ✅ | ❌ | ✅ | Was missing |
| cameraTargeted(id) | ✅ | ❌ | ✅ | Was missing |
| kanaRecognition(type) | ✅ | ❌ | ✅ | Was missing |
| kanaWriting(type) | ✅ | ❌ | ✅ | Was missing |
| radicalRecognition | ✅ | ❌ | ✅ | Was missing |
| radicalBuilder | ✅ | ❌ | ✅ | Was missing |
| radicalDetail(id) | ✅ | ❌ | ✅ | Was missing |
| collection | ✅ | ❌ | ✅ | Was missing |
| flashcards | ✅ | ❌ | ✅ | Was missing |
| flashcardStudy(deckId) | ✅ | ❌ | ✅ | Was missing |
| progress | ✅ | ✅ | ✅ | |
| achievements | ✅ | ❌ | ✅ | Was missing |
| shop | ✅ | ❌ | ✅ | Was missing |
| subscription | ✅ | ✅ | ✅ | |
| settings | ✅ | ✅ | ✅ | |
| wordDetail(id) | ✅ | ❌ | ✅ | Was missing |
| devChat | ✅ | ❌ | ✅ | Was missing |
| fieldJournal | ✅ | ❌ | ✅ | Was missing |
| calligraphySession | ❌ (Android has no calligraphy) | ✅ iPad only | ✅ iPad only | iOS-exclusive feature |

**Platform Differences (Intentional):**
- Android uses string-based routes (`"kanji/{kanjiId}"`). iOS uses enum with associated values.
- Android `NavType.IntType` / `NavType.LongType` → iOS `Int32` / `Int64` associated values.
- iOS adds `#if IPAD_TARGET` calligraphy route (not on Android).

### AppNavigation

| Aspect | Android | iOS (Old) | iOS (New) | Notes |
|--------|---------|-----------|-----------|-------|
| Start destination | Splash | Login (direct) | Splash | iOS was skipping splash |
| Navigation type | `NavHost` + `composable()` | `NavigationStack` + `.navigationDestination` | `NavigationStack` + `.navigationDestination` | Same pattern |
| Route count | 23+ composable blocks | 7 cases | 23+ cases | Full parity now |
| FeedbackFAB | Global Scaffold overlay | Not present | Global overlay | Ported |
| FeedbackDialog | `hiltViewModel()` scoped | Not present | `@StateObject` scoped | Ported |
| Deep linking | `kanjiquest://collect?kanji_id=X` | Not present | TODO (Phase 5+) | Deferred |
| Post-login routing | Placement test check → Home | Direct to Home | Placement test check → Home | Ported |

**Platform Differences (Intentional):**
- Android `Scaffold(floatingActionButton:)` → iOS `ZStack` with overlay.
- Android `hiltViewModel()` → iOS `@StateObject` with container injection.
- Android `popUpTo(inclusive=true)` → iOS `path = NavigationPath()` (reset stack).
- Deep linking deferred on iOS — Android uses `Uri` parsing, iOS would use `onOpenURL`.

### AppContainer (DI)

| Dependency | Android (Hilt) | iOS (Old) | iOS (New) | Notes |
|------------|----------------|-----------|-----------|-------|
| KanjiQuestDatabase | ✅ Singleton | ✅ | ✅ | Same |
| KanjiRepository | ✅ Singleton | ✅ | ✅ | Same |
| SrsRepository | ✅ Singleton | ✅ | ✅ | Same |
| UserRepository | ✅ Singleton | ✅ | ✅ | Same |
| SessionRepository | ✅ Singleton | ✅ | ✅ | Same |
| AchievementRepository | ✅ Singleton | ✅ | ✅ | Same |
| VocabSrsRepository | ✅ Singleton | ✅ | ✅ | Same |
| AuthRepository | ✅ Singleton | ✅ | ✅ | Same |
| JCoinRepository | ✅ Singleton | ❌ | ✅ | Was missing |
| FlashcardRepository | ✅ Singleton | ❌ | ✅ | Was missing |
| KanaRepository | ✅ Singleton | ❌ | ✅ | Was missing |
| KanaSrsRepository | ✅ Singleton | ❌ | ✅ | Was missing |
| RadicalRepository | ✅ Singleton | ❌ | ✅ | Was missing |
| RadicalSrsRepository | ✅ Singleton | ❌ | ✅ | Was missing |
| CollectionRepository | ✅ Singleton | ❌ | ✅ | Was missing |
| DevChatRepository | ✅ Singleton | ❌ | ✅ | Was missing |
| FeedbackRepository | ✅ Singleton | ❌ | ✅ | Was missing |
| FieldJournalRepository | ✅ Singleton | ❌ | ✅ | Was missing |
| LearningSyncRepository | ✅ Singleton | ❌ | ✅ | Was missing |
| SrsAlgorithm (Sm2) | ✅ Singleton | ✅ | ✅ | Same |
| ScoringEngine | ✅ Singleton | ✅ | ✅ | Same |
| EncounterEngine | ✅ Singleton | ❌ | ✅ | Was missing |
| ItemLevelEngine | ✅ Singleton | ❌ | ✅ | Was missing |
| UserSessionProvider | ✅ | ❌ | ✅ | Was missing |
| WordOfTheDayUseCase | ✅ | ❌ | ✅ | Was missing |
| CompleteSessionUseCase | ✅ | ❌ | ✅ | Was missing |
| QuestionGenerator | ✅ Factory | ✅ (partial, nils) | ✅ (full) | Was passing nil for optional deps |
| KanaQuestionGenerator | ✅ Factory | ❌ | ✅ | Was missing |
| RadicalQuestionGenerator | ✅ Factory | ❌ | ✅ | Was missing |
| GameEngine | ✅ Factory | ✅ (partial) | ✅ (full) | Was missing kana/radical/collection deps |
| PreviewTrialManager | ✅ Singleton (SharedPrefs) | ❌ | ✅ (UserDefaults) | New Swift class |
| GeminiClient | ✅ Singleton | ❌ (CalligraphyFeedbackService only) | ✅ | Shared for writing mode + calligraphy |
| HandwritingChecker | ✅ Singleton | ❌ | ✅ | Was missing |
| AiFeedbackReporter | ✅ Singleton | ❌ | TODO | Deferred to writing mode phase |
| StrokeMatcher | ❌ | ✅ | ✅ Kept | iOS-only (calligraphy) |

**Platform Differences (Intentional):**
- Android uses Hilt `@Provides @Singleton`. iOS uses manual init in `AppContainer`.
- Android `@ApplicationContext context: Context` → iOS `Bundle.main` / `UserDefaults.standard`.
- iOS propagates container via `@EnvironmentObject`. Android uses `@Inject` constructor injection.
- iOS keeps `StrokeMatcher` (calligraphy). Android doesn't have this.

---

## Phase 2: Home Screen

### HomeView

| Section | Android | iOS (Old) | iOS (New) | Notes |
|---------|---------|-----------|-----------|-------|
| Top bar: J Coin button | ✅ Gold "J" + balance | ❌ | ✅ | |
| Top bar: Settings gear | ✅ | ❌ | ✅ | |
| Top bar: Admin badge | ✅ Red/gold level name | ❌ | ✅ | |
| Profile card: Tier (JP) | ✅ `tierNameJp` | ❌ | ✅ | |
| Profile card: Tier + Level | ✅ | ❌ (had basic XP only) | ✅ | |
| Profile card: J Coins | ✅ Clickable → Shop | ❌ | ✅ | |
| Profile card: XP bar | ✅ LinearProgressIndicator | ✅ (basic) | ✅ (full) | |
| Profile card: Next tier | ✅ Conditional | ❌ | ✅ | |
| Upgrade banner | ✅ $4.99/mo for free users | ❌ | ✅ | |
| Progress button | ✅ | ❌ | ✅ | |
| Achievements button | ✅ | ❌ | ✅ | |
| Grade mastery badges | ✅ Horizontal scroll | ❌ | ✅ | |
| Word of the Day | ✅ Card with reading+meaning | ❌ | ✅ | |
| Learning Path cards | ✅ Hiragana/Katakana/Radicals/Grades | ❌ | ✅ | |
| Kana Practice buttons | ✅ Hiragana+Katakana | ❌ | ✅ | |
| Radical modes | ✅ Recognition+Builder | ❌ | ✅ | |
| Kanji study modes (4) | ✅ Recognition/Writing/Vocabulary/Camera | ✅ (Recognition only) | ✅ (all 4) | |
| Preview/trial system | ✅ Daily trials for free users | ❌ | ✅ | |
| Flashcard button | ✅ With deck count | ❌ | ✅ | |
| Collection button | ✅ XX/YY count | ❌ | ✅ | |
| Multi-tab browser | ✅ 4 tabs (Hiragana/Katakana/Radicals/Kanji) | ❌ | ✅ | |
| Kanji sort modes | ✅ 4 modes (Grade/JLPT/Strokes/Freq) | ❌ | ✅ | |
| Kanji filter tabs | ✅ Dynamic per sort mode | ❌ | ✅ | |
| Kanji grid | ✅ 5-col with rarity borders + mode badges | ❌ | ✅ | |
| Kana grid | ✅ 5-col with rarity borders | ❌ | ✅ | |
| Radical grid | ✅ 4-col with images + borders | ❌ | ✅ | |
| `#if IPAD_TARGET` calligraphy | ❌ | ✅ | ✅ | iOS-exclusive |

### HomeViewModel

| State Property | Android | iOS (Old) | iOS (New) | Notes |
|---------------|---------|-----------|-----------|-------|
| profile (UserProfile) | ✅ | ✅ (partial) | ✅ | |
| kanjiCount | ✅ | ✅ | ✅ | |
| coinBalance | ✅ Observable Flow | ❌ | ✅ | |
| wordOfTheDay | ✅ | ❌ | ✅ | |
| effectiveLevel | ✅ | ❌ | ✅ | |
| isPremium / isAdmin | ✅ | ❌ | ✅ | |
| tierName / tierNameJp | ✅ | ❌ | ✅ | |
| displayLevel | ✅ | ❌ | ✅ | |
| nextTierName / nextTierLevel | ✅ | ❌ | ✅ | |
| gradeMasteryList | ✅ | ❌ | ✅ | |
| gradeOneKanji (filtered) | ✅ | ❌ | ✅ | |
| hiraganaList / katakanaList | ✅ | ❌ | ✅ | |
| radicals | ✅ | ❌ | ✅ | |
| selectedMainTab | ✅ | ❌ | ✅ | |
| kanjiSortMode | ✅ | ❌ | ✅ | |
| selectedGrade / jlptLevel / etc. | ✅ | ❌ | ✅ | |
| collectedItems (by type) | ✅ | ❌ | ✅ | |
| kanjiPracticeCounts | ✅ | ❌ | ✅ | |
| kanjiModeStats | ✅ | ❌ | ✅ | |
| flashcardDeckCount | ✅ | ❌ | ✅ | |
| previewTrials | ✅ | ❌ | ✅ | |
| hiraganaProgress / katakanaProgress | ✅ | ❌ | ✅ | |
| radicalProgress | ✅ | ❌ | ✅ | |
| availableStrokeCounts | ✅ | ❌ | ✅ | |
| perGrade/perJlpt counts | ✅ | ❌ | ✅ | |

**Platform Differences (Intentional):**
- Android `Lifecycle.Event.ON_RESUME` refresh → iOS `onAppear` / `scenePhase` change.
- Android `StateFlow` → iOS `@Published`.
- Android `viewModelScope.launch` → iOS `Task { }`.

---

## Phase 3: Game Modes

### Recognition Mode

| Aspect | Android | iOS (Old) | iOS (New) | Notes |
|--------|---------|-----------|-----------|-------|
| File | RecognitionScreen.kt (588 lines) | RecognitionView.swift (378 lines) | RecognitionView.swift (rewritten) | |
| GameEngine integration | ✅ SKIE StateFlow | ✅ SKIE AsyncSequence | ✅ SKIE AsyncSequence | Same pattern |
| Targeted mode | ✅ `targetKanjiId` param | ✅ | ✅ | |
| Discovery overlay | ✅ Shows on new collection | ❌ | ✅ | Was missing |
| Session complete stats | ✅ Full stats card | ✅ (basic) | ✅ (full) | |
| Haptic feedback | ✅ HapticManager | ❌ | ✅ UIImpactFeedbackGenerator | Platform-specific |
| Sound effects | ✅ SoundManager | ❌ | TODO | Deferred |

### Writing Mode

| Aspect | Android | iOS | Notes |
|--------|---------|-----|-------|
| Files | 7 files (WritingScreen, ViewModel, DrawingCanvas, StrokeRenderer, SvgPathRenderer, HandwritingChecker, AiFeedbackReporter) | 7 new files (mirror) | Full port |
| Canvas | Compose Canvas + pointer input | UIViewRepresentable + touch handling | Platform canvas APIs |
| AI checking | GeminiClient → HandwritingChecker | GeminiClient → HandwritingChecker | Same API, Swift wrapper |
| SVG rendering | Custom SvgPathRenderer | Custom SvgPathRenderer (CoreGraphics) | Different rendering APIs |
| Stroke feedback | AiFeedbackReporter (Gemini) | AiFeedbackReporter (Gemini) | Same backend |

**Key iOS Calligraphy Difference:**
- iPad has Apple Pencil calligraphy (11 dedicated files) — this is an iOS-exclusive premium feature.
- Android writing mode uses finger/stylus drawing with AI grading.
- Both platforms share the same Gemini AI backend for handwriting evaluation.

### Vocabulary Mode

| Aspect | Android | iOS | Notes |
|--------|---------|-----|-------|
| Question types | meaning, reading, kanji_fill, sentence | Same (from shared-core) | QuestionGenerator handles this |
| Example sentences | ✅ | ✅ | From shared-core |
| Kanji breakdown | ✅ Shows component meanings | ✅ | From shared-core |

### Kana Modes (Hiragana/Katakana Recognition)

| Aspect | Android | iOS | Notes |
|--------|---------|-----|-------|
| KanaRecognitionScreen | 311 lines | ~300 lines (new) | Direct port |
| Kana type parameter | String enum (HIRAGANA/KATAKANA) | Same | |
| KanaWritingScreen | 280 lines (uses DrawingCanvas) | iPad: reuse calligraphy canvas / iPhone: excluded | Platform-specific |

**Platform Difference (Intentional):**
- Android KanaWriting uses the same DrawingCanvas as kanji writing.
- iOS iPad: `#if IPAD_TARGET` uses CalligraphyCanvasView (Apple Pencil).
- iOS iPhone: KanaWriting excluded (requires stylus for good UX).

### Radical Modes

| Aspect | Android | iOS | Notes |
|--------|---------|-----|-------|
| RadicalRecognition | 294 lines, similar to Recognition | ~280 lines (new) | Direct port |
| RadicalBuilder | 307 lines, "which kanji contains these radicals?" | ~300 lines (new) | Direct port |

### Camera Challenge

| Aspect | Android | iOS | Notes |
|--------|---------|-----|-------|
| Camera API | CameraX (Jetpack) | AVFoundation | Different APIs entirely |
| ML Kit OCR | Google ML Kit (Android) | Google ML Kit (iOS pod) | Same ML model, different SDK |
| FieldJournal | 415 lines, SQLite storage | Same architecture | |

**Platform Difference (Intentional):**
- Camera permission flow differs (Android manifest vs iOS Info.plist).
- Preview rendering: CameraX `PreviewView` vs AVFoundation `AVCaptureVideoPreviewLayer`.

### DiscoveryOverlay

| Aspect | Android | iOS | Notes |
|--------|---------|-----|-------|
| Trigger | New kanji collected during gameplay | Same | |
| Display | Fullscreen overlay with rarity animation | Same | |
| Rarity colors | Inline in composable | Uses KanjiQuestTheme.rarity* | Centralized in iOS |

---

## Phase 4: Supporting Screens

### Splash Screen

| Aspect | Android | iOS (Old) | iOS (New) | Notes |
|--------|---------|-----------|-----------|-------|
| Exists | ✅ 164 lines | ❌ | ✅ | Was missing entirely |
| Animation | Logo fade-in + scale | N/A | Logo fade-in + scale | |
| Duration | ~2s then auto-navigate | N/A | ~2s | |

### Login / Auth

| Aspect | Android | iOS (Old) | iOS (New) | Notes |
|--------|---------|-----------|-----------|-------|
| LoginScreen | 344 lines | 82 lines | ~300 lines | Was too minimal |
| AuthViewModel | 109 lines | 56 lines | ~100 lines | |
| "Continue without account" | ✅ | ❌ | ✅ | Was missing |
| Error display | ✅ Snackbar | ✅ Alert | ✅ Alert | Platform-idiomatic |
| Loading state | ✅ CircularProgressIndicator | ❌ | ✅ ProgressView | |
| App branding | ✅ Logo + tagline | ❌ | ✅ | |

### Progress Screen

| Aspect | Android | iOS (Old) | iOS (New) | Notes |
|--------|---------|-----------|-----------|-------|
| ProgressScreen | 593 lines | 120 lines | ~550 lines | Major expansion |
| ProgressViewModel | 103 lines | N/A (inline) | ~100 lines | Extracted to VM |
| Daily stats calendar | ✅ Heatmap grid | ❌ | ✅ | Was missing |
| Recent sessions list | ✅ | ❌ | ✅ | Was missing |
| Grade mastery breakdown | ✅ Per-grade bars | ✅ (basic) | ✅ (full) | Expanded |
| Streak display | ✅ | ✅ | ✅ | |

### Achievements Screen

| Aspect | Android | iOS | Notes |
|--------|---------|-----|-------|
| AchievementsScreen | 285 lines | ~270 lines (new) | New file |
| AchievementsViewModel | 297 lines | ~280 lines (new) | New file |
| Badge display | Grid of locked/unlocked | Same | |
| Progress bars | Per-achievement | Same | |

### Settings Screen

| Aspect | Android | iOS (Old) | iOS (New) | Notes |
|--------|---------|-----------|-----------|-------|
| SettingsScreen | 630 lines | 49 lines | ~580 lines | Massive expansion |
| SettingsViewModel | 180 lines | N/A | ~170 lines | New file |
| Admin level switcher | ✅ | ✅ (basic) | ✅ (full) | |
| Dev chat link | ✅ | ❌ | ✅ | |
| Retake assessment | ✅ | ❌ | ✅ | |
| Daily goal setting | ✅ | ❌ | ✅ | |
| Account section | ✅ Sign out + delete | ✅ (sign out only) | ✅ (full) | |
| About section | ✅ Version, licenses | ❌ | ✅ | |
| Calligraphy settings | ❌ | ✅ (brush size, ghost strokes) | ✅ (kept, `#if IPAD_TARGET`) | iOS-exclusive |

### Shop Screen

| Aspect | Android | iOS | Notes |
|--------|---------|-----|-------|
| ShopScreen | 537 lines | ~500 lines (new) | New file |
| ShopViewModel | 124 lines | ~120 lines (new) | New file |
| J Coin balance header | ✅ | ✅ | |
| Item categories | ✅ (Theme/Booster/Utility/Content/CrossBusiness) | ✅ | |
| Purchase flow | ✅ Coin deduction + confirmation | ✅ | |
| TutoringJay featured banner | ✅ | ✅ | Cross-business promotion |

### Subscription Screen

| Aspect | Android | iOS (Old) | iOS (New) | Notes |
|--------|---------|-----------|-----------|-------|
| SubscriptionScreen | 222 lines | 98 lines | ~210 lines | Expanded |
| SubscriptionViewModel | 53 lines | N/A | ~50 lines | New file |
| Price | $4.99/mo (Google Play Billing) | $9.99/mo (StoreKit 2) | $4.99/mo (StoreKit 2) | Price was wrong on iOS |
| Feature list | ✅ Detailed | ✅ (basic) | ✅ (detailed) | |
| Current plan display | ✅ | ❌ | ✅ | |

**Platform Difference (Intentional):**
- Android: Google Play Billing Library. iOS: StoreKit 2.
- Different product IDs, different receipt validation.
- iOS subscription price aligned to $4.99/mo to match Android.

### Collection Screen

| Aspect | Android | iOS | Notes |
|--------|---------|-----|-------|
| CollectionScreen | 333 lines | ~320 lines (new) | New file |
| CollectionViewModel | 89 lines | ~85 lines (new) | New file |
| Rarity filter tabs | ✅ | ✅ | |
| Item type filter | ✅ (Kanji/Kana/Radical) | ✅ | |
| Grid with rarity borders | ✅ | ✅ | Uses KanjiQuestTheme.rarity* |

### Flashcard Screens

| Aspect | Android | iOS | Notes |
|--------|---------|-----|-------|
| FlashcardScreen | 380 lines | ~360 lines (new) | Deck management |
| FlashcardViewModel | 144 lines | ~140 lines (new) | |
| FlashcardStudyScreen | 331 lines | ~320 lines (new) | Study session |
| FlashcardStudyViewModel | 121 lines | ~115 lines (new) | |
| Card flip animation | ✅ 3D rotation | ✅ rotation3DEffect | Platform-specific animation API |

### Placement Test

| Aspect | Android | iOS | Notes |
|--------|---------|-----|-------|
| PlacementTestScreen | 503 lines | ~480 lines (new) | New file |
| PlacementTestViewModel | 363 lines | ~350 lines (new) | New file |
| Adaptive difficulty | ✅ | ✅ | Shared-core algorithm |
| Post-test level assignment | ✅ | ✅ | |

---

## Phase 5: Detail & Auxiliary Screens

### KanjiDetail

| Aspect | Android | iOS | Notes |
|--------|---------|-----|-------|
| KanjiDetailScreen | 410 lines | ~390 lines (new) | |
| KanjiDetailViewModel | 206 lines | ~200 lines (new) | |
| Stroke order SVG | ✅ | ✅ (CoreGraphics) | Different SVG rendering |
| Readings (on/kun) | ✅ | ✅ | |
| Example words | ✅ | ✅ | |
| Practice buttons | ✅ Writing + Camera | ✅ Writing + Camera + Calligraphy (iPad) | iOS adds calligraphy |
| Collection info | ✅ Rarity, level, XP | ✅ | |

### RadicalDetail

| Aspect | Android | iOS | Notes |
|--------|---------|-----|-------|
| RadicalDetailScreen | 195 lines | ~185 lines (new) | |
| RadicalDetailViewModel | 65 lines | ~60 lines (new) | |
| Kanji containing radical | ✅ Grid of kanji | ✅ | |

### WordDetail

| Aspect | Android | iOS | Notes |
|--------|---------|-----|-------|
| WordDetailScreen | 203 lines | ~195 lines (new) | |
| WordDetailViewModel | 52 lines | ~50 lines (new) | |
| Example sentence | ✅ JP + EN | ✅ | |
| Component kanji | ✅ Clickable kanji | ✅ | |

### DevChat

| Aspect | Android | iOS | Notes |
|--------|---------|-----|-------|
| DevChatScreen | 272 lines | ~260 lines (new) | |
| DevChatViewModel | 147 lines | ~140 lines (new) | |
| Message polling | ✅ 10s interval | ✅ 10s interval | Same backend |

### Feedback System

| Aspect | Android | iOS | Notes |
|--------|---------|-----|-------|
| FeedbackFAB | 27 lines | ~25 lines (new) | Floating action button |
| FeedbackDialog | 277 lines | ~260 lines (new) | Modal sheet on iOS |
| FeedbackViewModel | 244 lines | ~230 lines (new) | |

**Platform Difference (Intentional):**
- Android: Material FAB + Dialog. iOS: overlay button + `.sheet` modal.

### Camera Challenge & Field Journal

| Aspect | Android | iOS | Notes |
|--------|---------|-----|-------|
| CameraChallengeScreen | 553 lines | ~500 lines (new) | AVFoundation vs CameraX |
| CameraChallengeViewModel | 216 lines | ~200 lines (new) | |
| FieldJournalScreen | 415 lines | ~400 lines (new) | |
| FieldJournalViewModel | 63 lines | ~60 lines (new) | |
| ML Kit | Google ML Kit Android SDK | Google ML Kit iOS Pod | Same model |
| Camera preview | CameraX PreviewView | AVCaptureVideoPreviewLayer | Platform-specific |

---

## iOS-Exclusive Features (Not on Android)

| Feature | Files | Notes |
|---------|-------|-------|
| Apple Pencil calligraphy | 11 files in Calligraphy/ | iPad-only, pressure-sensitive 書道 mode |
| Gemini AI calligraphy grading | CalligraphyFeedbackService.swift | 5-aspect evaluation (Balance, Stroke Order, Endings, Pressure, Flow) |
| Stroke ending detection | StrokeEndingDetector.swift | 止め/はね/はらい classification |
| Pressure analysis | PressureAnalyzer.swift | Apple Pencil pressure metrics |
| Brush engine | BrushEngine.swift | Simulates traditional brush behavior |
| `#if IPAD_TARGET` gating | Throughout codebase | Compile-time iPhone exclusion |
| Dual target (iPad + iPhone) | project.yml + project-iphone.yml | Separate build targets |

---

## Android-Exclusive Features (Not on iOS)

| Feature | Files | Notes |
|---------|-------|-------|
| HapticManager | audio/HapticManager.kt | Android vibration patterns (iOS uses UIFeedbackGenerator inline) |
| SoundManager | audio/SoundManager.kt | Sound effects (deferred on iOS) |
| CoinSyncWorker | workers/CoinSyncWorker.kt | WorkManager background sync (iOS uses BGTaskScheduler — TODO) |
| LearningSyncWorker | workers/LearningSyncWorker.kt | WorkManager (iOS TODO) |
| FeedbackFCMService | service/FeedbackFCMService.kt | Firebase Cloud Messaging (iOS uses APNs — TODO) |
| OllamaClient | network/OllamaClient.kt | Local LLM inference (desktop/server only) |
| Deep linking | KanjiQuestNavHost.kt (L68-111) | `kanjiquest://collect?kanji_id=X` (iOS TODO) |

---

## Known Simplifications in iOS Port

1. **No Hilt/Dagger** — Manual DI via AppContainer (acceptable for project size)
2. **No WorkManager** — Background sync deferred; will use BGTaskScheduler later
3. **No FCM** — Push notifications deferred; will use APNs later
4. **No deep linking** — URL scheme handling deferred
5. **No sound effects** — SoundManager deferred
6. **Single dark mode approach** — Tokens defined but `@Environment(\.colorScheme)` applied per-view vs Android's global theme wrapper

---

## File Mapping Quick Reference

| Android Path | iOS Path | Status |
|-------------|----------|--------|
| `di/AppModule.kt` | `DI/AppContainer.swift` | ✅ Expanded |
| `ui/theme/Theme.kt` | `Theme/KanjiQuestTheme.swift` | ✅ Rewritten |
| `ui/theme/KanjiText.kt` | `Theme/KanjiText.swift` | ✅ Created |
| `ui/components/AssetImage.kt` | `Components/AssetImage.swift` | ✅ Created |
| `ui/navigation/NavRoutes.kt` | `Navigation/NavRoutes.swift` | ✅ Created |
| `ui/navigation/KanjiQuestNavHost.kt` | `Navigation/AppNavigation.swift` | ✅ Rewritten |
| `ui/splash/SplashScreen.kt` | `Screens/Splash/SplashView.swift` | ⏳ Pending |
| `ui/auth/LoginScreen.kt` | `Screens/Auth/LoginView.swift` | ⏳ Pending |
| `ui/auth/AuthViewModel.kt` | `Screens/Auth/AuthViewModel.swift` | ⏳ Pending |
| `ui/home/HomeScreen.kt` | `Screens/Home/HomeView.swift` | ✅ Rewritten |
| `ui/home/HomeViewModel.kt` | `Screens/Home/HomeViewModel.swift` | ✅ Rewritten |
| `ui/game/RecognitionScreen.kt` | `Screens/Game/RecognitionView.swift` | ✅ Rewritten |
| `ui/game/RecognitionViewModel.kt` | `Screens/Game/RecognitionViewModel.swift` | ✅ Rewritten |
| `ui/game/writing/WritingScreen.kt` | `Screens/Game/Writing/WritingView.swift` | ✅ Rewritten |
| `ui/game/writing/WritingViewModel.kt` | `Screens/Game/Writing/WritingViewModel.swift` | ✅ Rewritten |
| `ui/game/writing/DrawingCanvas.kt` | `Screens/Game/Writing/DrawingCanvas.swift` | ✅ Rewritten |
| `ui/game/writing/StrokeRenderer.kt` | `Screens/Game/Writing/WritingStrokeRenderer.swift` | ✅ Created |
| `ui/game/writing/SvgPathRenderer.kt` | (merged into DrawingCanvas.swift) | ✅ Merged |
| `ui/game/writing/HandwritingChecker.kt` | `Screens/Game/Writing/HandwritingChecker.swift` | ✅ Created |
| `ui/game/writing/AiFeedbackReporter.kt` | `Screens/Game/Writing/AiFeedbackReporter.swift` | ✅ Updated |
| `ui/game/vocabulary/VocabularyScreen.kt` | `Screens/Game/VocabularyView.swift` | ✅ Created |
| `ui/game/vocabulary/VocabularyViewModel.kt` | `Screens/Game/VocabularyViewModel.swift` | ✅ Created |
| `ui/game/kana/KanaRecognitionScreen.kt` | `Screens/Game/Kana/KanaRecognitionView.swift` | ✅ Created |
| `ui/game/kana/KanaRecognitionViewModel.kt` | `Screens/Game/Kana/KanaRecognitionViewModel.swift` | ✅ Created |
| `ui/game/radical/RadicalRecognitionScreen.kt` | `Screens/Game/Radical/RadicalRecognitionView.swift` | ✅ Created |
| `ui/game/radical/RadicalRecognitionViewModel.kt` | `Screens/Game/Radical/RadicalRecognitionViewModel.swift` | ✅ Created |
| `ui/game/radical/RadicalBuilderScreen.kt` | `Screens/Game/Radical/RadicalBuilderView.swift` | ✅ Created |
| `ui/game/radical/RadicalBuilderViewModel.kt` | `Screens/Game/Radical/RadicalBuilderViewModel.swift` | ✅ Created |
| `ui/game/DiscoveryOverlay.kt` | `Components/DiscoveryOverlay.swift` | ✅ Updated |
| `ui/progress/ProgressScreen.kt` | `Screens/Progress/ProgressView.swift` | ⏳ Pending |
| `ui/progress/ProgressViewModel.kt` | `Screens/Progress/ProgressViewModel.swift` | ⏳ Pending |
| `ui/achievements/AchievementsScreen.kt` | `Screens/Achievements/AchievementsView.swift` | ⏳ Pending |
| `ui/achievements/AchievementsViewModel.kt` | `Screens/Achievements/AchievementsViewModel.swift` | ⏳ Pending |
| `ui/settings/SettingsScreen.kt` | `Screens/Settings/SettingsView.swift` | ⏳ Pending |
| `ui/settings/SettingsViewModel.kt` | `Screens/Settings/SettingsViewModel.swift` | ⏳ Pending |
| `ui/shop/ShopScreen.kt` | `Screens/Shop/ShopView.swift` | ⏳ Pending |
| `ui/shop/ShopViewModel.kt` | `Screens/Shop/ShopViewModel.swift` | ⏳ Pending |
| `ui/subscription/SubscriptionScreen.kt` | `Screens/Subscription/SubscriptionView.swift` | ⏳ Pending |
| `ui/subscription/SubscriptionViewModel.kt` | `Screens/Subscription/SubscriptionViewModel.swift` | ⏳ Pending |
| `ui/collection/CollectionScreen.kt` | `Screens/Collection/CollectionView.swift` | ⏳ Pending |
| `ui/collection/CollectionViewModel.kt` | `Screens/Collection/CollectionViewModel.swift` | ⏳ Pending |
| `ui/flashcard/FlashcardScreen.kt` | `Screens/Flashcard/FlashcardView.swift` | ⏳ Pending |
| `ui/flashcard/FlashcardViewModel.kt` | `Screens/Flashcard/FlashcardViewModel.swift` | ⏳ Pending |
| `ui/flashcard/FlashcardStudyScreen.kt` | `Screens/Flashcard/FlashcardStudyView.swift` | ⏳ Pending |
| `ui/flashcard/FlashcardStudyViewModel.kt` | `Screens/Flashcard/FlashcardStudyViewModel.swift` | ⏳ Pending |
| `ui/placement/PlacementTestScreen.kt` | `Screens/Placement/PlacementTestView.swift` | ⏳ Pending |
| `ui/placement/PlacementTestViewModel.kt` | `Screens/Placement/PlacementTestViewModel.swift` | ⏳ Pending |
| `ui/detail/KanjiDetailScreen.kt` | `Screens/Detail/KanjiDetailView.swift` | ⏳ Pending |
| `ui/detail/KanjiDetailViewModel.kt` | `Screens/Detail/KanjiDetailViewModel.swift` | ⏳ Pending |
| `ui/detail/RadicalDetailScreen.kt` | `Screens/Detail/RadicalDetailView.swift` | ⏳ Pending |
| `ui/detail/RadicalDetailViewModel.kt` | `Screens/Detail/RadicalDetailViewModel.swift` | ⏳ Pending |
| `ui/worddetail/WordDetailScreen.kt` | `Screens/Detail/WordDetailView.swift` | ⏳ Pending |
| `ui/worddetail/WordDetailViewModel.kt` | `Screens/Detail/WordDetailViewModel.swift` | ⏳ Pending |
| `ui/devchat/DevChatScreen.kt` | `Screens/DevChat/DevChatView.swift` | ⏳ Pending |
| `ui/devchat/DevChatViewModel.kt` | `Screens/DevChat/DevChatViewModel.swift` | ⏳ Pending |
| `ui/feedback/FeedbackFAB.kt` | `Components/FeedbackFAB.swift` | ⏳ Pending |
| `ui/feedback/FeedbackDialog.kt` | `Components/FeedbackDialog.swift` | ⏳ Pending |
| `ui/feedback/FeedbackViewModel.kt` | `Components/FeedbackViewModel.swift` | ⏳ Pending |
| `ui/game/camera/CameraChallengeScreen.kt` | `Screens/Game/Camera/CameraChallengeView.swift` | ⏳ Pending |
| `ui/game/camera/CameraChallengeViewModel.kt` | `Screens/Game/Camera/CameraChallengeViewModel.swift` | ⏳ Pending |
| `ui/game/camera/FieldJournalScreen.kt` | `Screens/Game/Camera/FieldJournalView.swift` | ⏳ Pending |
| `ui/game/camera/FieldJournalViewModel.kt` | `Screens/Game/Camera/FieldJournalViewModel.swift` | ⏳ Pending |
| `data/PreviewTrialManager.kt` | `Data/PreviewTrialManager.swift` | ✅ Created |
| N/A (iOS-exclusive) | `Calligraphy/Canvas/*.swift` (4 files) | ✅ Kept |
| N/A (iOS-exclusive) | `Calligraphy/Feedback/*.swift` (4 files) | ✅ Kept |
| N/A (iOS-exclusive) | `Screens/Calligraphy/*.swift` (3 files) | ✅ Kept |
