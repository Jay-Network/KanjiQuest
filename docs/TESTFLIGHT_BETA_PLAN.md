# KanjiQuest iPad - TestFlight Beta Testing Plan

## Overview

2-week beta testing program for KanjiQuest iPad (書道 Calligraphy Edition) before App Store submission. Tests Apple Pencil calligraphy, AI feedback, KMP shared-core integration, and subscription flow.

## Timeline

| Week | Phase | Focus |
|------|-------|-------|
| Day 0 | Build uploaded | First TestFlight build available |
| Days 1-3 | Internal beta | JWorks team (10 testers) |
| Days 4-10 | External beta | Public testers (15-25) |
| Days 11-13 | Fix & polish | Address critical bugs |
| Day 14 | Final build | Release candidate uploaded |

## Beta Tester Groups

### Internal Testers (10 max)

Internal testers are added via App Store Connect and get builds instantly (no Apple review required).

| Role | Count | Focus Area |
|------|-------|------------|
| Jay (developer) | 1 | Full feature coverage |
| JWorks team | 2-3 | General UX, Japanese accuracy |
| TutoringJay tutors | 2-3 | Educational effectiveness, grade-level accuracy |
| KanjiQuest Android users | 2-3 | Cross-platform consistency, feature parity |

**How to add internal testers:**
1. App Store Connect > Users and Access
2. Add user with "App Manager" or "Developer" role
3. They appear automatically in TestFlight > Internal Testing

### External Testers (15-25)

External testers require an Apple review of the first build (~24-48 hours). After approval, new builds distribute automatically.

| Segment | Count | Recruitment Source |
|---------|-------|--------------------|
| Japanese learners (intermediate+) | 8-10 | TutoringJay students, Reddit r/LearnJapanese |
| Calligraphy enthusiasts | 3-5 | iPad art communities, r/Calligraphy |
| iPad power users | 3-5 | TestFlight forums, social media |
| Accessibility testers | 1-2 | VoiceOver/Dynamic Type users |

**How to add external testers:**
1. App Store Connect > TestFlight > External Testing
2. Create group: "KanjiQuest Beta"
3. Add testers by email (they get a TestFlight invitation)
4. Or share a public link (up to 10,000 testers)

**Recruitment message:**
> Test KanjiQuest - a kanji calligraphy app for iPad with Apple Pencil. Practice 書道 with AI-powered feedback on your brush technique. Free during beta. Join: [TestFlight link]

## Testing Checklist

### Priority 1: Core Calligraphy (Must Pass)

- [ ] **Apple Pencil drawing**: Strokes render smoothly at 60fps
- [ ] **Pressure sensitivity**: Line width varies with pencil pressure
- [ ] **Ghost strokes**: Reference stroke order shows as gray overlay
- [ ] **Stroke matching**: Correct/incorrect detection works for 一, 大, 学, 漢
- [ ] **Auto-submit**: Triggers when drawn stroke count matches expected
- [ ] **Manual submit**: Submit button works before reaching stroke count
- [ ] **Clear/Undo**: Canvas reset and single-stroke undo work
- [ ] **XP calculation**: Points awarded after submission, combo tracked

### Priority 2: AI Feedback (Must Pass)

- [ ] **Gemini API call**: AI feedback loads after submission (may take 2-5 seconds)
- [ ] **5-aspect evaluation**: Balance, stroke order, endings, pressure, movement all scored
- [ ] **Japanese text**: AI response includes Japanese terms (筆圧, 運筆, etc.)
- [ ] **Error handling**: Graceful fallback if Gemini API is unreachable
- [ ] **Loading indicator**: Spinner shows while AI is processing

### Priority 3: Session Flow (Must Pass)

- [ ] **Session start**: GameEngine starts with WRITING mode
- [ ] **Next kanji**: Advances to next SRS card after result
- [ ] **Session complete**: Stats overlay shows after all questions
- [ ] **Stats accuracy**: Cards studied, correct count, combo, XP, duration correct
- [ ] **SRS update**: Spaced repetition interval updates in database

### Priority 4: Navigation & Auth (Must Pass)

- [ ] **Login screen**: Email/password fields, sign in/up buttons
- [ ] **Supabase auth**: Can create account and log in
- [ ] **Home screen**: Level, XP bar, practice button visible
- [ ] **Navigation**: All screens reachable (Home, Calligraphy, Progress, Settings)
- [ ] **Sign out**: Returns to login screen
- [ ] **Persistent session**: Stays logged in after app kill/relaunch

