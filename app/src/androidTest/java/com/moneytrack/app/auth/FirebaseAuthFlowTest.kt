package com.moneytrack.app.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Integration tests: the real [FirebaseAuthRepository] against the local Firebase Auth emulator
 * (host port 9099, reached from the Android emulator as 10.0.2.2). Nothing here talks to a real
 * Firebase project. CI starts the emulator before running these; locally run:
 *
 *   npx firebase-tools emulators:start --only auth --project demo-moneytrack
 *   ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class FirebaseAuthFlowTest {
    companion object {
        private const val APP_NAME = "auth-flow-test"
        private lateinit var auth: FirebaseAuth

        @BeforeClass
        @JvmStatic
        fun connectToEmulator() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val app = FirebaseApp.getApps(context).firstOrNull { it.name == APP_NAME }
                ?: FirebaseApp.initializeApp(
                    context,
                    FirebaseOptions.Builder()
                        .setApiKey("demo-api-key")
                        .setApplicationId("1:000000000000:android:0000000000000000")
                        .setProjectId("demo-moneytrack")
                        .build(),
                    APP_NAME,
                )
            auth = FirebaseAuth.getInstance(app)
            auth.useEmulator("10.0.2.2", 9099)
        }
    }

    private lateinit var repo: FirebaseAuthRepository

    /** A fresh address per test, so tests never depend on each other or on leftover accounts. */
    private val email = "user-${UUID.randomUUID()}@example.com"
    private val password = "correct-horse-7"

    @Before
    fun setUp() {
        auth.signOut()
        repo = FirebaseAuthRepository(auth)
    }

    @After
    fun tearDown() = auth.signOut()

    private fun <T> blocking(block: suspend () -> T): T = runBlocking { withTimeout(30_000) { block() } }

    private fun waitForUser(): AuthUser? = blocking { repo.user.first { it != null } }

    private fun waitForSignedOut() = blocking { repo.user.first { it == null } }

    // ---- sign up ----

    @Test fun signUp_createsTheAccountAndSignsIn() {
        assertNull(blocking { repo.signUp(email, password) })

        val user = waitForUser()
        assertEquals(email, user?.email)
        assertNotNull(user?.uid)
    }

    @Test fun signUp_trimsTheEmail() {
        assertNull(blocking { repo.signUp("  $email  ", password) })
        assertEquals(email, waitForUser()?.email)
    }

    @Test fun signUp_existingEmail_isEmailInUse() {
        assertNull(blocking { repo.signUp(email, password) })
        repo.signOut()
        waitForSignedOut()

        assertEquals(AuthError.EmailInUse, blocking { repo.signUp(email, password) })
    }

    @Test fun signUp_weakPassword_isWeakPassword() {
        assertEquals(AuthError.WeakPassword, blocking { repo.signUp(email, "abc") })
    }

    @Test fun signUp_malformedEmail_isInvalidEmail() {
        assertEquals(AuthError.InvalidEmail, blocking { repo.signUp("not-an-email", password) })
    }

    // ---- sign in ----

    @Test fun signIn_withCorrectPassword_succeeds() {
        assertNull(blocking { repo.signUp(email, password) })
        repo.signOut()
        waitForSignedOut()

        assertNull(blocking { repo.signIn(email, password) })
        assertEquals(email, waitForUser()?.email)
    }

    @Test fun signIn_wrongPassword_isInvalidCredentials() {
        assertNull(blocking { repo.signUp(email, password) })
        repo.signOut()
        waitForSignedOut()

        assertEquals(AuthError.InvalidCredentials, blocking { repo.signIn(email, "wrong-password-1") })
        assertNull(repo.user.value)
    }

    @Test fun signIn_unknownAccount_isInvalidCredentials_notRevealingThatItDoesNotExist() {
        assertEquals(AuthError.InvalidCredentials, blocking { repo.signIn(email, password) })
    }

    @Test fun signOut_clearsTheUser() {
        assertNull(blocking { repo.signUp(email, password) })
        waitForUser()

        repo.signOut()
        waitForSignedOut()
        assertNull(repo.user.value)
    }

    // ---- forgot password ----

    @Test fun passwordReset_forARegisteredAddress_succeeds() {
        assertNull(blocking { repo.signUp(email, password) })
        repo.signOut()
        waitForSignedOut()

        assertNull(blocking { repo.sendPasswordReset(email) })
    }

    @Test fun passwordReset_forAnUnknownAddress_looksTheSame() {
        // Firebase hides whether an address is registered, and so does the repository.
        assertNull(blocking { repo.sendPasswordReset(email) })
    }

    // Malformed addresses are not tested here: the Auth emulator reports them like an unknown
    // user, which the repository deliberately treats as success. The email format is checked by
    // the form before the repository is called (see AuthValidationTest).

    // ---- delete account ----

    @Test fun deleteAccount_withCorrectPassword_removesTheAccount() {
        assertNull(blocking { repo.signUp(email, password) })
        waitForUser()

        assertNull(blocking { repo.deleteAccount(password) })
        waitForSignedOut()

        // The account is really gone: signing in again no longer works.
        assertEquals(AuthError.InvalidCredentials, blocking { repo.signIn(email, password) })
    }

    @Test fun deleteAccount_withWrongPassword_keepsTheAccount() {
        assertNull(blocking { repo.signUp(email, password) })
        waitForUser()

        assertEquals(AuthError.InvalidCredentials, blocking { repo.deleteAccount("wrong-password-1") })

        repo.signOut()
        waitForSignedOut()
        assertNull(blocking { repo.signIn(email, password) })
    }

    @Test fun deleteAccount_whenSignedOut_isAnError() {
        assertEquals(AuthError.Unknown, blocking { repo.deleteAccount(password) })
    }
}
