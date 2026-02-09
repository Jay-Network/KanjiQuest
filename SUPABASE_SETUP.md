# Supabase Configuration for J Coin Integration

## Required Configuration

To enable J Coin backend sync, you need to configure Supabase credentials.

### Step 1: Get Supabase Credentials

From your Supabase project (https://supabase.com/dashboard):
1. Go to Project Settings → API
2. Copy the **Project URL** (e.g., `https://xxxxx.supabase.co`)
3. Copy the **anon/public** key

### Step 2: Add to local.properties

Add the following lines to `local.properties` (this file is git-ignored):

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key-here
```

### Step 3: Update build.gradle.kts (if not already done)

In `android-app/build.gradle.kts`, add:

```kotlin
android {
    defaultConfig {
        // Load from local.properties
        val properties = Properties()
        file("../local.properties").takeIf { it.exists() }?.let {
            properties.load(it.inputStream())
        }

        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${properties.getProperty("SUPABASE_URL", "")}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"${properties.getProperty("SUPABASE_ANON_KEY", "")}\""
        )
    }

    buildFeatures {
        buildConfig = true
    }
}
```

### Step 4: Initialize in Application

The `KanjiQuestApplication` class will initialize Supabase on startup using these credentials.

## Testing Without Backend

If you don't have Supabase credentials yet, J Coin will work in **offline-only mode**:
- Coins are earned and stored locally
- Balance displays correctly
- Sync queue accumulates events
- When credentials are added, all queued events will sync automatically

## Production Setup

For production builds, use environment variables or a secrets management system instead of `local.properties`.
