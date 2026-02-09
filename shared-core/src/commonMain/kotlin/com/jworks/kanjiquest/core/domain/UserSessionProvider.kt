package com.jworks.kanjiquest.core.domain

import com.jworks.kanjiquest.core.domain.model.LOCAL_USER_ID
import com.jworks.kanjiquest.core.domain.model.UserLevel
import com.jworks.kanjiquest.core.domain.repository.AuthRepository

interface UserSessionProvider {
    suspend fun getUserId(): String
    fun isPremium(): Boolean
    fun isAdmin(): Boolean
    fun getEffectiveLevel(): UserLevel
    fun setAdminOverrideLevel(level: UserLevel?)
    fun getAdminOverrideLevel(): UserLevel?
    suspend fun getUserEmail(): String?
    fun setAdminPlayerLevelOverride(level: Int?)
    fun getAdminPlayerLevelOverride(): Int?
}

class UserSessionProviderImpl(
    private val authRepository: AuthRepository
) : UserSessionProvider {

    private var cachedPremium = false
    private var cachedEmail: String? = null
    private var adminOverrideLevel: UserLevel? = null
    private var adminPlayerLevelOverride: Int? = null

    override suspend fun getUserId(): String {
        return authRepository.getCurrentUserId() ?: LOCAL_USER_ID
    }

    override suspend fun getUserEmail(): String? {
        if (cachedEmail == null) {
            val state = authRepository.observeAuthState()
            // Just return null if not available synchronously
        }
        return cachedEmail
    }

    override fun isPremium(): Boolean {
        // Admin override takes precedence
        adminOverrideLevel?.let { override ->
            return override == UserLevel.PREMIUM || override == UserLevel.ADMIN
        }
        return cachedPremium
    }

    override fun isAdmin(): Boolean {
        return cachedEmail?.lowercase() in UserLevel.ADMIN_EMAILS.map { it.lowercase() }
    }

    override fun getEffectiveLevel(): UserLevel {
        // Admin override for testing different views
        adminOverrideLevel?.let { return it }

        return when {
            isAdmin() -> UserLevel.ADMIN
            cachedPremium -> UserLevel.PREMIUM
            else -> UserLevel.FREE
        }
    }

    override fun setAdminOverrideLevel(level: UserLevel?) {
        adminOverrideLevel = level
    }

    override fun getAdminOverrideLevel(): UserLevel? {
        return adminOverrideLevel
    }

    override fun setAdminPlayerLevelOverride(level: Int?) {
        adminPlayerLevelOverride = level
    }

    override fun getAdminPlayerLevelOverride(): Int? {
        return adminPlayerLevelOverride
    }

    suspend fun refresh() {
        cachedPremium = authRepository.checkSubscription()
        // Cache email from auth state
        val userId = authRepository.getCurrentUserId()
        if (userId != null) {
            try {
                authRepository.observeAuthState().collect { state ->
                    cachedEmail = state.email
                    return@collect
                }
            } catch (_: Exception) { }
        }
    }

    fun updateEmail(email: String?) {
        cachedEmail = email
    }
}
