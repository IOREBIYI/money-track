package com.moneytrack.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneytrack.app.MoneyViewModel
import com.moneytrack.app.data.Transaction

private enum class Tab(val label: String, val icon: ImageVector) {
    Transactions("Transactions", Icons.AutoMirrored.Filled.List),
    Budgets("Budgets", Icons.Default.Star),
    Reports("Reports", Icons.Default.DateRange),
    Settings("Settings", Icons.Default.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: MoneyViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.Transactions) }
    var editing by remember { mutableStateOf<Transaction?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val transactions by vm.transactions.collectAsStateWithLifecycle()
    val budgets by vm.budgets.collectAsStateWithLifecycle()
    val month by vm.month.collectAsStateWithLifecycle()
    val currency by vm.currency.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalCurrency provides currency) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(tab.label) }) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach {
                    NavigationBarItem(
                        selected = tab == it,
                        onClick = { tab = it },
                        icon = { Icon(it.icon, contentDescription = null) },
                        label = { Text(it.label, maxLines = 1) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == Tab.Transactions) {
                FloatingActionButton(onClick = { editing = null; showEditor = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add transaction")
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (tab != Tab.Settings) {
                MonthSwitcher(month, onPrev = { vm.shiftMonth(-1) }, onNext = { vm.shiftMonth(1) })
            }
            when (tab) {
                Tab.Transactions -> TransactionsScreen(transactions, month) { editing = it; showEditor = true }
                Tab.Budgets -> BudgetsScreen(transactions, budgets, month, onSet = vm::setBudget)
                Tab.Reports -> ReportsScreen(transactions, month)
                Tab.Settings -> SettingsScreen(vm, snackbar)
            }
        }
    }

    if (showEditor) {
        TransactionEditor(
            initial = editing,
            onDismiss = { showEditor = false },
            onSave = { vm.save(it); showEditor = false },
            onDelete = { editing?.let(vm::delete); showEditor = false },
        )
    }
    }
}

@Composable
private fun MonthSwitcher(month: java.time.YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onPrev) { Text("‹ Prev") }
        Text(month.label(), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onNext) { Text("Next ›") }
    }
}
