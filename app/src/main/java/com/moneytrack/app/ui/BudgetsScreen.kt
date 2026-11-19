package com.moneytrack.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytrack.app.data.Budget
import com.moneytrack.app.data.Categories
import com.moneytrack.app.data.Csv
import com.moneytrack.app.data.Transaction
import java.time.YearMonth

@Composable
fun BudgetsScreen(
    all: List<Transaction>,
    budgets: List<Budget>,
    month: YearMonth,
    onSet: (String, Long) -> Unit,
) {
    val spent = remember(all, month) {
        all.filter { !it.isIncome && inMonth(it, month) }
            .groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amountCents } }
    }
    val limits = budgets.associate { it.category to it.limitCents }
    var editing by remember { mutableStateOf<String?>(null) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 24.dp)) {
        item {
            Text(
                "Set a monthly limit per category. Tap a row to change it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
        items(Categories.expense) { cat ->
            val s = spent[cat] ?: 0L
            val limit = limits[cat]
            val over = limit != null && s > limit
            Column(Modifier.fillMaxWidth().clickable { editing = cat }.padding(vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryTile(cat, 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(cat, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(
                        if (limit != null) "${money(s)} / ${money(limit)}" else "${money(s)} · no limit",
                        style = MoneyStyle.copy(fontSize = 13.sp),
                        color = if (over) ExpenseRed else MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (limit != null) {
                    val fraction = s.toFloat() / limit
                    Spacer(Modifier.height(10.dp))
                    LedgerBar(
                        fraction = fraction,
                        color = when {
                            over -> ExpenseRed
                            fraction >= 0.75f -> Marigold
                            else -> MaterialTheme.colorScheme.primary
                        },
                    )
                    Spacer(Modifier.height(4.dp))
                    if (over) {
                        Text("Over budget by ${money(s - limit)}", color = ExpenseRed, style = MaterialTheme.typography.labelMedium)
                    } else {
                        Text(
                            "${money(limit - s)} left",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }

    editing?.let { cat ->
        var text by remember(cat) { mutableStateOf(limits[cat]?.let { Csv.formatPlain(it) } ?: "") }
        val cents = Csv.parseCents(text)
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("$cat budget") },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text("Monthly limit (empty = none)") }, singleLine = true,
                    prefix = { Text(LocalCurrency.current.symbol) },
                    isError = text.isNotEmpty() && cents == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = text.isEmpty() || cents != null,
                    onClick = { onSet(cat, if (text.isEmpty()) 0 else cents!!); editing = null },
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
}
