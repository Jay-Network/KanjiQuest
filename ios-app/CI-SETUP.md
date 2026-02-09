# KanjiQuest iPad - CI/CD Setup Guide

## Overview
The iOS app is built and deployed via GitHub Actions on macOS runners.
You develop on Ubuntu (VS Code), push to GitHub, and CI handles compilation + TestFlight.

## Prerequisites
1. **Apple Developer Account** ($99/year) - developer.apple.com
2. **App Store Connect** - Create the app record for `com.jworks.kanjiquest.ipad`
3. **GitHub repository** - Jay-Network repo with Actions enabled

## GitHub Repository Secrets

Go to: Repository Settings > Secrets and variables > Actions

### App Secrets (4)
| Secret | Description | Where to get |
|--------|-------------|--------------|
| `SUPABASE_URL` | Supabase project URL | supabase.com dashboard |
| `SUPABASE_ANON_KEY` | Supabase anon/public key | supabase.com > Settings > API |
| `SUPABASE_SERVICE_ROLE_KEY` | Supabase service role key | supabase.com > Settings > API |
| `GEMINI_API_KEY` | Google AI Studio API key | aistudio.google.com |

### Apple Signing (6)
| Secret | Description | How to create |
|--------|-------------|---------------|
| `APPLE_TEAM_ID` | Apple Developer Team ID | developer.apple.com > Membership |
| `APPLE_CERTIFICATE_BASE64` | Distribution certificate (.p12, base64) | See below |
| `APPLE_CERTIFICATE_PASSWORD` | Password for the .p12 file | Set when exporting |
| `APPLE_PROVISIONING_PROFILE_BASE64` | App Store provisioning profile (base64) | See below |
| `KEYCHAIN_PASSWORD` | Temporary keychain password | Any random string |

### App Store Connect API (3)
| Secret | Description | How to create |
|--------|-------------|---------------|
| `ASC_KEY_ID` | API Key ID | App Store Connect > Users > Keys |
| `ASC_ISSUER_ID` | Issuer ID | App Store Connect > Users > Keys |
| `ASC_PRIVATE_KEY` | Private key (.p8 content) | Download when creating key |

## Step-by-Step Setup

### 1. Create App ID
- Go to developer.apple.com > Certificates, Identifiers & Profiles
- Create App ID: `com.jworks.kanjiquest.ipad`
- Enable capabilities: (none special needed for MVP)

### 2. Create Distribution Certificate
```bash
# On any Mac (can use GitHub Codespaces with macOS):
# 1. Open Keychain Access > Certificate Assistant > Request Certificate from CA
# 2. Upload CSR to developer.apple.com > Certificates > Distribution
# 3. Download .cer, double-click to install in Keychain
# 4. Export as .p12 from Keychain (set a password)
# 5. Base64 encode:
base64 -i certificate.p12 | pbcopy
# Paste into APPLE_CERTIFICATE_BASE64 secret
```

### 3. Create Provisioning Profile
```bash
# 1. developer.apple.com > Profiles > New
# 2. Type: App Store Distribution
# 3. Select App ID: com.jworks.kanjiquest.ipad
# 4. Select distribution certificate
# 5. Download .mobileprovision
# 6. Base64 encode:
base64 -i profile.mobileprovision | pbcopy
# Paste into APPLE_PROVISIONING_PROFILE_BASE64 secret
```

### 4. Create App Store Connect API Key
- App Store Connect > Users and Access > Integrations > Keys
- Generate new key with "App Manager" role
- Save the Key ID, Issuer ID, and download the .p8 file
- The .p8 content goes into `ASC_PRIVATE_KEY`

### 5. Create App Record
- App Store Connect > My Apps > New App
- Platform: iOS
- Name: KanjiQuest
- Bundle ID: com.jworks.kanjiquest.ipad
- SKU: kanjiquest-ipad

## Triggering a Build

Builds trigger automatically when you push changes to:
- `apps/KanjiQuest/shared-core/`
- `apps/KanjiQuest/shared-japanese/`
- `apps/KanjiQuest/ios-app/`

Or manually via: Actions tab > KanjiQuest iPad > Run workflow

## Development Workflow

```
[VS Code on Ubuntu] → edit Swift/Kotlin → git push
                                              ↓
[GitHub Actions macOS runner] → build XCFramework → xcodegen → xcodebuild → TestFlight
                                              ↓
[iPad] → install from TestFlight → test with Apple Pencil
```

## VS Code Setup (Ubuntu)

Install these extensions:
- **Swift** (sswg.swift-lang) - syntax highlighting + LSP
- **Kotlin** (mathiasfrohlich.Kotlin) - Kotlin syntax

Note: Swift LSP won't fully work on Linux (no SourceKit), but you get syntax
highlighting and basic editing. The real compilation happens in CI.
