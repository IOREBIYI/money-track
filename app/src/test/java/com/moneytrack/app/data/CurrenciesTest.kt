package com.moneytrack.app.data

import org.junit.Assert.*
import org.junit.Test
import java.util.Currency
import java.util.Locale

class CurrenciesTest {

    private val usd = Currencies.byCode("USD")!!
    private fun cur(code: String) = Currencies.byCode(code)!!

    // ---------- list integrity ----------

    @Test fun codesAreUnique() {
        val codes = Currencies.all.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test fun everyCodeIsARealIsoCurrency() {
        Currencies.all.forEach { c ->
            assertNotNull("${c.code} is not a valid ISO 4217 code", runCatching { Currency.getInstance(c.code) }.getOrNull())
        }
    }

    @Test fun everyEntryHasNameAndSymbol() {
        Currencies.all.forEach { c ->
            assertTrue("${c.code} name blank", c.name.isNotBlank())
            assertTrue("${c.code} symbol blank", c.symbol.isNotBlank())
        }
    }

    @Test fun listIncludesTheMainCurrencies() {
        listOf("USD", "EUR", "GBP", "NGN", "GHS", "KES", "ZAR", "INR", "JPY", "CAD", "AUD").forEach {
            assertNotNull("$it missing", Currencies.byCode(it))
        }
    }

    // ---------- lookup and default ----------

    @Test fun byCode_unknownOrNull_isNull() {
        assertNull(Currencies.byCode("XXX"))
        assertNull(Currencies.byCode(null))
        assertNull(Currencies.byCode(""))
    }

    @Test fun default_usesDeviceCurrencyWhenSupported() {
        assertEquals("NGN", Currencies.default(Locale("en", "NG")).code)
        assertEquals("GBP", Currencies.default(Locale.UK).code)
        assertEquals("JPY", Currencies.default(Locale.JAPAN).code)
        assertEquals("EUR", Currencies.default(Locale.GERMANY).code)
    }

    @Test fun default_fallsBackToUsdWhenUnsupportedOrUnknown() {
        assertEquals("USD", Currencies.default(Locale.ROOT).code)         // no country, no currency
        assertEquals("USD", Currencies.default(Locale("en", "IS")).code)  // ISK is not in our list
    }

    // ---------- formatting ----------

    @Test fun format_usdWithGrouping() {
        assertEquals("$1,234.56", formatMoney(123_456, usd, Locale.US))
        assertEquals("$0.00", formatMoney(0, usd, Locale.US))
        assertEquals("$0.05", formatMoney(5, usd, Locale.US))
        assertEquals("$1,000,000.00", formatMoney(100_000_000, usd, Locale.US))
    }

    @Test fun format_negativeAmountsPutSignFirst() {
        assertEquals("-$18.75", formatMoney(-1_875, usd, Locale.US))
        assertEquals("-₦2,500.00", formatMoney(-250_000, cur("NGN"), Locale.US))
    }

    @Test fun format_symbolsPerCurrency() {
        assertEquals("€10.00", formatMoney(1_000, cur("EUR"), Locale.US))
        assertEquals("£10.00", formatMoney(1_000, cur("GBP"), Locale.US))
        assertEquals("₦10.00", formatMoney(1_000, cur("NGN"), Locale.US))
        assertEquals("GH₵10.00", formatMoney(1_000, cur("GHS"), Locale.US))
        assertEquals("₹10.00", formatMoney(1_000, cur("INR"), Locale.US))
        assertEquals("R10.00", formatMoney(1_000, cur("ZAR"), Locale.US))
        assertEquals("CA$10.00", formatMoney(1_000, cur("CAD"), Locale.US))
    }

    @Test fun format_letterOnlyMultiCharSymbolsGetASpace() {
        assertEquals("CHF 10.00", formatMoney(1_000, cur("CHF"), Locale.US))
        assertEquals("KSh 10.00", formatMoney(1_000, cur("KES"), Locale.US))
        assertEquals("AED 10.00", formatMoney(1_000, cur("AED"), Locale.US))
    }

    @Test fun format_singleLetterSymbolHasNoSpace() {
        assertEquals("R10.00", formatMoney(1_000, cur("ZAR"), Locale.US))
    }

    @Test fun format_alwaysTwoDecimals_evenForYen() {
        // Amounts are stored as hundredths, so 0-decimal currencies still show two places.
        assertEquals("¥500.00", formatMoney(50_000, cur("JPY"), Locale.US))
    }

    @Test fun format_respectsLocaleSeparators() {
        assertEquals("€1.234,56", formatMoney(123_456, cur("EUR"), Locale.GERMANY))
    }

    @Test fun format_extremeValuesDoNotOverflow() {
        assertEquals("$92,233,720,368,547,758.07", formatMoney(Long.MAX_VALUE, usd, Locale.US))
        assertEquals("-$92,233,720,368,547,758.08", formatMoney(Long.MIN_VALUE, usd, Locale.US))
    }

    @Test fun switchingCurrencyDoesNotChangeTheNumber() {
        // Display only: 12.34 stays 12.34 in every currency (no conversion).
        Currencies.all.forEach { c ->
            assertTrue("${c.code}: ${formatMoney(1_234, c, Locale.US)}", formatMoney(1_234, c, Locale.US).endsWith("12.34"))
        }
    }
}
