package com.example.expensetracker.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {

    @Volatile private var INSTANCE: AppDatabase? = null

    fun get(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "expense_tracker.db"
            )
                .addMigrations(MIGRATION_1_2)
                // (Optional during development only)
                // .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
        }
    }
}

/**
 * Migration 1 -> 2:
 * merchant_rules changed from old schema (pattern, isRegex, ignoreCase, ...)
 * to new schema (pattern, matchType, createdAtEpochMs, ...)
 *
 * Room requires we rebuild the table to REMOVE old columns.
 */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // 1) Create the new table with the EXACT schema Room expects now
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS merchant_rules_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                pattern TEXT NOT NULL,
                matchType TEXT NOT NULL DEFAULT 'CONTAINS',
                categoryId INTEGER NOT NULL,
                priority INTEGER NOT NULL,
                enabled INTEGER NOT NULL,
                createdAtEpochMs INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        // 2) Copy data from the old table into the new table.
        // Convert old isRegex -> matchType
        // createdAtEpochMs didn't exist before => default 0
        db.execSQL(
            """
            INSERT INTO merchant_rules_new (id, pattern, matchType, categoryId, priority, enabled, createdAtEpochMs)
            SELECT 
                id,
                pattern,
                CASE 
                    WHEN isRegex = 1 THEN 'REGEX'
                    ELSE 'CONTAINS'
                END AS matchType,
                categoryId,
                priority,
                enabled,
                0 AS createdAtEpochMs
            FROM merchant_rules
            """.trimIndent()
        )

        // 3) Drop old table
        db.execSQL("DROP TABLE merchant_rules")

        // 4) Rename new table
        db.execSQL("ALTER TABLE merchant_rules_new RENAME TO merchant_rules")

        // 5) Recreate index
        db.execSQL("CREATE INDEX IF NOT EXISTS index_merchant_rules_pattern ON merchant_rules(pattern)")
    }
}
