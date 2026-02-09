package com.jworks.kanjiquest.pipeline.parser

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.sql.Connection
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

/**
 * Parses JMDict XML to extract vocabulary entries.
 * Only includes entries that have kanji forms (filters out kana-only entries).
 * Also builds the kanji_vocabulary junction table.
 */
object JMDictParser {

    fun parse(file: File, conn: Connection): Int {
        val vocabStmt = conn.prepareStatement(
            """INSERT INTO vocabulary(kanji_form, reading, meanings_en, jlpt_level, frequency)
               VALUES (?, ?, ?, ?, ?)"""
        )
        val junctionStmt = conn.prepareStatement(
            "INSERT OR IGNORE INTO kanji_vocabulary(kanji_id, vocab_id) VALUES (?, ?)"
        )

        // Load existing kanji IDs for junction building
        val kanjiIds = mutableSetOf<Int>()
        conn.createStatement().use { stmt ->
            stmt.executeQuery("SELECT id FROM kanji").use { rs ->
                while (rs.next()) kanjiIds.add(rs.getInt(1))
            }
        }

        var vocabCount = 0
        var junctionCount = 0

        val factory = SAXParserFactory.newInstance()
        // JMDict has an embedded DTD with ~200 entity definitions (like &n; for noun).
        // We MUST allow entity expansion for them to resolve, but raise the limit.
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        val parser = factory.newSAXParser()
        // Raise entity expansion limit (JMDict uses entities extensively)
        parser.setProperty("http://www.oracle.com/xml/jaxp/properties/entityExpansionLimit", "500000")

        val handler = object : DefaultHandler() {
            private var inEntry = false
            private val kanjiElements = mutableListOf<String>()
            private val readingElements = mutableListOf<String>()
            private val meanings = mutableListOf<String>()
            private var currentElement = ""
            private val text = StringBuilder()
            private var glossLang = ""
            private var priority: Int? = null

            override fun startElement(uri: String, localName: String, qName: String, attrs: Attributes) {
                currentElement = qName
                text.clear()
                when (qName) {
                    "entry" -> {
                        inEntry = true
                        kanjiElements.clear()
                        readingElements.clear()
                        meanings.clear()
                        priority = null
                    }
                    "gloss" -> glossLang = attrs.getValue("xml:lang") ?: "eng"
                    "ke_pri", "re_pri" -> {}
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                text.append(ch, start, length)
            }

            override fun endElement(uri: String, localName: String, qName: String) {
                val content = text.toString().trim()
                when (qName) {
                    "keb" -> kanjiElements.add(content)
                    "reb" -> readingElements.add(content)
                    "gloss" -> {
                        if (glossLang == "eng" || glossLang.isEmpty()) {
                            meanings.add(content)
                        }
                    }
                    "ke_pri", "re_pri" -> {
                        // Map priority tags to frequency rank
                        val p = when {
                            content.startsWith("nf") -> content.removePrefix("nf").toIntOrNull()?.let { it * 500 }
                            content == "ichi1" -> 5000
                            content == "ichi2" -> 15000
                            content == "news1" -> 8000
                            content == "news2" -> 20000
                            content == "spec1" -> 10000
                            content == "spec2" -> 25000
                            else -> null
                        }
                        if (p != null && (priority == null || p < priority!!)) {
                            priority = p
                        }
                    }
                    "entry" -> {
                        if (kanjiElements.isNotEmpty() && meanings.isNotEmpty()) {
                            val primaryKanji = kanjiElements.first()
                            val primaryReading = readingElements.firstOrNull() ?: ""
                            val meaningsJson = Json.encodeToString(meanings.toList())

                            vocabStmt.setString(1, primaryKanji)
                            vocabStmt.setString(2, primaryReading)
                            vocabStmt.setString(3, meaningsJson)
                            vocabStmt.setNull(4, java.sql.Types.INTEGER)
                            if (priority != null) vocabStmt.setInt(5, priority!!) else vocabStmt.setNull(5, java.sql.Types.INTEGER)

                            vocabStmt.executeUpdate()
                            vocabCount++

                            // Get the auto-generated vocab ID
                            val vocabId = conn.createStatement().use { stmt ->
                                stmt.executeQuery("SELECT last_insert_rowid()").use { rs ->
                                    rs.next(); rs.getLong(1)
                                }
                            }

                            // Build junction entries for each kanji in the word
                            for (kanjiForm in kanjiElements) {
                                for (ch in kanjiForm) {
                                    val cp = ch.code
                                    if (cp in kanjiIds) {
                                        junctionStmt.setInt(1, cp)
                                        junctionStmt.setLong(2, vocabId)
                                        junctionStmt.executeUpdate()
                                        junctionCount++
                                    }
                                }
                            }

                            if (vocabCount % 5000 == 0) {
                                conn.commit()
                                print("\r  Processed $vocabCount vocab entries...")
                            }
                        }
                        inEntry = false
                    }
                }
                text.clear()
            }
        }

        parser.parse(file, handler)
        vocabStmt.close()
        junctionStmt.close()

        println("\r  Processed $vocabCount vocab entries, $junctionCount junction links")
        return vocabCount
    }
}
