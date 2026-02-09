package com.jworks.kanjiquest.core.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.MemorySessionManager
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Supabase client factory for TutoringJay authentication.
 * Connects to the TutoringJay Supabase project (sitmlszxevzczfydemok)
 * for user auth and subscription queries.
 */
object AuthSupabaseClientFactory {

    private var _instance: SupabaseClient? = null

    fun initialize(supabaseUrl: String, supabaseKey: String) {
        if (_instance != null) return // Already initialized

        _instance = createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        ) {
            install(Auth) {
                sessionManager = MemorySessionManager()
            }
            install(Postgrest)
        }
    }

    fun getInstance(): SupabaseClient {
        return _instance ?: throw IllegalStateException(
            "Auth SupabaseClient not initialized. Call initialize() first."
        )
    }

    fun isInitialized(): Boolean = _instance != null

    internal fun reset() {
        _instance = null
    }
}
