package com.moneytrack.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytrack.app.data.Transaction
import java.time.YearMonth

/**
 * Share of [total] taken by [part], rounded to the nearest whole percent.
 * A non-zero share that rounds to 0 is shown as "<1%" rather than a misleading "0%".
 */
internal fun percentLabel(part: Long, total: Long): String {
    if (total <= 0L || part <= 0L) return "0%"
    val pct = Math.round(part * 100.0 / total)
    return if (pct == 0L) "<1%" else "$pct%"
}

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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("SPENDING BY CATEGORY", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp), color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        if (byCategory.isEmpty()) {
            Text(
                "No expenses this month.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        byCategory.forEach { (cat, cents) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(cat, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text("${money(cents)} · ${percentLabel(cents, total)}", style = MoneyStyle.copy(fontSize = 13.sp))
            }
            Spacer(Modifier.height(6.dp))
            LedgerBar(fraction = cents.toFloat() / total, color = categoryColor(cat), height = 16.dp)
            Spacer(Modifier.height(14.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text("LAST 6 MONTHS", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp), color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("■ Income", color = IncomeGreen, style = MaterialTheme.typography.labelMedium)
            Text("■ Expenses", color = ExpenseRed, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(10.dp))
        TrendChart(trend.map { Triple(it.first.month.name.take(3), it.second, it.third) })
    }
}

@Composable
private fun TrendChart(data: List<Triple<String, Long, Long>>) {
    val maxV = (data.maxOfOrNull { maxOf(it.second, it.third) } ?: 0L).coerceAtLeast(1L)
    val outline = MaterialTheme.colorScheme.outline
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        val stroke = Stroke(width = 1.5.dp.toPx())
        val slot = size.width / data.size
        val barW = slot * 0.3f
        data.forEachIndexed { i, (_, inc, exp) ->
            val x = i * slot + slot * 0.15f
            val hi = size.height * inc / maxV
            val he = size.height * exp / maxV
            if (hi > 0f) {
                drawRect(IncomeGreen, Offset(x, size.height - hi), Size(barW, hi))
                drawRect(outline, Offset(x, size.height - hi), Size(barW, hi), style = stroke)
            }
            if (he > 0f) {
                drawRect(ExpenseRed, Offset(x + barW + 4f, size.height - he), Size(barW, he))
                drawRect(outline, Offset(x + barW + 4f, size.height - he), Size(barW, he), style = stroke)
            }
        }
        // baseline
        drawLine(outline, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.5.dp.toPx())
    }
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth()) {
        data.forEach {
            Text(
                it.first, Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
