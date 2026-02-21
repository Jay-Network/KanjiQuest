@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.jworks.kanjiquest.core.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.jworks.kanjiquest.db.KanjiQuestDatabase
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
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

        if (fileManager.fileExistsAtPath(dbDestination)) return

        val bundlePath = NSBundle.mainBundle.pathForResource(
            name = DB_NAME.removeSuffix(".db"),
            ofType = "db"
        ) ?: return

        fileManager.copyItemAtPath(bundlePath, toPath = dbDestination, error = null)
    }

    companion object {
        private const val DB_NAME = "kanjiquest.db"
    }
}
