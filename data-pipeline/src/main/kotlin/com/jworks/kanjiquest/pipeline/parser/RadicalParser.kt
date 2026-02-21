package com.jworks.kanjiquest.pipeline.parser

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.sql.Connection

/**
 * Parses KRADFILE data to build a real radical/component decomposition database.
 *
 * KRADFILE contains 6,355 kanji, each decomposed into visual components drawn from
 * a set of ~253 unique elements. This replaces the old 214 Kangxi-only approach
 * with actual visual decomposition data.
 *
 * Data source: EDRDG KRADFILE (CC BY-SA 3.0)
 * See: https://www.edrdg.org/krad/kradinf.html
 */
object RadicalParser {

    /**
     * Curated component data: literal → (meaningEn, meaningJp, strokeCount, position)
     *
     * Sources for meanings:
     * - Standard Kangxi radical names where applicable
     * - Learner-friendly descriptions for non-standard components
     * - All meanings chosen to be child-friendly and intuitive
     */
    private data class ComponentInfo(
        val meaningEn: String,
        val meaningJp: String,
        val strokeCount: Int,
        val position: String?,
        val variantOf: String? = null  // base component literal if this is a variant
    )

    private val COMPONENT_DATA: Map<String, ComponentInfo> = mapOf(
        // 1-stroke components
        "一" to ComponentInfo("one", "いち", 1, null),
        "｜" to ComponentInfo("line", "ぼう", 1, null),
        "丶" to ComponentInfo("dot", "てん", 1, null),
        "ノ" to ComponentInfo("slash", "の", 1, null),
        "乙" to ComponentInfo("hook", "おつ", 1, null),
        "亅" to ComponentInfo("hook", "はねぼう", 1, null),

        // 2-stroke components
        "二" to ComponentInfo("two", "に", 2, null),
        "亠" to ComponentInfo("lid", "なべぶた", 2, "kanmuri"),
        "人" to ComponentInfo("person", "ひと", 2, "hen"),
        "儿" to ComponentInfo("legs", "にんにょう", 2, "ashi"),
        "入" to ComponentInfo("enter", "いる", 2, null),
        "ハ" to ComponentInfo("eight", "はち", 2, null),
        "冂" to ComponentInfo("border", "けいがまえ", 2, "kamae"),
        "冖" to ComponentInfo("cover", "わかんむり", 2, "kanmuri"),
        "冫" to ComponentInfo("ice", "にすい", 2, "hen"),
        "几" to ComponentInfo("table", "つくえ", 2, null),
        "凵" to ComponentInfo("open box", "うけばこ", 2, "kamae"),
        "刀" to ComponentInfo("sword", "かたな", 2, "tsukuri"),
        "力" to ComponentInfo("power", "ちから", 2, "tsukuri"),
        "勹" to ComponentInfo("wrap", "つつみがまえ", 2, "kamae"),
        "匕" to ComponentInfo("spoon", "さじ", 2, null),
        "匚" to ComponentInfo("box", "はこがまえ", 2, "kamae"),
        "十" to ComponentInfo("ten", "じゅう", 2, null),
        "卜" to ComponentInfo("fortune telling", "ぼく", 2, null),
        "卩" to ComponentInfo("seal", "ふしづくり", 2, "tsukuri"),
        "厂" to ComponentInfo("cliff", "がんだれ", 2, "tare"),
        "厶" to ComponentInfo("private", "む", 2, null),
        "又" to ComponentInfo("again", "また", 2, "tsukuri"),
        "九" to ComponentInfo("nine", "きゅう", 2, null),

        // 3-stroke components
        "口" to ComponentInfo("mouth", "くち", 3, "hen"),
        "囗" to ComponentInfo("enclosure", "くにがまえ", 3, "kamae"),
        "土" to ComponentInfo("earth", "つち", 3, "hen"),
        "士" to ComponentInfo("samurai", "さむらい", 3, "kanmuri"),
        "夂" to ComponentInfo("winter top", "ふゆがしら", 3, null),
        "夕" to ComponentInfo("evening", "ゆうべ", 3, null),
        "大" to ComponentInfo("big", "だい", 3, null),
        "女" to ComponentInfo("woman", "おんな", 3, "hen"),
        "子" to ComponentInfo("child", "こ", 3, "hen"),
        "宀" to ComponentInfo("roof", "うかんむり", 3, "kanmuri"),
        "寸" to ComponentInfo("inch", "すん", 3, "tsukuri"),
        "小" to ComponentInfo("small", "しょう", 3, null),
        "尸" to ComponentInfo("body", "しかばね", 3, "tare"),
        "山" to ComponentInfo("mountain", "やま", 3, "hen"),
        "巛" to ComponentInfo("river", "まがりがわ", 3, null),
        "川" to ComponentInfo("river", "かわ", 3, null),
        "工" to ComponentInfo("craft", "こう", 3, null),
        "巾" to ComponentInfo("cloth", "はば", 3, "hen"),
        "干" to ComponentInfo("dry", "ほす", 3, null),
        "幺" to ComponentInfo("thread", "いとがしら", 3, null),
        "广" to ComponentInfo("house", "まだれ", 3, "tare"),
        "廴" to ComponentInfo("long stride", "えんにょう", 3, "nyou"),
        "廾" to ComponentInfo("two hands", "にじゅうあし", 3, "ashi"),
        "弋" to ComponentInfo("arrow", "しきがまえ", 3, null),
        "弓" to ComponentInfo("bow", "ゆみ", 3, "hen"),
        "彡" to ComponentInfo("hair strokes", "さんづくり", 3, "tsukuri"),
        "彳" to ComponentInfo("step", "ぎょうにんべん", 3, "hen"),
        "マ" to ComponentInfo("katakana ma", "マ", 2, null),
        "ユ" to ComponentInfo("katakana yu", "ユ", 2, null),
        "ヨ" to ComponentInfo("katakana yo", "ヨ", 3, null),

        // 4-stroke components
        "心" to ComponentInfo("heart", "こころ", 4, "ashi"),
        "戈" to ComponentInfo("halberd", "ほこ", 4, "tsukuri"),
        "戸" to ComponentInfo("door", "とだれ", 4, "tare"),
        "手" to ComponentInfo("hand", "て", 4, "hen"),
        "支" to ComponentInfo("branch", "えだ", 4, null),
        "攵" to ComponentInfo("strike", "ぼくづくり", 4, "tsukuri"),
        "文" to ComponentInfo("writing", "ぶん", 4, null),
        "斗" to ComponentInfo("ladle", "とます", 4, "tsukuri"),
        "斤" to ComponentInfo("axe", "おのづくり", 4, "tsukuri"),
        "方" to ComponentInfo("direction", "ほう", 4, "hen"),
        "无" to ComponentInfo("nothing", "なし", 4, null),
        "日" to ComponentInfo("sun", "にち", 4, "hen"),
        "曰" to ComponentInfo("say", "ひらび", 4, null),
        "月" to ComponentInfo("moon", "つき", 4, "hen"),
        "木" to ComponentInfo("tree", "き", 4, "hen"),
        "欠" to ComponentInfo("yawn", "あくび", 4, "tsukuri"),
        "止" to ComponentInfo("stop", "とめる", 4, "hen"),
        "歹" to ComponentInfo("bare bones", "がつ", 4, "hen"),
        "殳" to ComponentInfo("weapon", "ほこづくり", 4, "tsukuri"),
        "毋" to ComponentInfo("mother", "なかれ", 4, null),
        "母" to ComponentInfo("mother", "はは", 5, null),
        "比" to ComponentInfo("compare", "くらべる", 4, null),
        "毛" to ComponentInfo("fur", "け", 4, null),
        "氏" to ComponentInfo("family", "うじ", 4, null),
        "气" to ComponentInfo("steam", "きがまえ", 4, "kamae"),
        "水" to ComponentInfo("water", "みず", 4, "hen"),
        "火" to ComponentInfo("fire", "ひ", 4, "hen"),
        "爪" to ComponentInfo("claw", "つめ", 4, "kanmuri"),
        "父" to ComponentInfo("father", "ちち", 4, null),
        "爻" to ComponentInfo("cross pattern", "こう", 4, null),
        "爿" to ComponentInfo("split wood", "しょうへん", 4, "hen"),
        "片" to ComponentInfo("one-sided", "かた", 4, "hen"),
        "牙" to ComponentInfo("fang", "きば", 4, null),
        "牛" to ComponentInfo("cow", "うし", 4, "hen"),
        "犬" to ComponentInfo("dog", "いぬ", 4, "hen"),

        // KRADFILE-specific visual components (non-Kangxi)
        "并" to ComponentInfo("together", "ならぶ", 4, null),
        "乞" to ComponentInfo("beg", "こう", 3, null),
        "个" to ComponentInfo("counter", "こ", 3, null),
        "杰" to ComponentInfo("fire base", "れっか", 4, "ashi"),
        "艾" to ComponentInfo("grass top", "くさかんむり", 4, "kanmuri"),  // variant of 艸
        "汁" to ComponentInfo("water drops", "さんずい", 3, "hen"),  // variant of 水
        "扎" to ComponentInfo("hand radical", "てへん", 3, "hen"),  // variant of 手
        "忙" to ComponentInfo("heart radical", "りっしんべん", 3, "hen"),  // variant of 心
        "刈" to ComponentInfo("sword radical", "りっとう", 2, "tsukuri"),  // variant of 刀
        "込" to ComponentInfo("road", "しんにょう", 4, "nyou"),  // variant of 辶
        "阡" to ComponentInfo("hill radical", "こざとへん", 3, "hen"),  // variant of 阜
        "化" to ComponentInfo("change", "にんべん", 4, null),  // left side of 化
        "犯" to ComponentInfo("dog radical", "けものへん", 3, "hen"),  // variant of 犬
        "礼" to ComponentInfo("spirit radical", "しめすへん", 4, "hen"),  // variant of 示
        "初" to ComponentInfo("clothes radical", "ころもへん", 5, "hen"),  // variant of 衣
        "尚" to ComponentInfo("esteem", "たっとぶ", 8, null),
        "疔" to ComponentInfo("sickness", "やまいだれ", 5, "tare"),

        // 5-stroke components
        "玄" to ComponentInfo("mysterious", "げん", 5, null),
        "王" to ComponentInfo("king", "おう", 4, "hen"),
        "瓜" to ComponentInfo("melon", "うり", 5, null),
        "瓦" to ComponentInfo("tile", "かわら", 5, null),
        "甘" to ComponentInfo("sweet", "あまい", 5, null),
        "生" to ComponentInfo("life", "うまれる", 5, null),
        "用" to ComponentInfo("use", "もちいる", 5, null),
        "田" to ComponentInfo("rice field", "た", 5, "hen"),
        "疋" to ComponentInfo("cloth counter", "ひき", 5, null),
        "癶" to ComponentInfo("footsteps", "はつがしら", 5, null),
        "白" to ComponentInfo("white", "しろ", 5, null),
        "皮" to ComponentInfo("skin", "かわ", 5, null),
        "皿" to ComponentInfo("dish", "さら", 5, "ashi"),
        "目" to ComponentInfo("eye", "め", 5, "hen"),
        "矛" to ComponentInfo("spear", "ほこ", 5, null),
        "矢" to ComponentInfo("arrow", "や", 5, "hen"),
        "石" to ComponentInfo("stone", "いし", 5, "hen"),
        "示" to ComponentInfo("altar", "しめす", 5, "hen"),
        "禾" to ComponentInfo("grain", "のぎ", 5, "hen"),
        "穴" to ComponentInfo("hole", "あな", 5, "kanmuri"),
        "立" to ComponentInfo("stand", "たつ", 5, "hen"),

        // More KRADFILE-specific components
        "禹" to ComponentInfo("legendary ruler", "う", 9, null),
        "尢" to ComponentInfo("bent leg", "まげあし", 3, "ashi"),
        "尤" to ComponentInfo("special", "もっとも", 4, null),
        "屮" to ComponentInfo("sprout", "てつ", 3, null),
        "已" to ComponentInfo("already", "すでに", 3, null),
        "巴" to ComponentInfo("comma pattern", "ともえ", 4, null),
        "彑" to ComponentInfo("pig snout", "けいがしら", 3, null),
        "斉" to ComponentInfo("equal", "せい", 8, null),
        "竜" to ComponentInfo("dragon", "りゅう", 10, null),
        "世" to ComponentInfo("generation", "よ", 5, null),
        "亡" to ComponentInfo("perish", "なくなる", 3, null),
        "元" to ComponentInfo("origin", "もと", 4, null),
        "免" to ComponentInfo("excuse", "まぬがれる", 8, null),
        "冊" to ComponentInfo("book counter", "さつ", 5, null),
        "品" to ComponentInfo("goods", "しな", 9, null),
        "勿" to ComponentInfo("must not", "なかれ", 4, null),
        "及" to ComponentInfo("reach", "およぶ", 3, null),
        "奄" to ComponentInfo("cover", "おおう", 8, null),
        "岡" to ComponentInfo("hill", "おか", 8, null),
        "巨" to ComponentInfo("giant", "きょだい", 5, null),
        "屯" to ComponentInfo("barracks", "たむろ", 4, null),
        "井" to ComponentInfo("well", "い", 4, null),
        "五" to ComponentInfo("five", "ご", 4, null),
        "乃" to ComponentInfo("from", "の", 2, null),
        "久" to ComponentInfo("long time", "ひさしい", 3, null),
        "也" to ComponentInfo("also", "なり", 3, null),
        "無" to ComponentInfo("nothing", "む", 12, null),
        "滴" to ComponentInfo("drops", "しずく", 14, null),
        "買" to ComponentInfo("buy", "かう", 12, null),
        "邦" to ComponentInfo("country", "くに", 7, null),

        // 6-stroke components
        "竹" to ComponentInfo("bamboo", "たけ", 6, "kanmuri"),
        "米" to ComponentInfo("rice", "こめ", 6, "hen"),
        "糸" to ComponentInfo("thread", "いと", 6, "hen"),
        "缶" to ComponentInfo("can", "ほとぎ", 6, null),
        "羊" to ComponentInfo("sheep", "ひつじ", 6, null),
        "羽" to ComponentInfo("wings", "はね", 6, null),
        "老" to ComponentInfo("old", "おいる", 6, null),
        "而" to ComponentInfo("moreover", "しこうして", 6, null),
        "耒" to ComponentInfo("plow", "すき", 6, "hen"),
        "耳" to ComponentInfo("ear", "みみ", 6, "hen"),
        "聿" to ComponentInfo("brush", "ふで", 6, null),
        "肉" to ComponentInfo("meat", "にく", 6, "hen"),
        "臣" to ComponentInfo("minister", "しん", 6, null),
        "自" to ComponentInfo("self", "みずから", 6, null),
        "至" to ComponentInfo("arrive", "いたる", 6, null),
        "臼" to ComponentInfo("mortar", "うす", 6, null),
        "舌" to ComponentInfo("tongue", "した", 6, null),
        "舛" to ComponentInfo("dance step", "まいあし", 6, null),
        "舟" to ComponentInfo("boat", "ふね", 6, "hen"),
        "艮" to ComponentInfo("stubborn", "こん", 6, null),
        "色" to ComponentInfo("color", "いろ", 6, null),
        "虍" to ComponentInfo("tiger", "とらがしら", 6, "kanmuri"),
        "虫" to ComponentInfo("insect", "むし", 6, "hen"),
        "血" to ComponentInfo("blood", "ち", 6, null),
        "行" to ComponentInfo("go", "いく", 6, "kamae"),
        "衣" to ComponentInfo("clothes", "ころも", 6, "hen"),
        "西" to ComponentInfo("west", "にし", 6, "kanmuri"),

        // 7-stroke components
        "見" to ComponentInfo("see", "みる", 7, null),
        "角" to ComponentInfo("angle", "つの", 7, "hen"),
        "言" to ComponentInfo("say", "ことば", 7, "hen"),
        "谷" to ComponentInfo("valley", "たに", 7, null),
        "豆" to ComponentInfo("bean", "まめ", 7, null),
        "豕" to ComponentInfo("pig", "いのこ", 7, null),
        "豸" to ComponentInfo("animal", "むじなへん", 7, "hen"),
        "貝" to ComponentInfo("shell", "かい", 7, "hen"),
        "赤" to ComponentInfo("red", "あか", 7, null),
        "走" to ComponentInfo("run", "はしる", 7, "nyou"),
        "足" to ComponentInfo("foot", "あし", 7, "hen"),
        "身" to ComponentInfo("body", "み", 7, "hen"),
        "車" to ComponentInfo("car", "くるま", 7, "hen"),
        "辛" to ComponentInfo("spicy", "からい", 7, null),
        "辰" to ComponentInfo("dragon sign", "たつ", 7, null),
        "酉" to ComponentInfo("sake jar", "とり", 7, "hen"),
        "釆" to ComponentInfo("divide", "のごめ", 7, null),
        "里" to ComponentInfo("village", "さと", 7, null),

        // 8-stroke components
        "金" to ComponentInfo("gold", "かね", 8, "hen"),
        "長" to ComponentInfo("long", "ながい", 8, null),
        "門" to ComponentInfo("gate", "もん", 8, "kamae"),
        "隶" to ComponentInfo("servant", "れいづくり", 8, null),
        "隹" to ComponentInfo("small bird", "ふるとり", 8, null),
        "雨" to ComponentInfo("rain", "あめ", 8, "kanmuri"),
        "青" to ComponentInfo("blue", "あお", 8, null),
        "非" to ComponentInfo("not", "あらず", 8, null),

        // 9-stroke components
        "面" to ComponentInfo("face", "めん", 9, null),
        "革" to ComponentInfo("leather", "かわ", 9, "hen"),
        "韋" to ComponentInfo("soft leather", "なめしがわ", 9, null),
        "韭" to ComponentInfo("leek", "にら", 9, null),
        "音" to ComponentInfo("sound", "おと", 9, null),
        "頁" to ComponentInfo("page", "おおがい", 9, "tsukuri"),
        "風" to ComponentInfo("wind", "かぜ", 9, null),
        "飛" to ComponentInfo("fly", "とぶ", 9, null),
        "食" to ComponentInfo("food", "しょく", 9, "hen"),
        "首" to ComponentInfo("neck", "くび", 9, null),
        "香" to ComponentInfo("fragrance", "かおり", 9, null),

        // 10+ stroke components
        "馬" to ComponentInfo("horse", "うま", 10, "hen"),
        "骨" to ComponentInfo("bone", "ほね", 10, "hen"),
        "高" to ComponentInfo("tall", "たかい", 10, null),
        "髟" to ComponentInfo("long hair", "かみがしら", 10, "kanmuri"),
        "鬥" to ComponentInfo("fight", "たたかいがまえ", 10, "kamae"),
        "鬯" to ComponentInfo("herbs", "ちょう", 10, null),
        "鬲" to ComponentInfo("tripod", "かなえ", 10, null),
        "鬼" to ComponentInfo("demon", "おに", 10, null),
        "魚" to ComponentInfo("fish", "うお", 11, "hen"),
        "鳥" to ComponentInfo("bird", "とり", 11, null),
        "鹵" to ComponentInfo("salt", "しお", 11, null),
        "鹿" to ComponentInfo("deer", "しか", 11, null),
        "麦" to ComponentInfo("wheat", "むぎ", 11, null),
        "麻" to ComponentInfo("hemp", "あさ", 11, "tare"),
        "黄" to ComponentInfo("yellow", "き", 12, null),
        "黍" to ComponentInfo("millet", "きび", 12, null),
        "黒" to ComponentInfo("black", "くろ", 12, null),
        "黹" to ComponentInfo("embroidery", "ぬいとり", 12, null),
        "黽" to ComponentInfo("frog", "べんあし", 13, null),
        "鼎" to ComponentInfo("ancient pot", "かなえ", 13, null),
        "鼓" to ComponentInfo("drum", "つづみ", 13, null),
        "鼠" to ComponentInfo("mouse", "ねずみ", 13, null),
        "鼻" to ComponentInfo("nose", "はな", 14, null),
        "齊" to ComponentInfo("uniform", "せい", 14, null),
        "歯" to ComponentInfo("tooth", "は", 12, null),
        "亀" to ComponentInfo("turtle", "かめ", 11, null),
        "龠" to ComponentInfo("flute", "やく", 17, null)
    )

