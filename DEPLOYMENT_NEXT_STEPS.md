# KanjiQuest Deployment - Coordinated Next Steps

**Date:** 2026-02-07
**Status:** Phase 2 - Implementation Coordination

## Team Status Summary

### ✅ Complete
- **jworks:35** - Backend subscription system (Migration 013, 3 Edge Functions)
- **jworks:25** - BJL cross-business J Coin UI (9 tasks, 10 files)
- **jworks:43** - KanjiLens monetization + strategy (34 tests pass)

### 🔄 In Progress
- **jworks:44** - KanjiQuest Android app (Stripe configured, auth pending)
- **tutoringjay:0** - Portal integration (cross-promo done, monetization pending)

## Monetization Decisions (Final)

### Pricing Model: Freemium Subscription
- **Free Tier**: Recognition mode only, 10 cards/day, no J Coin earning
- **Premium Tier**: $9.99/mo, all modes, unlimited, J Coin earning + 100 coin monthly bonus
- **Annual Option**: $99.99/year (17% discount) - to be added later

### Distribution Model
- **APK Hosting**: Portal infrastructure (Supabase Storage bucket 'apps')
- **Download Auth**: Gated behind TutoringJay student login
- **Checkout Flow**: Stripe checkout on portal (unified billing)
- **Download Tokens**: Generated via app-download-token Edge Function (jworks:35)

### Technical Architecture
```
Student Login → Portal /apps → Stripe Checkout (Premium) → Download APK →
App Launch → Auth with Portal → Check Subscription → Feature Gating
```

## Next Steps by Agent

### tutoringjay:0 (Portal Integration) - Priority 1

**Phase 2: Subscription & Download Pages**

1. **Create /apps/kanjiquest page**
   - App description, screenshots, features
   - Pricing tiers comparison table
   - "Download Free" button (requires login)
   - "Upgrade to Premium" button → Stripe checkout

2. **Stripe Checkout Integration**
   - Create Stripe Checkout session for $9.99/mo Premium
   - Product: "KanjiQuest Premium"
   - Price ID: (jworks:43 will provide)
   - Success URL: /apps/kanjiquest/download?session_id={CHECKOUT_SESSION_ID}
   - Cancel URL: /apps/kanjiquest

3. **Download Page (/apps/kanjiquest/download)**
   - Verify login (redirect to /login if not authenticated)
   - Call app-download-token Edge Function (jworks:35)
   - Generate signed download URL from Supabase Storage
   - Track download event (analytics)
   - Show installation instructions

4. **APK Upload Infrastructure**
   - Create Supabase Storage bucket 'apps' (if not exists)
   - Upload signed KanjiQuest APK (jworks:44 will provide)
   - Set bucket policy: private (download via signed URLs only)

**Blockers to resolve:**
- Need Stripe Price ID for $9.99/mo Premium (jworks:43 action)
- Need signed APK file (jworks:44 action)

### jworks:44 (KanjiQuest Android) - Priority 1

**Phase 2-9: Auth, Subscription, Feature Gating**

**Phase 2: Authentication Foundation**
1. Create AuthRepository with Supabase GoTrue client (TutoringJay project)
2. Add login/logout functions
3. Store auth token in SharedPreferences
4. Add AuthViewModel

**Phase 3: Login Screen**
1. Create LoginScreen composable
2. Email + password fields
3. "Continue as Guest" option (free tier, no J Coin)
4. Deep link to portal login if no account
5. Update NavHost startDestination to Login

**Phase 4: Subscription Status Integration**
1. Create SubscriptionRepository
2. Call app-subscription-status Edge Function on app launch
3. Parse subscription JSONB (free_tier_limits, premium_features)
4. Cache subscription status locally
5. Refresh daily or on resume

**Phase 5: Feature Gating Logic**
1. Update HomeViewModel to check subscription tier
2. Gate Writing, Vocabulary, Camera modes behind premium
3. Show "[PRO]" label on locked modes
4. Redirect to SubscriptionScreen on locked mode click

