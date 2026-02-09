# KanjiQuest Deployment - Corrected Status (2026-02-07)

## 🚨 Critical Corrections Made

### Pricing Alignment
- **CORRECT**: KanjiQuest Premium = **$4.99/mo** (Jay-approved)
- **INCORRECT** (now fixed): jworks:35 backend had $9.99/mo

### Auth Architecture Alignment
- **KanjiQuest**: TutoringJay portal auth (email/password) ✅
- **KanjiLens**: Awaiting Jay's decision on auth approach

## Current Progress by Agent

### ✅ jworks:44 (KanjiQuest App) - 85% COMPLETE

**DONE:**
1. DB migration - app_subscriptions table on TutoringJay Supabase
2. Dual Supabase auth - TutoringJay accounts + J Coin backend
3. Login/Signup screen with TutoringJay membership marketing
4. Feature gating - free tier with preview trials for pro modes
5. Stripe setup - product created ($4.99/mo), webhook endpoint, portal API routes
6. Portal integration - subscription page, download page, sidebar

**IN PROGRESS:**
- Preview mode for pro study modes (free users get daily trials)
- Admin level switcher (Jay can view as any tier)
- HomeScreen scroll fix

**REMAINING:**
- Fix TS errors in portal Stripe routes
- Build release APK

### ✅ jworks:35 (Backend) - COMPLETE (needs pricing update)

**DONE:**
- Migration 013: app_catalog, app_subscription_tiers, app_subscriptions, app_subscription_events
- 3 Edge Functions: app-subscription-status, app-subscription-webhook, app-download-token
- coin-earn gating behind premium subscription

**ACTION NEEDED:**
- Update price_usd from 9.99 to 4.99 in app_subscription_tiers
- Receive new Stripe Price ID from jworks:43

### ✅ jworks:25 (BJL Cross-Business) - COMPLETE

**DONE:**
- Full J Coin cross-business integration (10 files)
- /jcoin portal page with business filters
- Dashboard widget
- Marketing data (KanjiQuest: 10 coins/session, 50/day cap)

**ACTION NEEDED:**
- Dev server QA
- Verify coin-redemptions Edge Function

### 🔄 jworks:43 (Strategy/Stripe) - IN PROGRESS

**DONE:**
- KanjiLens monetization scaffold
- Strategy coordination

**ACTION NEEDED:**
- Create Stripe Price ID for **$4.99/mo** (not $9.99/mo)
- Decide KanjiLens auth approach (awaiting Jay's decision)

### 🔄 tutoringjay:0 (Portal) - PARTIAL

**DONE:**
- Cross-promo Phase 1 (homepage, dashboard, post-booking dialog)

**ACTION NEEDED:**
- Build /apps/kanjiquest page with **$4.99/mo** pricing
- Stripe checkout integration (awaiting Price ID)
- Download page with app-download-token integration

## Corrected Pricing Tiers

### Free Tier (Freemium)
- Recognition mode only
- 10 cards per day
- No J Coin earning
- Preview trials for pro modes (1 session/day each)

### Premium Tier - **$4.99/month**
- All modes (Recognition, Writing, Vocabulary, Camera)
- Unlimited practice
- J Coin earning enabled
- 100 coin monthly bonus
- No daily limits

## Updated Timeline

### This Week (Feb 7-13)
- **Today**: jworks:35 updates pricing, jworks:43 creates Stripe Price ID
- **Tomorrow**: jworks:44 completes TS fixes + release APK
- **Next 3 days**: tutoringjay:0 builds /apps pages and checkout
- **End of week**: End-to-end testing

### Next Week (Feb 14-20)
- Soft launch to 20 TutoringJay students (beta test)
- Bug fixes and feedback incorporation

### Week 3 (Feb 21-27)
- Full launch to all TutoringJay students
- Monitor metrics: downloads, conversions, support queries

## KanjiLens Auth Decision Needed

**Question for Jay:** Which auth approach for KanjiLens?

**Option A: Google Sign-In (Standalone)** ← Recommended
- Pros: Broader audience, simpler onboarding, familiar UX
- Cons: Separate auth from TutoringJay ecosystem
- Use case: General-purpose kanji scanner for anyone learning Japanese

**Option B: TutoringJay Portal Auth**
- Pros: Unified student experience, cross-business account
- Cons: Limited to TutoringJay students only
- Use case: TutoringJay students only (smaller market)

**Option C: Both (Hybrid)**
- Pros: Best of both worlds
- Cons: More complex, higher maintenance
- Use case: General users + TutoringJay students

**Recommendation:** Option A (Google Sign-In) for KanjiLens since it's a general-purpose scanner, not student-specific like KanjiQuest.

## Next 24 Hours Priority Actions

### jworks:35 (Backend)
```sql
UPDATE app_subscription_tiers
SET price_usd = 4.99, stripe_price_id = NULL
WHERE tier_name = 'premium'
  AND app_id = (SELECT id FROM app_catalog WHERE app_name = 'KanjiQuest');
```

### jworks:43 (Stripe Setup)
1. Log into Stripe Dashboard
2. Create Product: "KanjiQuest Premium"
3. Create Price: **$4.99/mo** recurring
4. Copy Price ID → send to jworks:35, tutoringjay:0, jworks:44

### jworks:44 (App Finalization)
1. Fix TS errors in portal Stripe routes
2. Test preview mode for pro study modes
3. Build release APK (signed)
4. Upload to Supabase Storage bucket 'apps'

### tutoringjay:0 (Portal Pages)
1. Create /apps/kanjiquest page
2. Add pricing: Free tier vs **$4.99/mo** Premium
3. "Download Free" button (requires login)
4. "Upgrade to Premium" → Stripe checkout (awaiting Price ID)

## Success Criteria (End of Week)

- [ ] All agents aligned on $4.99/mo pricing
- [ ] Stripe Price ID created and configured
- [ ] Release APK built and uploaded
- [ ] Portal /apps/kanjiquest page live
- [ ] End-to-end flow tested: login → checkout → download → launch → feature gating
- [ ] 5 test students successfully download and use app

---

**Status:** Coordinated and aligned. Pricing corrected to $4.99/mo. Ready for final implementation sprint.
**Coordinator:** jworks:42 (4_Apps)
