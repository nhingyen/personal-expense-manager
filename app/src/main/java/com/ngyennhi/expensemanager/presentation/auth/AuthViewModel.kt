package com.ngyennhi.expensemanager.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngyennhi.expensemanager.domain.model.AuthError
import com.ngyennhi.expensemanager.domain.model.AuthFailure
import com.ngyennhi.expensemanager.domain.model.AuthUser
import com.ngyennhi.expensemanager.domain.model.AuthValidation
import com.ngyennhi.expensemanager.domain.repository.AuthRepository
import com.ngyennhi.expensemanager.domain.usecase.ValidateAuthCredentialsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val user: AuthUser? = null,
    val isInitialized: Boolean = false,
    val sessionError: Boolean = false,
    val isSubmitting: Boolean = false,
    val registering: Boolean = false,
    val email: String = "",
    val validation: AuthValidation = AuthValidation(),
    val error: AuthError? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val validate: ValidateAuthCredentialsUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val uiState = mutableState.asStateFlow()
    private var observation: Job? = null

    init { retrySession() }

    fun retrySession() {
        observation?.cancel()
        mutableState.update { it.copy(sessionError = false) }
        observation = viewModelScope.launch {
            try {
                repository.observeUser().collect { user ->
                    mutableState.update { it.copy(user = user, isInitialized = true, sessionError = false) }
                }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(isInitialized = true, sessionError = true) }
            }
        }
    }

    fun startLogin() {
        if (uiState.value.isSubmitting) return
        mutableState.update { it.copy(registering = false, email = "", validation = AuthValidation(), error = null) }
    }

    fun setEmail(email: String) {
        if (!uiState.value.isSubmitting) mutableState.update {
            it.copy(email = email, validation = it.validation.copy(invalidEmail = false), error = null)
        }
    }

    fun setRegistering(registering: Boolean) {
        if (!uiState.value.isSubmitting) mutableState.update {
            it.copy(registering = registering, validation = AuthValidation(), error = null)
        }
    }

    fun clearPasswordErrors() {
        if (!uiState.value.isSubmitting) mutableState.update {
            it.copy(validation = it.validation.copy(missingPassword = false, weakPassword = false, passwordMismatch = false), error = null)
        }
    }

    // Passwords are request arguments only: never StateFlow/SavedStateHandle/persistent state.
    fun submit(password: String, confirmation: String) {
        val state = uiState.value
        if (state.isSubmitting || !state.isInitialized || state.sessionError || state.user != null) return
        val validation = validate(state.email, password, confirmation, state.registering)
        mutableState.update { it.copy(validation = validation, error = null) }
        if (!validation.isValid) return
        request {
            if (state.registering) repository.register(state.email.trim(), password)
            else repository.login(state.email.trim(), password)
        }
    }

    fun logout() {
        if (uiState.value.user != null) request { repository.logout() }
    }

    // The Activity owns the credential picker coroutine and cancels it on destruction.
    // Only the existing request state and repository handle the result; no token is retained.
    suspend fun loginWithGoogle(getIdToken: suspend () -> String?) {
        val state = uiState.value
        if (!state.isInitialized || state.sessionError || state.user != null || !beginRequest()) return
        mutableState.update { it.copy(validation = AuthValidation()) }
        executeRequest {
            val token = getIdToken() ?: return@executeRequest // User dismissed the picker.
            repository.loginWithGoogle(token)
        }
    }

    fun dismissForm() {
        if (!uiState.value.isSubmitting) mutableState.update {
            it.copy(email = "", registering = false, validation = AuthValidation(), error = null)
        }
    }

    private fun request(action: suspend () -> Unit) {
        if (!beginRequest()) return
        viewModelScope.launch { executeRequest(action) }
    }

    private fun beginRequest(): Boolean {
        if (uiState.value.isSubmitting) return false
        mutableState.update { it.copy(isSubmitting = true, error = null) }
        return true
    }

    private suspend fun executeRequest(action: suspend () -> Unit) {
        try { action()
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (failure: AuthFailure) {
            mutableState.update { it.copy(error = failure.reason) }
        } catch (_: Exception) {
            mutableState.update { it.copy(error = AuthError.UNKNOWN) }
        } finally { mutableState.update { it.copy(isSubmitting = false) } }
    }
}
