# KanjiQuest QA Testing Checklist

## Section 22: Kana Recognition Mode

- [ ] **22.1** Hiragana recognition: Launch → answer 10 questions → XP awarded
- [ ] **22.2** Katakana recognition: Launch → answer 10 questions → XP awarded
- [ ] **22.3** Correct answers show green highlight + XP gained animation
- [ ] **22.4** Incorrect answers show red highlight + correct answer highlighted
- [ ] **22.5** Progress bar advances with each question (1/10 → 10/10)
- [ ] **22.6** Combo counter shows for consecutive correct answers (2x, 3x...)
- [ ] **22.7** Session complete screen shows cards studied, correct count, XP earned
- [ ] **22.8** SRS cards created/updated after session (check DB)
- [ ] **22.9** Distractors are from same kana group when possible (k-row for k-row)
- [ ] **22.10** Hiragana uses pink accent (#E91E63), Katakana uses cyan (#00BCD4)
- [ ] **22.11** Back button returns to Home without crash
- [ ] **22.12** Sound effects play on correct/incorrect/session complete

## Section 23: Kana Writing Mode

- [ ] **23.1** Hiragana writing: Launch → canvas appears with romanization prompt
- [ ] **23.2** Katakana writing: Launch → canvas appears with romanization prompt
- [ ] **23.3** Drawing canvas accepts touch input, strokes render correctly
- [ ] **23.4** AI feedback shows after stroke submission (via Gemini/Ollama)
- [ ] **23.5** Correct/incorrect feedback with sound and haptics
- [ ] **23.6** Session complete shows XP earned and accuracy
- [ ] **23.7** SRS cards updated after session
- [ ] **23.8** "Clear" button resets canvas for retry
- [ ] **23.9** Back button returns to Home without crash
- [ ] **23.10** Writing mode delegates to existing WritingScreen correctly

## Section 24: Radical Recognition Mode

- [ ] **24.1** Launch → answer 10 questions about radical meanings → XP awarded
- [ ] **24.2** Radical character displayed at 80sp in card
- [ ] **24.3** 4 meaning choices shown in 2x2 grid
- [ ] **24.4** Correct answers highlight green, incorrect highlight red
- [ ] **24.5** Combo counter and XP tracker visible in header
- [ ] **24.6** Session complete shows stats (cards, correct, XP)
- [ ] **24.7** Brown accent color (#795548) used throughout
- [ ] **24.8** SRS cards created/updated in radical_srs_card table
- [ ] **24.9** Back button returns to Home
- [ ] **24.10** Sound effects and haptics on correct/incorrect

## Section 25: Radical Builder Mode

- [ ] **25.1** Launch → radical composition prompt displayed (e.g., "氵+ 日")
- [ ] **25.2** 4 kanji choices shown in 2x2 grid (large 32sp text)
- [ ] **25.3** Correct kanji contains all shown radicals
- [ ] **25.4** Distractors are partial matches (contain some but not all radicals)
- [ ] **25.5** Correct answer shows green, incorrect shows red + reveals correct answer
- [ ] **25.6** "Which kanji contains these radicals?" prompt displayed
- [ ] **25.7** Session complete with full stats
- [ ] **25.8** Premium-gated: free users see preview trial or upgrade prompt
- [ ] **25.9** Higher XP than recognition (18/14/10 for quality 5/4/3)
- [ ] **25.10** Combo multiplier works correctly
- [ ] **25.11** Back button returns to Home
- [ ] **25.12** Sound effects and haptics on correct/incorrect

## Section 26: Expanded Placement Test

- [ ] **26.1** New users see "Japanese Placement Test" intro (not "Kanji Assessment")
- [ ] **26.2** Stage 1 (Hiragana): 5 questions - "What is the reading?" with romanization choices
- [ ] **26.3** Stage 2 (Katakana): 5 questions - "What is the reading?" with romanization choices
- [ ] **26.4** Stage 3 (Radicals): 5 questions - "What does this radical mean?" with meaning choices
- [ ] **26.5** Stages 4-9 (Grade 1-6): Existing kanji questions unchanged
- [ ] **26.6** Pass 4/5 to advance to next stage; fail = stop and assign level
- [ ] **26.7** Level assignments: Fail hiragana=1 (Newcomer), pass hiragana=2 (Script Learner), pass katakana=3 (Foundation), pass radicals=4 (Beginner), pass Grade 1=5 (Novice), etc.
- [ ] **26.8** Results screen shows all attempted stages with PASS/FAIL

## Section 27: Learning Path Progression

- [ ] **27.1** Home screen shows "Learning Path" horizontal scroll with progress cards
- [ ] **27.2** Hiragana/Katakana/Radical progress bars update after sessions
- [ ] **27.3** "Kana Practice" section shows Hiragana + Katakana recognition buttons (always free)
- [ ] **27.4** Radical section shows Recognition (free) + Builder (premium/preview) buttons
- [ ] **27.5** "Kanji Study Modes" section preserves existing 4-mode layout
- [ ] **27.6** LevelProgression tiers include Newcomer (L1), Script Learner (L2), Foundation (L3)

## Summary

| Section | Tests | Status |
|---------|-------|--------|
| 22. Kana Recognition | 12 | Pending |
| 23. Kana Writing | 10 | Pending |
| 24. Radical Recognition | 10 | Pending |
| 25. Radical Builder | 12 | Pending |
| 26. Expanded Placement Test | 8 | Pending |
| 27. Learning Path Progression | 6 | Pending |
| **Total New** | **58** | |
