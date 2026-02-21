# KanjiQuest: XP & J Coin Allocation Plan

**Version:** 1.1
**Last Updated:** 2026-02-21
**Status:** Active (reviewed for child-friendliness)

---

## 1. XP Awards Per Game Mode

Each question awards XP based on answer quality. Quality tiers map to SRS response ratings.

| Game Mode | Q5 (Perfect) | Q4 (Good) | Q3 (Pass) | Q2 (Attempt) | Q1 (Skip) |
|-----------|:---:|:---:|:---:|:---:|:---:|
| Recognition (Kanji) | 15 | 12 | 8 | 2 | 0 |
| Writing + AI Feedback | 20 | 15 | 10 | 2 | 0 |
| Kana Writing | 20 | 15 | 10 | 2 | 0 |
| Radical Recognition | 10 | 8 | 5 | 1 | 0 |
| Radical Builder | 18 | 14 | 10 | 2 | 0 |
| Kana Recognition | 10 | 8 | 5 | 1 | 0 |
| Vocabulary | 15 | 12 | 8 | 2 | 0 |
| Camera Challenge | 15 | 12 | 8 | 2 | 0 |

**Design rationale:**
- Writing awards the most XP because it requires the hardest skill (production vs recognition)
- Kana and Radical Recognition award less because these are simpler building blocks
- Radical Builder awards more than Recognition (18 vs 10) because it requires combining knowledge
- **Q2 (Attempt) awards 1-2 XP** to teach children that trying is better than skipping. This is deliberately small (vs 5-15 for correct) to prevent gaming while keeping effort visible.
- Q1 (Skip/no attempt) awards 0 XP

---

## 2. Combo & Bonus Multipliers

### Streak Combos

Consecutive correct answers build a combo multiplier:

| Streak Length | Multiplier | Example (15 XP base) |
|:---:|:---:|:---:|
| 0-2 | 1.0x | 15 XP |
| 3-4 | 1.2x | 18 XP |
| 5-9 | 1.5x | 22 XP |
| 10+ | 2.0x | 30 XP |

**Combo degradation (child-friendly):** An incorrect answer drops the combo by one tier instead of resetting to zero. Example: a 10-streak drops to 5-9 tier (1.5x) on one wrong answer. A second consecutive wrong answer drops to 3-4 tier (1.2x). This prevents the frustration of losing a long streak to a single mistake.

### New Card Bonus

First-time study of a new kanji/radical/kana: **1.5x** multiplier (stacks with combo).

Example: New card at 10-streak = 15 base x 2.0 combo x 1.5 new = **45 XP**

### Accuracy Bonus (End of Session)

Applied to total session XP at completion:

| Accuracy | Min Cards | Bonus |
|:---:|:---:|:---:|
| 90%+ | 10 | +25% XP |
| 85%+ | 5 | +15% XP |
| < 85% | any | +0% |

### Daily Welcome Bonus

Opening the app and starting any study session: **+5 XP** (once per day). This encourages daily engagement and gives children an immediate reward for showing up.

### Booster Items (Premium)

| Booster | Multiplier | Duration | Cost |
|---------|:---:|:---:|:---:|
| Double XP | 2.0x | 24 hours | 150 J Coins |
| Triple XP | 3.0x | 12 hours | 250 J Coins |

Booster multipliers apply to base XP only (before combo). Boosters do NOT affect SRS intervals or learning pace.

---

## 3. Level Progression

### Formula

```
XP required for level N = N^2 x 50
```

### Full Level Table (1-99)

