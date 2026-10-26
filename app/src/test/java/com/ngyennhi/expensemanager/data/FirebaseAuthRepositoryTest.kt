package com.ngyennhi.expensemanager.data

import com.ngyennhi.expensemanager.data.repository.AuthClient
import com.ngyennhi.expensemanager.data.repository.FirebaseAuthRepository
import com.ngyennhi.expensemanager.data.repository.firebaseAuthError
import com.ngyennhi.expensemanager.domain.model.AuthError
import com.ngyennhi.expensemanager.domain.model.AuthFailure
import com.ngyennhi.expensemanager.domain.model.AuthUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAuthRepositoryTest {
    @Test fun restoredSessionGuestAndAccountChangesEmitAndCancellationRemovesListener() = runTest {
        val client = Client().apply { currentUser = AuthUser("a", "a@example.com") }
        val repository = FirebaseAuthRepository(client)
        val users = mutableListOf<AuthUser?>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.observeUser().collect { users += it } }
        runCurrent()
        assertEquals(listOf(client.currentUser), users)
        repository.login("b@example.com", " secret "); runCurrent()
        repository.logout(); runCurrent()
        repository.register("c@example.com", "new-password"); runCurrent()
        assertEquals(listOf("a@example.com", "b@example.com", null, "c@example.com"), users.map { it?.email })
        job.cancel(); runCurrent()
        assertTrue(client.listeners.isEmpty())
        assertEquals(1, client.removed)
    }

    @Test fun unknownErrorsAreSanitizedAndKnownFailuresCanBeRetried() = runTest {
        val client = Client()
        val repository = FirebaseAuthRepository(client)
        client.failure = IllegalStateException("private backend detail")
        try { repository.login("a@example.com", "secret"); fail()
        } catch (failure: AuthFailure) {
            assertEquals(AuthError.UNKNOWN, failure.reason)
            assertNull(failure.cause)
            assertFalse(failure.toString().contains("private backend detail"))
        }
        client.failure = AuthFailure(AuthError.NETWORK)
        try { repository.register("a@example.com", "secret"); fail()
        } catch (failure: AuthFailure) { assertEquals(AuthError.NETWORK, failure.reason) }
        client.failure = null
        repository.login("a@example.com", "secret")
        assertEquals("a@example.com", client.currentUser?.email)
    }

    @Test fun cancellationIsNotConvertedToAnAuthenticationError() = runTest {
        val client = Client().apply { failure = CancellationException("cancelled") }
        try { FirebaseAuthRepository(client).login("a@example.com", "secret"); fail()
        } catch (_: CancellationException) { /* Required coroutine contract. */ }
    }

    @Test fun backendCodesMapToStableDomainErrorsWithoutExposingMessages() {
        mapOf(
            "ERROR_INVALID_CREDENTIAL" to AuthError.INVALID_CREDENTIALS,
            "ERROR_INVALID_LOGIN_CREDENTIALS" to AuthError.INVALID_CREDENTIALS,
            "ERROR_WRONG_PASSWORD" to AuthError.INVALID_CREDENTIALS,
            "ERROR_USER_NOT_FOUND" to AuthError.INVALID_CREDENTIALS,
            "ERROR_EMAIL_ALREADY_IN_USE" to AuthError.EMAIL_IN_USE,
            "ERROR_WEAK_PASSWORD" to AuthError.WEAK_PASSWORD,
            "ERROR_PASSWORD_DOES_NOT_MEET_REQUIREMENTS" to AuthError.WEAK_PASSWORD,
            "ERROR_NETWORK_REQUEST_FAILED" to AuthError.NETWORK,
            "ERROR_TOO_MANY_REQUESTS" to AuthError.TOO_MANY_REQUESTS,
            "ERROR_USER_DISABLED" to AuthError.USER_DISABLED,
            "ERROR_OPERATION_NOT_ALLOWED" to AuthError.NOT_CONFIGURED,
            "ERROR_INVALID_API_KEY" to AuthError.NOT_CONFIGURED,
            "NEW_UNRECOGNIZED_CODE" to AuthError.UNKNOWN,
        ).forEach { (code, expected) -> assertEquals(code, expected, firebaseAuthError(code)) }
    }

    @Test fun listenerSetupFailureReleasesListenerAndReportsSanitizedFailure() = runTest {
        val client = Client().apply { failListener = true }
        var failure: Throwable? = null
        val job = launch {
            try { FirebaseAuthRepository(client).observeUser().collect()
            } catch (error: AuthFailure) { failure = error }
        }
        runCurrent(); job.join()
        assertEquals(AuthError.UNKNOWN, (failure as AuthFailure).reason)
        assertTrue(client.listeners.isEmpty())
    }

    @Test fun googleRequiresSeparateAccountPolicyAndCannotReplaceCurrentSession() = runTest {
        val client = Client()
        try { FirebaseAuthRepository(client).loginWithGoogle("id-token"); fail()
        } catch (failure: AuthFailure) { assertEquals(AuthError.NOT_CONFIGURED, failure.reason) }
        assertEquals(0, client.googleRequests)
        client.currentUser = AuthUser("password", "same@example.com")
        try { FirebaseAuthRepository(client, separateGoogleAccountsConfirmed = true).loginWithGoogle("id-token"); fail()
        } catch (failure: AuthFailure) { assertEquals(AuthError.PROVIDER_CONFLICT, failure.reason) }
        assertEquals("password", client.currentUser?.id)
        assertEquals(0, client.googleRequests)
    }

    @Test fun googleUsesFirebaseListenerAndProviderFailureIsSafe() = runTest {
        val client = Client()
        val repository = FirebaseAuthRepository(client, separateGoogleAccountsConfirmed = true)
        client.failure = AuthFailure(AuthError.PROVIDER_CONFLICT)
        try { repository.loginWithGoogle("first-token"); fail()
        } catch (failure: AuthFailure) { assertEquals(AuthError.PROVIDER_CONFLICT, failure.reason) }
        assertNull(client.currentUser)
        client.failure = null
        repository.loginWithGoogle("second-token")
        assertEquals(2, client.googleRequests)
        assertEquals("google@example.com", client.currentUser?.email)
    }

    @Test fun logoutRetainsGuestModeWhenCredentialCleanupFails() = runTest {
        val client = Client().apply { currentUser = AuthUser("google", "google@example.com") }
        val repository = FirebaseAuthRepository(client, true, clearCredentialState = { error("provider unavailable") })
        repository.logout()
        assertNull(client.currentUser)
    }

    private class Client : AuthClient {
        override var currentUser: AuthUser? = null
        val listeners = mutableSetOf<() -> Unit>()
        var failure: Exception? = null
        var failListener = false
        var removed = 0
        var googleRequests = 0
        override fun addListener(listener: () -> Unit) {
            listeners += listener
            if (failListener) throw IllegalStateException("backend detail")
            listener()
        }
        override fun removeListener(listener: () -> Unit) { listeners -= listener; removed++ }
        override suspend fun login(email: String, password: String) {
            failure?.let { throw it }
            currentUser = AuthUser(email, email)
            listeners.toList().forEach { it() }
        }
        override suspend fun register(email: String, password: String) = login(email, password)
        override suspend fun loginWithGoogle(idToken: String) {
            googleRequests++
            failure?.let { throw it }
            currentUser = AuthUser("google", "google@example.com")
            listeners.toList().forEach { it() }
        }
        override fun logout() {
            failure?.let { throw it }
            currentUser = null
            listeners.toList().forEach { it() }
        }
    }
}
