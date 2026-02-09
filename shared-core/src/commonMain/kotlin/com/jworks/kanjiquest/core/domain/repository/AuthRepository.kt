package com.jworks.kanjiquest.core.domain.repository

import com.jworks.kanjiquest.core.domain.model.Subscription
import kotlinx.coroutines.flow.Flow

data class AuthState(
    val isLoggedIn: Boolean,
    val userId: String?,
    val email: String?,
    val isPremium: Boolean
)

interface AuthRepository {
    fun observeAuthState(): Flow<AuthState>
    suspend fun getCurrentUserId(): String?
    suspend fun signIn(email: String, password: String): Result<String>
    suspend fun signUp(email: String, password: String): Result<String>
    suspend fun signOut()
    suspend fun checkSubscription(): Boolean
    suspend fun getSubscription(): Subscription?
}
