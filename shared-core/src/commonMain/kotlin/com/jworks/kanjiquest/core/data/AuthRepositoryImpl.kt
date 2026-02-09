package com.jworks.kanjiquest.core.data

import com.jworks.kanjiquest.core.data.remote.AuthSupabaseClientFactory
import com.jworks.kanjiquest.core.domain.model.Subscription
import com.jworks.kanjiquest.core.domain.model.SubscriptionPlan
import com.jworks.kanjiquest.core.domain.model.SubscriptionStatus
import com.jworks.kanjiquest.core.domain.repository.AuthRepository
import com.jworks.kanjiquest.core.domain.repository.AuthState
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

class AuthRepositoryImpl : AuthRepository {

    private val _authState = MutableStateFlow(AuthState(false, null, null, false))

    // Cache premium status with timestamp
    private var cachedPremium: Boolean = false
    private var cacheTimestamp: Long = 0
    private val cacheTtlMs = 24 * 60 * 60 * 1000L // 24 hours

    override fun observeAuthState(): Flow<AuthState> = _authState

    override suspend fun getCurrentUserId(): String? {
        if (!AuthSupabaseClientFactory.isInitialized()) return null
        return try {
            val client = AuthSupabaseClientFactory.getInstance()
            client.auth.currentSessionOrNull()?.user?.id
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun signIn(email: String, password: String): Result<String> {
        if (!AuthSupabaseClientFactory.isInitialized()) {
            return Result.failure(IllegalStateException("Auth not configured"))
        }
        return try {
            val client = AuthSupabaseClientFactory.getInstance()
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val userId = client.auth.currentSessionOrNull()?.user?.id
                ?: return Result.failure(IllegalStateException("No session after sign in"))
            val isPremium = checkSubscription()
            _authState.value = AuthState(true, userId, email, isPremium)
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String): Result<String> {
        if (!AuthSupabaseClientFactory.isInitialized()) {
            return Result.failure(IllegalStateException("Auth not configured"))
        }
        return try {
            val client = AuthSupabaseClientFactory.getInstance()
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val userId = client.auth.currentSessionOrNull()?.user?.id
                ?: return Result.failure(IllegalStateException("No session after sign up"))
            _authState.value = AuthState(true, userId, email, false)
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        if (!AuthSupabaseClientFactory.isInitialized()) return
        try {
            val client = AuthSupabaseClientFactory.getInstance()
            client.auth.signOut()
        } catch (_: Exception) {
            // Ignore sign out errors
        }
        cachedPremium = false
        cacheTimestamp = 0
        _authState.value = AuthState(false, null, null, false)
    }

    override suspend fun checkSubscription(): Boolean {
        // Return cached value if within TTL
        val now = kotlinx.datetime.Clock.System.now().epochSeconds * 1000
        if (now - cacheTimestamp < cacheTtlMs) {
            return cachedPremium
        }

        val sub = getSubscription()
        val isPremium = sub?.plan == SubscriptionPlan.PREMIUM &&
            sub.status in listOf(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING)
        cachedPremium = isPremium
        cacheTimestamp = now
        return isPremium
    }

    override suspend fun getSubscription(): Subscription? {
        if (!AuthSupabaseClientFactory.isInitialized()) return null
        val userId = getCurrentUserId() ?: return null
        return try {
            val client = AuthSupabaseClientFactory.getInstance()
            val result = client.postgrest["app_subscriptions"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("app_id", "kanjiquest")
                    }
                }
                .decodeSingleOrNull<SubscriptionRow>()

            result?.toSubscription(userId)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Refresh auth state from current session (call on app start).
     */
    suspend fun refreshAuthState() {
        if (!AuthSupabaseClientFactory.isInitialized()) return
        try {
            val client = AuthSupabaseClientFactory.getInstance()
            val session = client.auth.currentSessionOrNull()
            if (session != null) {
                val userId = session.user?.id
                val email = session.user?.email
                val isPremium = checkSubscription()
                _authState.value = AuthState(true, userId, email, isPremium)
            }
        } catch (_: Exception) {
            // No active session
        }
    }
}

@Serializable
private data class SubscriptionRow(
    val plan: String = "free",
    val status: String = "active",
    val cancel_at_period_end: Boolean = false
) {
    fun toSubscription(userId: String) = Subscription(
        userId = userId,
        plan = SubscriptionPlan.fromString(plan),
        status = SubscriptionStatus.fromString(status),
        cancelAtPeriodEnd = cancel_at_period_end
    )
}
