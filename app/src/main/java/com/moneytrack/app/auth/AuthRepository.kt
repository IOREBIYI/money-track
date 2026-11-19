package com.moneytrack.app.auth

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseOptions
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.moneytrack.app.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import java.io.IOException

/** All account operations return null on success, or the [AuthError] that occurred. */
interface AuthRepository {
    val user: StateFlow<AuthUser?>
    suspend fun signIn(email: String, password: String): AuthError?
    suspend fun signUp(email: String, password: String): AuthError?
    suspend fun sendPasswordReset(email: String): AuthError?
    suspend fun deleteAccount(password: String): AuthError?
    fun signOut()
}

/** Used when the build has no Firebase project, so the app explains instead of crashing. */
object UnconfiguredAuthRepository : AuthRepository {
    override val user: StateFlow<AuthUser?> = MutableStateFlow(null)
    override suspend fun signIn(email: String, password: String) = AuthError.NotConfigured
    override suspend fun signUp(email: String, password: String) = AuthError.NotConfigured
    override suspend fun sendPasswordReset(email: String) = AuthError.NotConfigured
    override suspend fun deleteAccount(password: String) = AuthError.NotConfigured
    override fun signOut() = Unit
}

class FirebaseAuthRepository(private val auth: FirebaseAuth) : AuthRepository {
    private val _user = MutableStateFlow(auth.currentUser?.toAuthUser())
    override val user: StateFlow<AuthUser?> = _user

    init {
        auth.addAuthStateListener { _user.value = it.currentUser?.toAuthUser() }
    }

    override suspend fun signIn(email: String, password: String) =
        attempt { auth.signInWithEmailAndPassword(email.trim(), password).await() }

    override suspend fun signUp(email: String, password: String) =
        attempt { auth.createUserWithEmailAndPassword(email.trim(), password).await() }

    /**
     * Firebase hides whether an address is registered (to prevent probing), so an unknown
     * address is treated the same as a known one.
     */
    override suspend fun sendPasswordReset(email: String): AuthError? {
        val error = attempt { auth.sendPasswordResetEmail(email.trim()).await() }
        return if (error == AuthError.InvalidCredentials) null else error
    }

    /** Re-checks the password first (Firebase requires a recent sign-in), then deletes the account. */
    override suspend fun deleteAccount(password: String): AuthError? {
        val current = auth.currentUser ?: return AuthError.Unknown
        val email = current.email ?: return AuthError.Unknown
        return attempt {
            current.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
            current.delete().await()
        }
    }

    override fun signOut() = auth.signOut()

    private suspend fun attempt(block: suspend () -> Unit): AuthError? = try {
        block()
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        mapAuthException(e)
    }
}

private fun FirebaseUser.toAuthUser() = AuthUser(uid = uid, email = email)

/** Order matters: the weak-password exception is a subtype of the invalid-credentials one. */
fun mapAuthException(e: Throwable): AuthError = when (e) {
    is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword
    is FirebaseAuthUserCollisionException -> AuthError.EmailInUse
    is FirebaseAuthRecentLoginRequiredException -> AuthError.RequiresRecentLogin
    is FirebaseAuthInvalidUserException -> AuthError.InvalidCredentials
    is FirebaseAuthInvalidCredentialsException ->
        if (e.errorCode == "ERROR_INVALID_EMAIL") AuthError.InvalidEmail else AuthError.InvalidCredentials
    is FirebaseNetworkException -> AuthError.Network
    is FirebaseTooManyRequestsException -> AuthError.TooManyRequests
    else -> if (e.looksLikeNetworkProblem()) AuthError.Network else AuthError.Unknown
}

/**
 * When the server cannot be reached at all (connection refused, DNS failure, timeout) Firebase
 * often reports a generic "internal error" instead of a dedicated network exception.
 */
private fun Throwable.looksLikeNetworkProblem(): Boolean {
    if (this is FirebaseAuthException && errorCode == "ERROR_NETWORK_REQUEST_FAILED") return true
    var cause: Throwable? = this
    repeat(5) {
        if (cause is IOException) return true
        cause = cause?.cause
    }
    val text = message.orEmpty()
    return listOf("Failed to connect", "Unable to resolve host", "timed out", "Network is unreachable")
        .any { text.contains(it, ignoreCase = true) }
}

/** Debug builds can point at the local Firebase Auth emulator; release builds never do. */
object AuthBackend {
    fun init(app: Application) {
        if (!BuildConfig.USE_AUTH_EMULATOR) return
        if (FirebaseApp.getApps(app).isEmpty()) {
            FirebaseApp.initializeApp(
                app,
                FirebaseOptions.Builder()
                    .setApiKey("demo-api-key")
                    .setApplicationId("1:000000000000:android:0000000000000000")
                    .setProjectId("demo-moneytrack")
                    .build(),
            )
        }
        FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
    }

    fun create(app: Application): AuthRepository =
        if (FirebaseApp.getApps(app).isEmpty()) UnconfiguredAuthRepository
        else FirebaseAuthRepository(FirebaseAuth.getInstance())
}
