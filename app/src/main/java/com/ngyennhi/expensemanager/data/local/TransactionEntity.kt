
package com.ngyennhi.expensemanager.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions", foreignKeys = [androidx.room.ForeignKey(
    entity = WalletEntity::class, parentColumns = ["id"], childColumns = ["wallet_id"],
    onDelete = androidx.room.ForeignKey.RESTRICT,
)], indices = [androidx.room.Index("wallet_id")])
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    @ColumnInfo(name = "amount_vnd") val amountVnd: Long,
    val category: String,
    @ColumnInfo(name = "date_epoch_day") val dateEpochDay: Long,
    val note: String?,
    @ColumnInfo(name = "wallet_id", defaultValue = "1") val walletId: Long = 1,
)
