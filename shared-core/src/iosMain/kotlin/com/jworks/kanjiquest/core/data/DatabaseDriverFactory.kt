@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.jworks.kanjiquest.core.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.jworks.kanjiquest.db.KanjiQuestDatabase
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSLog
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual class DatabaseDriverFactory {
    // Expose the full DB path so Swift can verify it
    val resolvedDbPath: String = run {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true
        )
        val docs = paths.firstOrNull() as? String ?: ""
        "$docs/$DB_NAME"
    }

    actual fun createDriver(): SqlDriver {
        NSLog("KanjiQuest [KN]: Opening DB at FULL PATH: %s", resolvedDbPath)

        val fm = NSFileManager.defaultManager
        val exists = fm.fileExistsAtPath(resolvedDbPath)
        val size = if (exists) {
            (fm.attributesOfItemAtPath(resolvedDbPath, error = null)
                ?.get("NSFileSize") as? Long) ?: 0L
        } else 0L
        NSLog("KanjiQuest [KN]: DB exists=%s, size=%d bytes", exists.toString(), size)

        // Use FULL PATH so NativeSqliteDriver opens exactly where Swift copied the DB.
        // When name contains "/", sqliter uses it as-is (no path resolution).
        val driver = NativeSqliteDriver(
            schema = KanjiQuestDatabase.Schema,
            name = resolvedDbPath
        )

        // Verify kanji count right after opening
        driver.execute(null, "SELECT COUNT(*) FROM kanji", 0)
        NSLog("KanjiQuest [KN]: DB opened at %s (exists=%s, size=%d)", resolvedDbPath, exists.toString(), size)

        ensureNewTables(driver)
        return driver
    }

    fun createDatabase(driver: SqlDriver): KanjiQuestDatabase {
        return KanjiQuestDatabase(driver)
    }

    private fun ensureNewTables(driver: SqlDriver) {
        val statements = listOf(
            """CREATE TABLE IF NOT EXISTS collection (
                item_id INTEGER NOT NULL,
                item_type TEXT NOT NULL,
                rarity TEXT NOT NULL DEFAULT 'common',
                item_level INTEGER NOT NULL DEFAULT 1,
                item_xp INTEGER NOT NULL DEFAULT 0,
                discovered_at INTEGER NOT NULL,
                source TEXT NOT NULL DEFAULT 'gameplay',
                PRIMARY KEY (item_id, item_type)
            )""",
            """CREATE TABLE IF NOT EXISTS field_journal (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                image_path TEXT NOT NULL,
                location_label TEXT NOT NULL DEFAULT '',
                kanji_found TEXT NOT NULL DEFAULT '[]',
                kanji_count INTEGER NOT NULL DEFAULT 0,
                captured_at INTEGER NOT NULL
            )""",
            """CREATE TABLE IF NOT EXISTS flashcard_deck (
                deck_id INTEGER NOT NULL DEFAULT 1,
                kanji_id INTEGER NOT NULL,
                added_at TEXT NOT NULL DEFAULT (datetime('now')),
                last_studied_at TEXT,
                study_count INTEGER NOT NULL DEFAULT 0,
                notes TEXT,
                PRIMARY KEY (deck_id, kanji_id)
            )""",
            """CREATE TABLE IF NOT EXISTS coin_balance (
                user_id TEXT PRIMARY KEY NOT NULL,
                local_balance INTEGER NOT NULL DEFAULT 0,
                synced_balance INTEGER NOT NULL DEFAULT 0,
                lifetime_earned INTEGER NOT NULL DEFAULT 0,
                lifetime_spent INTEGER NOT NULL DEFAULT 0,
                tier TEXT NOT NULL DEFAULT 'bronze',
                last_synced_at INTEGER NOT NULL DEFAULT 0,
                needs_sync INTEGER NOT NULL DEFAULT 0
            )""",
            """CREATE TABLE IF NOT EXISTS coin_sync_queue (
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
            )""",
            """CREATE TABLE IF NOT EXISTS premium_content_unlocks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                user_id TEXT NOT NULL,
                content_type TEXT NOT NULL,
                content_id TEXT NOT NULL,
                unlocked_at INTEGER NOT NULL,
                cost_coins INTEGER NOT NULL,
                UNIQUE(user_id, content_type, content_id)
            )""",
            """CREATE TABLE IF NOT EXISTS active_boosters (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                user_id TEXT NOT NULL,
                booster_type TEXT NOT NULL,
                multiplier REAL NOT NULL DEFAULT 1.0,
                activated_at INTEGER NOT NULL,
                expires_at INTEGER NOT NULL
            )""",
            """CREATE TABLE IF NOT EXISTS kanji_mode_stats (
                kanji_id INTEGER NOT NULL,
                game_mode TEXT NOT NULL,
                review_count INTEGER NOT NULL DEFAULT 0,
                correct_count INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (kanji_id, game_mode)
            )""",
            """CREATE TABLE IF NOT EXISTS learning_sync_queue (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                user_id TEXT NOT NULL,
                event_type TEXT NOT NULL,
                payload TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                sync_status TEXT NOT NULL DEFAULT 'pending',
                retry_count INTEGER NOT NULL DEFAULT 0,
                last_attempt_at INTEGER,
                error_message TEXT
            )""",
            """CREATE TABLE IF NOT EXISTS learning_sync_metadata (
                user_id TEXT PRIMARY KEY NOT NULL,
                last_synced_at INTEGER NOT NULL DEFAULT 0,
                last_push_at INTEGER NOT NULL DEFAULT 0,
                last_pull_at INTEGER NOT NULL DEFAULT 0
            )""",
            """CREATE TABLE IF NOT EXISTS vocab_srs_card (
                vocab_id INTEGER PRIMARY KEY NOT NULL,
                ease_factor REAL NOT NULL DEFAULT 2.5,
                interval INTEGER NOT NULL DEFAULT 0,
                repetitions INTEGER NOT NULL DEFAULT 0,
                next_review INTEGER NOT NULL DEFAULT 0,
                state TEXT NOT NULL DEFAULT 'new',
                total_reviews INTEGER NOT NULL DEFAULT 0,
                correct_count INTEGER NOT NULL DEFAULT 0
            )""",
            """CREATE TABLE IF NOT EXISTS sync_version (
                user_id TEXT PRIMARY KEY NOT NULL,
                device_id TEXT,
                server_version INTEGER NOT NULL DEFAULT 0,
                last_push_at INTEGER NOT NULL DEFAULT 0,
                last_pull_at INTEGER NOT NULL DEFAULT 0,
                last_full_pull_at INTEGER NOT NULL DEFAULT 0
            )""",
            // Indexes
            "CREATE INDEX IF NOT EXISTS idx_sync_queue_status ON coin_sync_queue(sync_status, created_at)",
            "CREATE INDEX IF NOT EXISTS idx_unlocks_user ON premium_content_unlocks(user_id, content_type)",
            "CREATE INDEX IF NOT EXISTS idx_boosters_user ON active_boosters(user_id, expires_at)",
            "CREATE INDEX IF NOT EXISTS idx_learning_sync_status ON learning_sync_queue(sync_status, created_at)",
            "CREATE INDEX IF NOT EXISTS idx_collection_type ON collection(item_type)",
            "CREATE INDEX IF NOT EXISTS idx_collection_rarity ON collection(item_type, rarity)"
        )

        for (sql in statements) {
            driver.execute(null, sql.trimIndent(), 0)
        }
    }

    companion object {
        private const val DB_NAME = "kanjiquest.db"
    }
}
