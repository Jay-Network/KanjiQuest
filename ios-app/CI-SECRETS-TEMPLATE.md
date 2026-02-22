# KanjiQuest iOS - CI Secrets Template

**For Jay to fill in and configure in GitHub Actions.**

Go to: `github.com/Jay-Network/KanjiQuest/settings/secrets/actions`

---

## App Secrets (4)

| Secret Name | Value | Notes |
|-------------|-------|-------|
| `SUPABASE_URL` | `https://__________.supabase.co` | From supabase.com dashboard |
| `SUPABASE_ANON_KEY` | `eyJ___...` | Supabase > Settings > API > anon/public |
| `SUPABASE_SERVICE_ROLE_KEY` | `eyJ___...` | Supabase > Settings > API > service_role |
| `GEMINI_API_KEY` | `AIza___...` | From aistudio.google.com |

---

## Apple Developer Signing (4)

| Secret Name | Value | How to Get |
|-------------|-------|------------|
| `APPLE_CERTIFICATE_BASE64` | (long base64 string) | See instructions below |
| `APPLE_CERTIFICATE_PASSWORD` | `__________` | Password you set when exporting .p12 |
| `APPLE_PROVISIONING_PROFILE_IPAD_BASE64` | (long base64 string) | iPad App Store profile (see below) |
| `APPLE_PROVISIONING_PROFILE_IPHONE_BASE64` | (long base64 string) | iPhone App Store profile (see below) |

### Creating APPLE_CERTIFICATE_BASE64

On a Mac:
```bash
# 1. Open Keychain Access
# 2. Request certificate from CA (Certificate Assistant menu)
# 3. Upload CSR to developer.apple.com > Certificates > Distribution
# 4. Download .cer, double-click to install
# 5. Export as .p12 from Keychain (right-click > Export, set password)
# 6. Base64 encode:
base64 -i certificate.p12 | pbcopy
# Paste the clipboard content into the secret
```

### Creating Provisioning Profiles (2 required)

```bash
# iPad profile:
# 1. developer.apple.com > Profiles > New
# 2. Type: App Store Distribution
# 3. App ID: com.jworks.kanjiquest.ipad
# 4. Select your distribution certificate
# 5. Download .mobileprovision file
# 6. Base64 encode:
base64 -i KanjiQuest_iPad_AppStore.mobileprovision | pbcopy
# Paste into APPLE_PROVISIONING_PROFILE_IPAD_BASE64

# iPhone profile:
# 1. developer.apple.com > Profiles > New
# 2. Type: App Store Distribution
# 3. App ID: com.jworks.kanjiquest.iphone
# 4. Select your distribution certificate
# 5. Download .mobileprovision file
# 6. Base64 encode:
base64 -i KanjiQuest_iPhone_AppStore.mobileprovision | pbcopy
# Paste into APPLE_PROVISIONING_PROFILE_IPHONE_BASE64
```

---

## App Store Connect API (3)

| Secret Name | Value | How to Get |
|-------------|-------|------------|
| `APPLE_API_KEY_ID` | `__________` | App Store Connect > Users > Integrations > Keys |
| `APPLE_API_ISSUER_ID` | `__________` | Same page as above (top of page) |
| `APPLE_API_KEY_BASE64` | (base64 of .p8 file) | Downloaded when creating key |

### Creating App Store Connect API Key

1. Go to: `appstoreconnect.apple.com`
2. Users and Access > Integrations > App Store Connect API
3. Click "+" to generate a new key
4. Role: **App Manager** (minimum needed)
5. Save:
   - **Key ID** → `APPLE_API_KEY_ID`
   - **Issuer ID** → `APPLE_API_ISSUER_ID`
   - Download .p8 file → base64 encode and paste into `APPLE_API_KEY_BASE64`:
     ```bash
     base64 -i AuthKey_XXXXXXXXXX.p8 | pbcopy
     ```

⚠️ You can only download the .p8 file ONCE. Store it securely.

---

## Summary Checklist

- [ ] `SUPABASE_URL`
- [ ] `SUPABASE_ANON_KEY`
- [ ] `SUPABASE_SERVICE_ROLE_KEY`
- [ ] `GEMINI_API_KEY`
- [ ] `APPLE_CERTIFICATE_BASE64`
- [ ] `APPLE_CERTIFICATE_PASSWORD`
- [ ] `APPLE_PROVISIONING_PROFILE_IPAD_BASE64`
- [ ] `APPLE_PROVISIONING_PROFILE_IPHONE_BASE64`
- [ ] `APPLE_API_KEY_ID`
- [ ] `APPLE_API_ISSUER_ID`
- [ ] `APPLE_API_KEY_BASE64`

**Total: 11 secrets**

---

## Quick Setup Order

1. **Apple Developer Account** - Ensure membership is active at developer.apple.com
2. **Create App IDs** - `com.jworks.kanjiquest.ipad` + `com.jworks.kanjiquest.iphone`
3. **Create/Reuse Distribution Certificate** - Requires Mac with Keychain Access
4. **Create 2 Provisioning Profiles** - One per App ID (App Store Distribution)
5. **Create App Store Connect API Key** - For automated uploads (reuse if already created)
6. **Register iPhone app** in App Store Connect
7. **Configure GitHub Secrets** - Add all 11 secrets above
8. **Push code** → triggers `kanjiquest-ipad.yml` + `kanjiquest-iphone.yml` → TestFlight