### Priority 5: Device Compatibility (Should Pass)

- [ ] **iPad Pro 12.9"**: Full canvas utilization, no layout overflow
- [ ] **iPad Pro 11"**: Correct scaling
- [ ] **iPad Air**: Acceptable performance
- [ ] **iPad mini**: Layout adapts to smaller screen
- [ ] **Apple Pencil 2nd gen**: Pressure + tilt both detected
- [ ] **Apple Pencil 1st gen**: Pressure works, tilt gracefully degraded
- [ ] **Finger drawing**: Falls back to fixed pressure (no Apple Pencil)
- [ ] **iPadOS 16**: Minimum supported version works
- [ ] **iPadOS 17+**: No deprecation warnings or crashes

### Priority 6: Polish & Edge Cases (Nice to Have)

- [ ] **Dark mode**: All screens render correctly
- [ ] **Dynamic Type**: Text scales with system font size
- [ ] **Rotation**: Landscape and portrait both work
- [ ] **Multitasking**: Split view doesn't crash
- [ ] **Memory**: No leaks during extended sessions (10+ kanji)
- [ ] **Offline mode**: Core features work without internet (except AI feedback)
- [ ] **Subscription screen**: Premium tier UI displays correctly
- [ ] **Settings**: Brush size, ghost strokes toggle, AI toggle all persist

## Bug Reporting Process

### For Internal Testers

1. **In-app**: Screenshot > TestFlight automatically attaches device info
2. **TestFlight app**: Open TestFlight > KanjiQuest > Send Beta Feedback
3. **Direct**: Message in the TutoringJay Slack #kanjiquest-beta channel

### For External Testers

1. **TestFlight feedback**: Built-in screenshot + text feedback
2. **Form**: Google Form link included in TestFlight description
3. **Email**: kanjiquest-beta@jworks.com

### Bug Report Template

```
Device: iPad Pro 12.9" (6th gen)
iPadOS: 17.4
Pencil: Apple Pencil 2nd gen
Build: 1.0 (build 5)

What happened:
[Description]

What I expected:
[Description]

Steps to reproduce:
1. ...
2. ...
3. ...

Screenshot/Recording: [attached]
```

### Bug Severity Levels

| Severity | Definition | Response Time |
|----------|-----------|---------------|
| Critical | App crash, data loss, auth broken | Fix within 24 hours |
| High | Feature doesn't work, wrong results | Fix within 48 hours |
| Medium | UI glitch, minor wrong behavior | Fix in next build |
| Low | Cosmetic, enhancement request | Backlog for post-launch |

## TestFlight Build Distribution

### Build Naming Convention

```
Version: 1.0.0
Build:   1  (increment per TestFlight upload)
```

| Build | Date | Notes |
|-------|------|-------|
| 1 | Day 0 | First internal beta |
| 2 | Day 3 | Internal bug fixes |
| 3 | Day 4 | First external beta (after Apple review) |
| 4 | Day 8 | External bug fixes |
| 5 | Day 14 | Release candidate |

### What's New Notes

Each build should include brief release notes in TestFlight:

```
Build 3 - First External Beta
- Calligraphy canvas with Apple Pencil pressure support
- AI feedback on brush technique (5 aspects)
- 10-kanji practice sessions with SRS
- XP tracking and progress stats
Known issues: [list any known bugs]
```

## Success Criteria

The beta is successful and ready for App Store submission when:

- [ ] Zero critical bugs open
- [ ] Zero high bugs open for 48+ hours
- [ ] At least 80% of Priority 1-4 checklist items pass
- [ ] At least 5 external testers have submitted feedback
- [ ] Average session length > 3 minutes (users are engaged)
- [ ] Crash-free rate > 99% (TestFlight analytics)
- [ ] AI feedback successfully returns for > 95% of submissions

## Post-Beta Actions

1. **Fix remaining medium/low bugs** from feedback
2. **Prepare App Store listing**: Screenshots, description, keywords
3. **App Store review prep**: Privacy policy, data collection disclosure
4. **Submit for review**: App Store Connect > Submit for Review
5. **Pricing**: Set $9.99/mo subscription in App Store Connect (Phase 3)

## Tools & Links

- TestFlight analytics: App Store Connect > TestFlight > Builds > Analytics
- Crash reports: App Store Connect > TestFlight > Crashes
- Feedback inbox: App Store Connect > TestFlight > Feedback
- GitHub Actions builds: github.com/Jay-Network/KanjiQuest/actions
