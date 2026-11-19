package com.moneytrack.app.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthErrorMappingTest {

    @Test fun wrongPasswordMapsToInvalidCredentials() {
        val e = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "bad")
        assertEquals(AuthError.InvalidCredentials, mapAuthException(e))
    }

    @Test fun badEmailFormatMapsToInvalidEmail() {
        val e = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_EMAIL", "bad email")
        assertEquals(AuthError.InvalidEmail, mapAuthException(e))
    }

    @Test fun unknownUserIsReportedAsInvalidCredentials() {
        // Never reveal whether an address is registered.
        val e = FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "no user")
        assertEquals(AuthError.InvalidCredentials, mapAuthException(e))
    }

    @Test fun existingEmailMapsToEmailInUse() {
        val e = FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "taken")
        assertEquals(AuthError.EmailInUse, mapAuthException(e))
    }

    @Test fun weakPasswordWinsOverItsInvalidCredentialsParent() {
        // FirebaseAuthWeakPasswordException extends FirebaseAuthInvalidCredentialsException.
        val e = FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "weak", "too short")
        assertEquals(AuthError.WeakPassword, mapAuthException(e))
    }

    @Test fun recentLoginRequired() {
        val e = FirebaseAuthRecentLoginRequiredException("ERROR_REQUIRES_RECENT_LOGIN", "stale")
        assertEquals(AuthError.RequiresRecentLogin, mapAuthException(e))
    }

    @Test fun networkProblem() {
        assertEquals(AuthError.Network, mapAuthException(FirebaseNetworkException("offline")))
    }

    @Test fun rateLimited() {
        assertEquals(AuthError.TooManyRequests, mapAuthException(FirebaseTooManyRequestsException("slow down")))
    }

    @Test fun connectionRefusedReportedAsInternalErrorMapsToNetwork() {
        // What Firebase actually threw when the server was unreachable on the emulator.
        val e = FirebaseAuthException(
            "ERROR_INTERNAL_ERROR", "An internal error has occurred. [ Failed to connect to /10.0.2.2:9099 ]",
        )
        assertEquals(AuthError.Network, mapAuthException(e))
    }

    @Test fun networkRequestFailedCodeMapsToNetwork() {
        val e = FirebaseAuthException("ERROR_NETWORK_REQUEST_FAILED", "A network error occurred")
        assertEquals(AuthError.Network, mapAuthException(e))
    }

    @Test fun anIoExceptionAnywhereInTheCauseChainMapsToNetwork() {
        val e = RuntimeException("outer", IllegalStateException("middle", java.io.IOException("socket closed")))
        assertEquals(AuthError.Network, mapAuthException(e))
    }

    @Test fun dnsAndTimeoutMessagesMapToNetwork() {
        assertEquals(AuthError.Network, mapAuthException(RuntimeException("Unable to resolve host \"x\"")))
        assertEquals(AuthError.Network, mapAuthException(RuntimeException("Connection timed out")))
    }

    @Test fun otherInternalErrorsStayUnknown() {
        val e = FirebaseAuthException("ERROR_INTERNAL_ERROR", "Something else broke")
        assertEquals(AuthError.Unknown, mapAuthException(e))
    }

    @Test fun anythingElseIsUnknown() {
        assertEquals(AuthError.Unknown, mapAuthException(IllegalStateException("boom")))
        assertEquals(AuthError.Unknown, mapAuthException(RuntimeException()))
    }
}