**Phase 6: Subscription Screen UI**
1. Create SubscriptionScreen composable (already exists per system reminder)
2. Pricing tiers comparison
3. "Upgrade on Portal" button → deep link to portal /apps/kanjiquest
4. "Manage Subscription" button → portal /account/subscriptions

**Phase 7: J Coin Integration with Subscription**
1. Update coin-earn calls to respect subscription tier
2. Block earning for free tier users
3. Award 100 coin monthly bonus for premium users
4. Show "Upgrade to earn J Coins" prompt in shop

**Phase 8: Daily Limit Enforcement (Free Tier)**
1. Track cards studied per day in local database
2. Show "X/10 cards studied today" progress bar (free users)
3. Block new sessions when limit reached
4. Show upgrade prompt on limit hit

**Phase 9: Deep Link Handling**
1. Handle kanjiquest://subscription deep link (already configured per system reminder)
2. Parse return from portal Stripe checkout
3. Refresh subscription status on return
4. Show success message

**Deliverables:**
- Signed APK for portal upload (debug for testing, release for production)
- local.properties updated with AUTH_SUPABASE_URL and AUTH_SUPABASE_ANON_KEY (already done per system reminder)

### jworks:43 (Strategy/Stripe Setup) - Priority 2

**Actions:**
1. **Create Stripe Price ID for $9.99/mo Premium**
   - Log into Stripe Dashboard
   - Create Product: "KanjiQuest Premium"
   - Create Price: $9.99/mo recurring
   - Copy Price ID (e.g., price_xxxxxxxxxxxxx)
   - Send Price ID to tutoringjay:0 and jworks:44

2. **Update Database**
   - Run SQL: `UPDATE app_subscription_tiers SET stripe_price_id = 'price_xxxxxxxxxxxxx' WHERE tier_name = 'premium' AND app_id = (SELECT id FROM app_catalog WHERE app_name = 'KanjiQuest');`

3. **Configure Webhook Endpoint**
   - Add webhook endpoint in Stripe: https://[SUPABASE_URL]/functions/v1/app-subscription-webhook
   - Select events: checkout.session.completed, customer.subscription.created, customer.subscription.updated, customer.subscription.deleted
   - Copy webhook signing secret
   - Share with jworks:35 for Edge Function config

### jworks:25 (BJL Portal) - Priority 3

**Actions:**
1. **Dev Server Visual QA**
   - Test /jcoin page rendering
   - Verify business filter tabs work
   - Check tier progress display
   - Test dashboard widget

2. **Verify Edge Function**
   - Test coin-redemptions Edge Function accepts X-BJL-API-Key
   - Ensure cross-business transactions display correctly

3. **Update Marketing Copy**
   - Replace placeholder copy with final KanjiQuest messaging
   - Ensure 10 coins/session, 50/day cap is accurate

### jworks:35 (Backend) - Priority 3

**Actions:**
1. **Create Supabase Storage Bucket**
   - Bucket name: 'apps'
   - Policy: private (signed URLs only)
   - Update app-download-token Edge Function to use bucket instead of fallback download_url

2. **Webhook Secret Configuration**
   - Receive webhook signing secret from jworks:43
   - Update app-subscription-webhook Edge Function config
   - Test webhook handling with Stripe CLI

3. **Testing**
   - Test app-subscription-status with real user IDs
   - Verify premium_features JSONB parsing
   - Test monthly coin bonus award (trigger manually)

## Shared Blockers & Dependencies

### Critical Path (Blocking Launch)
1. **Stripe Price ID** (jworks:43) → Blocks portal checkout (tutoringjay:0)
2. **Signed APK** (jworks:44) → Blocks portal download (tutoringjay:0)
3. **Auth Foundation** (jworks:44) → Blocks feature gating
4. **Supabase Storage Bucket** (jworks:35) → Blocks download tokens

