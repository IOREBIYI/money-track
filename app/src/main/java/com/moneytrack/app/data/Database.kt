package com.moneytrack.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val isIncome: Boolean,
    val amountCents: Long,
    val category: String,
    val note: String = "",
    val epochDay: Long,
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val category: String,
    val limitCents: Long,
)

@Dao
interface MoneyDao {
    @Query("SELECT * FROM transactions ORDER BY epochDay DESC, id DESC")
    fun observeTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY epochDay DESC, id DESC")
    suspend fun allTransactions(): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(t: Transaction)

    @Insert
    suspend fun insertAll(list: List<Transaction>)

    @Delete
    suspend fun delete(t: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("SELECT * FROM budgets")
    fun observeBudgets(): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(b: Budget)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteBudget(category: String)

    @androidx.room.Transaction
    suspend fun replaceAll(list: List<Transaction>) {
        clearTransactions()
        insertAll(list)
    }
}

@Database(entities = [Transaction::class, Budget::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): MoneyDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "moneytrack.db"
            ).build().also { instance = it }
        }
    }
}
