package com.jworks.kanjiquest.pipeline.parser

import java.sql.Connection

object RadicalParser {

    /**
     * 214 Kangxi radicals — hardcoded reference table (this data never changes).
     * Format: (kangxi_number, literal, meaning_en, meaning_jp, stroke_count, position)
     */
    private val KANGXI_RADICALS = listOf(
        R(1, "一", "one", "いち", 1, null),
        R(2, "丨", "line", "ぼう", 1, null),
        R(3, "丶", "dot", "てん", 1, null),
        R(4, "丿", "slash", "の", 1, null),
        R(5, "乙", "second", "おつ", 1, null),
        R(6, "亅", "hook", "はねぼう", 1, null),
        R(7, "二", "two", "に", 2, null),
        R(8, "亠", "lid", "なべぶた", 2, "kanmuri"),
        R(9, "人", "person", "ひと", 2, "hen"),
        R(10, "儿", "legs", "にんにょう", 2, "ashi"),
        R(11, "入", "enter", "いる", 2, null),
        R(12, "八", "eight", "はち", 2, null),
        R(13, "冂", "upside down box", "けいがまえ", 2, "kamae"),
        R(14, "冖", "cover", "わかんむり", 2, "kanmuri"),
        R(15, "冫", "ice", "にすい", 2, "hen"),
        R(16, "几", "table", "つくえ", 2, null),
        R(17, "凵", "open mouth", "うけばこ", 2, "kamae"),
        R(18, "刀", "sword", "かたな", 2, "tsukuri"),
        R(19, "力", "power", "ちから", 2, "tsukuri"),
        R(20, "勹", "wrap", "つつみがまえ", 2, "kamae"),
        R(21, "匕", "spoon", "さじ", 2, null),
        R(22, "匚", "box", "はこがまえ", 2, "kamae"),
        R(23, "匸", "hiding", "かくしがまえ", 2, "kamae"),
        R(24, "十", "ten", "じゅう", 2, null),
        R(25, "卜", "divination", "ぼく", 2, null),
        R(26, "卩", "seal", "ふしづくり", 2, "tsukuri"),
        R(27, "厂", "cliff", "がんだれ", 2, "tare"),
        R(28, "厶", "private", "む", 2, null),
        R(29, "又", "again", "また", 2, "tsukuri"),
        R(30, "口", "mouth", "くち", 3, "hen"),
        R(31, "囗", "enclosure", "くにがまえ", 3, "kamae"),
        R(32, "土", "earth", "つち", 3, "hen"),
        R(33, "士", "scholar", "さむらい", 3, "kanmuri"),
        R(34, "夂", "go", "ふゆがしら", 3, null),
        R(35, "夊", "go slowly", "すいにょう", 3, "ashi"),
        R(36, "夕", "evening", "ゆうべ", 3, null),
        R(37, "大", "big", "だい", 3, null),
        R(38, "女", "woman", "おんな", 3, "hen"),
        R(39, "子", "child", "こ", 3, "hen"),
        R(40, "宀", "roof", "うかんむり", 3, "kanmuri"),
        R(41, "寸", "inch", "すん", 3, "tsukuri"),
        R(42, "小", "small", "しょう", 3, null),
        R(43, "尢", "lame", "まげあし", 3, "ashi"),
        R(44, "尸", "corpse", "しかばね", 3, "tare"),
        R(45, "屮", "sprout", "てつ", 3, null),
        R(46, "山", "mountain", "やま", 3, "hen"),
        R(47, "巛", "river", "まがりがわ", 3, null),
        R(48, "工", "work", "こう", 3, null),
        R(49, "己", "oneself", "おのれ", 3, null),
        R(50, "巾", "towel", "はば", 3, "hen"),
        R(51, "干", "dry", "ほす", 3, null),
        R(52, "幺", "short thread", "いとがしら", 3, null),
        R(53, "广", "dotted cliff", "まだれ", 3, "tare"),
        R(54, "廴", "long stride", "えんにょう", 3, "nyou"),
        R(55, "廾", "two hands", "にじゅうあし", 3, "ashi"),
        R(56, "弋", "shoot", "しきがまえ", 3, null),
        R(57, "弓", "bow", "ゆみ", 3, "hen"),
        R(58, "彐", "snout", "けいがしら", 3, null),
        R(59, "彡", "bristle", "さんづくり", 3, "tsukuri"),
        R(60, "彳", "step", "ぎょうにんべん", 3, "hen"),
        R(61, "心", "heart", "こころ", 4, "hen"),
        R(62, "戈", "halberd", "ほこ", 4, "tsukuri"),
        R(63, "戸", "door", "とだれ", 4, "tare"),
        R(64, "手", "hand", "て", 4, "hen"),
        R(65, "支", "branch", "えだ", 4, null),
        R(66, "攴", "rap", "ぼくづくり", 4, "tsukuri"),
        R(67, "文", "script", "ぶん", 4, null),
        R(68, "斗", "dipper", "とます", 4, "tsukuri"),
        R(69, "斤", "axe", "おのづくり", 4, "tsukuri"),
        R(70, "方", "square", "ほう", 4, "hen"),
        R(71, "无", "not", "なし", 4, null),
        R(72, "日", "sun", "にち", 4, "hen"),
        R(73, "曰", "say", "ひらび", 4, null),
        R(74, "月", "moon", "つき", 4, "hen"),
        R(75, "木", "tree", "き", 4, "hen"),
        R(76, "欠", "lack", "あくび", 4, "tsukuri"),
        R(77, "止", "stop", "とめる", 4, "hen"),
        R(78, "歹", "death", "がつ", 4, "hen"),
        R(79, "殳", "weapon", "ほこづくり", 4, "tsukuri"),
        R(80, "毋", "do not", "なかれ", 4, null),
        R(81, "比", "compare", "くらべる", 4, null),
        R(82, "毛", "fur", "け", 4, null),
        R(83, "氏", "clan", "うじ", 4, null),
        R(84, "气", "steam", "きがまえ", 4, "kamae"),
        R(85, "水", "water", "みず", 4, "hen"),
        R(86, "火", "fire", "ひ", 4, "hen"),
        R(87, "爪", "claw", "つめ", 4, "kanmuri"),
        R(88, "父", "father", "ちち", 4, null),
        R(89, "爻", "mix", "こう", 4, null),
        R(90, "丬", "split wood", "しょうへん", 4, "hen"),
        R(91, "片", "slice", "かた", 4, "hen"),
        R(92, "牙", "fang", "きば", 4, null),
        R(93, "牛", "cow", "うし", 4, "hen"),
        R(94, "犬", "dog", "いぬ", 4, "hen"),
        R(95, "玄", "mysterious", "げん", 5, null),
        R(96, "玉", "jade", "たま", 5, "hen"),
        R(97, "瓜", "melon", "うり", 5, null),
        R(98, "瓦", "tile", "かわら", 5, null),
        R(99, "甘", "sweet", "あまい", 5, null),
        R(100, "生", "life", "うまれる", 5, null),
        R(101, "用", "use", "もちいる", 5, null),
        R(102, "田", "field", "た", 5, "hen"),
        R(103, "疋", "bolt of cloth", "ひき", 5, null),
        R(104, "疒", "sickness", "やまいだれ", 5, "tare"),
        R(105, "癶", "dotted tent", "はつがしら", 5, null),
        R(106, "白", "white", "しろ", 5, null),
        R(107, "皮", "skin", "かわ", 5, null),
        R(108, "皿", "dish", "さら", 5, "ashi"),
        R(109, "目", "eye", "め", 5, "hen"),
        R(110, "矛", "spear", "ほこ", 5, null),
        R(111, "矢", "arrow", "や", 5, "hen"),
        R(112, "石", "stone", "いし", 5, "hen"),
        R(113, "示", "spirit", "しめす", 5, "hen"),
        R(114, "禸", "track", "ぐうのあし", 5, null),
        R(115, "禾", "grain", "のぎ", 5, "hen"),
        R(116, "穴", "hole", "あな", 5, "kanmuri"),
        R(117, "立", "stand", "たつ", 5, "hen"),
        R(118, "竹", "bamboo", "たけ", 6, "kanmuri"),
        R(119, "米", "rice", "こめ", 6, "hen"),
        R(120, "糸", "silk", "いと", 6, "hen"),
        R(121, "缶", "jar", "ほとぎ", 6, null),
        R(122, "网", "net", "あみがしら", 6, "kanmuri"),
        R(123, "羊", "sheep", "ひつじ", 6, null),
        R(124, "羽", "feather", "はね", 6, null),
        R(125, "老", "old", "おいる", 6, null),
        R(126, "而", "and", "しこうして", 6, null),
        R(127, "耒", "plow", "すき", 6, "hen"),
        R(128, "耳", "ear", "みみ", 6, "hen"),
        R(129, "聿", "brush", "ふで", 6, null),
        R(130, "肉", "meat", "にく", 6, "hen"),
        R(131, "臣", "minister", "しん", 6, null),
        R(132, "自", "self", "みずから", 6, null),
        R(133, "至", "arrive", "いたる", 6, null),
        R(134, "臼", "mortar", "うす", 6, null),
        R(135, "舌", "tongue", "した", 6, null),
        R(136, "舛", "oppose", "まいあし", 6, null),
        R(137, "舟", "boat", "ふね", 6, "hen"),
        R(138, "艮", "stopping", "こん", 6, null),
        R(139, "色", "color", "いろ", 6, null),
        R(140, "艸", "grass", "くさ", 6, "kanmuri"),
        R(141, "虍", "tiger", "とらがしら", 6, "kanmuri"),
        R(142, "虫", "insect", "むし", 6, "hen"),
        R(143, "血", "blood", "ち", 6, null),
        R(144, "行", "go", "いく", 6, "kamae"),
        R(145, "衣", "clothes", "ころも", 6, "hen"),
        R(146, "襾", "west", "にし", 6, "kanmuri"),
        R(147, "見", "see", "みる", 7, null),
        R(148, "角", "horn", "つの", 7, "hen"),
        R(149, "言", "speech", "ことば", 7, "hen"),
        R(150, "谷", "valley", "たに", 7, null),
        R(151, "豆", "bean", "まめ", 7, null),
        R(152, "豕", "pig", "いのこ", 7, null),
        R(153, "豸", "badger", "むじなへん", 7, "hen"),
        R(154, "貝", "shell", "かい", 7, "hen"),
        R(155, "赤", "red", "あか", 7, null),
        R(156, "走", "run", "はしる", 7, "nyou"),
        R(157, "足", "foot", "あし", 7, "hen"),
        R(158, "身", "body", "み", 7, "hen"),
        R(159, "車", "car", "くるま", 7, "hen"),
        R(160, "辛", "bitter", "からい", 7, null),
        R(161, "辰", "morning", "たつ", 7, null),
        R(162, "辶", "walk", "しんにょう", 7, "nyou"),
        R(163, "邑", "city", "むら", 7, "tsukuri"),
        R(164, "酉", "wine", "とり", 7, "hen"),
        R(165, "釆", "distinguish", "のごめ", 7, null),
        R(166, "里", "village", "さと", 7, null),
        R(167, "金", "gold", "かね", 8, "hen"),
        R(168, "長", "long", "ながい", 8, null),
        R(169, "門", "gate", "もん", 8, "kamae"),
        R(170, "阜", "mound", "こざとへん", 8, "hen"),
        R(171, "隶", "slave", "れいづくり", 8, null),
        R(172, "隹", "short-tailed bird", "ふるとり", 8, null),
        R(173, "雨", "rain", "あめ", 8, "kanmuri"),
        R(174, "青", "blue", "あお", 8, null),
        R(175, "非", "wrong", "あらず", 8, null),
        R(176, "面", "face", "めん", 9, null),
        R(177, "革", "leather", "かわ", 9, "hen"),
        R(178, "韋", "tanned leather", "なめしがわ", 9, null),
        R(179, "韭", "leek", "にら", 9, null),
        R(180, "音", "sound", "おと", 9, null),
        R(181, "頁", "leaf", "おおがい", 9, "tsukuri"),
        R(182, "風", "wind", "かぜ", 9, null),
        R(183, "飛", "fly", "とぶ", 9, null),
        R(184, "食", "eat", "しょく", 9, "hen"),
        R(185, "首", "head", "くび", 9, null),
        R(186, "香", "fragrant", "かおり", 9, null),
        R(187, "馬", "horse", "うま", 10, "hen"),
        R(188, "骨", "bone", "ほね", 10, "hen"),
        R(189, "高", "tall", "たかい", 10, null),
        R(190, "髟", "hair", "かみがしら", 10, "kanmuri"),
        R(191, "鬥", "fight", "たたかいがまえ", 10, "kamae"),
        R(192, "鬯", "sacrificial wine", "ちょう", 10, null),
        R(193, "鬲", "cauldron", "かなえ", 10, null),
        R(194, "鬼", "ghost", "おに", 10, null),
        R(195, "魚", "fish", "うお", 11, "hen"),
        R(196, "鳥", "bird", "とり", 11, null),
        R(197, "鹵", "salt", "しお", 11, null),
        R(198, "鹿", "deer", "しか", 11, null),
        R(199, "麦", "wheat", "むぎ", 11, null),
        R(200, "麻", "hemp", "あさ", 11, "tare"),
        R(201, "黄", "yellow", "き", 12, null),
        R(202, "黍", "millet", "きび", 12, null),
        R(203, "黒", "black", "くろ", 12, null),
        R(204, "黹", "embroidery", "ぬいとり", 12, null),
        R(205, "黽", "frog", "べんあし", 13, null),
        R(206, "鼎", "tripod", "かなえ", 13, null),
        R(207, "鼓", "drum", "つづみ", 13, null),
        R(208, "鼠", "rat", "ねずみ", 13, null),
        R(209, "鼻", "nose", "はな", 14, null),
        R(210, "齊", "even", "せい", 14, null),
        R(211, "歯", "tooth", "は", 15, null),
        R(212, "龍", "dragon", "りゅう", 16, null),
        R(213, "亀", "turtle", "かめ", 16, null),
        R(214, "龠", "flute", "やく", 17, null)
    )

