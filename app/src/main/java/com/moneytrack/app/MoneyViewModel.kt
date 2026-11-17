package com.moneytrack.app

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneytrack.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

class MoneyViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()

    val transactions: StateFlow<List<Transaction>> = dao.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val budgets: StateFlow<List<Budget>> = dao.observeBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _currency = MutableStateFlow(
        Currencies.byCode(prefs.getString("currency", null)) ?: Currencies.default()
    )
    val currency: StateFlow<AppCurrency> = _currency

    /** Display only: existing amounts are NOT converted. */
    fun setCurrency(c: AppCurrency) {
        _currency.value = c
        prefs.edit().putString("currency", c.code).apply()
    }

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month

    fun shiftMonth(delta: Long) { _month.value = _month.value.plusMonths(delta) }

    fun save(t: Transaction) = viewModelScope.launch { dao.upsert(t) }
    fun delete(t: Transaction) = viewModelScope.launch { dao.delete(t) }

    fun setBudget(category: String, limitCents: Long) = viewModelScope.launch {
        if (limitCents > 0) dao.upsertBudget(Budget(category, limitCents)) else dao.deleteBudget(category)
    }

    /** Returns a short result message for the UI. */
    suspend fun exportCsv(uri: Uri): String = withContext(Dispatchers.IO) {
        runCatching {
            val text = Csv.export(dao.allTransactions())
            getApplication<Application>().contentResolver.openOutputStream(uri)!!
                .use { it.write(text.toByteArray()) }
            "Exported successfully"
        }.getOrElse { "Export failed: ${it.message}" }
    }

    /** Replaces all transactions with the contents of the CSV. */
    suspend fun importCsv(uri: Uri): String = withContext(Dispatchers.IO) {
        runCatching {
            val text = getApplication<Application>().contentResolver.openInputStream(uri)!!
                .use { it.readBytes().toString(Charsets.UTF_8) }
            val list = Csv.parse(text)
            dao.replaceAll(list)
            "Restored ${list.size} transactions"
        }.getOrElse { "Import failed: ${it.message}" }
    }
}
