package com.jworks.kanjiquest.pipeline.parser

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.sql.Connection
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

/**
 * Parses KANJIDIC2 XML into the kanji table.
 * Maps JLPT levels from raw-data/jlpt-mapping.csv (community-consensus lists).
 */
object Kanjidic2Parser {

    // JLPT mapping: literal -> level (N5=5 through N1=1)
    // Loaded from raw-data/jlpt-mapping.csv
    private lateinit var jlptMap: Map<String, Int>

    private var rawDataDir: String = "raw-data"

    fun parse(file: File, conn: Connection, dataDir: String = "raw-data"): Int {
        rawDataDir = dataDir
        jlptMap = loadJlptMap()
        println("  Loaded ${jlptMap.size} JLPT kanji mappings from jlpt-mapping.csv")

        val stmt = conn.prepareStatement(
            """INSERT OR REPLACE INTO kanji(id, literal, grade, jlpt_level, frequency, stroke_count, meanings_en, on_readings, kun_readings)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"""
        )

        var count = 0
        val factory = SAXParserFactory.newInstance()
        // Disable DTD loading to avoid network calls
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        val parser = factory.newSAXParser()

        val handler = object : DefaultHandler() {
            private var inCharacter = false
            private var literal = ""
            private var grade: Int? = null
            private var frequency: Int? = null
            private var strokeCount = 0
            private val meanings = mutableListOf<String>()
            private val onReadings = mutableListOf<String>()
            private val kunReadings = mutableListOf<String>()
            private var currentElement = ""
            private var rType = ""
            private var mLang = ""
            private val text = StringBuilder()

            override fun startElement(uri: String, localName: String, qName: String, attrs: Attributes) {
                currentElement = qName
                text.clear()
                when (qName) {
                    "character" -> {
                        inCharacter = true
                        literal = ""
                        grade = null
                        frequency = null
                        strokeCount = 0
                        meanings.clear()
                        onReadings.clear()
                        kunReadings.clear()
                    }
                    "reading" -> rType = attrs.getValue("r_type") ?: ""
                    "meaning" -> mLang = attrs.getValue("m_lang") ?: ""
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                text.append(ch, start, length)
            }

            override fun endElement(uri: String, localName: String, qName: String) {
                val content = text.toString().trim()
                when (qName) {
                    "literal" -> literal = content
                    "grade" -> grade = content.toIntOrNull()
                    "freq" -> frequency = content.toIntOrNull()
                    "stroke_count" -> if (strokeCount == 0) strokeCount = content.toIntOrNull() ?: 0
                    "reading" -> {
                        when (rType) {
                            "ja_on" -> onReadings.add(content)
                            "ja_kun" -> kunReadings.add(content)
                        }
                    }
                    "meaning" -> {
                        if (mLang.isEmpty() || mLang == "en") {
                            meanings.add(content)
                        }
                    }
                    "character" -> {
                        if (literal.isNotEmpty() && strokeCount > 0) {
                            val codepoint = literal.codePointAt(0)
                            val jlptLevel = jlptMap[literal]

                            stmt.setInt(1, codepoint)
                            stmt.setString(2, literal)
                            if (grade != null) stmt.setInt(3, grade!!) else stmt.setNull(3, java.sql.Types.INTEGER)
                            if (jlptLevel != null) stmt.setInt(4, jlptLevel) else stmt.setNull(4, java.sql.Types.INTEGER)
                            if (frequency != null) stmt.setInt(5, frequency!!) else stmt.setNull(5, java.sql.Types.INTEGER)
                            stmt.setInt(6, strokeCount)
                            stmt.setString(7, Json.encodeToString(meanings.toList()))
                            stmt.setString(8, Json.encodeToString(onReadings.toList()))
                            stmt.setString(9, Json.encodeToString(kunReadings.toList()))

                            stmt.executeUpdate()
                            count++

                            if (count % 2000 == 0) {
                                conn.commit()
                                print("\r  Processed $count kanji...")
                            }
                        }
                        inCharacter = false
                    }
                }
                text.clear()
            }
        }

        parser.parse(file, handler)
        stmt.close()
        println("\r  Processed $count kanji total")
        return count
    }

    private fun loadJlptMap(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        val csvFile = File(rawDataDir, "jlpt-mapping.csv")

        if (!csvFile.exists()) {
            println("  WARNING: jlpt-mapping.csv not found in $rawDataDir - no JLPT data will be set")
            return map
        }

        csvFile.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("literal")) return@forEachLine
            val parts = trimmed.split(",")
            if (parts.size == 2) {
                val kanji = parts[0].trim()
                val level = parts[1].trim().toIntOrNull()
                if (kanji.isNotEmpty() && level != null && level in 1..5) {
                    map[kanji] = level
                }
            }
        }

        return map
    }
}
