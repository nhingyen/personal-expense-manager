package com.ngyennhi.expensemanager.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.ngyennhi.expensemanager.domain.model.AuthError
import com.ngyennhi.expensemanager.domain.model.AuthFailure
import com.ngyennhi.expensemanager.domain.model.AuthUser
import com.ngyennhi.expensemanager.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

/** Small SDK boundary so listener lifecycle and repository behavior can run in local tests. */
internal interface AuthClient {
    val currentUser: AuthUser?
    fun addListener(listener: () -> Unit)
    fun removeListener(listener: () -> Unit)
    suspend fun login(email: String, password: String)
    suspend fun register(email: String, password: String)
    suspend fun loginWithGoogle(idToken: String)
    fun logout()
}

internal class FirebaseAuthClient(private val auth: FirebaseAuth) : AuthClient {
    private val listeners = mutableMapOf<() -> Unit, FirebaseAuth.AuthStateListener>()
    override val currentUser: AuthUser?
        get() = auth.currentUser?.let { AuthUser(it.uid, it.email) }

    override fun addListener(listener: () -> Unit) {
        val sdkListener = FirebaseAuth.AuthStateListener { listener() }
        listeners[listener] = sdkListener
        auth.addAuthStateListener(sdkListener)
    }

    override fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)?.let(auth::removeAuthStateListener)
    }

    override suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun register(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).await()
    }

    override suspend fun loginWithGoogle(idToken: String) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    override fun logout() = auth.signOut()
}

internal class FirebaseAuthRepository(
    private val client: AuthClient,
    private val separateGoogleAccountsConfirmed: Boolean = false,
    private val clearCredentialState: suspend () -> Unit = {},
) : AuthRepository {
    override fun observeUser() = callbackFlow {
        val listener: () -> Unit = { trySend(client.currentUser); Unit }
        try {
            client.addListener(listener)
            listener() // Also supports an immediately available, offline restored session.
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (error: Exception) {
            client.removeListener(listener)
            throw AuthFailure(error.toAuthError())
        }
        awaitClose { client.removeListener(listener) }
    }.distinctUntilChanged()

    override suspend fun login(email: String, password: String) = request { client.login(email, password) }
    override suspend fun register(email: String, password: String) = request { client.register(email, password) }
    override suspend fun loginWithGoogle(idToken: String) = request {
        // Do not rely on fetchSignInMethodsForEmail: enumeration protection hides providers,
        // and the default Firebase policy can automatically replace an untrusted provider.
        if (!separateGoogleAccountsConfirmed) throw AuthFailure(AuthError.NOT_CONFIGURED)
        if (client.currentUser != null) throw AuthFailure(AuthError.PROVIDER_CONFLICT)
        if (idToken.isBlank()) throw AuthFailure(AuthError.INVALID_CREDENTIALS)
        client.loginWithGoogle(idToken)
    }

    override suspend fun logout() = request {
        client.logout()
        try { clearCredentialState()
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) { /* Local sign-out must still leave the app in Guest Mode. */ }
    }

    private suspend fun request(action: suspend () -> Unit) {
        try { action()
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (error: Exception) { throw AuthFailure(error.toAuthError()) }
    }
}

private fun Exception.toAuthError(): AuthError = when (this) {
    is AuthFailure -> reason
    is FirebaseNetworkException -> AuthError.NETWORK
    is FirebaseTooManyRequestsException -> AuthError.TOO_MANY_REQUESTS
    is FirebaseAuthException -> firebaseAuthError(errorCode)
    else -> AuthError.UNKNOWN
}

internal fun firebaseAuthError(code: String): AuthError = when (code) {
    "ERROR_INVALID_EMAIL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND",
    "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS" -> AuthError.INVALID_CREDENTIALS
    "ERROR_EMAIL_ALREADY_IN_USE" -> AuthError.EMAIL_IN_USE
    "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", "ERROR_CREDENTIAL_ALREADY_IN_USE",
    "ERROR_PROVIDER_ALREADY_LINKED" -> AuthError.PROVIDER_CONFLICT
    "ERROR_WEAK_PASSWORD", "ERROR_PASSWORD_DOES_NOT_MEET_REQUIREMENTS" -> AuthError.WEAK_PASSWORD
    "ERROR_NETWORK_REQUEST_FAILED" -> AuthError.NETWORK
    "ERROR_TOO_MANY_REQUESTS" -> AuthError.TOO_MANY_REQUESTS
    "ERROR_USER_DISABLED" -> AuthError.USER_DISABLED
    "ERROR_OPERATION_NOT_ALLOWED", "ERROR_INVALID_API_KEY", "ERROR_APP_NOT_AUTHORIZED",
    "ERROR_API_NOT_AVAILABLE", "ERROR_CONFIGURATION_NOT_FOUND" -> AuthError.NOT_CONFIGURED
    else -> AuthError.UNKNOWN
}
