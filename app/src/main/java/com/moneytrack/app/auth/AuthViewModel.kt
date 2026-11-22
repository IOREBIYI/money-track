package com.moneytrack.app.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val busy: Boolean = false,
    val error: AuthError? = null,
    /** Set after a reset email was requested, to show the confirmation. */
    val resetSentTo: String? = null,
)

class AuthViewModel internal constructor(
    app: Application,
    private val repo: AuthRepository,
) : AndroidViewModel(app) {
    /** What the app uses (via `by viewModels()`): the real backend for this build. */
    constructor(app: Application) : this(app, AuthBackend.create(app))

    val user: StateFlow<AuthUser?> = repo.user

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui

    fun clearMessages() = _ui.update { AuthUiState() }

    fun signIn(email: String, password: String) = perform { repo.signIn(email, password) }

    fun signUp(email: String, password: String) = perform { repo.signUp(email, password) }

    fun sendPasswordReset(email: String) {
        _ui.update { AuthUiState(busy = true) }
        viewModelScope.launch {
            val error = repo.sendPasswordReset(email)
            _ui.update { AuthUiState(error = error, resetSentTo = if (error == null) email.trim() else null) }
        }
    }

    /** Returns the outcome through [onResult] (null = deleted). */
    fun deleteAccount(password: String, onResult: (AuthError?) -> Unit) {
        viewModelScope.launch { onResult(repo.deleteAccount(password)) }
    }

    fun signOut() {
        clearMessages()
        repo.signOut()
    }

    private fun perform(action: suspend () -> AuthError?) {
        _ui.update { AuthUiState(busy = true) }
        viewModelScope.launch {
            val error = action()
            _ui.update { AuthUiState(error = error) }
        }
    }
}
