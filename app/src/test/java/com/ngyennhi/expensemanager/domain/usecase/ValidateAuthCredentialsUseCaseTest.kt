package com.ngyennhi.expensemanager.domain.usecase

import org.junit.Assert.*
import org.junit.Test

class ValidateAuthCredentialsUseCaseTest {
    private val validate = ValidateAuthCredentialsUseCase()

    @Test fun emailAcceptsTrimmedAddressAndRejectsMalformedAddresses() {
        assertTrue(validate("  person+test@example.com  ", "secret", "secret", true).isValid)
        listOf("", "person", "@example.com", "p@", "p@example", "p q@example.com", "p@@example.com").forEach {
            assertTrue(it, validate(it, "secret", "secret", true).invalidEmail)
        }
    }

    @Test fun registrationRequiresSixCharactersAndExactConfirmation() {
        assertTrue(validate("p@example.com", "", "", true).missingPassword)
        assertTrue(validate("p@example.com", "12345", "12345", true).weakPassword)
        assertTrue(validate("p@example.com", "secret", "Secret", true).passwordMismatch)
        assertTrue(validate("p@example.com", " secret ", " secret ", true).isValid)
        assertTrue(validate("p@example.com", " secret ", "secret", true).passwordMismatch)
    }

    @Test fun loginRequiresPasswordButDoesNotApplyNewRegistrationPolicyOrConfirmation() {
        assertTrue(validate("p@example.com", "x", "different", false).isValid)
        assertTrue(validate("p@example.com", " ", "", false).isValid)
        assertTrue(validate("p@example.com", "", "", false).missingPassword)
    }
}
