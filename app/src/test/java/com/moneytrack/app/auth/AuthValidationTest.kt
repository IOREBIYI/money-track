package com.moneytrack.app.auth

import org.junit.Assert.*
import org.junit.Test

class AuthValidationTest {

    // ---------- email ----------

    @Test fun email_acceptsNormalAddresses() {
        listOf(
            "a@b.co", "user@example.com", "first.last@example.com", "user+tag@example.co.uk",
            "o'brien@example.org", "user_name@sub.domain.example.com", "  padded@example.com  ",
        ).forEach { assertTrue("$it should be valid", AuthValidation.isValidEmail(it)) }
    }

    @Test fun email_rejectsBadAddresses() {
        listOf(
            "", "   ", "plain", "@example.com", "user@", "user@example", "user@.com",
            "user@example.", "user name@example.com", "user@exam ple.com", "user@@example.com",
            "user@example.c",
        ).forEach { assertFalse("$it should be invalid", AuthValidation.isValidEmail(it)) }
    }

    // ---------- password ----------

    @Test fun password_acceptsLettersAndDigitsOfEnoughLength() {
        assertNull(AuthValidation.passwordProblem("abcdefg1"))
        assertNull(AuthValidation.passwordProblem("Passw0rd!"))
        assertNull(AuthValidation.passwordProblem("1234567a"))
        assertNull(AuthValidation.passwordProblem("a very long passphrase with 1 number"))
    }

    @Test fun password_tooShort() {
        assertEquals(PasswordProblem.TooShort, AuthValidation.passwordProblem(""))
        assertEquals(PasswordProblem.TooShort, AuthValidation.passwordProblem("abc1"))
        assertEquals(PasswordProblem.TooShort, AuthValidation.passwordProblem("abcdef1")) // 7 chars
    }

    @Test fun password_needsALetter() {
        assertEquals(PasswordProblem.NeedsLetter, AuthValidation.passwordProblem("12345678"))
        assertEquals(PasswordProblem.NeedsLetter, AuthValidation.passwordProblem("1234!@#$"))
    }

    @Test fun password_needsADigit() {
        assertEquals(PasswordProblem.NeedsDigit, AuthValidation.passwordProblem("abcdefgh"))
        assertEquals(PasswordProblem.NeedsDigit, AuthValidation.passwordProblem("Abcdefg!"))
    }

    @Test fun password_lengthIsCheckedBeforeContent() {
        // "abc" is too short AND has no digit; the length message wins.
        assertEquals(PasswordProblem.TooShort, AuthValidation.passwordProblem("abc"))
    }

    @Test fun password_unicodeLettersCount() {
        assertNull(AuthValidation.passwordProblem("pässwörd1"))
    }

    // ---------- messages ----------

    @Test fun everyErrorHasAUserFacingMessage() {
        AuthError.entries.forEach { assertTrue("${it.name} has no text", it.message().isNotBlank()) }
    }

    @Test fun invalidCredentialsMessageDoesNotRevealWhichPartWasWrong() {
        val text = AuthError.InvalidCredentials.message().lowercase()
        assertTrue(text.contains("email or password"))
    }

    @Test fun everyPasswordProblemHasAMessage() {
        PasswordProblem.entries.forEach { assertTrue(it.message().isNotBlank()) }
    }
}