| Level | XP Required | Cumulative XP | Tier |
|:---:|---:|---:|---|
| 1 | 50 | 50 | Newcomer |
| 2 | 200 | 250 | Script Learner |
| 3 | 450 | 700 | Script Learner |
| 4 | 800 | 1,500 | Foundation |
| 5 | 1,250 | 2,750 | Novice |
| 6 | 1,800 | 4,550 | Novice |
| 7 | 2,450 | 7,000 | Novice |
| 8 | 3,200 | 10,200 | Novice |
| 9 | 4,050 | 14,250 | Novice |
| 10 | 5,000 | 19,250 | Apprentice |
| 11 | 6,050 | 25,300 | Apprentice |
| 12 | 7,200 | 32,500 | Apprentice |
| 13 | 8,450 | 40,950 | Apprentice |
| 14 | 9,800 | 50,750 | Apprentice |
| 15 | 11,250 | 62,000 | Intermediate |
| 16 | 12,800 | 74,800 | Intermediate |
| 17 | 14,450 | 89,250 | Intermediate |
| 18 | 16,200 | 105,450 | Intermediate |
| 19 | 18,050 | 123,500 | Intermediate |
| 20 | 20,000 | 143,500 | Advanced |
| 21-24 | 22,050-28,800 | 165,550-229,100 | Advanced |
| 25-29 | 31,250-42,050 | 260,350-378,350 | Scholar |
| 30-36 | 45,000-64,800 | 423,350-720,950 | Expert |
| 37-50 | 68,450-125,000 | 789,400-1,905,750 | Master |
| 51-99 | 130,050-490,050 | 2,035,800-16,417,500 | Master |

### Tier Names (matches codebase LevelProgression.kt)

| Levels | Tier | Description |
|--------|------|-------------|
| 1 | Newcomer | First steps |
| 2-3 | Script Learner | Learning kana |
| 4 | Foundation | Building basics |
| 5-9 | Novice | Active learner |
| 10-14 | Apprentice | Growing skill |
| 15-19 | Intermediate | Solid progress |
| 20-24 | Advanced | Strong ability |
| 25-29 | Scholar | Deep knowledge |
| 30-36 | Expert | Near mastery |
| 37+ | Master | Kanji master |

### Time-to-Level Estimates

Assuming 20 cards/session, 15 average XP/card (with combos), 1 session/day:

| Level | Daily XP | Days | Calendar |
|:---:|:---:|:---:|---|
| 1 | ~300 | 1 | Day 1 (first session!) |
| 5 | ~300 | ~9 | ~1.5 weeks |
| 10 | ~300 | ~64 | ~2 months |
| 20 | ~300 | ~478 | ~16 months |

**Design note:** Level 1 is achievable in a single session (50 XP = ~4 correct answers). This gives children an immediate win on their very first day.

---

## 4. Content Unlocks by Level

| Level | Tier | Unlock |
|:---:|---|---|
| 1 | Newcomer | Recognition mode, Hiragana, Priority 1 radicals |
| 2 | Script Learner | Katakana recognition |
| 3 | Script Learner | Writing mode, Kana Writing |
| 4 | Foundation | Grade 1 kanji, Radical Builder mode |
| 5 | Novice | Grade 1-2 kanji, Vocabulary mode, Camera Challenge (preview: 1/day) |
| 8 | Novice | Camera Challenge (unlimited for premium) |
| 10 | Apprentice | Grade 1-3 kanji, Priority 2 radicals |
| 15 | Intermediate | Grade 1-4 kanji |
| 20 | Advanced | Grade 1-5 kanji |
| 25 | Scholar | Grade 1-6 kanji, JLPT N3 content, Priority 3 radicals |
| 30 | Expert | JLPT N2 content |
| 37 | Master | JLPT N1 content |

**Child-friendly notes:**
- New modes unlock gradually to avoid overwhelming young learners
- Camera Challenge unlocks as a preview at level 5 (children love cameras), with full access at level 8
- Priority 1 radicals (口 mouth, 木 tree, 日 sun) are available from day 1 so children see recognizable components
- Grade 1 kanji don't unlock until level 4, giving children time to master kana first

---

## 5. J Coin Earning Rules

J Coins are the cross-business currency ($0.01 each). **Premium subscribers only** earn full J Coins. Free users earn a small trickle (see Section 8).

### Per-Session Earnings

| Trigger | Coins | Condition |
|---------|:---:|---|
| Session complete (large) | 10 | 20+ cards studied |
| Session complete (medium) | 5 | 10-19 cards studied |
| Session complete (small) | 2 | 5-9 cards studied |
| Session complete (tiny) | 0 | < 5 cards |
| Perfect quiz | 25 | 100% accuracy AND 5+ cards |

