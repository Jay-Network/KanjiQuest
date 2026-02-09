# KanjiQuest Release Build Fix

**Date:** 2026-02-07
**Issue:** assembleRelease failed with WorkManagerInitializer configuration error
**Status:** Fix in progress (jworks:44)

## Error Details

### Fatal Error (Release Only)
```
AndroidManifest.xml:7 - RemoveWorkManagerInitializer
App uses on-demand WorkManager init (Configuration.Provider)
but AndroidManifest still has default WorkManagerInitializer.
Fatal in release builds only.
```

### Root Cause
The app implements `Configuration.Provider` for on-demand WorkManager initialization (for background J Coin sync), but the AndroidManifest.xml still includes the default `WorkManagerInitializer` in the startup provider list.

This causes a conflict in release builds (lint enforces this in release mode).

## Fix Required

### 1. Remove WorkManagerInitializer from AndroidManifest.xml

**Current AndroidManifest.xml (incorrect):**
```xml
<application
    android:name=".KanjiQuestApplication"
    ...>

    <!-- This needs to be removed -->
    <provider
        android:name="androidx.startup.InitializationProvider"
        android:authorities="${applicationId}.androidx-startup"
        android:exported="false"
        tools:node="merge">
        <meta-data
            android:name="androidx.work.WorkManagerInitializer"
            android:value="androidx.startup" />
    </provider>

</application>
```

**Fixed AndroidManifest.xml (correct):**
```xml
<application
    android:name=".KanjiQuestApplication"
    ...>

    <!-- Remove the WorkManagerInitializer meta-data -->
    <provider
        android:name="androidx.startup.InitializationProvider"
        android:authorities="${applicationId}.androidx-startup"
        android:exported="false"
        tools:node="merge">
        <!-- WorkManagerInitializer removed - using Configuration.Provider instead -->
    </provider>

</application>
```

**Or completely remove the provider if WorkManager is the only initialization:**
```xml
<application
    android:name=".KanjiQuestApplication"
    ...>

    <!-- Provider completely removed since we use Configuration.Provider -->

</application>
```

### 2. Create Release Keystore (APK Signing)

**Generate keystore:**
```bash
cd ~/Data_ubuntu/GitHub/Jay-Network/apps/KanjiQuest/android-app
keytool -genkey -v -keystore kanjiquest-release.keystore \
  -alias kanjiquest \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

**Prompts to fill:**
- Keystore password: [create strong password]
- First and last name: Jay
- Organizational unit: JWorks
- Organization: JWorks
- City/Locality: Boston
- State/Province: MA
- Country code: US

**Update build.gradle.kts:**
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("kanjiquest-release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = "kanjiquest"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

**Set environment variables:**
```bash
export KEYSTORE_PASSWORD="[password]"
export KEY_PASSWORD="[password]"  # Usually same as keystore password
```

### 3. Retry Release Build

```bash
./gradlew assembleRelease
```

**Expected output:**
```
BUILD SUCCESSFUL in Xs
```

**APK location:**
```
android-app/build/outputs/apk/release/android-app-release.apk
```

## Testing After Fix

1. **Verify APK is signed:**
   ```bash
   jarsigner -verify -verbose -certs android-app/build/outputs/apk/release/android-app-release.apk
   ```

2. **Check APK size:** Should be ~15-25 MB (minified)

3. **Test install on device:**
   ```bash
   adb install android-app/build/outputs/apk/release/android-app-release.apk
   ```

4. **Verify WorkManager background sync works** (J Coin sync)

## Timeline Impact

**Original Timeline:**
- Feb 7-8: APK build ← **Currently here (blocked)**

**Updated Timeline:**
- Feb 7 (today): Fix WorkManagerInitializer + create keystore
- Feb 8 (tomorrow): Retry assembleRelease → Success
- Feb 9-10: Upload to Supabase Storage, portal integration
- Feb 11-13: E2E testing
- Feb 14: Beta launch ✅ **Still on track**

**Delay:** ~1 day (not critical, still on schedule for Feb 14 beta launch)

## Security Notes

### Keystore Storage
- **DO NOT** commit keystore to git
- Add to `.gitignore`: `*.keystore`
- Store backup in secure location (password manager, encrypted drive)

### Environment Variables
- Use `.env.local` for development
- Use GitHub Secrets for CI/CD (future)
- Never commit passwords to git

### Play Store Publishing (Future)
- Upload keystore to Google Play Console
- Enable App Signing by Google Play
- Google manages final signing key

## Verification Checklist

After fix:
- [ ] WorkManagerInitializer removed from AndroidManifest.xml
- [ ] Keystore created and secured
- [ ] build.gradle.kts updated with signingConfig
- [ ] Environment variables set
- [ ] assembleRelease succeeds
- [ ] APK is signed (verified with jarsigner)
- [ ] APK installs on device
- [ ] Background J Coin sync works
- [ ] No lint errors
- [ ] APK uploaded to Supabase Storage

---

**Assigned to:** jworks:44
**Reporter:** jayhub:31
**Coordinator:** jworks:42 (4_Apps)
**Priority:** High (blocking beta launch)
**ETA:** Feb 8 (tomorrow)
