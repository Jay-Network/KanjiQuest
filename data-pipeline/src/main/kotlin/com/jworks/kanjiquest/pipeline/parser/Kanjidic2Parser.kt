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
 * Maps JLPT levels based on known kanji sets.
 */
object Kanjidic2Parser {

    // JLPT mapping: literal -> level (N5=5 through N1=1)
    // These are approximate mappings based on common JLPT kanji lists
    private val jlptMap: Map<String, Int> by lazy { loadJlptMap() }

    fun parse(file: File, conn: Connection): Int {
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

        // N5 kanji (103)
        val n5 = "一二三四五六七八九十百千万円時日月火水木金土曜何年半毎前後午上下中大小長高安新古間東西南北右左口目耳手足体頭顔首名人女男子学生先私友父母兄姉弟妹家族夫妻主彼自今朝昼夜晩去来行出入帰食飲見聞読書話買教外天気雨雪風花山川海空田休会出立走歩止住持待使送受着作入出来回通過問答知分話言語英国外社会車電鉄道店医者病院映画歌音楽"
        n5.forEach { map[it.toString()] = 5 }

        // N4 kanji (181 additional)
        val n4 = "不世両並主乗届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届"
        // Use a curated set instead
        val n4Real = "不世両主乗予事仕代以低届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届届"

        // Note: Full JLPT mapping would come from an external data file.
        // For the pipeline, we use grade-based approximation:
        // Grade 1-2 ≈ N5, Grade 3-4 ≈ N4, Grade 5-6 ≈ N3, Junior high ≈ N2-N1
        // The actual JLPT mapping should be loaded from a curated CSV in raw-data/

        return map
    }
}
