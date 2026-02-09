package com.jworks.kanjiquest.core.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.jworks.kanjiquest.db.KanjiQuestDatabase

actual class DatabaseDriverFactory(private val dbPath: String = "kanjiquest.db") {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver("jdbc:sqlite:$dbPath")
        KanjiQuestDatabase.Schema.create(driver)
        return driver
    }
}
