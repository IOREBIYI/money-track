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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moneytrack.app.data.Categories
import com.moneytrack.app.data.Csv
import com.moneytrack.app.data.Transaction
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

fun inMonth(t: Transaction, m: YearMonth) = YearMonth.from(LocalDate.ofEpochDay(t.epochDay)) == m

@Composable
fun TransactionsScreen(all: List<Transaction>, month: YearMonth, onClick: (Transaction) -> Unit) {
    val list = remember(all, month) { all.filter { inMonth(it, month) } }
    val income = list.filter { it.isIncome }.sumOf { it.amountCents }
    val expense = list.filterNot { it.isIncome }.sumOf { it.amountCents }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Balance", style = MaterialTheme.typography.labelLarge)
                    Text(money(income - expense), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Income  ${money(income)}", color = IncomeGreen)
                        Text("Expenses  ${money(expense)}", color = ExpenseRed)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (list.isEmpty()) {
            item {
                Text(
                    "No transactions this month.\nTap + to add one.",
                    Modifier.fillMaxWidth().padding(top = 48.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        items(list, key = { it.id }) { t ->
            ListItem(
                modifier = Modifier.clickable { onClick(t) },
                headlineContent = { Text(t.category) },
                supportingContent = {
                    Text(LocalDate.ofEpochDay(t.epochDay).label() + if (t.note.isNotBlank()) " · ${t.note}" else "")
                },
                trailingContent = {
                    Text(
                        (if (t.isIncome) "+" else "−") + money(t.amountCents),
                        color = if (t.isIncome) IncomeGreen else ExpenseRed,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditor(
    initial: Transaction?,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
    onDelete: () -> Unit,
) {
    var isIncome by remember { mutableStateOf(initial?.isIncome ?: false) }
    var amount by remember { mutableStateOf(initial?.let { Csv.formatPlain(it.amountCents) } ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: Categories.expense.first()) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var epochDay by remember { mutableLongStateOf(initial?.epochDay ?: LocalDate.now().toEpochDay()) }
    var showDate by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }

    val cents = Csv.parseCents(amount)
    val categories = if (isIncome) Categories.income else Categories.expense

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add transaction" else "Edit transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !isIncome, onClick = {
                        isIncome = false; category = Categories.expense.first()
                    }, label = { Text("Expense") })
                    FilterChip(selected = isIncome, onClick = {
                        isIncome = true; category = Categories.income.first()
                    }, label = { Text("Income") })
                }
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Amount") }, singleLine = true,
                    prefix = { Text(LocalCurrency.current.symbol) },
                    isError = amount.isNotEmpty() && (cents == null || cents <= 0),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Box {
                    OutlinedButton(onClick = { menu = true }, Modifier.fillMaxWidth()) { Text("Category: $category") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        categories.forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { category = it; menu = false })
                        }
                    }
                }
                OutlinedButton(onClick = { showDate = true }, Modifier.fillMaxWidth()) {
                    Text("Date: ${LocalDate.ofEpochDay(epochDay).label()}")
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Note (optional)") }, singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = cents != null && cents > 0,
                onClick = {
                    onSave(
                        Transaction(
                            id = initial?.id ?: 0, isIncome = isIncome, amountCents = cents!!,
                            category = category, note = note.trim(), epochDay = epochDay,
                        )
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (initial != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = ExpenseRed) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = epochDay * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        epochDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}
