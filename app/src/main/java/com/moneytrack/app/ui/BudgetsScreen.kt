package com.moneytrack.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text(
                "Set a monthly limit per category. Tap a row to change it.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
        }
        items(Categories.expense) { cat ->
            val s = spent[cat] ?: 0L
            val limit = limits[cat]
            Column(Modifier.fillMaxWidth().clickable { editing = cat }.padding(vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(cat, style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (limit != null) "${money(s)} / ${money(limit)}" else "${money(s)} · no limit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (limit != null && s > limit) ExpenseRed else MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (limit != null) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (s.toFloat() / limit).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (s > limit) ExpenseRed else MaterialTheme.colorScheme.primary,
                    )
                    if (s > limit) {
                        Text("Over budget by ${money(s - limit)}", color = ExpenseRed, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            HorizontalDivider()
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
