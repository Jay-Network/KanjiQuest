@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.jworks.kanjiquest.core.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.jworks.kanjiquest.db.KanjiQuestDatabase
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        copyDatabaseIfNeeded()
        return NativeSqliteDriver(
            schema = KanjiQuestDatabase.Schema,
            name = DB_NAME
        )
    }

    fun createDatabase(driver: SqlDriver): KanjiQuestDatabase {
        return KanjiQuestDatabase(driver)
    }

    private fun copyDatabaseIfNeeded() {
        val fileManager = NSFileManager.defaultManager
        val documentsPath = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true
        ).firstOrNull() as? String ?: return

        val dbDestination = "$documentsPath/$DB_NAME"

        val defaults = NSUserDefaults.standardUserDefaults
        val installedVersion = defaults.integerForKey(DB_VERSION_KEY)

        if (fileManager.fileExistsAtPath(dbDestination) && installedVersion >= DB_VERSION) return

        val bundlePath = NSBundle.mainBundle.pathForResource(
            name = DB_NAME.removeSuffix(".db"),
            ofType = "db"
        ) ?: return

        if (fileManager.fileExistsAtPath(dbDestination)) {
            fileManager.removeItemAtPath(dbDestination, error = null)
        }

        fileManager.copyItemAtPath(bundlePath, toPath = dbDestination, error = null)
        defaults.setInteger(DB_VERSION.toLong(), forKey = DB_VERSION_KEY)
    }

    companion object {
        private const val DB_NAME = "kanjiquest.db"
        private const val DB_VERSION_KEY = "kanjiquest_db_version"
        private const val DB_VERSION = 1
    }
}