    /**
     * Variant → base component mapping.
     * When a variant appears in KRADFILE decomposition, it's visually the same
     * concept as the base but drawn differently depending on position.
     */
    private val VARIANT_TO_BASE = mapOf(
        "汁" to "水",    // さんずい (3 drops) → water
        "扎" to "手",    // てへん → hand
        "忙" to "心",    // りっしんべん → heart
        "杰" to "火",    // れっか (4 dots bottom) → fire
        "艾" to "艸",    // くさかんむり → grass (note: 艸 not in KRADFILE, 艾 IS the component)
        "刈" to "刀",    // りっとう → sword
        "込" to "辶",    // しんにょう → road (note: 辶 not in KRADFILE, 込 IS used)
        "阡" to "阜",    // こざとへん → hill (阜 not in KRADFILE)
        "犯" to "犬",    // けものへん → dog
        "礼" to "示",    // しめすへん → spirit/altar
        "初" to "衣",    // ころもへん → clothes
        "化" to "人",    // にんべん variation
        "疔" to "疒"     // やまいだれ → sickness (疒 not in KRADFILE)
    )

    fun parse(connection: Connection, rawDataDir: String = "raw-data"): Int {
        // Step 1: Read KRADFILE (EUC-JP → UTF-8)
        val kradFile = File(rawDataDir, "kradfile")
        val kradUtf8File = File(rawDataDir, "kradfile-utf8")

        // Use UTF-8 version if available, otherwise convert
        val kradLines = if (kradUtf8File.exists()) {
            kradUtf8File.readLines()
        } else if (kradFile.exists()) {
            // Read as EUC-JP
            BufferedReader(InputStreamReader(kradFile.inputStream(), "EUC-JP")).readLines()
        } else {
            println("  WARNING: No kradfile found in $rawDataDir — falling back to built-in data")
            return parseBuiltInOnly(connection)
        }

        // Step 2: Parse kanji → component decomposition
        // Format: "漢 : 口 廿 一 冫 又"
        val kanjiToComponents = mutableMapOf<String, List<String>>()
        for (line in kradLines) {
            if (line.startsWith("#") || line.isBlank()) continue
            val parts = line.split(" : ", limit = 2)
            if (parts.size != 2) continue
            val kanjiLiteral = parts[0].trim()
            val components = parts[1].trim().split(" ").filter { it.isNotBlank() }
            kanjiToComponents[kanjiLiteral] = components
        }

        // Step 3: Collect all unique components and their frequencies
        val componentFrequency = mutableMapOf<String, Int>()
        for ((_, components) in kanjiToComponents) {
            for (comp in components) {
                componentFrequency[comp] = (componentFrequency[comp] ?: 0) + 1
            }
        }

        println("  KRADFILE: ${kanjiToComponents.size} kanji decomposed into ${componentFrequency.size} unique components")

        // Step 4: Insert components as radicals (ID range 300001+)
        val radicalStmt = connection.prepareStatement(
            "INSERT OR REPLACE INTO radical(id, literal, meaning_en, meaning_jp, stroke_count, stroke_svg, frequency, example_kanji, position, priority) VALUES (?,?,?,?,?,?,?,?,?,?)"
        )

        // Sort components for stable ID assignment: by frequency DESC (most common first)
        val sortedComponents = componentFrequency.entries.sortedByDescending { it.value }
        val literalToRadId = mutableMapOf<String, Int>()
        var nextId = 1

        for ((literal, freq) in sortedComponents) {
            val radId = 300_000 + nextId
            literalToRadId[literal] = radId

            val info = COMPONENT_DATA[literal]
            val meaningEn = info?.meaningEn ?: guessComponentMeaning(literal)
            val meaningJp = info?.meaningJp ?: ""
            val strokeCount = info?.strokeCount ?: guessStrokeCount(literal)
            val position = info?.position

            // Calculate priority based on frequency
            val priority = when {
                freq >= 100 -> 1  // Top tier: essential components (covers ~75% of kanji)
                freq >= 20 -> 2   // Common tier
                else -> 3         // Uncommon tier
            }

            radicalStmt.setInt(1, radId)
            radicalStmt.setString(2, literal)
            radicalStmt.setString(3, meaningEn)
            radicalStmt.setString(4, meaningJp)
            radicalStmt.setInt(5, strokeCount)
            radicalStmt.setNull(6, java.sql.Types.VARCHAR) // stroke_svg
            radicalStmt.setInt(7, freq)
            radicalStmt.setString(8, "[]") // example_kanji — filled below
            radicalStmt.setString(9, position)
            radicalStmt.setInt(10, priority)
            radicalStmt.addBatch()

            nextId++
        }
        radicalStmt.executeBatch()

        // Step 5: Build kanji_radical junction from KRADFILE decomposition
        val kanjiRadicalStmt = connection.prepareStatement(
            "INSERT OR IGNORE INTO kanji_radical(kanji_id, radical_id) VALUES (?, ?)"
        )

        // Build kanji literal → DB ID mapping
        val kanjiLiteralToId = mutableMapOf<String, Int>()
        val kanjiStmt = connection.createStatement()
        val kanjiRs = kanjiStmt.executeQuery("SELECT id, literal FROM kanji")
        while (kanjiRs.next()) {
            kanjiLiteralToId[kanjiRs.getString("literal")] = kanjiRs.getInt("id")
        }

        // Track which kanji are linked to each radical (for example_kanji)
        val radicalToKanjiIds = mutableMapOf<Int, MutableList<Int>>()
        var linkCount = 0

        for ((kanjiLiteral, components) in kanjiToComponents) {
            val kanjiId = kanjiLiteralToId[kanjiLiteral] ?: continue
            for (comp in components) {
                val radId = literalToRadId[comp] ?: continue
                kanjiRadicalStmt.setInt(1, kanjiId)
                kanjiRadicalStmt.setInt(2, radId)
                kanjiRadicalStmt.addBatch()
                radicalToKanjiIds.getOrPut(radId) { mutableListOf() }.add(kanjiId)
                linkCount++
            }
        }
        kanjiRadicalStmt.executeBatch()

        // Step 6: Update example_kanji for each radical (up to 5 common kanji)
        // Prefer joyo kanji (grade 1-6) and frequent kanji
        val updateStmt = connection.prepareStatement(
            "UPDATE radical SET example_kanji = ? WHERE id = ?"
        )

        // Get kanji frequency/grade for sorting examples
        val kanjiGrades = mutableMapOf<Int, Int>()
        val kanjiFreqs = mutableMapOf<Int, Int>()
        val gradeRs = kanjiStmt.executeQuery("SELECT id, grade, frequency FROM kanji")
        while (gradeRs.next()) {
            val id = gradeRs.getInt("id")
            val grade = gradeRs.getInt("grade")
            if (!gradeRs.wasNull()) kanjiGrades[id] = grade
            val freq = gradeRs.getInt("frequency")
            if (!gradeRs.wasNull()) kanjiFreqs[id] = freq
        }

        for ((radId, kanjiIds) in radicalToKanjiIds) {
            // Sort: prefer lower grade (easier), then higher frequency
            val sorted = kanjiIds.sortedWith(compareBy(
                { kanjiGrades[it] ?: 99 },  // grade 1 first, ungraded last
                { -(kanjiFreqs[it] ?: 0) }  // higher frequency first
            ))
            val exampleJson = sorted.take(5).joinToString(",", "[", "]")
            updateStmt.setString(1, exampleJson)
            updateStmt.setInt(2, radId)
            updateStmt.addBatch()
        }
        updateStmt.executeBatch()

        println("  Inserted ${componentFrequency.size} components with $linkCount kanji-radical links")

        // Print priority breakdown
        val p1 = sortedComponents.count { it.value >= 100 }
        val p2 = sortedComponents.count { it.value in 20..99 }
        val p3 = sortedComponents.count { it.value < 20 }
        println("  Priority breakdown: P1=$p1 (essential), P2=$p2 (common), P3=$p3 (uncommon)")

        return componentFrequency.size
    }

