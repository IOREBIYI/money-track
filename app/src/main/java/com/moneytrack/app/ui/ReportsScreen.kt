package com.moneytrack.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.moneytrack.app.data.Transaction
import java.time.YearMonth

@Composable
fun ReportsScreen(all: List<Transaction>, month: YearMonth) {
    val byCategory = remember(all, month) {
        all.filter { !it.isIncome && inMonth(it, month) }
            .groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amountCents } }
            .entries.sortedByDescending { it.value }
    }
    val total = byCategory.sumOf { it.value }

    // Income vs expenses for the 6 months ending at the selected month.
    val trend = remember(all, month) {
        (5 downTo 0L).map { back ->
            val m = month.minusMonths(back)
            val inM = all.filter { inMonth(it, m) }
            Triple(m, inM.filter { it.isIncome }.sumOf { it.amountCents }, inM.filterNot { it.isIncome }.sumOf { it.amountCents })
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Spending by category", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (byCategory.isEmpty()) {
            Text("No expenses this month.")
        }
        byCategory.forEach { (cat, cents) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(cat)
                Text("${money(cents)} · ${(cents * 100 / total)}%")
            }
            Box(
                Modifier.fillMaxWidth().height(10.dp).padding(vertical = 2.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))
            ) {
                Box(
                    Modifier.fillMaxWidth(cents.toFloat() / total).fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp))
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(20.dp))
        Text("Last 6 months", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("■ Income", color = IncomeGreen, style = MaterialTheme.typography.labelMedium)
            Text("■ Expenses", color = ExpenseRed, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(8.dp))
        TrendChart(trend.map { Triple(it.first.month.name.take(3), it.second, it.third) })
    }
}

@Composable
private fun TrendChart(data: List<Triple<String, Long, Long>>) {
    val maxV = (data.maxOfOrNull { maxOf(it.second, it.third) } ?: 0L).coerceAtLeast(1L)
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        val slot = size.width / data.size
        val barW = slot * 0.3f
        data.forEachIndexed { i, (_, inc, exp) ->
            val x = i * slot + slot * 0.15f
            val hi = size.height * inc / maxV
            val he = size.height * exp / maxV
            drawRect(IncomeGreen, Offset(x, size.height - hi), Size(barW, hi))
            drawRect(ExpenseRed, Offset(x + barW + 2f, size.height - he), Size(barW, he))
        }
    }
    Row(Modifier.fillMaxWidth()) {
        data.forEach {
            Text(
                it.first, Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
