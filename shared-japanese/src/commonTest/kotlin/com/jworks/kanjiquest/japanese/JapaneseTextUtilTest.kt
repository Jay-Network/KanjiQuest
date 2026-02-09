package com.jworks.kanjiquest.japanese

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JapaneseTextUtilTest {

    @Test
    fun containsKanji_withKanji_returnsTrue() {
        assertTrue(JapaneseTextUtil.containsKanji("漢字"))
        assertTrue(JapaneseTextUtil.containsKanji("今日は天気です"))
    }

    @Test
    fun containsKanji_withoutKanji_returnsFalse() {
        assertFalse(JapaneseTextUtil.containsKanji("ひらがな"))
        assertFalse(JapaneseTextUtil.containsKanji("カタカナ"))
        assertFalse(JapaneseTextUtil.containsKanji("hello"))
    }

    @Test
    fun containsHiragana_withHiragana_returnsTrue() {
        assertTrue(JapaneseTextUtil.containsHiragana("ひらがな"))
    }

    @Test
    fun containsKatakana_withKatakana_returnsTrue() {
        assertTrue(JapaneseTextUtil.containsKatakana("カタカナ"))
    }

    @Test
    fun isKanji_recognizesCommonKanji() {
        assertTrue(JapaneseTextUtil.isKanji('漢'))
        assertTrue(JapaneseTextUtil.isKanji('字'))
        assertTrue(JapaneseTextUtil.isKanji('日'))
        assertFalse(JapaneseTextUtil.isKanji('あ'))
        assertFalse(JapaneseTextUtil.isKanji('A'))
    }

    @Test
    fun kanjiCount_countsCorrectly() {
        assertEquals(2, JapaneseTextUtil.kanjiCount("漢字"))
        assertEquals(4, JapaneseTextUtil.kanjiCount("今日は天気です"))
        assertEquals(0, JapaneseTextUtil.kanjiCount("ひらがな"))
    }

    @Test
    fun extractKanji_extractsAllKanji() {
        assertEquals(listOf('今', '日', '天', '気'), JapaneseTextUtil.extractKanji("今日は天気です"))
    }

    @Test
    fun japaneseRatio_calculatesCorrectly() {
        assertEquals(1.0f, JapaneseTextUtil.japaneseRatio("あいう"))
        assertEquals(0.0f, JapaneseTextUtil.japaneseRatio("abc"))
        assertEquals(0.0f, JapaneseTextUtil.japaneseRatio(""))
    }

    @Test
    fun katakanaToHiragana_convertsCorrectly() {
        assertEquals("あいうえお", JapaneseTextUtil.katakanaToHiragana("アイウエオ"))
        assertEquals("hello", JapaneseTextUtil.katakanaToHiragana("hello"))
    }

    @Test
    fun hiraganaToKatakana_convertsCorrectly() {
        assertEquals("アイウエオ", JapaneseTextUtil.hiraganaToKatakana("あいうえお"))
    }
}