    private data class R(
        val id: Int, val literal: String, val meaningEn: String,
        val meaningJp: String, val strokeCount: Int, val position: String?
    )

    fun parse(connection: Connection): Int {
        // Insert radicals
        val radicalStmt = connection.prepareStatement(
            "INSERT OR REPLACE INTO radical(id, literal, meaning_en, meaning_jp, stroke_count, stroke_svg, frequency, example_kanji, position) VALUES (?,?,?,?,?,?,?,?,?)"
        )

        // Use IDs in the 300K range to avoid collision with kanji/kana IDs
        for (r in KANGXI_RADICALS) {
            val radId = 300_000 + r.id
            radicalStmt.setInt(1, radId)
            radicalStmt.setString(2, r.literal)
            radicalStmt.setString(3, r.meaningEn)
            radicalStmt.setString(4, r.meaningJp)
            radicalStmt.setInt(5, r.strokeCount)
            radicalStmt.setNull(6, java.sql.Types.VARCHAR)
            radicalStmt.setInt(7, 0) // frequency calculated after linking
            radicalStmt.setString(8, "[]")
            radicalStmt.setString(9, r.position)
            radicalStmt.addBatch()
        }
        radicalStmt.executeBatch()

        // Build kanji_radical junction from KANJIDIC2 radical references
        // Query all kanji that have readings (they're in the DB), then try to match radicals
        val kanjiRadicalStmt = connection.prepareStatement(
            "INSERT OR IGNORE INTO kanji_radical(kanji_id, radical_id) VALUES (?, ?)"
        )

        // Build a map of radical literal → radical ID
        val literalToRadId = KANGXI_RADICALS.associate { it.literal to (300_000 + it.id) }

        // For each kanji, check if its literal contains radical-like components
        // This is a simplified approach — for full accuracy, use KANJIDIC2 radical data
        val kanjiStmt = connection.createStatement()
        val kanjiRs = kanjiStmt.executeQuery("SELECT id, literal FROM kanji")
        val kanjiRadicalLinks = mutableMapOf<Int, MutableList<Int>>()

        while (kanjiRs.next()) {
            val kanjiId = kanjiRs.getInt("id")
            val kanjiLiteral = kanjiRs.getString("literal")

            // Match: if the kanji IS a radical literal itself
            val matchedRadId = literalToRadId[kanjiLiteral]
            if (matchedRadId != null) {
                kanjiRadicalStmt.setInt(1, kanjiId)
                kanjiRadicalStmt.setInt(2, matchedRadId)
                kanjiRadicalStmt.addBatch()
                kanjiRadicalLinks.getOrPut(matchedRadId) { mutableListOf() }.add(kanjiId)
            }
        }
        kanjiRadicalStmt.executeBatch()

        // Update frequency and example_kanji for each radical
        val updateStmt = connection.prepareStatement(
            "UPDATE radical SET frequency = ?, example_kanji = ? WHERE id = ?"
        )
        for ((radId, kanjiIds) in kanjiRadicalLinks) {
            val exampleJson = kanjiIds.take(5).joinToString(",", "[", "]")
            updateStmt.setInt(1, kanjiIds.size)
            updateStmt.setString(2, exampleJson)
            updateStmt.setInt(3, radId)
            updateStmt.addBatch()
        }
        updateStmt.executeBatch()

        return KANGXI_RADICALS.size
    }
}
