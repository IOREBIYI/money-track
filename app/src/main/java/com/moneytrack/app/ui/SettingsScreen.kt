package com.moneytrack.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneytrack.app.MoneyViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: MoneyViewModel, snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch { snackbar.showSnackbar(vm.exportCsv(uri)) }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Backup & restore", style = MaterialTheme.typography.titleMedium)
        Text(
            "Your data is stored only on this device. Export a CSV file regularly to keep a backup " +
                "or to move to a new phone.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = { exporter.launch("moneytrack-backup.csv") }, Modifier.fillMaxWidth()) {
            Text("Export transactions to CSV")
        }
        OutlinedButton(
            onClick = { importer.launch(arrayOf("text/*", "application/csv", "application/octet-stream")) },
            Modifier.fillMaxWidth(),
        ) { Text("Restore from CSV") }

        Spacer(Modifier.height(16.dp))
        Text("Privacy", style = MaterialTheme.typography.titleMedium)
        Text(
            "MoneyTrack has no account, no ads and no internet access. Nothing leaves your phone unless you export it.",
            style = MaterialTheme.typography.bodyMedium,
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