**Child-friendly change:** The minimum for earning coins was lowered from 10 to 5 cards, and a small "small session" tier was added at 2 coins. This respects shorter attention spans in young children (ages 6-8) who may only study 5-8 cards per session. The perfect quiz threshold was also lowered from 10 to 5 cards.

### Streak Milestones

| Streak | First Time | Repeat |
|:---:|:---:|:---:|
| 7-day | 50 coins | 25 coins |
| 30-day | 300 coins | 100 coins |
| 90-day | 500 coins | 200 coins |

**Streak forgiveness:** Milestones are repeatable. A child who loses a streak due to illness or vacation can rebuild it and earn again, at a reduced rate to prevent cycling exploits.

### Level-Up Milestones (NOT YET IMPLEMENTED)

| Level Reached | Coins | Status |
|:---:|:---:|---|
| Every level-up | 10 | Planned |
| Level 10 | +50 bonus | Planned |
| Level 25 | +100 bonus | Planned |
| Level 50 | +200 bonus | Planned |

### Kanji Mastery Milestones (NOT YET IMPLEMENTED)

| Milestone | Coins | Status |
|-----------|:---:|---|
| First 100 kanji mastered | 200 | Planned |
| First 500 kanji mastered | 1,000 | Planned |
| First 2,000 kanji mastered | 5,000 | Planned |

### Earning Caps

| Type | Cap | Period |
|------|:---:|:---:|
| Engagement sources | 200 coins | per month |
| Money-backed | uncapped | - |
| Milestones (level-up, streak, mastery) | uncapped | - |

**Partial awards:** If a player is at 195/200 and earns 10 coins, they receive 5 (the remaining capacity). No earnings are silently lost.

---

## 6. J Coin Economy Balance

### Monthly Earning Projections

**Young Learner** (1 short session/day, ~7 cards):
- Session coins: 2/day x 30 = 60 coins/month
- Occasional 7-day streak: ~25 coins/month
- **Total: ~85 coins/month**

**Casual Player** (1 session/day, 15 cards):
- Session coins: 5/day x 30 = 150 coins/month
- Occasional perfect: ~25 coins/month
- **Total: ~175 coins/month**

**Active Player** (2 sessions/day, 25 cards):
- Session coins: 10 x 2 x 30 = 600 (capped at 200)
- **Total: ~200 coins/month** (hits cap)

### Time-to-Earn for Shop Items

| Item | Cost | Young Learner | Casual | Active |
|------|:---:|:---:|:---:|:---:|
| Hint Pack (10) | 50 | 0.6 mo | 0.3 mo | 0.25 mo |
| SRS Freeze (3-day) | 50 | 0.6 mo | 0.3 mo | 0.25 mo |
| Double XP (24h) | 150 | 1.8 mo | 0.9 mo | 0.75 mo |
| Theme | 200 | 2.4 mo | 1.1 mo | 1.0 mo |
| Custom Avatar | 300 | 3.5 mo | 1.7 mo | 1.5 mo |
| TutoringJay $5 Credit | 500 | 5.9 mo | 2.9 mo | 2.5 mo |
| Kanken Kanji Set | 750 | 8.8 mo | 4.3 mo | 3.75 mo |
| TutoringJay Trial Lesson | 1,000 | 11.8 mo | 5.7 mo | 5.0 mo |

---

## 7. Shop Item Pricing

### Themes (Cosmetic)

| Item | Price | Rationale |
|------|:---:|---|
| Sakura Theme | 200 | ~1 month of play, purely cosmetic reward |
| Ocean Theme | 200 | Same tier as Sakura |
| Forest Theme | 200 | Same tier |
| Night Theme | 200 | Same tier |
| Autumn Theme | 200 | Same tier |

### Boosters (Temporary Power-ups)

| Item | Price | Rationale |
|------|:---:|---|
| Double XP (24h) | 150 | Affordable for occasional use, encourages intensive study days |
| Triple XP (12h) | 250 | Premium but shorter, for power sessions |

### Utilities

