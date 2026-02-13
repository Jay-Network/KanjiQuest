import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    // Firebase/Google Services temporarily disabled for local feedback polling tests
}

android {
    namespace = "com.jworks.kanjiquest.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jworks.kanjiquest"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.0-beta2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Load Supabase credentials from local.properties
        val localPropertiesFile = rootProject.file("local.properties")
        val localProperties = Properties().apply {
            if (localPropertiesFile.exists()) {
                load(localPropertiesFile.inputStream())
            }
        }

        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperties.getProperty("SUPABASE_URL", "")}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"${localProperties.getProperty("SUPABASE_ANON_KEY", "")}\""
        )

        // Gemini Flash API for handwriting evaluation
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\""
        )

        // Auth Supabase (TutoringJay project) for user authentication
        buildConfigField(
            "String",
            "AUTH_SUPABASE_URL",
            "\"${localProperties.getProperty("AUTH_SUPABASE_URL", "")}\""
        )
        buildConfigField(
            "String",
            "AUTH_SUPABASE_ANON_KEY",
            "\"${localProperties.getProperty("AUTH_SUPABASE_ANON_KEY", "")}\""
        )
    }

    signingConfigs {
        create("release") {
            val localPropertiesFile = rootProject.file("local.properties")
            val localProperties = Properties().apply {
                if (localPropertiesFile.exists()) load(localPropertiesFile.inputStream())
            }
            storeFile = file(localProperties.getProperty("RELEASE_STORE_FILE", "../keystore/kanjiquest-release.jks"))
            storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD", "")
            keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS", "kanjiquest")
            keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD", "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/CONTRIBUTORS.md"
            excludes += "/META-INF/LICENSE.md"
        }
    }
}

dependencies {
    implementation(project(":shared-core"))
    implementation(project(":shared-japanese"))
    implementation(project(":shared-tokenizer"))

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // ML Kit
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.text.recognition.japanese)
    implementation(libs.mlkit.digital.ink)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // kotlinx-datetime (needed for JCoin default parameters)
    implementation(libs.kotlinx.datetime)

    // WorkManager for background J Coin sync
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.hilt.compiler)

    // Firebase Cloud Messaging for feedback push notifications (plugin disabled, libs kept for compile)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Testing
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Google Services plugin disabled (no google-services.json required for polling tests)
