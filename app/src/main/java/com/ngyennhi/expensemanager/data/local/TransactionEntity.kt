
package com.ngyennhi.expensemanager.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    @ColumnInfo(name = "amount_vnd") val amountVnd: Long,
    val category: String,
    @ColumnInfo(name = "date_epoch_day") val dateEpochDay: Long,
    val note: String?,
)
