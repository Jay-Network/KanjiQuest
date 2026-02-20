package com.jworks.kanjiquest.pipeline.parser

import java.sql.Connection

object KanaDataGenerator {

    data class KanaEntry(
        val id: Int,
        val literal: String,
        val type: String,
        val romanization: String,
        val group: String,
        val strokeCount: Int,
        val variant: String,
        val baseKanaId: Int?
    )

    fun generate(connection: Connection): Int {
        val entries = mutableListOf<KanaEntry>()

        // Hiragana basic (46) — IDs 100_001..100_046
        entries.addAll(generateBasicHiragana())
        // Hiragana dakuten (20) — IDs 100_047..100_066
        entries.addAll(generateDakutenHiragana())
        // Hiragana handakuten (5) — IDs 100_067..100_071
        entries.addAll(generateHandakutenHiragana())
        // Hiragana combinations (33) — IDs 100_072..100_104
        entries.addAll(generateComboHiragana())

        // Katakana basic (46) — IDs 200_001..200_046
        entries.addAll(generateBasicKatakana())
        // Katakana dakuten (20) — IDs 200_047..200_066
        entries.addAll(generateDakutenKatakana())
        // Katakana handakuten (5) — IDs 200_067..200_071
        entries.addAll(generateHandakutenKatakana())
        // Katakana combinations (33) — IDs 200_072..200_104
        entries.addAll(generateComboKatakana())

        val stmt = connection.prepareStatement(
            "INSERT OR REPLACE INTO kana(id, literal, type, romanization, kana_group, stroke_count, stroke_svg, variant, base_kana_id) VALUES (?,?,?,?,?,?,?,?,?)"
        )

        var count = 0
        for (entry in entries) {
            stmt.setInt(1, entry.id)
            stmt.setString(2, entry.literal)
            stmt.setString(3, entry.type)
            stmt.setString(4, entry.romanization)
            stmt.setString(5, entry.group)
            stmt.setInt(6, entry.strokeCount)
            stmt.setNull(7, java.sql.Types.VARCHAR) // stroke_svg set by KanjiVG parser
            stmt.setString(8, entry.variant)
            if (entry.baseKanaId != null) stmt.setInt(9, entry.baseKanaId) else stmt.setNull(9, java.sql.Types.INTEGER)
            stmt.addBatch()
            count++
        }
        stmt.executeBatch()
        return count
    }

    // ── Hiragana Basic (46 characters) ──

    private fun generateBasicHiragana(): List<KanaEntry> {
        val base = 100_001
        val data = listOf(
            // a-row
            Triple("あ", "a", "a-row"), Triple("い", "i", "a-row"), Triple("う", "u", "a-row"),
            Triple("え", "e", "a-row"), Triple("お", "o", "a-row"),
            // ka-row
            Triple("か", "ka", "ka-row"), Triple("き", "ki", "ka-row"), Triple("く", "ku", "ka-row"),
            Triple("け", "ke", "ka-row"), Triple("こ", "ko", "ka-row"),
            // sa-row
            Triple("さ", "sa", "sa-row"), Triple("し", "shi", "sa-row"), Triple("す", "su", "sa-row"),
            Triple("せ", "se", "sa-row"), Triple("そ", "so", "sa-row"),
            // ta-row
            Triple("た", "ta", "ta-row"), Triple("ち", "chi", "ta-row"), Triple("つ", "tsu", "ta-row"),
            Triple("て", "te", "ta-row"), Triple("と", "to", "ta-row"),
            // na-row
            Triple("な", "na", "na-row"), Triple("に", "ni", "na-row"), Triple("ぬ", "nu", "na-row"),
            Triple("ね", "ne", "na-row"), Triple("の", "no", "na-row"),
            // ha-row
            Triple("は", "ha", "ha-row"), Triple("ひ", "hi", "ha-row"), Triple("ふ", "fu", "ha-row"),
            Triple("へ", "he", "ha-row"), Triple("ほ", "ho", "ha-row"),
            // ma-row
            Triple("ま", "ma", "ma-row"), Triple("み", "mi", "ma-row"), Triple("む", "mu", "ma-row"),
            Triple("め", "me", "ma-row"), Triple("も", "mo", "ma-row"),
            // ya-row
            Triple("や", "ya", "ya-row"), Triple("ゆ", "yu", "ya-row"), Triple("よ", "yo", "ya-row"),
            // ra-row
            Triple("ら", "ra", "ra-row"), Triple("り", "ri", "ra-row"), Triple("る", "ru", "ra-row"),
            Triple("れ", "re", "ra-row"), Triple("ろ", "ro", "ra-row"),
            // wa-row
            Triple("わ", "wa", "wa-row"), Triple("を", "wo", "wa-row"),
            // n
            Triple("ん", "n", "n-row")
        )
        val strokeCounts = mapOf(
            "あ" to 3, "い" to 2, "う" to 2, "え" to 2, "お" to 3,
            "か" to 3, "き" to 4, "く" to 1, "け" to 3, "こ" to 2,
            "さ" to 3, "し" to 1, "す" to 2, "せ" to 3, "そ" to 1,
            "た" to 4, "ち" to 2, "つ" to 1, "て" to 1, "と" to 2,
            "な" to 4, "に" to 3, "ぬ" to 2, "ね" to 2, "の" to 1,
            "は" to 3, "ひ" to 1, "ふ" to 4, "へ" to 1, "ほ" to 4,
            "ま" to 3, "み" to 2, "む" to 3, "め" to 2, "も" to 3,
            "や" to 3, "ゆ" to 2, "よ" to 2,
            "ら" to 2, "り" to 2, "る" to 1, "れ" to 2, "ろ" to 1,
            "わ" to 2, "を" to 3, "ん" to 1
        )
        return data.mapIndexed { idx, (literal, rom, group) ->
            KanaEntry(base + idx, literal, "hiragana", rom, group, strokeCounts[literal] ?: 2, "basic", null)
        }
    }

