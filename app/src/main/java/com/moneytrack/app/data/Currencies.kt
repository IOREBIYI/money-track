package com.moneytrack.app.data

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

data class AppCurrency(val code: String, val name: String, val symbol: String)

object Currencies {
    val all: List<AppCurrency> = listOf(
        AppCurrency("USD", "US Dollar", "$"),
        AppCurrency("EUR", "Euro", "€"),
        AppCurrency("GBP", "British Pound", "£"),
        AppCurrency("NGN", "Nigerian Naira", "₦"),
        AppCurrency("GHS", "Ghanaian Cedi", "GH₵"),
        AppCurrency("KES", "Kenyan Shilling", "KSh"),
        AppCurrency("UGX", "Ugandan Shilling", "USh"),
        AppCurrency("TZS", "Tanzanian Shilling", "TSh"),
        AppCurrency("ZAR", "South African Rand", "R"),
        AppCurrency("EGP", "Egyptian Pound", "E£"),
        AppCurrency("XOF", "West African CFA Franc", "CFA"),
        AppCurrency("INR", "Indian Rupee", "₹"),
        AppCurrency("PKR", "Pakistani Rupee", "Rs"),
        AppCurrency("BDT", "Bangladeshi Taka", "৳"),
        AppCurrency("JPY", "Japanese Yen", "¥"),
        AppCurrency("CNY", "Chinese Yuan", "CN¥"),
        AppCurrency("KRW", "South Korean Won", "₩"),
        AppCurrency("IDR", "Indonesian Rupiah", "Rp"),
        AppCurrency("PHP", "Philippine Peso", "₱"),
        AppCurrency("SGD", "Singapore Dollar", "S$"),
        AppCurrency("MYR", "Malaysian Ringgit", "RM"),
        AppCurrency("THB", "Thai Baht", "฿"),
        AppCurrency("VND", "Vietnamese Dong", "₫"),
        AppCurrency("AED", "UAE Dirham", "AED"),
        AppCurrency("SAR", "Saudi Riyal", "SAR"),
        AppCurrency("TRY", "Turkish Lira", "₺"),
        AppCurrency("CAD", "Canadian Dollar", "CA$"),
        AppCurrency("AUD", "Australian Dollar", "A$"),
        AppCurrency("NZD", "New Zealand Dollar", "NZ$"),
        AppCurrency("MXN", "Mexican Peso", "MX$"),
        AppCurrency("BRL", "Brazilian Real", "R$"),
        AppCurrency("ARS", "Argentine Peso", "AR$"),
        AppCurrency("CHF", "Swiss Franc", "CHF"),
        AppCurrency("SEK", "Swedish Krona", "kr"),
        AppCurrency("NOK", "Norwegian Krone", "kr"),
        AppCurrency("DKK", "Danish Krone", "kr"),
        AppCurrency("PLN", "Polish Zloty", "zł"),
        AppCurrency("RUB", "Russian Ruble", "₽"),
        AppCurrency("UAH", "Ukrainian Hryvnia", "₴"),
    )

    fun byCode(code: String?): AppCurrency? = all.firstOrNull { it.code == code }

    /** The device's own currency if we support it, otherwise US Dollar. */
    fun default(locale: Locale = Locale.getDefault()): AppCurrency {
        val code = runCatching { Currency.getInstance(locale).currencyCode }.getOrNull()
        return byCode(code) ?: all.first()
    }
}

/**
 * Formats an amount held in minor units (cents) for display, e.g. 123456 -> "$1,234.56".
 * Always shows two decimals because amounts are stored as whole hundredths.
 * This only changes how a number is shown; it never converts between currencies.
 */
fun formatMoney(cents: Long, currency: AppCurrency, locale: Locale = Locale.getDefault()): String {
    val nf = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    val body = nf.format(BigDecimal.valueOf(cents).abs().movePointLeft(2))
    val sign = if (cents < 0) "-" else ""
    // Letter-only multi-character symbols read better with a space ("CHF 5.00", "KSh 5.00").
    val gap = if (currency.symbol.length > 1 && currency.symbol.all { it.isLetter() }) " " else ""
    return sign + currency.symbol + gap + body
}
