# Apple Certificate & Signing Guide for KanjiQuest iPad

Complete step-by-step guide for setting up all 13 GitHub Actions secrets needed to build and deploy KanjiQuest iPad to TestFlight.

## Prerequisites

- Apple Developer Program membership ($99/year) at [developer.apple.com](https://developer.apple.com)
- Access to a Mac (even temporarily - for certificate generation only)
- GitHub repository admin access (to add secrets)

## Quick Reference: All 13 Secrets

| # | Secret Name | Category | Value Type |
|---|-------------|----------|------------|
| 1 | `SUPABASE_URL` | App | URL string |
| 2 | `SUPABASE_ANON_KEY` | App | JWT string |
| 3 | `SUPABASE_SERVICE_ROLE_KEY` | App | JWT string |
| 4 | `GEMINI_API_KEY` | App | API key string |
| 5 | `APPLE_TEAM_ID` | Signing | 10-char alphanumeric |
| 6 | `APPLE_CERTIFICATE_BASE64` | Signing | Base64-encoded .p12 |
| 7 | `APPLE_CERTIFICATE_PASSWORD` | Signing | Password string |
| 8 | `APPLE_PROVISIONING_PROFILE_BASE64` | Signing | Base64-encoded .mobileprovision |
| 9 | `KEYCHAIN_PASSWORD` | Signing | Random string |
| 10 | `ASC_KEY_ID` | ASC API | 10-char Key ID |
| 11 | `ASC_ISSUER_ID` | ASC API | UUID string |
| 12 | `ASC_PRIVATE_KEY` | ASC API | .p8 file contents |
| 13 | `APPLE_SIGNING_IDENTITY` | Signing | Certificate common name |

---

## Part 1: App Secrets (4 secrets)

These are non-Apple secrets your app needs at build time.

### Secret 1: SUPABASE_URL

1. Go to [supabase.com](https://supabase.com) and sign in
2. Select your project (inygcrdhfmoerborxehq)
3. Go to **Settings** > **API**
4. Copy the **Project URL** (starts with `https://`)

```
Example: https://inygcrdhfmoerborxehq.supabase.co
```

### Secret 2: SUPABASE_ANON_KEY

1. Same page: **Settings** > **API**
2. Under **Project API keys**, copy the `anon` / `public` key
3. This is a long JWT string starting with `eyJ...`

### Secret 3: SUPABASE_SERVICE_ROLE_KEY

1. Same page: **Settings** > **API**
2. Under **Project API keys**, click **Reveal** next to `service_role`
3. Copy the full JWT string

**Warning**: This key has full database access. Never expose it client-side. It's injected at build time into the app binary via xcconfig, not hardcoded in source.

### Secret 4: GEMINI_API_KEY

1. Go to [aistudio.google.com](https://aistudio.google.com)
2. Click **Get API Key** in the sidebar
3. Create a new key or copy an existing one
4. The key is used for AI calligraphy feedback (Gemini 2.5 Flash)

---

## Part 2: Apple Developer Setup

### Step 2.1: Find Your Team ID (Secret 5)

1. Go to [developer.apple.com/account](https://developer.apple.com/account)
2. Sign in with your Apple ID
3. Look at **Membership Details** in the sidebar
4. Your **Team ID** is a 10-character alphanumeric string (e.g., `ABC1234DEF`)
5. Save this as `APPLE_TEAM_ID`

### Step 2.2: Register the App ID

Before creating certificates, register the bundle identifier:

1. Go to [developer.apple.com/account/resources/identifiers/list](https://developer.apple.com/account/resources/identifiers/list)
2. Click the **+** button
3. Select **App IDs** > **App**
4. Fill in:
   - **Description**: `KanjiQuest iPad`
   - **Bundle ID**: Select **Explicit** and enter `com.jworks.kanjiquest.ipad`
5. Under **Capabilities**: No special capabilities needed for MVP
6. Click **Continue** > **Register**

### Step 2.3: Generate a Distribution Certificate (Secrets 6 & 7)

You need a Mac for this step. If you don't have one, you can use a cloud Mac service (MacStadium, AWS EC2 Mac) or ask someone with a Mac to do this once.

#### On the Mac:

**A. Create a Certificate Signing Request (CSR)**

1. Open **Keychain Access** (Applications > Utilities > Keychain Access)
2. Menu: **Keychain Access** > **Certificate Assistant** > **Request a Certificate From a Certificate Authority...**
3. Fill in:
   - **User Email Address**: jay@jworks.com
   - **Common Name**: JWorks Distribution
   - **Request is**: Saved to disk
4. Click **Continue** and save the `.certSigningRequest` file

**B. Upload CSR to Apple Developer Portal**

1. Go to [developer.apple.com/account/resources/certificates/list](https://developer.apple.com/account/resources/certificates/list)
2. Click the **+** button
3. Under **Software**, select **Apple Distribution**
4. Click **Continue**
5. Upload the `.certSigningRequest` file you just created
6. Click **Continue**
7. **Download** the `.cer` certificate file

**C. Install the Certificate**

1. Double-click the downloaded `.cer` file
2. It will install into your **login** keychain in Keychain Access
3. You should see it listed under **My Certificates** as `Apple Distribution: <Your Name> (<Team ID>)`

**D. Export as .p12 (PKCS#12)**

1. In **Keychain Access**, go to **My Certificates** (sidebar)
2. Find the `Apple Distribution: ...` certificate
3. Expand it (click the triangle) - you should see a private key underneath
4. **Right-click** the certificate (not the key) and select **Export...**
5. Choose format: **Personal Information Exchange (.p12)**
6. Save the file (e.g., `KanjiQuest_Distribution.p12`)
7. **Set a strong password** when prompted - remember this password!

**E. Base64 Encode the .p12**

```bash
# On the Mac terminal:
base64 -i KanjiQuest_Distribution.p12 -o certificate_base64.txt

# Copy the contents:
cat certificate_base64.txt | pbcopy
```

8. The clipboard contents go into `APPLE_CERTIFICATE_BASE64` (Secret 6)
9. The password you set goes into `APPLE_CERTIFICATE_PASSWORD` (Secret 7)

**F. Note the Signing Identity (Secret 13)**

The signing identity is the full common name of the certificate. Find it:

```bash
security find-identity -v -p codesigning
```

Look for a line like:
```
1) ABC123... "Apple Distribution: Jay Works (ABC1234DEF)"
```

The quoted string (e.g., `Apple Distribution: Jay Works (ABC1234DEF)`) is your `APPLE_SIGNING_IDENTITY`.

### Step 2.4: Create a Provisioning Profile (Secret 8)

1. Go to [developer.apple.com/account/resources/profiles/list](https://developer.apple.com/account/resources/profiles/list)
2. Click the **+** button
3. Under **Distribution**, select **App Store Connect**
4. Click **Continue**
5. Select the App ID: `com.jworks.kanjiquest.ipad (KanjiQuest iPad)`
6. Click **Continue**
7. Select the distribution certificate you just created
8. Click **Continue**
9. Name the profile: `KanjiQuest iPad App Store`
10. Click **Generate**
11. **Download** the `.mobileprovision` file

**Base64 encode it:**

```bash
# On the Mac terminal:
base64 -i KanjiQuest_iPad_App_Store.mobileprovision -o profile_base64.txt

# Copy the contents:
cat profile_base64.txt | pbcopy
```

The clipboard contents go into `APPLE_PROVISIONING_PROFILE_BASE64` (Secret 8).

### Step 2.5: Set Keychain Password (Secret 9)

This is a temporary password used during the CI build to create a keychain on the GitHub Actions runner. It can be any random string.

```bash
# Generate a random password:
openssl rand -base64 32
```

Save the output as `KEYCHAIN_PASSWORD`. Example: `a3Bx9K2mPqR7vN1wYz8cDf4gHj6tLs0u`

---

## Part 3: App Store Connect API Key (Secrets 10, 11, 12)

The API key lets GitHub Actions upload builds to TestFlight without interactive login.

### Step 3.1: Create the API Key

1. Go to [appstoreconnect.apple.com](https://appstoreconnect.apple.com)
2. Sign in with your Apple ID
3. Click **Users and Access** in the sidebar
4. Select the **Integrations** tab at the top
5. Click **App Store Connect API** in the sidebar
6. Under **Team Keys**, click the **+** button
7. Fill in:
   - **Name**: `KanjiQuest CI`
   - **Access**: **App Manager** (sufficient for uploads)
8. Click **Generate**

### Step 3.2: Save the Key Details

After generating, you'll see:

- **Key ID**: A 10-character string (e.g., `ABC123DEF4`) -> `ASC_KEY_ID` (Secret 10)
- **Issuer ID**: A UUID at the top of the page (e.g., `12345678-abcd-efgh-ijkl-123456789012`) -> `ASC_ISSUER_ID` (Secret 11)

### Step 3.3: Download the Private Key

1. Click **Download API Key** next to the key you just created
2. This downloads a `.p8` file (e.g., `AuthKey_ABC123DEF4.p8`)
3. **You can only download this once!** Save it securely.

**Read the .p8 contents:**

```bash
cat AuthKey_ABC123DEF4.p8
```

Output looks like:
```
-----BEGIN PRIVATE KEY-----
MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQg...
...several lines of base64...
-----END PRIVATE KEY-----
```

Copy the **entire contents** (including the BEGIN/END lines) into `ASC_PRIVATE_KEY` (Secret 12).

---

## Part 4: Create the App Record

Before your first TestFlight upload, create the app in App Store Connect:

1. Go to [appstoreconnect.apple.com/apps](https://appstoreconnect.apple.com/apps)
2. Click the **+** button > **New App**
3. Fill in:
   - **Platforms**: iOS
   - **Name**: `KanjiQuest`
   - **Primary Language**: English (U.S.)
   - **Bundle ID**: Select `com.jworks.kanjiquest.ipad`
   - **SKU**: `kanjiquest-ipad`
   - **User Access**: Full Access
4. Click **Create**

---

## Part 5: Add Secrets to GitHub

1. Go to your GitHub repository: `github.com/Jay-Network/KanjiQuest`
2. Click **Settings** > **Secrets and variables** > **Actions**
3. Click **New repository secret** for each:

| Secret Name | Value |
|-------------|-------|
| `SUPABASE_URL` | `https://inygcrdhfmoerborxehq.supabase.co` |
| `SUPABASE_ANON_KEY` | (from Supabase dashboard) |
| `SUPABASE_SERVICE_ROLE_KEY` | (from Supabase dashboard) |
| `GEMINI_API_KEY` | (from Google AI Studio) |
| `APPLE_TEAM_ID` | (from developer.apple.com Membership) |
| `APPLE_CERTIFICATE_BASE64` | (base64 of .p12 file) |
| `APPLE_CERTIFICATE_PASSWORD` | (password set during .p12 export) |
| `APPLE_PROVISIONING_PROFILE_BASE64` | (base64 of .mobileprovision) |
| `KEYCHAIN_PASSWORD` | (random string) |
| `ASC_KEY_ID` | (from App Store Connect API key) |
| `ASC_ISSUER_ID` | (from App Store Connect API key page) |
| `ASC_PRIVATE_KEY` | (contents of .p8 file) |
| `APPLE_SIGNING_IDENTITY` | (certificate common name from Keychain) |

---

## Part 6: Trigger Your First Build

After all secrets are configured:

### Option A: Push a change
```bash
# Any change to ios-app/, shared-core/, or shared-japanese/ triggers the workflow
cd ~/Data_ubuntu/GitHub/Jay-Network/apps/KanjiQuest
echo "// trigger build" >> ios-app/KanjiQuest/Core/KMPBridge.swift
git add -A && git commit -m "ci: trigger first iOS build" && git push
```

### Option B: Manual trigger
1. Go to **Actions** tab in GitHub
2. Select **KanjiQuest iPad - Build & Deploy**
3. Click **Run workflow** > **Run workflow**

### Monitor the build
1. Watch the Actions tab for build progress
2. The workflow has 3 jobs:
   - **Build SharedCore XCFramework** (~5 min) - Compiles KMP for iOS
   - **Build iOS App** (~10 min) - XcodeGen + Xcode build + TestFlight upload
   - **Verify Gradle (Linux)** (~3 min) - Ensures shared code still compiles on Linux

### First build troubleshooting

| Error | Fix |
|-------|-----|
| `No signing certificate found` | Check APPLE_CERTIFICATE_BASE64 is correct base64 |
| `No provisioning profile` | Ensure profile matches the bundle ID and certificate |
| `Unable to upload to App Store Connect` | Check ASC_KEY_ID, ASC_ISSUER_ID, ASC_PRIVATE_KEY |
| `XCFramework not found` | The build-xcframework job may have failed - check its logs |
| `No app record found` | Create the app in App Store Connect first (Part 4) |

---

## Certificate Renewal

- **Distribution certificates** expire after **1 year**
- **Provisioning profiles** expire after **1 year**
- **API keys** do not expire

When certificates expire:
1. Repeat Steps 2.3 and 2.4
2. Update the corresponding GitHub secrets
3. No code changes needed

---

## Security Notes

- Never commit `.p12`, `.mobileprovision`, or `.p8` files to git
- The `.xcconfig` file (containing Supabase/Gemini keys) is gitignored
- GitHub Actions secrets are encrypted at rest and masked in logs
- The temporary keychain created during CI is deleted in the cleanup step
