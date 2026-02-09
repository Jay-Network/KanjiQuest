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
 */
object KanjiVgParser {

    fun parse(file: File, conn: Connection): Int {
        val stmt = conn.prepareStatement(
            "UPDATE kanji SET stroke_svg = ? WHERE id = ?"
        )

        var count = 0

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
                    // Only store CJK Unified Ideographs (skip ASCII punctuation etc.)
                    if (cp >= 0x3400) {
                        val strokeJson = Json.encodeToString(currentPaths.toList())
                        stmt.setString(1, strokeJson)
                        stmt.setInt(2, cp)
                        val updated = stmt.executeUpdate()
                        if (updated > 0) count++

                        if (count % 500 == 0 && count > 0) {
                            conn.commit()
                            print("\r  Processed $count kanji stroke entries...")
                        }
                    }
                    currentCodepoint = null
                    currentPaths.clear()
                }
            }
        }

        saxParser.parse(file, handler)
        stmt.close()
        println("\r  Processed $count kanji stroke entries total")
        return count
    }
}
