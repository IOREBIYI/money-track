package com.moneytrack.app.auth

import android.app.Application
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** A repository whose answers the test controls. [gate] lets a test hold an action "in flight". */
private class FakeAuthRepository : AuthRepository {
    override val user: StateFlow<AuthUser?> = MutableStateFlow(null)
    var result: AuthError? = null
    var gate: CompletableDeferred<Unit>? = null
    val calls = mutableListOf<String>()
    var signedOut = false

    private suspend fun answer(call: String): AuthError? {
        calls += call
        gate?.await()
        return result
    }

    override suspend fun signIn(email: String, password: String) = answer("signIn:$email")
    override suspend fun signUp(email: String, password: String) = answer("signUp:$email")
    override suspend fun sendPasswordReset(email: String) = answer("reset:$email")
    override suspend fun deleteAccount(password: String) = answer("delete")
    override fun signOut() {
        signedOut = true
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeAuthRepository
    private lateinit var vm: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = FakeAuthRepository()
        vm = AuthViewModel(Application(), repo)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.settle() = advanceUntilIdle()

    // ---- sign in ----

    @Test fun signIn_success_endsIdleWithNoError() = runTest(dispatcher) {
        vm.signIn("a@b.co", "password1")
        settle()
        assertEquals(AuthUiState(), vm.ui.value)
        assertEquals(listOf("signIn:a@b.co"), repo.calls)
    }

    @Test fun signIn_isBusyWhileInFlight() = runTest(dispatcher) {
        repo.gate = CompletableDeferred()
        vm.signIn("a@b.co", "password1")
        runCurrent()
        assertTrue(vm.ui.value.busy)

        repo.gate!!.complete(Unit)
        settle()
        assertFalse(vm.ui.value.busy)
    }

    @Test fun signIn_wrongPassword_showsInvalidCredentials() = runTest(dispatcher) {
        repo.result = AuthError.InvalidCredentials
        vm.signIn("a@b.co", "wrong")
        settle()
        assertEquals(AuthError.InvalidCredentials, vm.ui.value.error)
        assertFalse(vm.ui.value.busy)
    }

    @Test fun signIn_offline_showsNetworkError() = runTest(dispatcher) {
        repo.result = AuthError.Network
        vm.signIn("a@b.co", "password1")
        settle()
        assertEquals(AuthError.Network, vm.ui.value.error)
    }

    @Test fun anEarlierErrorIsClearedWhenTheNextAttemptStarts() = runTest(dispatcher) {
        repo.result = AuthError.InvalidCredentials
        vm.signIn("a@b.co", "wrong")
        settle()
        assertEquals(AuthError.InvalidCredentials, vm.ui.value.error)

        repo.result = null
        repo.gate = CompletableDeferred()
        vm.signIn("a@b.co", "password1")
        runCurrent()
        assertNull(vm.ui.value.error)
        assertTrue(vm.ui.value.busy)

        repo.gate!!.complete(Unit)
        settle()
        assertEquals(AuthUiState(), vm.ui.value)
    }

    // ---- sign up ----

    @Test fun signUp_success_endsIdleWithNoError() = runTest(dispatcher) {
        vm.signUp("new@b.co", "password1")
        settle()
        assertEquals(AuthUiState(), vm.ui.value)
        assertEquals(listOf("signUp:new@b.co"), repo.calls)
    }

    @Test fun signUp_existingEmail_showsEmailInUse() = runTest(dispatcher) {
        repo.result = AuthError.EmailInUse
        vm.signUp("taken@b.co", "password1")
        settle()
        assertEquals(AuthError.EmailInUse, vm.ui.value.error)
    }

    @Test fun signUp_weakPassword_showsWeakPassword() = runTest(dispatcher) {
        repo.result = AuthError.WeakPassword
        vm.signUp("a@b.co", "abc")
        settle()
        assertEquals(AuthError.WeakPassword, vm.ui.value.error)
    }

    // ---- forgot password ----

    @Test fun passwordReset_success_confirmsTheTrimmedAddress() = runTest(dispatcher) {
        vm.sendPasswordReset("  a@b.co  ")
        settle()
        assertEquals("a@b.co", vm.ui.value.resetSentTo)
        assertNull(vm.ui.value.error)
        assertFalse(vm.ui.value.busy)
    }

    @Test fun passwordReset_isBusyWhileInFlight() = runTest(dispatcher) {
        repo.gate = CompletableDeferred()
        vm.sendPasswordReset("a@b.co")
        runCurrent()
        assertTrue(vm.ui.value.busy)
        assertNull(vm.ui.value.resetSentTo)

        repo.gate!!.complete(Unit)
        settle()
        assertFalse(vm.ui.value.busy)
    }

    @Test fun passwordReset_failure_showsErrorAndNoConfirmation() = runTest(dispatcher) {
        repo.result = AuthError.TooManyRequests
        vm.sendPasswordReset("a@b.co")
        settle()
        assertEquals(AuthError.TooManyRequests, vm.ui.value.error)
        assertNull(vm.ui.value.resetSentTo)
    }

    @Test fun clearMessages_removesErrorAndConfirmation() = runTest(dispatcher) {
        vm.sendPasswordReset("a@b.co")
        settle()
        assertEquals("a@b.co", vm.ui.value.resetSentTo)

        vm.clearMessages()
        assertEquals(AuthUiState(), vm.ui.value)
    }

    // ---- delete account and sign out ----

    @Test fun deleteAccount_reportsSuccessThroughTheCallback() = runTest(dispatcher) {
        var outcome: AuthError? = AuthError.Unknown
        vm.deleteAccount("password1") { outcome = it }
        settle()
        assertNull(outcome)
        assertEquals(listOf("delete"), repo.calls)
    }

    @Test fun deleteAccount_wrongPassword_reportsTheErrorThroughTheCallback() = runTest(dispatcher) {
        repo.result = AuthError.InvalidCredentials
        var outcome: AuthError? = null
        vm.deleteAccount("wrong") { outcome = it }
        settle()
        assertEquals(AuthError.InvalidCredentials, outcome)
    }

    @Test fun signOut_signsOutAndClearsMessages() = runTest(dispatcher) {
        repo.result = AuthError.InvalidCredentials
        vm.signIn("a@b.co", "wrong")
        settle()
        assertEquals(AuthError.InvalidCredentials, vm.ui.value.error)

        vm.signOut()
        assertTrue(repo.signedOut)
        assertEquals(AuthUiState(), vm.ui.value)
    }

    @Test fun userComesFromTheRepository() {
        assertNull(vm.user.value)
        assertEquals(repo.user, vm.user)
    }
}
