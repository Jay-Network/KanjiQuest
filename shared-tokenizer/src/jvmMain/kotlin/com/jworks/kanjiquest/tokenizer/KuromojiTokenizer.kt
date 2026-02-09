package com.jworks.kanjiquest.tokenizer

import com.atilika.kuromoji.ipadic.Token
import com.atilika.kuromoji.ipadic.Tokenizer
import com.jworks.kanjiquest.japanese.JapaneseTextUtil
import com.jworks.kanjiquest.japanese.JapaneseToken

class KuromojiTokenizer {

    private var tokenizer: Tokenizer? = null
    private var initialized = false

    fun isReady(): Boolean = initialized

    fun initialize() {
        if (initialized) return
        tokenizer = Tokenizer()
        initialized = true
    }

    fun tokenize(text: String): List<JapaneseToken> {
        val tok = tokenizer ?: return emptyList()
        val tokens = tok.tokenize(text)
        return mapTokens(text, tokens)
    }

    private fun mapTokens(originalText: String, tokens: List<Token>): List<JapaneseToken> {
        val result = mutableListOf<JapaneseToken>()
        var pos = 0

        for (token in tokens) {
            val surface = token.surface
            val idx = originalText.indexOf(surface, pos)
            if (idx < 0) continue

            val reading = JapaneseTextUtil.katakanaToHiragana(token.reading ?: surface)
            val hasKanji = JapaneseTextUtil.containsKanji(surface)

            result.add(
                JapaneseToken(
                    surface = surface,
                    reading = reading,
                    startIndex = idx,
                    containsKanji = hasKanji
                )
            )
            pos = idx + surface.length
        }
        return result
    }
}
