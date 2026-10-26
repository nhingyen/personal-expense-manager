package com.ngyennhi.expensemanager.domain.usecase

import com.ngyennhi.expensemanager.domain.model.AuthValidation

class ValidateAuthCredentialsUseCase {
    operator fun invoke(email: String, password: String, confirmation: String, registering: Boolean) = AuthValidation(
        invalidEmail = !EMAIL.matches(email.trim()),
        missingPassword = password.isEmpty(),
        weakPassword = registering && password.isNotEmpty() && password.length < 6,
        passwordMismatch = registering && password != confirmation,
    )

    private companion object {
        val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
