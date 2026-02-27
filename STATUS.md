# KanjiQuest Development Tracker

> **Updated by**: jworks:44 (KanjiQuest agent)
> **Last updated**: 2026-02-25

---

## Current Status

- **Version**: 0.1.0-beta5 (Android: beta13, iPad: build 18, iPhone: build 2)
- **Platforms**: Android (Kotlin) + iOS/iPad (SwiftUI via KMP + SKIE)
- **Build**: Passing (both platforms)
- **Branch**: main
- **Stage**: Beta testing (4 students)

---

## Feature Parity Matrix

| Feature | Android | iOS/iPad |
|---------|:-------:|:--------:|
| **Core Gameplay** | | |
| Recognition Mode (kanji quiz) | DONE | DONE |
| Writing Mode + AI feedback (Gemini) | DONE | DONE |
| Vocabulary Mode (4 question types) | DONE | IN PROGRESS |
| Camera Challenge (OCR scanning) | DONE | - |
| Kana Recognition & Writing | DONE | - |
| Radical Recognition & Builder | DONE | - |
| Placement Test | DONE | - |
| **Collection System** | | |
| Catch mechanic (probability + pity) | DONE | DONE |
| 5-tier rarity system | DONE | DONE |
| Item level engine (1-10, XP) | DONE | DONE |
| Discovery overlay animation | DONE | IN PROGRESS |
| Collection screen (grid, filters) | DONE | DONE |
| **Progression** | | |
| Flashcard SRS (Again/Hard/Good/Easy) | DONE | IN PROGRESS |
| Spaced repetition scheduling | DONE | IN PROGRESS |
| XP + J Coin system | DONE | DONE |
| Level progression (N²×50 XP) | DONE | DONE |
| **Features** | | |
| Home screen (mode cards, tier, coins) | DONE | IN PROGRESS |
| Field Journal (camera history) | DONE | - |
| Adaptive difficulty + mastery badges | DONE | - |
| Feedback system | DONE | - |
| Developer chat (Discord via n8n) | DONE | - |
| KanjiLens deep link integration | DONE | N/A |
| **Premium** | | |
| Stripe subscription ($4.99/mo) | DONE | - |
| J Coin shop (boosters) | DONE | - |
| Admin level switcher | DONE | DONE |
| **Navigation** | | |
| Bottom navigation tabs | DONE | DONE |
| Study screen | DONE | DONE |
| Games screen | DONE | DONE |
| Test Mode screen | DONE | DONE |

**Legend**: DONE | IN PROGRESS | - (not started) | N/A

---

## Current Sprint

### Android
- **Current work**: Beta monitoring, bug fixes from student feedback
- **Next**: Polish based on beta feedback
- **Blockers**: Supabase storage decision (APK 66MB > free tier 50MB)

### iOS/iPad
- **Current work**: Feature porting (Study, Games, Collection screens)
- **Next**: Camera Challenge, Kana modes
- **Blockers**: KMP bridging issues (KotlinBoolean/KotlinInt conversions, mostly resolved)

---

## Tech Stack

| Component | Android | iOS/iPad |
|-----------|---------|----------|
| Language | Kotlin | Swift + KMP bridge |
| UI | Jetpack Compose | SwiftUI + UIKit (canvas) |
| Shared code | KMP (shared-core, shared-japanese, shared-tokenizer) | Same via SKIE 0.10.10 |
| Database | SQLDelight | SQLDelight (via KMP) |
| Auth | Dual Supabase (TutoringJay + KanjiQuest) | Same |
| AI | Gemini 2.5 Flash | Same |
| Payment | Stripe | Stripe (planned) |
| DI | Hilt | Manual |

---

## Economy

| Parameter | Value |
|-----------|-------|
| XP per recognition | 15 base |
| XP per writing | 20 base (quality tiered) |
| Combo multiplier | 1.0x-2.0x (streaks 0-10+) |
| J Coins per session | 10 |
| Daily coin cap | 50 |
| Monthly bonus | 100 |
| Premium price | $4.99/mo |
