package com.moneytrack.app.data

import java.time.LocalDate

/** Plain CSV export/import so users can back up and move their data. */
object Csv {
    private const val HEADER = "date,type,amount,category,note"

    fun export(list: List<Transaction>): String = buildString {
        appendLine(HEADER)
        for (t in list) {
            append(LocalDate.ofEpochDay(t.epochDay)).append(',')
            append(if (t.isIncome) "income" else "expense").append(',')
            append(formatPlain(t.amountCents)).append(',')
            append(quote(t.category)).append(',')
            appendLine(quote(t.note))
        }
    }

    /** Returns parsed transactions; throws IllegalArgumentException on malformed input. */
    fun parse(text: String): List<Transaction> {
        val rows = parseRows(text).filter { it.isNotEmpty() && !(it.size == 1 && it[0].isBlank()) }
        if (rows.isEmpty()) return emptyList()
        require(rows[0].joinToString(",").trim().lowercase() == HEADER) { "Not a MoneyTrack CSV file" }
        return rows.drop(1).mapIndexed { i, r ->
            require(r.size >= 5) { "Row ${i + 2} is incomplete" }
            val cents = parseCents(r[2]) ?: throw IllegalArgumentException("Row ${i + 2}: bad amount")
            val day = runCatching { LocalDate.parse(r[0].trim()).toEpochDay() }
                .getOrElse { throw IllegalArgumentException("Row ${i + 2}: bad date") }
            Transaction(
                isIncome = r[1].trim().equals("income", ignoreCase = true),
                amountCents = cents,
                category = r[3],
                note = r[4],
                epochDay = day,
            )
        }
    }

    fun formatPlain(cents: Long): String = "%d.%02d".format(cents / 100, cents % 100)

    fun parseCents(s: String): Long? {
        val t = s.trim().replace(",", ".")
        if (t.isEmpty()) return null
        val d = t.toBigDecimalOrNull() ?: return null
        if (d.signum() < 0) return null
        return d.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
    }

    private fun quote(s: String) = "\"" + s.replace("\"", "\"\"") + "\""

    private fun parseRows(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { cur.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == ',' -> { row.add(cur.toString()); cur.clear() }
                !inQuotes && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row.add(cur.toString()); cur.clear()
                    rows.add(row); row = mutableListOf()
                }
                else -> cur.append(c)
            }
            i++
        }
        if (cur.isNotEmpty() || row.isNotEmpty()) { row.add(cur.toString()); rows.add(row) }
        return rows
    }
}

object Categories {
    val expense = listOf("Food", "Transport", "Housing", "Bills", "Health", "Shopping", "Entertainment", "Education", "Other")
    val income = listOf("Salary", "Business", "Gift", "Investment", "Other")
}
