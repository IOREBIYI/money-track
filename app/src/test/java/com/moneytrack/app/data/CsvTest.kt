package com.moneytrack.app.data

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class CsvTest {

    private fun tx(
        income: Boolean = false,
        cents: Long = 1250,
        category: String = "Food",
        note: String = "",
        date: LocalDate = LocalDate.of(2026, 11, 16),
    ) = Transaction(isIncome = income, amountCents = cents, category = category, note = note, epochDay = date.toEpochDay())

    /** Compares everything except the auto-generated id, which is not part of the CSV. */
    private fun assertSameData(expected: List<Transaction>, actual: List<Transaction>) {
        assertEquals(expected.map { it.copy(id = 0) }, actual.map { it.copy(id = 0) })
    }

    // ---------- export ----------

    @Test fun export_emptyList_isHeaderOnly() {
        assertEquals("date,type,amount,category,note", Csv.export(emptyList()).trim())
    }

    @Test fun export_formatsRowsAsExpected() {
        val out = Csv.export(listOf(tx(income = true, cents = 320000, category = "Salary", note = "Nov")))
        assertEquals("2026-11-16,income,3200.00,\"Salary\",\"Nov\"", out.lines()[1])
    }

    @Test fun export_expenseTypeIsLabelledExpense() {
        assertTrue(Csv.export(listOf(tx())).lines()[1].contains(",expense,"))
    }

    // ---------- round trips ----------

    @Test fun roundTrip_simple() {
        val data = listOf(tx(), tx(income = true, cents = 500000, category = "Salary"))
        assertSameData(data, Csv.parse(Csv.export(data)))
    }

    @Test fun roundTrip_noteWithCommaQuoteAndNewline() {
        val data = listOf(tx(note = "Lunch, \"team\" outing\nsecond line"))
        assertSameData(data, Csv.parse(Csv.export(data)))
    }

    @Test fun roundTrip_unicodeAndEmptyNote() {
        val data = listOf(tx(note = "Café ☕ – ₦500"), tx(note = ""))
        assertSameData(data, Csv.parse(Csv.export(data)))
    }

    @Test fun roundTrip_smallAndLargeAmounts() {
        val data = listOf(tx(cents = 1), tx(cents = 99), tx(cents = 100), tx(cents = 123_456_789_00))
        assertSameData(data, Csv.parse(Csv.export(data)))
    }

    @Test fun roundTrip_manyRows() {
        val data = (1..500).map { tx(cents = it * 37L, note = "n$it", date = LocalDate.of(2026, 1, 1).plusDays(it.toLong())) }
        assertSameData(data, Csv.parse(Csv.export(data)))
    }

    // ---------- parse: accepted formats ----------

    @Test fun parse_headerOnly_returnsEmpty() {
        assertTrue(Csv.parse("date,type,amount,category,note\n").isEmpty())
    }

    @Test fun parse_emptyText_returnsEmpty() {
        assertTrue(Csv.parse("").isEmpty())
    }

    @Test fun parse_windowsLineEndings() {
        val text = "date,type,amount,category,note\r\n2026-11-16,expense,12.50,\"Food\",\"x\"\r\n"
        val list = Csv.parse(text)
        assertEquals(1, list.size)
        assertEquals(1250L, list[0].amountCents)
    }

    @Test fun parse_noTrailingNewline() {
        val list = Csv.parse("date,type,amount,category,note\n2026-11-16,income,5,Gift,hi")
        assertEquals(1, list.size)
        assertTrue(list[0].isIncome)
        assertEquals(500L, list[0].amountCents)
    }

    @Test fun parse_unquotedFieldsAndBlankLines() {
        val text = "date,type,amount,category,note\n\n2026-11-16,expense,3.2,Food,snack\n\n"
        val list = Csv.parse(text)
        assertEquals(1, list.size)
        assertEquals(320L, list[0].amountCents)
        assertEquals("snack", list[0].note)
    }

    @Test fun parse_headerIsCaseInsensitive() {
        assertEquals(1, Csv.parse("Date,Type,Amount,Category,Note\n2026-11-16,expense,1,Food,\n").size)
    }

    @Test fun parse_typeIsCaseInsensitive() {
        val list = Csv.parse("date,type,amount,category,note\n2026-11-16,INCOME,1,Gift,\n")
        assertTrue(list[0].isIncome)
    }

    @Test fun parse_utf8ByteOrderMarkIsIgnored() {
        // Excel and some editors prepend a BOM when saving CSV.
        val list = Csv.parse("﻿date,type,amount,category,note\n2026-11-16,expense,1,Food,\n")
        assertEquals(1, list.size)
    }

    // ---------- parse: rejected input ----------

    @Test fun parse_wrongHeader_throws() {
        assertThrows(IllegalArgumentException::class.java) { Csv.parse("a,b,c\n1,2,3\n") }
    }

    @Test fun parse_badDate_throwsWithRowNumber() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            Csv.parse("date,type,amount,category,note\nnot-a-date,expense,1,Food,\n")
        }
        assertTrue(e.message!!.contains("Row 2"))
    }

    @Test fun parse_badAmount_throwsWithRowNumber() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            Csv.parse("date,type,amount,category,note\n2026-11-16,expense,1,Food,\n2026-11-16,expense,abc,Food,\n")
        }
        assertTrue(e.message!!.contains("Row 3"))
    }

    @Test fun parse_negativeAmount_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Csv.parse("date,type,amount,category,note\n2026-11-16,expense,-5,Food,\n")
        }
    }

    @Test fun parse_shortRow_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Csv.parse("date,type,amount,category,note\n2026-11-16,expense,5\n")
        }
    }

    // ---------- parseCents ----------

    @Test fun parseCents_wholeAndDecimal() {
        assertEquals(1200L, Csv.parseCents("12"))
        assertEquals(1250L, Csv.parseCents("12.5"))
        assertEquals(1205L, Csv.parseCents("12.05"))
        assertEquals(5L, Csv.parseCents("0.05"))
        assertEquals(0L, Csv.parseCents("0"))
    }

    @Test fun parseCents_acceptsCommaAsDecimalSeparator() {
        assertEquals(1250L, Csv.parseCents("12,5"))
    }

    @Test fun parseCents_trimsWhitespace() {
        assertEquals(1000L, Csv.parseCents("  10  "))
    }

    @Test fun parseCents_roundsHalfUpBeyondTwoDecimals() {
        assertEquals(1L, Csv.parseCents("0.005"))
        assertEquals(0L, Csv.parseCents("0.004"))
        assertEquals(1001L, Csv.parseCents("10.005"))
    }

    @Test fun parseCents_rejectsInvalid() {
        assertNull(Csv.parseCents(""))
        assertNull(Csv.parseCents("   "))
        assertNull(Csv.parseCents("abc"))
        assertNull(Csv.parseCents("-1"))
        assertNull(Csv.parseCents("1.2.3"))
        assertNull(Csv.parseCents("12abc"))
    }

    @Test fun parseCents_noFloatingPointDrift() {
        // 0.1 + 0.2 style errors must not appear: values are parsed as decimals, not doubles.
        assertEquals(1999L, Csv.parseCents("19.99"))
        assertEquals(110L, Csv.parseCents("1.10"))
        assertEquals(29L, Csv.parseCents("0.29"))
    }

    // ---------- formatPlain ----------

    @Test fun formatPlain_padsCents() {
        assertEquals("0.00", Csv.formatPlain(0))
        assertEquals("0.05", Csv.formatPlain(5))
        assertEquals("0.50", Csv.formatPlain(50))
        assertEquals("12.34", Csv.formatPlain(1234))
        assertEquals("1000000.00", Csv.formatPlain(100_000_000))
    }

    @Test fun formatPlain_andParseCents_areInverse() {
        for (c in listOf(0L, 1L, 9L, 10L, 99L, 100L, 101L, 123456L, 9_999_999_99L)) {
            assertEquals(c, Csv.parseCents(Csv.formatPlain(c)))
        }
    }
}
