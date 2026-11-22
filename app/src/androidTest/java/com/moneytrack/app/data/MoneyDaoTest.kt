package com.moneytrack.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Integration tests: the real Room/SQLite stack on a device, using an in-memory database. */
@RunWith(AndroidJUnit4::class)
class MoneyDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: MoneyDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.dao()
    }

    @After
    fun tearDown() = db.close()

    private fun tx(day: Long, cents: Long = 1000, income: Boolean = false, category: String = "Food") =
        Transaction(isIncome = income, amountCents = cents, category = category, epochDay = day)

    @Test
    fun insertedTransactionsComeBackNewestFirst() = runTest {
        dao.upsert(tx(day = 10))
        dao.upsert(tx(day = 30))
        dao.upsert(tx(day = 20))

        assertEquals(listOf(30L, 20L, 10L), dao.allTransactions().map { it.epochDay })
        assertEquals(listOf(30L, 20L, 10L), dao.observeTransactions().first().map { it.epochDay })
    }

    @Test
    fun deleteRemovesOnlyThatTransaction() = runTest {
        dao.upsert(tx(day = 1))
        dao.upsert(tx(day = 2))
        val toDelete = dao.allTransactions().first { it.epochDay == 1L }

        dao.delete(toDelete)

        assertEquals(listOf(2L), dao.allTransactions().map { it.epochDay })
    }

    @Test
    fun replaceAllSwapsTheWholeSet() = runTest {
        dao.upsert(tx(day = 1))
        dao.upsert(tx(day = 2))

        dao.replaceAll(listOf(tx(day = 99, cents = 555, income = true, category = "Salary")))

        val all = dao.allTransactions()
        assertEquals(1, all.size)
        assertEquals(99L, all[0].epochDay)
        assertEquals(555L, all[0].amountCents)
        assertTrue(all[0].isIncome)
    }

    @Test
    fun budgetUpsertReplacesAndDeleteRemoves() = runTest {
        dao.upsertBudget(Budget("Food", 10_000))
        dao.upsertBudget(Budget("Food", 25_000))
        dao.upsertBudget(Budget("Rent", 80_000))

        val budgets = dao.observeBudgets().first().associate { it.category to it.limitCents }
        assertEquals(mapOf("Food" to 25_000L, "Rent" to 80_000L), budgets)

        dao.deleteBudget("Food")
        assertEquals(listOf("Rent"), dao.observeBudgets().first().map { it.category })
    }

    @Test
    fun csvExportThenRestoreRoundTripsThroughTheDatabase() = runTest {
        val original = listOf(
            tx(day = 5, cents = 1234, category = "Food"),
            tx(day = 6, cents = 99_900, income = true, category = "Salary"),
        )
        dao.insertAll(original)

        val exported = dao.allTransactions()
        dao.replaceAll(emptyList())
        assertTrue(dao.allTransactions().isEmpty())

        dao.replaceAll(exported.map { it.copy(id = 0) })
        val restored = dao.allTransactions()
        assertEquals(exported.map { Triple(it.epochDay, it.amountCents, it.isIncome) },
            restored.map { Triple(it.epochDay, it.amountCents, it.isIncome) })
    }
}
