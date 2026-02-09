package com.jworks.kanjiquest.pipeline

import com.jworks.kanjiquest.pipeline.parser.Kanjidic2Parser
import com.jworks.kanjiquest.pipeline.parser.KanjiVgParser
import com.jworks.kanjiquest.pipeline.parser.JMDictParser
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

fun main(args: Array<String>) {
    val rawDataDir = args.getOrNull(0) ?: "raw-data"
    val outputPath = args.getOrNull(1) ?: "android-app/src/main/assets/kanjiquest.db"

    println("KanjiQuest Data Pipeline")
    println("========================")
    println("Raw data directory: $rawDataDir")
    println("Output: $outputPath")

    val outputFile = File(outputPath)
    outputFile.parentFile?.mkdirs()
    if (outputFile.exists()) outputFile.delete()

    val connection = DriverManager.getConnection("jdbc:sqlite:$outputPath")
    connection.autoCommit = false

    try {
        createSchema(connection)

        // 1. Parse KANJIDIC2
        val kanjidic2File = File(rawDataDir, "kanjidic2.xml")
        if (kanjidic2File.exists()) {
            println("\nParsing KANJIDIC2...")
            val count = Kanjidic2Parser.parse(kanjidic2File, connection, rawDataDir)
            println("  Inserted $count kanji entries")
        } else {
            println("WARNING: kanjidic2.xml not found in $rawDataDir")
        }

        // 2. Parse KanjiVG stroke data
        val kanjiVgFile = File(rawDataDir, "kanjivg.xml")
        if (kanjiVgFile.exists()) {
            println("\nParsing KanjiVG stroke data...")
            val count = KanjiVgParser.parse(kanjiVgFile, connection)
            println("  Updated $count kanji with stroke data")
        } else {
            println("WARNING: kanjivg.xml not found in $rawDataDir")
        }

        // 3. Parse JMDict
        val jmdictFile = File(rawDataDir, "JMdict_e")
        if (!jmdictFile.exists()) {
            // Also try with .xml extension
            val altFile = File(rawDataDir, "JMdict_e.xml")
            if (altFile.exists()) {
                altFile.renameTo(jmdictFile)
            }
        }
        if (jmdictFile.exists()) {
            println("\nParsing JMDict...")
            val count = JMDictParser.parse(jmdictFile, connection)
            println("  Inserted $count vocabulary entries")
        } else {
            println("WARNING: JMdict_e.xml not found in $rawDataDir")
        }

        connection.commit()

        // Print stats
        printStats(connection)

        println("\nDatabase built successfully: $outputPath")
        println("File size: ${outputFile.length() / 1024}KB")

    } catch (e: Exception) {
        connection.rollback()
        println("ERROR: ${e.message}")
        e.printStackTrace()
    } finally {
        connection.close()
    }
}

