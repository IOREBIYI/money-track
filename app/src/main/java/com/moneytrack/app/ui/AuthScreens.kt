package com.moneytrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneytrack.app.auth.AuthValidation
import com.moneytrack.app.auth.AuthViewModel
import com.moneytrack.app.auth.message

private const val PRIVACY_URL = "https://github.com/IOREBIYI/money-track/blob/main/PRIVACY.md"

private enum class AuthRoute { SignIn, SignUp, Forgot }

/** Shows the app only while signed in; otherwise the sign in / sign up / forgot password flow. */
@Composable
fun AuthGate(auth: AuthViewModel, content: @Composable () -> Unit) {
    val user by auth.user.collectAsStateWithLifecycle()
    if (user != null) content() else AuthFlow(auth)
}

@Composable
private fun AuthFlow(auth: AuthViewModel) {
    var route by rememberSaveable { mutableStateOf(AuthRoute.SignIn) }
    var email by rememberSaveable { mutableStateOf("") }
    val ui by auth.ui.collectAsStateWithLifecycle()

    fun go(to: AuthRoute) { auth.clearMessages(); route = to }
    // Typing in any field dismisses the previous server error.
    val onEdit: () -> Unit = { if (ui.error != null) auth.clearMessages() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Brand()
            Spacer(Modifier.height(28.dp))
            when (route) {
                AuthRoute.SignIn -> SignInForm(
                    email = email, onEmail = { email = it }, onEdit = onEdit, busy = ui.busy, error = ui.error?.message(),
                    onSubmit = { pw -> auth.signIn(email, pw) },
                    onForgot = { go(AuthRoute.Forgot) },
                    onCreate = { go(AuthRoute.SignUp) },
                )
                AuthRoute.SignUp -> SignUpForm(
                    email = email, onEmail = { email = it }, onEdit = onEdit, busy = ui.busy, error = ui.error?.message(),
                    onSubmit = { pw -> auth.signUp(email, pw) },
                    onBack = { go(AuthRoute.SignIn) },
                )
                AuthRoute.Forgot -> ForgotForm(
                    email = email, onEmail = { email = it }, onEdit = onEdit, busy = ui.busy, error = ui.error?.message(),
                    sentTo = ui.resetSentTo,
                    onSubmit = { auth.sendPasswordReset(email) },
                    onBack = { go(AuthRoute.SignIn) },
                )
            }
        }
    }
}

@Composable
private fun Brand() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(Marigold)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("M", style = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp), color = Ink)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "MONEYTRACK",
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun Title(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun SignInForm(
    email: String, onEmail: (String) -> Unit, onEdit: () -> Unit, busy: Boolean, error: String?,
    onSubmit: (String) -> Unit, onForgot: () -> Unit, onCreate: () -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    fun submit() {
        localError = when {
            !AuthValidation.isValidEmail(email) -> "Enter a valid email address."
            password.isEmpty() -> "Enter your password."
            else -> null
        }
        if (localError == null) onSubmit(password)
    }

    Title("Welcome back", "Sign in to open your ledger.")
    EmailField(email, { onEmail(it); localError = null; onEdit() })
    Spacer(Modifier.height(12.dp))
    PasswordField("Password", password, { password = it; localError = null; onEdit() }, ImeAction.Done, onDone = ::submit)
    ErrorLine(localError ?: error)
    Spacer(Modifier.height(16.dp))
    PrimaryButton("Sign in", busy, ::submit)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onForgot, enabled = !busy) { Text("Forgot password?") }
    }
    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(12.dp))
    Text("New to MoneyTrack?", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(10.dp))
    OutlinedActionButton("Create an account", !busy, onCreate)
}

@Composable
private fun SignUpForm(
    email: String, onEmail: (String) -> Unit, onEdit: () -> Unit, busy: Boolean, error: String?,
    onSubmit: (String) -> Unit, onBack: () -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val uri = LocalUriHandler.current

    fun submit() {
        localError = when {
            !AuthValidation.isValidEmail(email) -> "Enter a valid email address."
            AuthValidation.passwordProblem(password) != null -> AuthValidation.passwordProblem(password)!!.message()
            password != confirm -> "The two passwords don't match."
            else -> null
        }
        if (localError == null) onSubmit(password)
    }

    Title("Create your account", "Your email is used to sign in and to reset your password.")
    EmailField(email, { onEmail(it); localError = null; onEdit() })
    Spacer(Modifier.height(12.dp))
    PasswordField("Password", password, { password = it; localError = null; onEdit() }, ImeAction.Next)
    Text(
        "At least 8 characters, with a letter and a number.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
    Spacer(Modifier.height(12.dp))
    PasswordField("Confirm password", confirm, { confirm = it; localError = null; onEdit() }, ImeAction.Done, onDone = ::submit)
    ErrorLine(localError ?: error)
    Spacer(Modifier.height(16.dp))
    PrimaryButton("Create account", busy, ::submit)
    Spacer(Modifier.height(8.dp))
    Text(
        "Your transactions and budgets stay on this device. Only your email address is stored by our sign-in provider (Firebase).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    TextButton(onClick = { uri.openUri(PRIVACY_URL) }) { Text("Read the privacy policy") }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(12.dp))
    Text("Already have an account?", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(10.dp))
    OutlinedActionButton("Sign in", !busy, onBack)
}

@Composable
private fun ForgotForm(
    email: String, onEmail: (String) -> Unit, onEdit: () -> Unit, busy: Boolean, error: String?,
    sentTo: String?, onSubmit: () -> Unit, onBack: () -> Unit,
) {
    var localError by remember { mutableStateOf<String?>(null) }

    fun submit() {
        localError = if (AuthValidation.isValidEmail(email)) null else "Enter a valid email address."
        if (localError == null) onSubmit()
    }

    Title("Reset your password", "Enter your email and we'll send you a link to choose a new password.")
    if (sentTo != null) {
        LedgerCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.secondaryContainer) {
            Column(Modifier.padding(16.dp)) {
                Text("Check your inbox", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "If an account exists for $sentTo, a reset link is on its way. It can take a few minutes; check your spam folder too.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Back to sign in", false, onBack)
    } else {
        EmailField(email, { onEmail(it); localError = null; onEdit() }, ImeAction.Done, onDone = ::submit)
        ErrorLine(localError ?: error)
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Send reset link", busy, ::submit)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack, enabled = !busy) { Text("Back to sign in") }
    }
}

// ---------------------------------------------------------------------------------------------
// Small building blocks
// ---------------------------------------------------------------------------------------------

@Composable
private fun EmailField(
    value: String, onChange: (String) -> Unit, ime: ImeAction = ImeAction.Next, onDone: () -> Unit = {},
) {
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text("Email") }, singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ime),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }, onDone = { onDone() }),
    )
}

@Composable
private fun PasswordField(
    label: String, value: String, onChange: (String) -> Unit, ime: ImeAction, onDone: () -> Unit = {},
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ime),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }, onDone = { onDone() }),
        trailingIcon = {
            TextButton(onClick = { visible = !visible }) {
                Text(if (visible) "HIDE" else "SHOW", style = MaterialTheme.typography.labelMedium)
            }
        },
    )
}

@Composable
private fun ErrorLine(text: String?) {
    if (text != null) {
        Spacer(Modifier.height(10.dp))
        Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PrimaryButton(label: String, busy: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !busy, shape = LedgerShape, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(label)
        }
    }
}

@Composable
private fun OutlinedActionButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, enabled = enabled, shape = LedgerShape,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) { Text(label) }
}
