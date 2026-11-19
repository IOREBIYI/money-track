package com.moneytrack.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneytrack.app.MoneyViewModel
import com.moneytrack.app.data.Transaction

private enum class Tab(val label: String, val icon: ImageVector) {
    Transactions("Transactions", Icons.AutoMirrored.Filled.List),
    Budgets("Budgets", Icons.Default.Star),
    Reports("Reports", Icons.Default.DateRange),
    Settings("Settings", Icons.Default.Settings),
}

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
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { LedgerHeader(tab.label) },
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                Column {
                    HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.outline)
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                        Tab.entries.forEach {
                            NavigationBarItem(
                                selected = tab == it,
                                onClick = { tab = it },
                                icon = { Icon(it.icon, contentDescription = null) },
                                label = {
                                    Text(
                                        it.label,
                                        maxLines = 1,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                        ),
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (tab == Tab.Transactions) {
                    FloatingActionButton(
                        onClick = { editing = null; showEditor = true },
                        shape = LedgerShape,
                        containerColor = Marigold,
                        contentColor = Ink,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                        modifier = Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, LedgerShape),
                    ) {
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
private fun LedgerHeader(title: String) {
    Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(22.dp).clip(CircleShape).background(Marigold)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("M", style = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Ink)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "MONEYTRACK",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun MonthSwitcher(month: java.time.YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StepperButton("‹", "Previous month", onPrev)
        Text(month.label(), style = MaterialTheme.typography.titleMedium)
        StepperButton("›", "Next month", onNext)
    }
}

@Composable
private fun StepperButton(symbol: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(LedgerShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, LedgerShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp))
    }
}