### Non-Blocking (Can Be Done in Parallel)
- BJL UI QA (jworks:25)
- Marketing copy updates (jworks:25)
- Webhook configuration (jworks:43 + jworks:35)

## Timeline (Updated)

### Week 1 (Feb 7-13) - Auth & Stripe Setup
- **Day 1-2**: jworks:43 creates Stripe Price ID, jworks:44 implements auth foundation
- **Day 3-4**: jworks:44 builds login screen + subscription status integration
- **Day 5-7**: jworks:44 implements feature gating, tutoringjay:0 builds /apps pages

### Week 2 (Feb 14-20) - Portal Integration
- **Day 1-3**: tutoringjay:0 completes Stripe checkout + download flow
- **Day 4-5**: jworks:35 creates Storage bucket, uploads APK
- **Day 6-7**: End-to-end testing (login → checkout → download → feature gating)

### Week 3 (Feb 21-27) - Testing & Polish
- **Day 1-3**: Bug fixes, edge case testing (expired subs, offline mode)
- **Day 4-5**: Student beta testing (10-20 TutoringJay students)
- **Day 6-7**: Gather feedback, critical fixes

### Week 4 (Feb 28 - Mar 6) - Soft Launch
- **Day 1**: Announce to all TutoringJay students via email
- **Day 2-7**: Monitor metrics (downloads, conversions, support queries)

### Week 5+ - Iteration & Play Store
- **Ongoing**: Feature improvements based on feedback
- **Week 6-8**: Prepare Play Store listing (optional, portal-first approach)

## Success Metrics

### Week 1 Goals
- [ ] Auth flow functional in app
- [ ] Stripe Price ID created and configured
- [ ] Portal /apps/kanjiquest page live

### Week 2 Goals
- [ ] End-to-end flow tested (login → checkout → download → launch → feature gating)
- [ ] 5 TutoringJay students successfully download and use app

### Week 3 Goals
- [ ] Beta test with 20 students
- [ ] 4.0+ average rating from beta testers
- [ ] <5 critical bugs reported

### Week 4 Goals (Soft Launch)
- [ ] 50+ downloads from TutoringJay students
- [ ] 10+ premium subscriptions ($90 MRR)
- [ ] <10% churn rate

## Communication Protocol

### Daily Standups (Async via tmux)
Each agent posts daily update:
- What I completed yesterday
- What I'm working on today
- Any blockers

### Blocker Escalation
If blocked >4 hours:
1. Send tmux message to jworks:42 (4_Apps coordinator)
2. Tag relevant agent who can unblock
3. Propose workaround if available

### Integration Points (Require Sync)
- jworks:43 → jworks:44: Stripe Price ID handoff
- jworks:44 → tutoringjay:0: Signed APK handoff
- jworks:35 → tutoringjay:0: app-download-token API usage
- jworks:43 → jworks:35: Webhook secret handoff

## Current Priorities (Next 24 Hours)

**Immediate (Today):**
1. **jworks:43**: Create Stripe Price ID for $9.99/mo → Send to team
2. **jworks:44**: Start Phase 2 (Auth foundation) - create AuthRepository
3. **jworks:35**: Create Supabase Storage bucket 'apps'

**Tomorrow:**
1. **jworks:44**: Build LoginScreen, integrate auth
2. **tutoringjay:0**: Start /apps/kanjiquest page with pricing tiers
3. **jworks:25**: Complete BJL UI QA

## Questions for Jay

1. **Beta Test Group**: Should we limit to specific TutoringJay student group or open to all?
2. **Support Channel**: Discord, email, or in-app support form for student queries?
3. **Pricing Adjustment**: Stick with $9.99/mo or test $14.99/mo (higher margin)?
4. **Annual Discount**: Offer $99.99/year (17% off) immediately or wait for Month 2?

---

**Next Review:** Feb 8 (tomorrow) - Check Stripe Price ID creation and auth foundation progress
**Coordinator:** jworks:42 (4_Apps)
