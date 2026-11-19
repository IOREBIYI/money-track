package com.moneytrack.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytrack.app.data.Currencies
import com.moneytrack.app.data.formatMoney
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

// ---------------------------------------------------------------------------
// "Ledger" palette: warm paper, deep ink, forest green and marigold.
// ---------------------------------------------------------------------------
val Ink = Color(0xFF17211F)
val Paper = Color(0xFFF4EFE6)
val PaperLight = Color(0xFFFFFBF2)
val Forest = Color(0xFF1E5B47)
val ForestDeep = Color(0xFF173F32)
val Marigold = Color(0xFFF2B632)
val MarigoldSoft = Color(0xFFF7DE9A)

val IncomeGreen = Color(0xFF2E8B57)
val ExpenseRed = Color(0xFFD2512F)

// Fixed colours for text that sits on the always-dark balance card.
val HeroLabel = Color(0xFFBFD8CC)
val HeroIncome = Color(0xFF9BE0B8)
val HeroExpense = Color(0xFFFFB59E)

private val LightScheme = lightColorScheme(
    primary = Forest, onPrimary = PaperLight,
    primaryContainer = MarigoldSoft, onPrimaryContainer = Ink,
    secondary = Marigold, onSecondary = Ink,
    secondaryContainer = MarigoldSoft, onSecondaryContainer = Ink,
    tertiary = Color(0xFF3B6E8F), onTertiary = PaperLight,
    background = Paper, onBackground = Ink,
    surface = PaperLight, onSurface = Ink,
    surfaceVariant = Color(0xFFE9E1D0), onSurfaceVariant = Color(0xFF4B5653),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = PaperLight,
    surfaceContainer = PaperLight,
    surfaceContainerHigh = PaperLight,
    surfaceContainerHighest = Color(0xFFF0E9DA),
    outline = Ink, outlineVariant = Color(0xFFCDC4B0),
    error = ExpenseRed, onError = Color.White,
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF7CC9A7), onPrimary = Color(0xFF0B241C),
    primaryContainer = Color(0xFF4A3D14), onPrimaryContainer = MarigoldSoft,
    secondary = Marigold, onSecondary = Ink,
    secondaryContainer = Color(0xFF4A3D14), onSecondaryContainer = MarigoldSoft,
    tertiary = Color(0xFF8FBBD9), onTertiary = Color(0xFF0B2230),
    background = Color(0xFF12171A), onBackground = Color(0xFFEDE7DA),
    surface = Color(0xFF1A2125), onSurface = Color(0xFFEDE7DA),
    surfaceVariant = Color(0xFF2A3338), onSurfaceVariant = Color(0xFFB9C2BE),
    surfaceContainerLowest = Color(0xFF0E1214),
    surfaceContainerLow = Color(0xFF161C1F),
    surfaceContainer = Color(0xFF1A2125),
    surfaceContainerHigh = Color(0xFF222A2F),
    surfaceContainerHighest = Color(0xFF2A3338),
    outline = Color(0xFFCFC8B8), outlineVariant = Color(0xFF3B464B),
    error = Color(0xFFFF8A6B), onError = Color(0xFF3A0D00),
)

// ---------------------------------------------------------------------------
// Type: serif for headings, monospace for figures, so numbers line up like a ledger.
// ---------------------------------------------------------------------------
private val Serif = FontFamily.Serif
private val Mono = FontFamily.Monospace

val MoneyStyle = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 15.sp)

private val LedgerTypography = Typography(
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    titleSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    labelLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.4.sp),
    labelMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp),
)

val LedgerShape = RoundedCornerShape(6.dp)

private val LedgerShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(10.dp),
)

@Composable
fun MoneyTheme(content: @Composable () -> Unit) {
    // Deliberately no dynamic (wallpaper) colour: the app keeps its own identity on every phone.
    val scheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme
    MaterialTheme(colorScheme = scheme, typography = LedgerTypography, shapes = LedgerShapes, content = content)
}

// ---------------------------------------------------------------------------
// Shared building blocks
// ---------------------------------------------------------------------------

/** A card with a hard outline and an offset block shadow, like a stamped ledger slip. */
@Composable
fun LedgerCard(
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    val outline = MaterialTheme.colorScheme.outline
    val shadow = if (isSystemInDarkTheme()) outline.copy(alpha = 0.3f) else outline
    Box(modifier.padding(end = 5.dp, bottom = 5.dp)) {
        Box(Modifier.matchParentSize().offset(5.dp, 5.dp).background(shadow, LedgerShape))
        Surface(shape = LedgerShape, color = container, border = BorderStroke(1.5.dp, outline)) { content() }
    }
}

/** A thick, outlined progress bar. */
@Composable
fun LedgerBar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 14.dp) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier.fillMaxWidth().height(height).clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, shape)
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(color))
    }
}

private val categoryColors = mapOf(
    "Food" to Color(0xFFF7DE9A),
    "Transport" to Color(0xFFB9D9EE),
    "Housing" to Color(0xFFBFE3CE),
    "Bills" to Color(0xFFCDD3E6),
    "Health" to Color(0xFFF6C3CB),
    "Shopping" to Color(0xFFDCC7EE),
    "Entertainment" to Color(0xFFFFC9A8),
    "Education" to Color(0xFFA9E0DC),
    "Salary" to Color(0xFFBFE3CE),
    "Business" to Color(0xFFB9D9EE),
    "Gift" to Color(0xFFF6C3CB),
    "Investment" to Color(0xFFF7DE9A),
    "Other" to Color(0xFFDAD5C8),
)

fun categoryColor(category: String): Color = categoryColors[category] ?: Color(0xFFDAD5C8)

/** A coloured tile showing the category's initial. */
@Composable
fun CategoryTile(category: String, size: Dp = 42.dp) {
    Box(
        Modifier.size(size).clip(LedgerShape)
            .background(categoryColor(category))
            .border(1.5.dp, MaterialTheme.colorScheme.outline, LedgerShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            category.take(1).uppercase(),
            style = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.45f).sp),
            color = Ink,
        )
    }
}

/** The currency amounts are currently displayed in; provided by [AppRoot] from the saved setting. */
val LocalCurrency = compositionLocalOf { Currencies.all.first() }

@Composable
@ReadOnlyComposable
fun money(cents: Long): String = formatMoney(cents, LocalCurrency.current)

fun YearMonth.label(): String =
    "${month.getDisplayName(DateTextStyle.FULL, Locale.getDefault())} $year"

fun LocalDate.label(): String = format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