    // ── Hiragana Dakuten (20) ──

    private fun generateDakutenHiragana(): List<KanaEntry> {
        val base = 100_047
        val data = listOf(
            Triple("が", "ga", "ka-row"), Triple("ぎ", "gi", "ka-row"), Triple("ぐ", "gu", "ka-row"),
            Triple("げ", "ge", "ka-row"), Triple("ご", "go", "ka-row"),
            Triple("ざ", "za", "sa-row"), Triple("じ", "ji", "sa-row"), Triple("ず", "zu", "sa-row"),
            Triple("ぜ", "ze", "sa-row"), Triple("ぞ", "zo", "sa-row"),
            Triple("だ", "da", "ta-row"), Triple("ぢ", "di", "ta-row"), Triple("づ", "du", "ta-row"),
            Triple("で", "de", "ta-row"), Triple("ど", "do", "ta-row"),
            Triple("ば", "ba", "ha-row"), Triple("び", "bi", "ha-row"), Triple("ぶ", "bu", "ha-row"),
            Triple("べ", "be", "ha-row"), Triple("ぼ", "bo", "ha-row")
        )
        val baseIds = mapOf(
            "が" to 100_006, "ぎ" to 100_007, "ぐ" to 100_008, "げ" to 100_009, "ご" to 100_010,
            "ざ" to 100_011, "じ" to 100_012, "ず" to 100_013, "ぜ" to 100_014, "ぞ" to 100_015,
            "だ" to 100_016, "ぢ" to 100_017, "づ" to 100_018, "で" to 100_019, "ど" to 100_020,
            "ば" to 100_026, "び" to 100_027, "ぶ" to 100_028, "べ" to 100_029, "ぼ" to 100_030
        )
        return data.mapIndexed { idx, (literal, rom, group) ->
            KanaEntry(base + idx, literal, "hiragana", rom, group, 0, "dakuten", baseIds[literal])
        }
    }

    // ── Hiragana Handakuten (5) ──

    private fun generateHandakutenHiragana(): List<KanaEntry> {
        val base = 100_067
        val data = listOf(
            Triple("ぱ", "pa", "ha-row"), Triple("ぴ", "pi", "ha-row"), Triple("ぷ", "pu", "ha-row"),
            Triple("ぺ", "pe", "ha-row"), Triple("ぽ", "po", "ha-row")
        )
        val baseIds = listOf(100_026, 100_027, 100_028, 100_029, 100_030)
        return data.mapIndexed { idx, (literal, rom, group) ->
            KanaEntry(base + idx, literal, "hiragana", rom, group, 0, "handakuten", baseIds[idx])
        }
    }

    // ── Hiragana Combinations (33) ──