private fun createSchema(conn: Connection) {
    println("\nCreating schema...")
    conn.createStatement().use { stmt ->
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS kanji (
                id INTEGER PRIMARY KEY NOT NULL,
                literal TEXT NOT NULL UNIQUE,
                grade INTEGER,
                jlpt_level INTEGER,
                frequency INTEGER,
                stroke_count INTEGER NOT NULL,
                meanings_en TEXT NOT NULL,
                on_readings TEXT NOT NULL,
                kun_readings TEXT NOT NULL,
                stroke_svg TEXT
            )
        """)

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS vocabulary (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                kanji_form TEXT NOT NULL,
                reading TEXT NOT NULL,
                meanings_en TEXT NOT NULL,
                jlpt_level INTEGER,
                frequency INTEGER
            )
        """)

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS kanji_vocabulary (
                kanji_id INTEGER NOT NULL,
                vocab_id INTEGER NOT NULL,
                PRIMARY KEY (kanji_id, vocab_id)
            )
        """)

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS example_sentence (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                vocab_id INTEGER NOT NULL,
                japanese TEXT NOT NULL,
                english TEXT NOT NULL
            )
        """)

        // User data tables (created empty, populated at runtime)
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS srs_card (
                kanji_id INTEGER PRIMARY KEY NOT NULL,
                ease_factor REAL NOT NULL DEFAULT 2.5,
                interval INTEGER NOT NULL DEFAULT 0,
                repetitions INTEGER NOT NULL DEFAULT 0,
                next_review INTEGER NOT NULL DEFAULT 0,
                state TEXT NOT NULL DEFAULT 'new',
                total_reviews INTEGER NOT NULL DEFAULT 0,
                correct_count INTEGER NOT NULL DEFAULT 0
            )
        """)

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS user_profile (
                id INTEGER PRIMARY KEY DEFAULT 1,
                total_xp INTEGER NOT NULL DEFAULT 0,
                level INTEGER NOT NULL DEFAULT 1,
                current_streak INTEGER NOT NULL DEFAULT 0,
                longest_streak INTEGER NOT NULL DEFAULT 0,
                last_study_date TEXT,
                daily_goal INTEGER NOT NULL DEFAULT 20
            )
        """)

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS study_session (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                game_mode TEXT NOT NULL,
                started_at INTEGER NOT NULL,
                cards_studied INTEGER NOT NULL DEFAULT 0,
                correct_count INTEGER NOT NULL DEFAULT 0,
                xp_earned INTEGER NOT NULL DEFAULT 0,
                duration_sec INTEGER NOT NULL DEFAULT 0
            )
        """)

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS achievement (
                id TEXT PRIMARY KEY NOT NULL,
                unlocked_at INTEGER,
                progress INTEGER NOT NULL DEFAULT 0,
                target INTEGER NOT NULL
            )
        """)

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS daily_stats (
                date TEXT PRIMARY KEY NOT NULL,
                cards_reviewed INTEGER NOT NULL DEFAULT 0,
                xp_earned INTEGER NOT NULL DEFAULT 0,
                study_time_sec INTEGER NOT NULL DEFAULT 0
            )
        """)

        // Indexes
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_kanji_grade ON kanji(grade)")
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_kanji_jlpt ON kanji(jlpt_level)")
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_kanji_freq ON kanji(frequency)")
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_vocab_jlpt ON vocabulary(jlpt_level)")
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_vocab_kanji_form ON vocabulary(kanji_form)")
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_srs_next_review ON srs_card(next_review)")
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_srs_state ON srs_card(state)")
    }
    conn.commit()
}

private fun printStats(conn: Connection) {
    println("\n--- Database Statistics ---")
    conn.createStatement().use { stmt ->
        stmt.executeQuery("SELECT COUNT(*) FROM kanji").use { rs ->
            rs.next(); println("  Kanji: ${rs.getInt(1)}")
        }
        stmt.executeQuery("SELECT COUNT(*) FROM kanji WHERE grade IS NOT NULL AND grade <= 6").use { rs ->
            rs.next(); println("  Joyo (Grades 1-6): ${rs.getInt(1)}")
        }
        stmt.executeQuery("SELECT COUNT(*) FROM kanji WHERE stroke_svg IS NOT NULL").use { rs ->
            rs.next(); println("  With stroke data: ${rs.getInt(1)}")
        }
        stmt.executeQuery("SELECT jlpt_level, COUNT(*) FROM kanji WHERE jlpt_level IS NOT NULL GROUP BY jlpt_level ORDER BY jlpt_level DESC").use { rs ->
            println("  JLPT levels:")
            while (rs.next()) {
                println("    N${rs.getInt(1)}: ${rs.getInt(2)} kanji")
            }
        }
        stmt.executeQuery("SELECT COUNT(*) FROM vocabulary").use { rs ->
            rs.next(); println("  Vocabulary: ${rs.getInt(1)}")
        }
        stmt.executeQuery("SELECT COUNT(*) FROM kanji_vocabulary").use { rs ->
            rs.next(); println("  Kanji-Vocabulary links: ${rs.getInt(1)}")
        }
        stmt.executeQuery("SELECT COUNT(*) FROM example_sentence").use { rs ->
            rs.next(); println("  Example sentences: ${rs.getInt(1)}")
        }
    }
}
