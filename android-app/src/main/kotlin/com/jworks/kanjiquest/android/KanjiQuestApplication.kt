package com.jworks.kanjiquest.android

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.jworks.kanjiquest.android.workers.CoinSyncWorker
import com.jworks.kanjiquest.android.workers.LearningSyncWorker
import com.jworks.kanjiquest.core.data.remote.AuthSupabaseClientFactory
import com.jworks.kanjiquest.core.data.remote.SupabaseClientFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KanjiQuestApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Initialize Supabase client for J Coin backend sync
        initializeSupabase()

        // Initialize Auth Supabase client for TutoringJay authentication
        initializeAuthSupabase()

        // Schedule background J Coin sync
        CoinSyncWorker.schedule(this)

        // Schedule background learning data sync
        LearningSyncWorker.schedule(this)
    }

    private fun initializeSupabase() {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val supabaseKey = BuildConfig.SUPABASE_ANON_KEY

        if (supabaseUrl.isNotBlank() && supabaseKey.isNotBlank()) {
            try {
                SupabaseClientFactory.initialize(supabaseUrl, supabaseKey)
                android.util.Log.i("KanjiQuest", "Supabase initialized for J Coin sync")
            } catch (e: Exception) {
                android.util.Log.w("KanjiQuest", "Failed to initialize Supabase: ${e.message}")
            }
        } else {
            android.util.Log.i(
                "KanjiQuest",
                "Supabase credentials not configured - J Coin running in offline-only mode"
            )
        }
    }

    private fun initializeAuthSupabase() {
        val authUrl = BuildConfig.AUTH_SUPABASE_URL
        val authKey = BuildConfig.AUTH_SUPABASE_ANON_KEY

        if (authUrl.isNotBlank() && authKey.isNotBlank()) {
            try {
                AuthSupabaseClientFactory.initialize(authUrl, authKey)
                android.util.Log.i("KanjiQuest", "Auth Supabase initialized for TutoringJay auth")
            } catch (e: Exception) {
                android.util.Log.w("KanjiQuest", "Failed to initialize Auth Supabase: ${e.message}")
            }
        } else {
            android.util.Log.i(
                "KanjiQuest",
                "Auth credentials not configured - running without authentication"
            )
        }
    }
}
