package com.ngyennhi.expensemanager.domain.repository

import com.ngyennhi.expensemanager.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Null means local Guest Mode. Financial storage is independent of this stream. */
    fun observeUser(): Flow<AuthUser?>
    suspend fun login(email: String, password: String)
    suspend fun register(email: String, password: String)
    /** Exchanges a request-scoped ID token; never links providers or stores the token. */
    suspend fun loginWithGoogle(idToken: String)
    suspend fun logout()
}