    private fun generateComboHiragana(): List<KanaEntry> {
        val base = 100_072
        val data = listOf(
            Triple("きゃ", "kya", "ka-row"), Triple("きゅ", "kyu", "ka-row"), Triple("きょ", "kyo", "ka-row"),
            Triple("しゃ", "sha", "sa-row"), Triple("しゅ", "shu", "sa-row"), Triple("しょ", "sho", "sa-row"),
            Triple("ちゃ", "cha", "ta-row"), Triple("ちゅ", "chu", "ta-row"), Triple("ちょ", "cho", "ta-row"),
            Triple("にゃ", "nya", "na-row"), Triple("にゅ", "nyu", "na-row"), Triple("にょ", "nyo", "na-row"),
            Triple("ひゃ", "hya", "ha-row"), Triple("ひゅ", "hyu", "ha-row"), Triple("ひょ", "hyo", "ha-row"),
            Triple("みゃ", "mya", "ma-row"), Triple("みゅ", "myu", "ma-row"), Triple("みょ", "myo", "ma-row"),
            Triple("りゃ", "rya", "ra-row"), Triple("りゅ", "ryu", "ra-row"), Triple("りょ", "ryo", "ra-row"),
            Triple("ぎゃ", "gya", "ka-row"), Triple("ぎゅ", "gyu", "ka-row"), Triple("ぎょ", "gyo", "ka-row"),
            Triple("じゃ", "ja", "sa-row"), Triple("じゅ", "ju", "sa-row"), Triple("じょ", "jo", "sa-row"),
            Triple("びゃ", "bya", "ha-row"), Triple("びゅ", "byu", "ha-row"), Triple("びょ", "byo", "ha-row"),
            Triple("ぴゃ", "pya", "ha-row"), Triple("ぴゅ", "pyu", "ha-row"), Triple("ぴょ", "pyo", "ha-row")
        )
        return data.mapIndexed { idx, (literal, rom, group) ->
            KanaEntry(base + idx, literal, "hiragana", rom, group, 0, "combination", null)
        }
    }

    // ── Katakana Basic (46) ──

    private fun generateBasicKatakana(): List<KanaEntry> {
        val base = 200_001
        val data = listOf(
            Triple("ア", "a", "a-row"), Triple("イ", "i", "a-row"), Triple("ウ", "u", "a-row"),
            Triple("エ", "e", "a-row"), Triple("オ", "o", "a-row"),
            Triple("カ", "ka", "ka-row"), Triple("キ", "ki", "ka-row"), Triple("ク", "ku", "ka-row"),
            Triple("ケ", "ke", "ka-row"), Triple("コ", "ko", "ka-row"),
            Triple("サ", "sa", "sa-row"), Triple("シ", "shi", "sa-row"), Triple("ス", "su", "sa-row"),
            Triple("セ", "se", "sa-row"), Triple("ソ", "so", "sa-row"),
            Triple("タ", "ta", "ta-row"), Triple("チ", "chi", "ta-row"), Triple("ツ", "tsu", "ta-row"),
            Triple("テ", "te", "ta-row"), Triple("ト", "to", "ta-row"),
            Triple("ナ", "na", "na-row"), Triple("ニ", "ni", "na-row"), Triple("ヌ", "nu", "na-row"),
            Triple("ネ", "ne", "na-row"), Triple("ノ", "no", "na-row"),
            Triple("ハ", "ha", "ha-row"), Triple("ヒ", "hi", "ha-row"), Triple("フ", "fu", "ha-row"),
            Triple("ヘ", "he", "ha-row"), Triple("ホ", "ho", "ha-row"),
            Triple("マ", "ma", "ma-row"), Triple("ミ", "mi", "ma-row"), Triple("ム", "mu", "ma-row"),
            Triple("メ", "me", "ma-row"), Triple("モ", "mo", "ma-row"),
            Triple("ヤ", "ya", "ya-row"), Triple("ユ", "yu", "ya-row"), Triple("ヨ", "yo", "ya-row"),
            Triple("ラ", "ra", "ra-row"), Triple("リ", "ri", "ra-row"), Triple("ル", "ru", "ra-row"),
            Triple("レ", "re", "ra-row"), Triple("ロ", "ro", "ra-row"),
            Triple("ワ", "wa", "wa-row"), Triple("ヲ", "wo", "wa-row"),
            Triple("ン", "n", "n-row")
        )
        val strokeCounts = mapOf(
            "ア" to 2, "イ" to 2, "ウ" to 3, "エ" to 3, "オ" to 3,
            "カ" to 2, "キ" to 3, "ク" to 2, "ケ" to 3, "コ" to 2,
            "サ" to 3, "シ" to 3, "ス" to 2, "セ" to 2, "ソ" to 2,
            "タ" to 3, "チ" to 3, "ツ" to 3, "テ" to 3, "ト" to 2,
            "ナ" to 2, "ニ" to 2, "ヌ" to 2, "ネ" to 4, "ノ" to 1,
            "ハ" to 2, "ヒ" to 2, "フ" to 1, "ヘ" to 1, "ホ" to 4,
            "マ" to 2, "ミ" to 3, "ム" to 2, "メ" to 2, "モ" to 3,
            "ヤ" to 2, "ユ" to 2, "ヨ" to 3,
            "ラ" to 2, "リ" to 2, "ル" to 2, "レ" to 1, "ロ" to 3,
            "ワ" to 2, "ヲ" to 3, "ン" to 2
        )
        return data.mapIndexed { idx, (literal, rom, group) ->
            KanaEntry(base + idx, literal, "katakana", rom, group, strokeCounts[literal] ?: 2, "basic", null)
        }
    }

