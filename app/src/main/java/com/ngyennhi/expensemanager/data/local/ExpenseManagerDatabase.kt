package com.ngyennhi.expensemanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Locale

@Database(entities = [TransactionEntity::class, WalletEntity::class, CategoryEntity::class], version = 3, exportSchema = true)
abstract class ExpenseManagerDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        private const val DEFAULT_WALLET_SQL = "INSERT OR IGNORE INTO wallets (id, name, type, opening_balance_vnd) VALUES (1, '', 'CASH', 0)"
        val SEED_DEFAULT_WALLET = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) { db.execSQL(DEFAULT_WALLET_SQL) }
        }
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS wallets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, opening_balance_vnd INTEGER NOT NULL)")
                db.execSQL(DEFAULT_WALLET_SQL)
                // Rebuild to install a required FK; copy IDs and every existing value verbatim.
                db.execSQL("CREATE TABLE transactions_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, amount_vnd INTEGER NOT NULL, category TEXT NOT NULL, date_epoch_day INTEGER NOT NULL, note TEXT, wallet_id INTEGER NOT NULL DEFAULT 1, FOREIGN KEY(wallet_id) REFERENCES wallets(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                db.execSQL("INSERT INTO transactions_new (id, type, amount_vnd, category, date_epoch_day, note, wallet_id) SELECT id, type, amount_vnd, category, date_epoch_day, note, 1 FROM transactions")
                // Preserve the sequence even when the highest allocated record was deleted.
                db.execSQL("UPDATE sqlite_sequence SET seq = MAX(seq, COALESCE((SELECT seq FROM sqlite_sequence WHERE name = 'transactions'), 0)) WHERE name = 'transactions_new'")
                db.execSQL("DROP TABLE transactions")
                db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
                db.execSQL("CREATE INDEX index_transactions_wallet_id ON transactions(wallet_id)")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, name TEXT NOT NULL, normalized_name TEXT NOT NULL, is_archived INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_type_normalized_name ON categories(type, normalized_name)")

                // Kotlin normalization is deliberate: SQLite lower()/NOCASE only handle ASCII.
                val seen = mutableSetOf<Pair<String, String>>()
                db.query("SELECT type, category FROM transactions ORDER BY id").use { cursor ->
                    val typeIndex = cursor.getColumnIndexOrThrow("type")
                    val categoryIndex = cursor.getColumnIndexOrThrow("category")
                    while (cursor.moveToNext()) {
                        val type = cursor.getString(typeIndex)
                        val name = cursor.getString(categoryIndex).trim()
                        val normalized = name.lowercase(Locale.ROOT)
                        if (name.isNotEmpty() && seen.add(type to normalized)) {
                            db.execSQL(
                                "INSERT INTO categories (type, name, normalized_name, is_archived) VALUES (?, ?, ?, 0)",
                                arrayOf(type, name, normalized),
                            )
                        }
                    }
                }
            }
        }
    }
}
