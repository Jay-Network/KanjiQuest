package com.jworks.kanjiquest.core.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Supabase client factory for J Coin backend integration.
 *
 * Configuration:
 * - SUPABASE_URL: Set in local.properties or BuildConfig
 * - SUPABASE_ANON_KEY: Set in local.properties or BuildConfig
 *
 * These values should be injected via Hilt in the Android app module.
 */
object SupabaseClientFactory {

    private var _instance: SupabaseClient? = null

    /**
     * Initialize the Supabase client with configuration.
     *
     * @param supabaseUrl The Supabase project URL (e.g., https://xxxxx.supabase.co)
     * @param supabaseKey The Supabase anon/public key
     */
    fun initialize(supabaseUrl: String, supabaseKey: String) {
        if (_instance != null) {
            throw IllegalStateException("SupabaseClient already initialized")
        }

        _instance = createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        ) {
            install(Postgrest)
            install(Functions)
        }
    }

    /**
     * Get the initialized Supabase client instance.
     *
     * @throws IllegalStateException if client hasn't been initialized
     */
    fun getInstance(): SupabaseClient {
        return _instance ?: throw IllegalStateException(
            "SupabaseClient not initialized. Call initialize() first."
        )
    }

    /**
     * Check if the client has been initialized.
     */
    fun isInitialized(): Boolean = _instance != null

    /**
     * Reset the client instance (for testing).
     */
    internal fun reset() {
        _instance = null
    }
}
