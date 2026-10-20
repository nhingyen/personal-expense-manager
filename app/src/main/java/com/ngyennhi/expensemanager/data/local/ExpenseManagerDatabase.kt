package com.ngyennhi.expensemanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TransactionEntity::class], version = 1, exportSchema = true)
abstract class ExpenseManagerDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}
