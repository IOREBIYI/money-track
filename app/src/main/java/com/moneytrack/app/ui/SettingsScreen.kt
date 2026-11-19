package com.moneytrack.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneytrack.app.MoneyViewModel
import com.moneytrack.app.auth.AuthViewModel
import com.moneytrack.app.auth.message
import com.moneytrack.app.data.Currencies
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: MoneyViewModel, auth: AuthViewModel, snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showDeleteAccount by remember { mutableStateOf(false) }
    val currency by vm.currency.collectAsStateWithLifecycle()
    val account by auth.user.collectAsStateWithLifecycle()

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch { snackbar.showSnackbar(vm.exportCsv(uri)) }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Account", style = MaterialTheme.typography.titleMedium)
        Text(
            "Signed in as ${account?.email ?: "unknown"}",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(
            onClick = { auth.signOut() },
            Modifier.fillMaxWidth(),
            shape = LedgerShape,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        ) { Text("Sign out") }
        TextButton(onClick = { showDeleteAccount = true }, Modifier.fillMaxWidth()) {
            Text("Delete my account", color = ExpenseRed)
        }

        Spacer(Modifier.height(8.dp))
        Text("Currency", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(
            onClick = { showCurrencyPicker = true },
            Modifier.fillMaxWidth(),
            shape = LedgerShape,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        ) {
            Text("${currency.code} · ${currency.name} (${currency.symbol})")
        }
        Text(
            "Changing the currency only changes how amounts are displayed. Existing amounts are not converted.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(8.dp))
        Text("Backup & restore", style = MaterialTheme.typography.titleMedium)
        Text(
            "Your data is stored only on this device. Export a CSV file regularly to keep a backup " +
                "or to move to a new phone.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = { exporter.launch("moneytrack-backup.csv") },
            Modifier.fillMaxWidth(),
            shape = LedgerShape,
        ) {
            Text("Export transactions to CSV")
        }
        OutlinedButton(
            onClick = { importer.launch(arrayOf("text/*", "application/csv", "application/octet-stream")) },
            Modifier.fillMaxWidth(),
            shape = LedgerShape,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        ) { Text("Restore from CSV") }

        Spacer(Modifier.height(16.dp))
        Text("Privacy", style = MaterialTheme.typography.titleMedium)
        Text(
            "Your transactions and budgets never leave this device, and there are no ads. " +
                "The internet is used only to sign you in and to send password-reset emails. " +
                "Your email address is handled by Firebase Authentication.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    if (showCurrencyPicker) {
        AlertDialog(
            onDismissRequest = { showCurrencyPicker = false },
            title = { Text("Choose currency") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(Currencies.all, key = { it.code }) { c ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { vm.setCurrency(c); showCurrencyPicker = false }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = c.code == currency.code, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.bodyLarge)
                                Text(c.code, style = MaterialTheme.typography.labelMedium)
                            }
                            Text(c.symbol, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCurrencyPicker = false }) { Text("Close") } },
        )
    }

    if (showDeleteAccount) {
        var password by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { if (!busy) showDeleteAccount = false },
            title = { Text("Delete your account?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "This permanently deletes your sign-in account and email address. " +
                            "Your transactions and budgets stay on this device, but you'll need a new account to open the app again.",
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("Confirm with your password") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && password.isNotEmpty(),
                    onClick = {
                        busy = true; error = null
                        auth.deleteAccount(password) { result ->
                            busy = false
                            if (result == null) showDeleteAccount = false else error = result.message()
                        }
                    },
                ) { Text(if (busy) "Deleting…" else "Delete", color = ExpenseRed) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { showDeleteAccount = false }) { Text("Cancel") } },
        )
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Replace all transactions?") },
            text = { Text("Restoring will delete your current transactions and replace them with the file's contents.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch { snackbar.showSnackbar(vm.importCsv(uri)) }
                }) { Text("Replace", color = ExpenseRed) }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Cancel") } },
        )
    }
}
