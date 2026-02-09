package com.jworks.kanjiquest.core.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.jworks.kanjiquest.db.KanjiQuestDatabase
import java.io.FileOutputStream

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val isFreshCopy = copyDatabaseFromAssets()
        if (isFreshCopy) {
            migratePreBuiltDatabase()
        }
        ensureNewTables()
        return AndroidSqliteDriver(
            schema = KanjiQuestDatabase.Schema,
            context = context,
            name = DB_NAME
        )
    }

    /** Returns true if a fresh copy was made (first install). */
    private fun copyDatabaseFromAssets(): Boolean {
        val dbFile = context.getDatabasePath(DB_NAME)
        if (dbFile.exists()) return false

        dbFile.parentFile?.mkdirs()
        context.assets.open(DB_NAME).use { input ->
            FileOutputStream(dbFile).use { output ->
                input.copyTo(output)
            }
        }
        return true
    }

    /**
     * The pre-built DB from the data pipeline has kanji/vocabulary data but:
     * 1. No schema version set (PRAGMA user_version = 0)
     * 2. No JCoin tables (added after pipeline was built)
     *
     * We set user_version = 1 so AndroidSqliteDriver won't try to re-create
     * all tables, and add JCoin tables via CREATE TABLE IF NOT EXISTS.
     */
    private fun migratePreBuiltDatabase() {
        val dbFile = context.getDatabasePath(DB_NAME)
        val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
        db.use {
            it.execSQL("PRAGMA user_version = 1")

            it.execSQL("""
                CREATE TABLE IF NOT EXISTS coin_balance (
                    user_id TEXT PRIMARY KEY NOT NULL,
                    local_balance INTEGER NOT NULL DEFAULT 0,
                    synced_balance INTEGER NOT NULL DEFAULT 0,
                    lifetime_earned INTEGER NOT NULL DEFAULT 0,
                    lifetime_spent INTEGER NOT NULL DEFAULT 0,
                    tier TEXT NOT NULL DEFAULT 'bronze',
                    last_synced_at INTEGER NOT NULL DEFAULT 0,
                    needs_sync INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            it.execSQL("""
                CREATE TABLE IF NOT EXISTS coin_sync_queue (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    user_id TEXT NOT NULL,
                    event_type TEXT NOT NULL,
                    source_business TEXT NOT NULL DEFAULT 'kanjiquests',
                    source_type TEXT NOT NULL,
                    base_amount INTEGER NOT NULL,
                    description TEXT NOT NULL,
                    metadata TEXT NOT NULL DEFAULT '{}',
                    created_at INTEGER NOT NULL,
                    sync_status TEXT NOT NULL DEFAULT 'pending',
                    retry_count INTEGER NOT NULL DEFAULT 0,
                    last_attempt_at INTEGER,
                    error_message TEXT
                )
            """.trimIndent())

            it.execSQL("""
                CREATE TABLE IF NOT EXISTS premium_content_unlocks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    user_id TEXT NOT NULL,
                    content_type TEXT NOT NULL,
                    content_id TEXT NOT NULL,
                    unlocked_at INTEGER NOT NULL,
                    cost_coins INTEGER NOT NULL,
                    UNIQUE(user_id, content_type, content_id)
                )
            """.trimIndent())

            it.execSQL("""
                CREATE TABLE IF NOT EXISTS active_boosters (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    user_id TEXT NOT NULL,
                    booster_type TEXT NOT NULL,
                    multiplier REAL NOT NULL DEFAULT 1.0,
                    activated_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL
                )
            """.trimIndent())

            it.execSQL("CREATE INDEX IF NOT EXISTS idx_sync_queue_status ON coin_sync_queue(sync_status, created_at)")
            it.execSQL("CREATE INDEX IF NOT EXISTS idx_unlocks_user ON premium_content_unlocks(user_id, content_type)")
            it.execSQL("CREATE INDEX IF NOT EXISTS idx_boosters_user ON active_boosters(user_id, expires_at)")
        }
    }

    /** Creates tables added after the initial schema - runs on every app start. */
    private fun ensureNewTables() {
        val dbFile = context.getDatabasePath(DB_NAME)
        if (!dbFile.exists()) return
        val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
        db.use {
            it.execSQL("""
                CREATE TABLE IF NOT EXISTS vocab_srs_card (
                    vocab_id INTEGER PRIMARY KEY NOT NULL,
                    ease_factor REAL NOT NULL DEFAULT 2.5,
                    interval INTEGER NOT NULL DEFAULT 0,
                    repetitions INTEGER NOT NULL DEFAULT 0,
                    next_review INTEGER NOT NULL DEFAULT 0,
                    state TEXT NOT NULL DEFAULT 'new',
                    total_reviews INTEGER NOT NULL DEFAULT 0,
                    correct_count INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            it.execSQL("""
                CREATE TABLE IF NOT EXISTS learning_sync_queue (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    user_id TEXT NOT NULL,
                    event_type TEXT NOT NULL,
                    payload TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    sync_status TEXT NOT NULL DEFAULT 'pending',
                    retry_count INTEGER NOT NULL DEFAULT 0,
                    last_attempt_at INTEGER,
                    error_message TEXT
                )
            """.trimIndent())

            it.execSQL("""
                CREATE TABLE IF NOT EXISTS learning_sync_metadata (
                    user_id TEXT PRIMARY KEY NOT NULL,
                    last_synced_at INTEGER NOT NULL DEFAULT 0,
                    last_push_at INTEGER NOT NULL DEFAULT 0,
                    last_pull_at INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            it.execSQL("CREATE INDEX IF NOT EXISTS idx_learning_sync_status ON learning_sync_queue(sync_status, created_at)")
        }
    }

    companion object {
        private const val DB_NAME = "kanjiquest.db"
    }
}
