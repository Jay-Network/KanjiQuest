package com.jworks.kanjiquest.pipeline.parser

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.sql.Connection
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

/**
 * Parses the combined KanjiVG XML file (kanjivg.xml) to extract stroke path data.
 * Format: <kanji id="kvg:kanji_XXXXX"> containing <path d="..."> elements.
 * The XXXXX is the hex Unicode codepoint (e.g., 04e00 for 一).
 *
 * Handles both kanji (CJK Unified Ideographs, U+3400+) and kana
 * (Hiragana U+3040-U+309F, Katakana U+30A0-U+30FF).
 */
object KanjiVgParser {

    fun parse(file: File, conn: Connection): Int {
        val kanjiStmt = conn.prepareStatement(
            "UPDATE kanji SET stroke_svg = ? WHERE id = ?"
        )
        val kanaStmt = conn.prepareStatement(
            "UPDATE kana SET stroke_svg = ? WHERE literal = ?"
        )

        var kanjiCount = 0
        var kanaCount = 0

        val factory = SAXParserFactory.newInstance()
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        factory.isNamespaceAware = true
        val saxParser = factory.newSAXParser()

        var currentCodepoint: Int? = null
        var currentPaths = mutableListOf<String>()

        val handler = object : DefaultHandler() {
            override fun startElement(uri: String, localName: String, qName: String, attrs: Attributes) {
                when {
                    qName == "kanji" || localName == "kanji" -> {
                        val id = attrs.getValue("id") ?: return
                        // id format: "kvg:kanji_04e00"
                        val hex = id.substringAfter("kanji_", "")
                        if (hex.isNotEmpty()) {
                            currentCodepoint = try { hex.toInt(16) } catch (_: Exception) { null }
                            currentPaths = mutableListOf()
                        }
                    }
                    qName == "path" || localName == "path" -> {
                        val d = attrs.getValue("d")
                        if (d != null && currentCodepoint != null) {
                            currentPaths.add(d)
                        }
                    }
                }
            }

            override fun endElement(uri: String, localName: String, qName: String) {
                if ((qName == "kanji" || localName == "kanji") && currentCodepoint != null && currentPaths.isNotEmpty()) {
                    val cp = currentCodepoint!!
                    val strokeJson = Json.encodeToString(currentPaths.toList())

                    when {
                        // Hiragana (U+3040-U+309F) or Katakana (U+30A0-U+30FF)
                        cp in 0x3040..0x30FF -> {
                            val literal = cp.toChar().toString()
                            kanaStmt.setString(1, strokeJson)
                            kanaStmt.setString(2, literal)
                            if (kanaStmt.executeUpdate() > 0) kanaCount++
                        }
                        // CJK Unified Ideographs
                        cp >= 0x3400 -> {
                            kanjiStmt.setString(1, strokeJson)
                            kanjiStmt.setInt(2, cp)
                            if (kanjiStmt.executeUpdate() > 0) kanjiCount++

                            if (kanjiCount % 500 == 0 && kanjiCount > 0) {
                                conn.commit()
                                print("\r  Processed $kanjiCount kanji stroke entries...")
                            }
                        }
                    }
                    currentCodepoint = null
                    currentPaths.clear()
                }
            }
        }

        saxParser.parse(file, handler)
        kanjiStmt.close()
        kanaStmt.close()
        println("\r  Processed $kanjiCount kanji + $kanaCount kana stroke entries")
        return kanjiCount + kanaCount
    }
}
