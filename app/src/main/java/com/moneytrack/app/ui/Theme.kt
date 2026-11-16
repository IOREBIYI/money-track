package com.moneytrack.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val IncomeGreen = Color(0xFF2E9D5B)
val ExpenseRed = Color(0xFFD64545)

private val Light = lightColorScheme(primary = Color(0xFF0F766E), secondary = Color(0xFF3B6E8F))
private val Dark = darkColorScheme(primary = Color(0xFF4FD1C5), secondary = Color(0xFF8FBBD9))

@Composable
fun MoneyTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

fun money(cents: Long): String = NumberFormat.getCurrencyInstance().format(cents / 100.0)

fun YearMonth.label(): String =
    "${month.getDisplayName(TextStyle.FULL, Locale.getDefault())} $year"

fun LocalDate.label(): String = format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