    // ── Katakana Dakuten (20) ──

    private fun generateDakutenKatakana(): List<KanaEntry> {
        val base = 200_047
        val data = listOf(
            Triple("ガ", "ga", "ka-row"), Triple("ギ", "gi", "ka-row"), Triple("グ", "gu", "ka-row"),
            Triple("ゲ", "ge", "ka-row"), Triple("ゴ", "go", "ka-row"),
            Triple("ザ", "za", "sa-row"), Triple("ジ", "ji", "sa-row"), Triple("ズ", "zu", "sa-row"),
            Triple("ゼ", "ze", "sa-row"), Triple("ゾ", "zo", "sa-row"),
            Triple("ダ", "da", "ta-row"), Triple("ヂ", "di", "ta-row"), Triple("ヅ", "du", "ta-row"),
            Triple("デ", "de", "ta-row"), Triple("ド", "do", "ta-row"),
            Triple("バ", "ba", "ha-row"), Triple("ビ", "bi", "ha-row"), Triple("ブ", "bu", "ha-row"),
            Triple("ベ", "be", "ha-row"), Triple("ボ", "bo", "ha-row")
        )
        val baseIds = mapOf(
            "ガ" to 200_006, "ギ" to 200_007, "グ" to 200_008, "ゲ" to 200_009, "ゴ" to 200_010,
            "ザ" to 200_011, "ジ" to 200_012, "ズ" to 200_013, "ゼ" to 200_014, "ゾ" to 200_015,
            "ダ" to 200_016, "ヂ" to 200_017, "ヅ" to 200_018, "デ" to 200_019, "ド" to 200_020,
            "バ" to 200_026, "ビ" to 200_027, "ブ" to 200_028, "ベ" to 200_029, "ボ" to 200_030
        )
        return data.mapIndexed { idx, (literal, rom, group) ->
            KanaEntry(base + idx, literal, "katakana", rom, group, 0, "dakuten", baseIds[literal])
        }
    }

    // ── Katakana Handakuten (5) ──

    private fun generateHandakutenKatakana(): List<KanaEntry> {
        val base = 200_067
        val data = listOf(
            Triple("パ", "pa", "ha-row"), Triple("ピ", "pi", "ha-row"), Triple("プ", "pu", "ha-row"),
            Triple("ペ", "pe", "ha-row"), Triple("ポ", "po", "ha-row")
        )
        val baseIds = listOf(200_026, 200_027, 200_028, 200_029, 200_030)
        return data.mapIndexed { idx, (literal, rom, group) ->
            KanaEntry(base + idx, literal, "katakana", rom, group, 0, "handakuten", baseIds[idx])
        }
    }

    // ── Katakana Combinations (33) ──

    private fun generateComboKatakana(): List<KanaEntry> {
        val base = 200_072
        val data = listOf(
            Triple("キャ", "kya", "ka-row"), Triple("キュ", "kyu", "ka-row"), Triple("キョ", "kyo", "ka-row"),
            Triple("シャ", "sha", "sa-row"), Triple("シュ", "shu", "sa-row"), Triple("ショ", "sho", "sa-row"),
            Triple("チャ", "cha", "ta-row"), Triple("チュ", "chu", "ta-row"), Triple("チョ", "cho", "ta-row"),
            Triple("ニャ", "nya", "na-row"), Triple("ニュ", "nyu", "na-row"), Triple("ニョ", "nyo", "na-row"),
            Triple("ヒャ", "hya", "ha-row"), Triple("ヒュ", "hyu", "ha-row"), Triple("ヒョ", "hyo", "ha-row"),
            Triple("ミャ", "mya", "ma-row"), Triple("ミュ", "myu", "ma-row"), Triple("ミョ", "myo", "ma-row"),
            Triple("リャ", "rya", "ra-row"), Triple("リュ", "ryu", "ra-row"), Triple("リョ", "ryo", "ra-row"),
            Triple("ギャ", "gya", "ka-row"), Triple("ギュ", "gyu", "ka-row"), Triple("ギョ", "gyo", "ka-row"),
            Triple("ジャ", "ja", "sa-row"), Triple("ジュ", "ju", "sa-row"), Triple("ジョ", "jo", "sa-row"),
            Triple("ビャ", "bya", "ha-row"), Triple("ビュ", "byu", "ha-row"), Triple("ビョ", "byo", "ha-row"),
            Triple("ピャ", "pya", "ha-row"), Triple("ピュ", "pyu", "ha-row"), Triple("ピョ", "pyo", "ha-row")
        )
        return data.mapIndexed { idx, (literal, rom, group) ->
            KanaEntry(base + idx, literal, "katakana", rom, group, 0, "combination", null)
        }
    }
}
