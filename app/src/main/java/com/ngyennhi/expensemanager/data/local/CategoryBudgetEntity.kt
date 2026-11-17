package com.ngyennhi.expensemanager.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "category_budgets",
    primaryKeys = ["category_id", "year", "month"],
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["category_id"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [Index("category_id")],
)
data class CategoryBudgetEntity(
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val year: Int,
    val month: Int,
    @ColumnInfo(name = "limit_vnd") val limitVnd: Long,
)
