package com.ngyennhi.expensemanager.domain.model

data class AuthUser(val id: String, val email: String?)

enum class AuthError {
    INVALID_CREDENTIALS, EMAIL_IN_USE, WEAK_PASSWORD, NETWORK, TOO_MANY_REQUESTS,
    USER_DISABLED, NOT_CONFIGURED, UNKNOWN,
}

// Never retain SDK exceptions, backend messages or credentials in public errors.
class AuthFailure(val reason: AuthError) : Exception(reason.name)

data class AuthValidation(
    val invalidEmail: Boolean = false,
    val missingPassword: Boolean = false,
    val weakPassword: Boolean = false,
    val passwordMismatch: Boolean = false,
) {
    val isValid: Boolean get() = !invalidEmail && !missingPassword && !weakPassword && !passwordMismatch
}