| Item | Price | Rationale |
|------|:---:|---|
| SRS Freeze (3-day) | 50 | Affordable streak protection for short breaks |
| SRS Freeze (7-day) | 100 | Vacation protection, ~2-3 weeks of casual play |
| Hint Pack (10) | 50 | Low barrier entry, helps stuck learners |
| Hint Pack (50) | 200 | Bulk discount (~4 coins each vs 5) |

### Content

| Item | Price | Rationale |
|------|:---:|---|
| Kanken Kanji Set | 750 | Premium content, ~4 months of play |
| Custom Avatar | 300 | Fun customization, about 1.5 months |

### Cross-Business

| Item | Price | Rationale |
|------|:---:|---|
| TutoringJay $5 Credit | 500 | Real monetary value, requires genuine engagement |
| TutoringJay Trial Lesson | 1,000 | Achievable in ~5 months for active players (reduced from 2,000) |

---

## 8. Earning Caps & Sustainability

### Why Caps Exist

Without caps, a bot or power-grinder could earn unlimited coins with monetary value. The 200 coins/month cap ensures:
- **Sustainability:** Maximum coin liability per user = $2/month
- **Fairness:** Casual and hardcore players converge within 1 month
- **Real value:** Coins earned represent genuine learning effort

### Cap Rules

1. **Engagement sources** (session_complete, perfect_quiz): 200 coins/month
2. **Money-backed sources** (purchases, referrals, subscription bonuses): Uncapped
3. **Milestone sources** (level_up, streak_milestone, mastery_milestone): Uncapped
4. **Partial awards:** Near-cap requests get partial amounts

### Free vs Premium

| Feature | Free | Premium ($4.99/mo) |
|---------|------|---------------------|
| XP earning | Full | Full |
| J Coin earning | 50/month cap (trickle) | 200/month cap |
| Daily trials (Writing) | 3/day | Unlimited |
| Daily trials (Vocabulary) | 3/day | Unlimited |
| Daily trials (Camera) | 1/day | Unlimited |
| Shop access | Hints only | Full |
| Themes | Default | All purchasable |

**Child-friendly note:** Free users earn a small trickle of J Coins (50/month cap) so children can save up for a hint pack after their first month. This prevents the demoralizing experience of seeing a currency they can never earn. The core learning experience (XP, levels, SRS) is identical for free and premium.

---

## 9. Adaptive Difficulty Impact on XP

### Mastery Formula

```
masteryScore = coverage x 0.4 + averageAccuracy x 0.6
```

Where:
- **coverage** = fraction of grade's kanji studied (0.0-1.0)
- **averageAccuracy** = correct answers / total reviews (0.0-1.0)

### Mastery Levels

| Level | Score | New Card Ratio | Effect |
|-------|:---:|:---:|---|
| Beginning | < 0.50 | 0% new | Focus on review, solidify basics |
| Developing | 0.50-0.69 | 10% new | Slow introduction of new material |
| Proficient | 0.70-0.84 | 20% new | Balanced new + review |
| Advanced | 0.85+ | 40% new | Rapid new content, minimal review |

### Impact on XP Rate

A player with **Advanced** mastery earns more XP per session because:
1. More new cards (40%) = more 1.5x new card bonuses
2. Higher accuracy = more Q5/Q4 responses = more base XP
3. Consistent accuracy = accuracy bonus (+15-25% at session end)

A **Beginning** player earns less XP per session but:
1. All cards are review = steady practice without overwhelm
2. Gradually improving accuracy increases XP naturally
3. Reaching Developing mastery unlocks new cards = fresh motivation

**This is intentionally fair:** Struggling learners don't fall behind forever. The system slows down to match their pace, and XP scales with genuine improvement rather than speed.

### Demotion System

If a player's accuracy on their highest unlocked grade drops below 40% over 30+ reviews, the system demotes them to the previous grade. This prevents advancing too fast and struggling with content that's too hard. XP is preserved (no XP loss on demotion).

---

## 10. Engagement & Fairness Principles

### For Young Learners (Children)