    /**
     * Fallback when KRADFILE is not available — uses built-in component data.
     */
    private fun parseBuiltInOnly(connection: Connection): Int {
        val radicalStmt = connection.prepareStatement(
            "INSERT OR REPLACE INTO radical(id, literal, meaning_en, meaning_jp, stroke_count, stroke_svg, frequency, example_kanji, position, priority) VALUES (?,?,?,?,?,?,?,?,?,?)"
        )

        var nextId = 1
        for ((literal, info) in COMPONENT_DATA) {
            val radId = 300_000 + nextId
            radicalStmt.setInt(1, radId)
            radicalStmt.setString(2, literal)
            radicalStmt.setString(3, info.meaningEn)
            radicalStmt.setString(4, info.meaningJp)
            radicalStmt.setInt(5, info.strokeCount)
            radicalStmt.setNull(6, java.sql.Types.VARCHAR)
            radicalStmt.setInt(7, 0)
            radicalStmt.setString(8, "[]")
            radicalStmt.setString(9, info.position)
            radicalStmt.setInt(10, 2) // default to common priority
            radicalStmt.addBatch()
            nextId++
        }
        radicalStmt.executeBatch()
        return COMPONENT_DATA.size
    }

    /**
     * Guess a learner-friendly meaning for components not in our curated list.
     */
    private fun guessComponentMeaning(literal: String): String {
        // Try Unicode name-based guesses for common kanji used as components
        return when (literal) {
            else -> "component" // safe fallback
        }
    }

    /**
     * Guess stroke count for components not in our curated list.
     */
    private fun guessStrokeCount(literal: String): Int {
        // Most KRADFILE components are simple, 2-4 strokes
        return 3
    }
}
