package com.jworks.kanjiquest.core

import com.jworks.kanjiquest.core.writing.SvgPathParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SvgPathParserTest {

    @Test
    fun parseSvgPath_simpleMoveAndCubic() {
        // Simple path: move to (50, 20) then relative cubic to end point
        val path = "M 50,20 c 1,2 3,4 5,6"
        val points = SvgPathParser.parseSvgPath(path)

        assertEquals(2, points.size)
        // First point is the moveTo
        assertEquals(50f, points[0].x)
        assertEquals(20f, points[0].y)
        // Second point is offset by the end delta of the cubic (5, 6)
        assertEquals(55f, points[1].x)
        assertEquals(26f, points[1].y)
    }

    @Test
    fun parseSvgPath_realKanjiVgData_ichi() {
        // Real KanjiVG stroke for 一 (one) - single horizontal stroke
        val path = "M 14.5,50.25 c 7.75,0.62 17.37,0.87 26.62,0.37 c 9.25,-0.5 18.12,-1.62 24.25,-3"
        val points = SvgPathParser.parseSvgPath(path)

        assertTrue(points.isNotEmpty())
        // Starts near (14.5, 50.25)
        assertEquals(14.5f, points[0].x, 0.01f)
        assertEquals(50.25f, points[0].y, 0.01f)
        // Should have 3 points: M + 2 cubic endpoints
        assertEquals(3, points.size)
    }

    @Test
    fun parseSvgPath_multipleCubicSegments() {
        val path = "M 50,16.5 c 0,1.75 -0.25,3.37 -0.75,4.87 c -0.5,1.5 -1,3 -1.5,4.5"
        val points = SvgPathParser.parseSvgPath(path)

        assertEquals(3, points.size)
        assertEquals(50f, points[0].x, 0.01f)
        assertEquals(16.5f, points[0].y, 0.01f)
    }

    @Test
    fun parseSvgPath_emptyInput_returnsEmpty() {
        assertEquals(emptyList(), SvgPathParser.parseSvgPath(""))
        assertEquals(emptyList(), SvgPathParser.parseSvgPath("  "))
    }

    @Test
    fun parseStrokePaths_jsonArray() {
        val json = """["M 14.5,50.25 c 7.75,0.62 17.37,0.87 26.62,0.37"]"""
        val strokes = SvgPathParser.parseStrokePaths(json)

        assertEquals(1, strokes.size)
        assertTrue(strokes[0].isNotEmpty())
        assertEquals(14.5f, strokes[0][0].x, 0.01f)
    }

    @Test
    fun parseStrokePaths_multipleStrokes() {
        // 三 (three) has 3 strokes
        val json = """[
            "M 20,25 c 5,0 10,-0.5 15,-1",
            "M 15,50 c 8,0.5 18,0 27,-1",
            "M 12,75 c 10,0.75 22,0.5 33,-1"
        ]"""
        val strokes = SvgPathParser.parseStrokePaths(json)

        assertEquals(3, strokes.size)
        for (stroke in strokes) {
            assertTrue(stroke.size >= 2)
        }
    }

    @Test
    fun parseStrokePaths_invalidJson_returnsEmpty() {
        assertEquals(emptyList(), SvgPathParser.parseStrokePaths("not json"))
        assertEquals(emptyList(), SvgPathParser.parseStrokePaths(""))
    }

    @Test
    fun tokenize_handlesNegativeNumbers() {
        val tokens = SvgPathParser.tokenize("M 50,-20 c -1,-2 -3,-4 -5,-6")
        // Should contain: M, 50, -20, c, -1, -2, -3, -4, -5, -6
        assertTrue(tokens.contains("M"))
        assertTrue(tokens.contains("c"))
        assertTrue(tokens.contains("-20"))
        assertTrue(tokens.contains("-5"))
    }

    @Test
    fun tokenize_handlesDecimalNumbers() {
        val tokens = SvgPathParser.tokenize("M 14.5,50.25")
        assertTrue(tokens.contains("14.5"))
        assertTrue(tokens.contains("50.25"))
    }
}