1. **Instant first win:** Level 1 requires only 50 XP (~4 correct answers). A child levels up in their first session.
2. **Participation XP:** Q2 (wrong but attempted) awards 1-2 XP. Trying is always better than skipping.
3. **Gentle combo:** Wrong answers degrade the combo by one tier, not reset to zero. A 10-streak drops to 1.5x, not 1.0x.
4. **Daily welcome:** +5 XP just for starting a session. Showing up is rewarded.
5. **Short session rewards:** 5-9 cards earns 2 J Coins. Children with shorter attention spans still earn.
6. **Priority radicals:** Children see common, recognizable components first (口 mouth, 木 tree, 日 sun).
7. **No time pressure:** All modes are untimed. Children can think as long as they need.
8. **Camera preview:** Camera Challenge available as a daily preview from level 5 (children love cameras).
9. **Repeatable streaks:** Losing a streak doesn't permanently lose rewards. Children can rebuild and earn again.
10. **Free coin trickle:** Even free users earn 50 coins/month so children see the economy working.

### Early Milestones (Micro-Achievements)

| Milestone | XP Bonus | When |
|-----------|:---:|---|
| First hiragana learned! | +10 XP | Complete first kana session |
| First kanji recognized! | +15 XP | Get first kanji correct |
| First writing attempt! | +10 XP | Complete first writing question |
| 10 kanji studied! | +25 XP | Study 10 unique kanji |
| First perfect session! | +50 XP | 100% accuracy in any session |

These micro-achievements provide early dopamine hits before the full progression system kicks in.

### Preventing Unfair Advantages

1. **200 coins/month cap:** No player can buy their way ahead through grinding
2. **Boosters don't affect SRS:** Boosters increase XP but not SRS intervals. A boosted player levels up faster but doesn't actually learn kanji faster.
3. **Accuracy-based rewards:** The accuracy bonus requires genuine performance, not just volume
4. **Adaptive difficulty:** The system meets each learner where they are

### Streak Forgiveness

- **SRS Freeze (3-day):** 50 coins. Short break protection.
- **SRS Freeze (7-day):** 100 coins. Vacation protection.
- **No streak punishment:** Missing a day resets the streak counter but doesn't affect SRS cards, level, or XP.
- **Repeatable milestones:** Streak milestones can be earned again after a break (at reduced rates).

---

## Appendix: Quick Reference

### XP Multiplier Stack

```
Final XP = base_xp x combo_mult x new_card_mult x booster_mult
Session bonus = session_total_xp x accuracy_bonus
```

All multipliers use integer truncation (`.toInt()`) after calculation.

### Monthly J Coin Summary

| Source | Max/Month | Type |
|--------|:---:|---|
| Session completions | 200 (capped) | Engagement |
| Perfect quizzes | (within 200 cap) | Engagement |
| Streak milestones | Uncapped | Milestone |
| Level-ups (planned) | Uncapped | Milestone |
| Mastery milestones (planned) | Uncapped | Milestone |
| **Realistic total (premium)** | **~200-300** | |
| **Realistic total (free)** | **~50** | |

### Implementation Status

| Feature | Status |
|---------|--------|
| Base XP per mode | Implemented |
| Combo multipliers | Implemented |
| New card bonus | Implemented |
| Accuracy bonus | Implemented |
| Level formula (N^2 x 50) | Implemented |
| Session coin awards | Implemented |
| Perfect quiz coins | Implemented |
| Streak milestone coins | Implemented |
| Q2 participation XP | **Needs implementation** |
| Combo degradation (vs reset) | **Needs implementation** |
| Daily welcome bonus | **Needs implementation** |
| Level-up coin awards | **Needs implementation** |
| Mastery milestone coins | **Needs implementation** |
| Free user coin trickle | **Needs implementation** |
| Small session (5-9 cards) tier | **Needs implementation** |
| Repeatable streak milestones | **Needs implementation** |
| Micro-achievements | **Needs implementation** |
| Booster application to scoring | **Needs implementation** |

### KRADFILE Attribution

Radical decomposition data is sourced from KRADFILE by Michael Raine, James Breen, and the Electronic Dictionary Research & Development Group (EDRDG). Licensed under CC BY-SA 3.0.
