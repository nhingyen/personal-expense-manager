package com.ngyennhi.expensemanager.presentation.auth

import androidx.lifecycle.ViewModelStore
import com.ngyennhi.expensemanager.domain.model.*
import com.ngyennhi.expensemanager.domain.repository.AuthRepository
import com.ngyennhi.expensemanager.domain.usecase.ValidateAuthCredentialsUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeAuth()
    private val store = ViewModelStore()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun create() = AuthViewModel(repository, ValidateAuthCredentialsUseCase()).also {
        store.put("${System.identityHashCode(it)}", it)
    }

    @Test fun guestStartsWithoutLoginAndRestoredSessionAndExternalChangesAreObserved() = runTest(dispatcher) {
        val vm = create(); runCurrent()
        assertTrue(vm.uiState.value.isInitialized)
        assertNull(vm.uiState.value.user)
        assertEquals(0, repository.writes)
        repository.user.value = AuthUser("a", "a@example.com"); runCurrent()
        val restored = create(); runCurrent()
        assertEquals(repository.user.value, restored.uiState.value.user)
        repository.user.value = null; runCurrent()
        assertNull(vm.uiState.value.user)
        assertNull(restored.uiState.value.user)
    }

    @Test fun invalidFieldsDoNotCallFirebaseAndRegistrationValidatesConfirmation() = runTest(dispatcher) {
        val vm = create(); runCurrent()
        vm.submit("", ""); runCurrent()
        assertTrue(vm.uiState.value.validation.invalidEmail)
        assertTrue(vm.uiState.value.validation.missingPassword)
        vm.setRegistering(true); vm.setEmail("a@example.com"); vm.submit("12345", "x"); runCurrent()
        assertTrue(vm.uiState.value.validation.weakPassword)
        assertTrue(vm.uiState.value.validation.passwordMismatch)
        assertEquals(0, repository.writes)
    }

    @Test fun loginTrimsOnlyEmailAndDoesNotExposePasswordInState() = runTest(dispatcher) {
        val vm = create(); runCurrent()
        vm.setEmail("  a@example.com  "); vm.submit(" secret ", "unused"); runCurrent()
        assertEquals("a@example.com", repository.lastEmail)
        assertEquals(" secret ", repository.lastPassword)
        assertFalse(vm.uiState.value.toString().contains("secret"))
        assertEquals("a@example.com", vm.uiState.value.user?.email)
    }

    @Test fun registrationBlocksDuplicateRequestsEditingAndModeChangesWhilePending() = runTest(dispatcher) {
        val vm = create(); runCurrent()
        repository.gate = CompletableDeferred()
        vm.setRegistering(true); vm.setEmail("a@example.com")
        vm.submit("secret", "secret")
        vm.submit("secret", "secret"); vm.setEmail("changed@example.com"); vm.setRegistering(false); vm.dismissForm()
        runCurrent()
        assertTrue(vm.uiState.value.isSubmitting)
        assertTrue(vm.uiState.value.registering)
        assertEquals("a@example.com", vm.uiState.value.email)
        assertEquals(1, repository.writes)
        assertEquals(1, repository.registrations)
        repository.gate!!.complete(Unit); runCurrent()
        assertFalse(vm.uiState.value.isSubmitting)
        assertEquals("a@example.com", vm.uiState.value.user?.email)
        vm.dismissForm()
        assertEquals("", vm.uiState.value.email)
    }

    @Test fun networkAndCredentialFailuresKeepEmailAndAllowRetry() = runTest(dispatcher) {
        val vm = create(); runCurrent(); vm.setEmail("a@example.com")
        listOf(AuthError.NETWORK, AuthError.INVALID_CREDENTIALS, AuthError.EMAIL_IN_USE, AuthError.TOO_MANY_REQUESTS).forEach { error ->
            repository.failure = AuthFailure(error)
            vm.submit("secret", ""); runCurrent()
            assertEquals(error, vm.uiState.value.error)
            assertEquals("a@example.com", vm.uiState.value.email)
            assertFalse(vm.uiState.value.isSubmitting)
            assertNull(vm.uiState.value.user)
        }
        repository.failure = null
        vm.submit("secret", ""); runCurrent()
        assertNull(vm.uiState.value.error)
        assertNotNull(vm.uiState.value.user)
    }

    @Test fun logoutWorksWithoutNetworkAndRetainsSessionOnFailure() = runTest(dispatcher) {
        repository.user.value = AuthUser("a", "a@example.com")
        val vm = create(); runCurrent()
        repository.failure = AuthFailure(AuthError.UNKNOWN)
        vm.logout(); runCurrent()
        assertNotNull(vm.uiState.value.user)
        assertEquals(AuthError.UNKNOWN, vm.uiState.value.error)
        repository.failure = null
        vm.logout(); vm.logout(); runCurrent()
        assertNull(vm.uiState.value.user)
        assertNull(vm.uiState.value.error)
        assertEquals(2, repository.writes)
    }

    @Test fun sessionFailureCanRetryWithoutAddingCollectorsOrWritingCredentials() = runTest(dispatcher) {
        repository.failObservation = true
        val vm = create(); runCurrent()
        assertTrue(vm.uiState.value.sessionError)
        vm.setEmail("a@example.com"); vm.submit("secret", ""); runCurrent()
        assertEquals(0, repository.writes)
        repository.failObservation = false
        vm.retrySession(); runCurrent()
        assertFalse(vm.uiState.value.sessionError)
        vm.retrySession(); runCurrent()
        assertEquals(1, repository.activeObservers)
        store.clear(); runCurrent()
        assertEquals(0, repository.activeObservers)
    }

    @Test fun coroutineCancellationDoesNotAppearAsAnError() = runTest(dispatcher) {
        val vm = create(); runCurrent()
        repository.failure = CancellationException()
        vm.setEmail("a@example.com"); vm.submit("secret", ""); runCurrent()
        assertFalse(vm.uiState.value.isSubmitting)
        assertNull(vm.uiState.value.error)
    }

    private class FakeAuth : AuthRepository {
        val user = MutableStateFlow<AuthUser?>(null)
        var failObservation = false
        var activeObservers = 0
        var failure: Exception? = null
        var gate: CompletableDeferred<Unit>? = null
        var writes = 0
        var registrations = 0
        var lastEmail: String? = null
        var lastPassword: String? = null
        override fun observeUser() = flow {
            if (failObservation) throw IllegalStateException()
            activeObservers++
            try { emitAll(user) } finally { activeObservers-- }
        }
        override suspend fun login(email: String, password: String) {
            write(); lastEmail = email; lastPassword = password
            user.value = AuthUser(email, email)
        }
        override suspend fun register(email: String, password: String) { registrations++; login(email, password) }
        override suspend fun logout() { write(); user.value = null }
        private suspend fun write() { writes++; gate?.await(); failure?.let { throw it } }
    }
}
