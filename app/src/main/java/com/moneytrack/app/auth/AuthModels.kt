package com.moneytrack.app.auth

data class AuthUser(val uid: String, val email: String?)

/** Everything that can go wrong with an account action, kept independent of Firebase. */
enum class AuthError {
    InvalidEmail,
    InvalidCredentials,
    EmailInUse,
    WeakPassword,
    Network,
    TooManyRequests,
    RequiresRecentLogin,
    NotConfigured,
    Unknown,
}

enum class PasswordProblem { TooShort, NeedsLetter, NeedsDigit }

object AuthValidation {
    const val MIN_PASSWORD_LENGTH = 8

    private val emailRegex =
        Regex("^[A-Za-z0-9._%+'-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean = emailRegex.matches(email.trim())

    /** Passwords need 8+ characters with at least one letter and one digit. */
    fun passwordProblem(password: String): PasswordProblem? = when {
        password.length < MIN_PASSWORD_LENGTH -> PasswordProblem.TooShort
        password.none { it.isLetter() } -> PasswordProblem.NeedsLetter
        password.none { it.isDigit() } -> PasswordProblem.NeedsDigit
        else -> null
    }
}

/** User-facing text for an [AuthError]. */
fun AuthError.message(): String = when (this) {
    AuthError.InvalidEmail -> "That email address doesn't look right."
    AuthError.InvalidCredentials -> "Incorrect email or password."
    AuthError.EmailInUse -> "An account with this email already exists. Try signing in."
    AuthError.WeakPassword -> "Choose a stronger password (8+ characters, with a letter and a number)."
    AuthError.Network -> "Can't reach the server. Check your internet connection and try again."
    AuthError.TooManyRequests -> "Too many attempts. Please wait a few minutes and try again."
    AuthError.RequiresRecentLogin -> "For your security, please enter your password again."
    AuthError.NotConfigured -> "Accounts aren't set up in this build of the app."
    AuthError.Unknown -> "Something went wrong. Please try again."
}

fun PasswordProblem.message(): String = when (this) {
    PasswordProblem.TooShort -> "Password must be at least ${AuthValidation.MIN_PASSWORD_LENGTH} characters."
    PasswordProblem.NeedsLetter -> "Password needs at least one letter."
    PasswordProblem.NeedsDigit -> "Password needs at least one number."
}
