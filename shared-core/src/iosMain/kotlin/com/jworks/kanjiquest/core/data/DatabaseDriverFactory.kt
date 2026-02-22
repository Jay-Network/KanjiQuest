@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.jworks.kanjiquest.core.data

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.jworks.kanjiquest.db.KanjiQuestDatabase
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSLog
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val copiedFromBundle = copyDatabaseIfNeeded()

        // If we copied the pre-built DB, it already has all kanji/vocab tables
        // but user_version=0. Using the real Schema.create() would fail because
        // it runs CREATE TABLE (without IF NOT EXISTS) on tables that already exist.
        // Use a no-op create to let NativeSqliteDriver just set user_version.
        val schema: SqlSchema<QueryResult.Value<Unit>> = if (copiedFromBundle) {
            object : SqlSchema<QueryResult.Value<Unit>> {
                override val version: Long = KanjiQuestDatabase.Schema.version
                override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
                    // Pre-built DB already has tables — skip creation
                    return QueryResult.Value(Unit)
                }
                override fun migrate(
                    driver: SqlDriver,
                    oldVersion: Long,
                    newVersion: Long,
                    vararg callbacks: AfterVersion
                ): QueryResult.Value<Unit> {
                    return KanjiQuestDatabase.Schema.migrate(driver, oldVersion, newVersion, *callbacks)
                }
            }
        } else {
            KanjiQuestDatabase.Schema
        }

        return NativeSqliteDriver(schema = schema, name = DB_NAME)
    }

    fun createDatabase(driver: SqlDriver): KanjiQuestDatabase {
        return KanjiQuestDatabase(driver)
    }

    /** Returns true if a pre-built DB was successfully copied from the app bundle. */
    private fun copyDatabaseIfNeeded(): Boolean {
        val fileManager = NSFileManager.defaultManager
        val documentsPath = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true
        ).firstOrNull() as? String ?: return false

        val dbDestination = "$documentsPath/$DB_NAME"

        val defaults = NSUserDefaults.standardUserDefaults
        val installedVersion = defaults.integerForKey(DB_VERSION_KEY)

        if (fileManager.fileExistsAtPath(dbDestination) && installedVersion >= DB_VERSION) {
            return true // Already have a valid copy
        }

        // Try pathForResource first (standard approach)
        var bundlePath = NSBundle.mainBundle.pathForResource(
            name = DB_NAME.removeSuffix(".db"),
            ofType = "db"
        )

        // Fallback: check bundle root directly
        if (bundlePath == null) {
            val resourcePath = NSBundle.mainBundle.resourcePath
            if (resourcePath != null) {
                val directPath = "$resourcePath/$DB_NAME"
                if (fileManager.fileExistsAtPath(directPath)) {
                    bundlePath = directPath
                }
            }
        }

        if (bundlePath == null) {
            NSLog("KanjiQuest: DB not found in bundle. resourcePath=%@", NSBundle.mainBundle.resourcePath ?: "nil")
            return false
        }

        // Remove stale DB if present
        if (fileManager.fileExistsAtPath(dbDestination)) {
            fileManager.removeItemAtPath(dbDestination, error = null)
        }

        val success = fileManager.copyItemAtPath(bundlePath, toPath = dbDestination, error = null)
        if (success) {
            defaults.setInteger(DB_VERSION.toLong(), forKey = DB_VERSION_KEY)
            NSLog("KanjiQuest: DB copied from bundle to %@", dbDestination)
        } else {
            NSLog("KanjiQuest: Failed to copy DB from %@ to %@", bundlePath, dbDestination)
        }
        return success
    }

    companion object {
        private const val DB_NAME = "kanjiquest.db"
        private const val DB_VERSION_KEY = "kanjiquest_db_version"
        private const val DB_VERSION = 1
    }
}
