package com.jworks.kanjiquest.core.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.jworks.kanjiquest.db.KanjiQuestDatabase
import java.io.FileOutputStream

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val isFreshCopy = copyDatabaseFromAssets()
        if (isFreshCopy) {
            migratePreBuiltDatabase()
        }
        ensureNewTables()
        return AndroidSqliteDriver(
            schema = KanjiQuestDatabase.Schema,
            context = context,
            name = DB_NAME
        )
    }

    /** Returns true if a fresh copy was made (first install). */
    private fun copyDatabaseFromAssets(): Boolean {
        val dbFile = context.getDatabasePath(DB_NAME)
        if (dbFile.exists()) return false

        dbFile.parentFile?.mkdirs()
        context.assets.open(DB_NAME).use { input ->
            FileOutputStream(dbFile).use { output ->
                input.copyTo(output)
            }
        }
        return true
    }

    /**
     * The pre-built DB from the data pipeline has kanji/vocabulary data but:
     * 1. No schema version set (PRAGMA user_version = 0)
     * 2. No JCoin tables (added after pipeline was built)
     *
     * We set user_version = 1 so AndroidSqliteDriver won't try to re-create
     * all tables, and add JCoin tables via CREATE TABLE IF NOT EXISTS.
     */
    private fun migratePreBuiltDatabase() {
        val dbFile = context.getDatabasePath(DB_NAME)
        val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
        db.use {
            it.execSQL("PRAGMA user_version = 1")

            it.execSQL("""
                CREATE TABLE IF NOT EXISTS coin_balance (
                    user_id TEXT PRIMARY KEY NOT NULL,
                    local_balance INTEGER NOT NULL DEFAULT 0,
                    synced_balance INTEGER NOT NULL DEFAULT 0,
                    lifetime_earned INTEGER NOT NULL DEFAULT 0,
                    lifetime_spent INTEGER NOT NULL DEFAULT 0,
                    tier TEXT NOT NULL DEFAULT 'bronze',
                    last_synced_at INTEGER NOT NULL DEFAULT 0,
                    needs_sync INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            it.execSQL("""
                CREATE TABLE IF NOT EXISTS coin_sync_queue (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    user_id TEXT NOT NULL,
                    event_type TEXT NOT NULL,
                    source_business TEXT NOT NULL DEFAULT 'kanjiquests',
                    source_type TEXT NOT NULL,
                    base_amount INTEGER NOT NULL,
                    description TEXT NOT NULL,
                    metadata TEXT NOT NULL DEFAULT '{}',
                    created_at INTEGER NOT NULL,
                    sync_status TEXT NOT NULL DEFAULT 'pending',
                    retry_count INTEGER NOT NULL DEFAULT 0,
                    last_attempt_at INTEGER,
                    error_message TEXT
                )
            """.trimIndent())

            it.execSQL("""
                CREATE TABLE IF NOT EXISTS premium_content_unlocks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    user_id TEXT NOT NULL,
                    content_type TEXT NOT NULL,
                    content_id TEXT NOT NULL,
                    unlocked_at INTEGER NOT NULL,
                    cost_coins INTEGER NOT NULL,
                    UNIQUE(user_id, content_type, content_id)
                )
            """.trimIndent())

            it.execSQL("""
                CREATE TABLE IF NOT EXISTS active_boosters (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    user_id TEXT NOT NULL,
                    booster_type TEXT NOT NULL,
                    multiplier REAL NOT NULL DEFAULT 1.0,
                    activated_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL
                )
            """.trimIndent())

            it.execSQL("CREATE INDEX IF NOT EXISTS idx_sync_queue_status ON coin_sync_queue(sync_status, created_at)")
            it.execSQL("CREATE INDEX IF NOT EXISTS idx_unlocks_user ON premium_content_unlocks(user_id, content_type)")
            it.execSQL("CREATE INDEX IF NOT EXISTS idx_boosters_user ON active_boosters(user_id, expires_at)")
        }
    }

    /** Creates tables added after the initial schema - runs on every app start. */
    private fun ensureNewTables() {
        val dbFile = context.getDatabasePath(DB_NAME)
        if (!dbFile.exists()) return
        val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
        db.use {
            it.execSQL("""
                CREATE TABLE IF NOT EXISTS vocab_srs_card (
                    vocab_id INTEGER PRIMARY KEY NOT NULL,
                    ease_factor REAL NOT NULL DEFAULT 2.5,
                    interval INTEGER NOT NULL DEFAULT 0,
                    repetitions INTEGER NOT NULL DEFAULT 0,
                    next_review INTEGER NOT NULL DEFAULT 0,
                    state TEXT NOT NULL DEFAULT 'new',
                    total_reviews INTEGER NOT NULL DEFAULT 0,
                    correct_count INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            it.execSQL("""
                CREATE TABLE IF NOT EXISTS learning_sync_queue (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    user_id TEXT NOT NULL,
                    event_type TEXT NOT NULL,
                    payload TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    sync_status TEXT NOT NULL DEFAULT 'pending',
                    retry_count INTEGER NOT NULL DEFAULT 0,
                    last_attempt_at INTEGER,
                    error_message TEXT
                )
            """.trimIndent())

            it.execSQL("""
                CREATE TABLE IF NOT EXISTS learning_sync_metadata (
                    user_id TEXT PRIMARY KEY NOT NULL,
                    last_synced_at INTEGER NOT NULL DEFAULT 0,
                    last_push_at INTEGER NOT NULL DEFAULT 0,
                    last_pull_at INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            it.execSQL("CREATE INDEX IF NOT EXISTS idx_learning_sync_status ON learning_sync_queue(sync_status, created_at)")

            it.execSQL("""
                CREATE TABLE IF NOT EXISTS flashcard_deck (
                    kanji_id INTEGER NOT NULL PRIMARY KEY,
                    added_at TEXT NOT NULL DEFAULT (datetime('now')),
                    last_studied_at TEXT,
                    study_count INTEGER NOT NULL DEFAULT 0,
                    notes TEXT
                )
            """.trimIndent())

            it.execSQL("""
                CREATE TABLE IF NOT EXISTS kanji_mode_stats (
                    kanji_id INTEGER NOT NULL,
                    game_mode TEXT NOT NULL,
                    review_count INTEGER NOT NULL DEFAULT 0,
                    correct_count INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (kanji_id, game_mode)
                )
            """.trimIndent())

            // Kana + Radical tables (added for hiragana/katakana/radical learning modes)
            it.execSQL("""
                CREATE TABLE IF NOT EXISTS kana (
                    id INTEGER PRIMARY KEY NOT NULL,
                    literal TEXT NOT NULL UNIQUE,
                    type TEXT NOT NULL,
                    romanization TEXT NOT NULL,
                    kana_group TEXT NOT NULL,
                    stroke_count INTEGER NOT NULL,
                    stroke_svg TEXT,
                    variant TEXT NOT NULL DEFAULT 'basic',
                    base_kana_id INTEGER
                )
            """.trimIndent())

            it.execSQL("""
                CREATE TABLE IF NOT EXISTS radical (
                    id INTEGER PRIMARY KEY NOT NULL,
                    literal TEXT NOT NULL,
                    meaning_en TEXT NOT NULL,
                    meaning_jp TEXT,
                    stroke_count INTEGER NOT NULL,
                    stroke_svg TEXT,
                    frequency INTEGER NOT NULL DEFAULT 0,
                    example_kanji TEXT NOT NULL DEFAULT '[]',
                    position TEXT
                )
            """.trimIndent())

            it.execSQL("""
                CREATE TABLE IF NOT EXISTS kanji_radical (
                    kanji_id INTEGER NOT NULL,
                    radical_id INTEGER NOT NULL,
                    PRIMARY KEY (kanji_id, radical_id)
                )
            """.trimIndent())

            it.execSQL("""
                CREATE TABLE IF NOT EXISTS kana_srs_card (
                    kana_id INTEGER PRIMARY KEY NOT NULL,
                    ease_factor REAL NOT NULL DEFAULT 2.5,
                    interval INTEGER NOT NULL DEFAULT 0,
                    repetitions INTEGER NOT NULL DEFAULT 0,
                    next_review INTEGER NOT NULL DEFAULT 0,
                    state TEXT NOT NULL DEFAULT 'new',
                    total_reviews INTEGER NOT NULL DEFAULT 0,
                    correct_count INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            it.execSQL("""
                CREATE TABLE IF NOT EXISTS radical_srs_card (
                    radical_id INTEGER PRIMARY KEY NOT NULL,
                    ease_factor REAL NOT NULL DEFAULT 2.5,
                    interval INTEGER NOT NULL DEFAULT 0,
                    repetitions INTEGER NOT NULL DEFAULT 0,
                    next_review INTEGER NOT NULL DEFAULT 0,
                    state TEXT NOT NULL DEFAULT 'new',
                    total_reviews INTEGER NOT NULL DEFAULT 0,
                    correct_count INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            it.execSQL("CREATE INDEX IF NOT EXISTS idx_kana_type ON kana(type)")
            it.execSQL("CREATE INDEX IF NOT EXISTS idx_kana_group ON kana(type, kana_group)")
            it.execSQL("CREATE INDEX IF NOT EXISTS idx_radical_strokes ON radical(stroke_count)")
            it.execSQL("CREATE INDEX IF NOT EXISTS idx_kanji_radical_kid ON kanji_radical(kanji_id)")
            it.execSQL("CREATE INDEX IF NOT EXISTS idx_kanji_radical_rid ON kanji_radical(radical_id)")

            // Seed kana/radical data if tables are empty (upgrade path)
            val kanaCount = it.rawQuery("SELECT COUNT(*) FROM kana", null).use { c -> c.moveToFirst(); c.getLong(0) }
            if (kanaCount == 0L) {
                seedKanaData(it)
            }
            val radicalCount = it.rawQuery("SELECT COUNT(*) FROM radical", null).use { c -> c.moveToFirst(); c.getLong(0) }
            if (radicalCount == 0L) {
                seedRadicalData(it)
            }
        }
    }

    private fun seedKanaData(db: SQLiteDatabase) {
        // Hiragana basic (46)
        val hiraganaBasic = listOf(
            "あ" to "a", "い" to "i", "う" to "u", "え" to "e", "お" to "o",
            "か" to "ka", "き" to "ki", "く" to "ku", "け" to "ke", "こ" to "ko",
            "さ" to "sa", "し" to "shi", "す" to "su", "せ" to "se", "そ" to "so",
            "た" to "ta", "ち" to "chi", "つ" to "tsu", "て" to "te", "と" to "to",
            "な" to "na", "に" to "ni", "ぬ" to "nu", "ね" to "ne", "の" to "no",
            "は" to "ha", "ひ" to "hi", "ふ" to "fu", "へ" to "he", "ほ" to "ho",
            "ま" to "ma", "み" to "mi", "む" to "mu", "め" to "me", "も" to "mo",
            "や" to "ya", "ゆ" to "yu", "よ" to "yo",
            "ら" to "ra", "り" to "ri", "る" to "ru", "れ" to "re", "ろ" to "ro",
            "わ" to "wa", "を" to "wo", "ん" to "n"
        )
        val hiraganaGroups = listOf(
            "a-row","a-row","a-row","a-row","a-row",
            "ka-row","ka-row","ka-row","ka-row","ka-row",
            "sa-row","sa-row","sa-row","sa-row","sa-row",
            "ta-row","ta-row","ta-row","ta-row","ta-row",
            "na-row","na-row","na-row","na-row","na-row",
            "ha-row","ha-row","ha-row","ha-row","ha-row",
            "ma-row","ma-row","ma-row","ma-row","ma-row",
            "ya-row","ya-row","ya-row",
            "ra-row","ra-row","ra-row","ra-row","ra-row",
            "wa-row","wa-row","n-row"
        )
        val hStrokeCounts = intArrayOf(3,2,2,2,3, 3,4,1,3,2, 3,1,2,3,1, 4,2,1,1,2, 4,3,2,2,1, 3,1,4,1,4, 3,2,3,2,3, 3,2,2, 2,2,1,2,1, 2,3,1)

        hiraganaBasic.forEachIndexed { i, (lit, rom) ->
            db.execSQL("INSERT OR REPLACE INTO kana VALUES(${100001+i},'$lit','hiragana','$rom','${hiraganaGroups[i]}',${hStrokeCounts[i]},NULL,'basic',NULL)")
        }

        // Hiragana dakuten (20)
        val hDakuten = listOf("が" to "ga","ぎ" to "gi","ぐ" to "gu","げ" to "ge","ご" to "go","ざ" to "za","じ" to "ji","ず" to "zu","ぜ" to "ze","ぞ" to "zo","だ" to "da","ぢ" to "di","づ" to "du","で" to "de","ど" to "do","ば" to "ba","び" to "bi","ぶ" to "bu","べ" to "be","ぼ" to "bo")
        val hDakutenGroups = listOf("ka-row","ka-row","ka-row","ka-row","ka-row","sa-row","sa-row","sa-row","sa-row","sa-row","ta-row","ta-row","ta-row","ta-row","ta-row","ha-row","ha-row","ha-row","ha-row","ha-row")
        val hDakutenBase = intArrayOf(100006,100007,100008,100009,100010,100011,100012,100013,100014,100015,100016,100017,100018,100019,100020,100026,100027,100028,100029,100030)
        hDakuten.forEachIndexed { i, (lit, rom) ->
            db.execSQL("INSERT OR REPLACE INTO kana VALUES(${100047+i},'$lit','hiragana','$rom','${hDakutenGroups[i]}',0,NULL,'dakuten',${hDakutenBase[i]})")
        }

        // Hiragana handakuten (5)
        val hHandakuten = listOf("ぱ" to "pa","ぴ" to "pi","ぷ" to "pu","ぺ" to "pe","ぽ" to "po")
        val hHandBase = intArrayOf(100026,100027,100028,100029,100030)
        hHandakuten.forEachIndexed { i, (lit, rom) ->
            db.execSQL("INSERT OR REPLACE INTO kana VALUES(${100067+i},'$lit','hiragana','$rom','ha-row',0,NULL,'handakuten',${hHandBase[i]})")
        }

        // Hiragana combinations (33)
        val hCombo = listOf("きゃ" to "kya","きゅ" to "kyu","きょ" to "kyo","しゃ" to "sha","しゅ" to "shu","しょ" to "sho","ちゃ" to "cha","ちゅ" to "chu","ちょ" to "cho","にゃ" to "nya","にゅ" to "nyu","にょ" to "nyo","ひゃ" to "hya","ひゅ" to "hyu","ひょ" to "hyo","みゃ" to "mya","みゅ" to "myu","みょ" to "myo","りゃ" to "rya","りゅ" to "ryu","りょ" to "ryo","ぎゃ" to "gya","ぎゅ" to "gyu","ぎょ" to "gyo","じゃ" to "ja","じゅ" to "ju","じょ" to "jo","びゃ" to "bya","びゅ" to "byu","びょ" to "byo","ぴゃ" to "pya","ぴゅ" to "pyu","ぴょ" to "pyo")
        val hComboGroups = listOf("ka-row","ka-row","ka-row","sa-row","sa-row","sa-row","ta-row","ta-row","ta-row","na-row","na-row","na-row","ha-row","ha-row","ha-row","ma-row","ma-row","ma-row","ra-row","ra-row","ra-row","ka-row","ka-row","ka-row","sa-row","sa-row","sa-row","ha-row","ha-row","ha-row","ha-row","ha-row","ha-row")
        hCombo.forEachIndexed { i, (lit, rom) ->
            db.execSQL("INSERT OR REPLACE INTO kana VALUES(${100072+i},'$lit','hiragana','$rom','${hComboGroups[i]}',0,NULL,'combination',NULL)")
        }

        // Katakana basic (46)
        val katakanaBasic = listOf(
            "ア" to "a","イ" to "i","ウ" to "u","エ" to "e","オ" to "o",
            "カ" to "ka","キ" to "ki","ク" to "ku","ケ" to "ke","コ" to "ko",
            "サ" to "sa","シ" to "shi","ス" to "su","セ" to "se","ソ" to "so",
            "タ" to "ta","チ" to "chi","ツ" to "tsu","テ" to "te","ト" to "to",
            "ナ" to "na","ニ" to "ni","ヌ" to "nu","ネ" to "ne","ノ" to "no",
            "ハ" to "ha","ヒ" to "hi","フ" to "fu","ヘ" to "he","ホ" to "ho",
            "マ" to "ma","ミ" to "mi","ム" to "mu","メ" to "me","モ" to "mo",
            "ヤ" to "ya","ユ" to "yu","ヨ" to "yo",
            "ラ" to "ra","リ" to "ri","ル" to "ru","レ" to "re","ロ" to "ro",
            "ワ" to "wa","ヲ" to "wo","ン" to "n"
        )
        val katakanaGroups = hiraganaGroups // same group pattern
        val kStrokeCounts = intArrayOf(2,2,3,3,3, 2,3,2,3,2, 3,3,2,2,2, 3,3,3,3,2, 2,2,2,4,1, 2,2,1,1,4, 2,3,2,2,3, 2,2,3, 2,2,2,1,3, 2,3,2)
        katakanaBasic.forEachIndexed { i, (lit, rom) ->
            db.execSQL("INSERT OR REPLACE INTO kana VALUES(${200001+i},'$lit','katakana','$rom','${katakanaGroups[i]}',${kStrokeCounts[i]},NULL,'basic',NULL)")
        }

        // Katakana dakuten (20)
        val kDakuten = listOf("ガ" to "ga","ギ" to "gi","グ" to "gu","ゲ" to "ge","ゴ" to "go","ザ" to "za","ジ" to "ji","ズ" to "zu","ゼ" to "ze","ゾ" to "zo","ダ" to "da","ヂ" to "di","ヅ" to "du","デ" to "de","ド" to "do","バ" to "ba","ビ" to "bi","ブ" to "bu","ベ" to "be","ボ" to "bo")
        val kDakutenBase = intArrayOf(200006,200007,200008,200009,200010,200011,200012,200013,200014,200015,200016,200017,200018,200019,200020,200026,200027,200028,200029,200030)
        kDakuten.forEachIndexed { i, (lit, rom) ->
            db.execSQL("INSERT OR REPLACE INTO kana VALUES(${200047+i},'$lit','katakana','$rom','${hDakutenGroups[i]}',0,NULL,'dakuten',${kDakutenBase[i]})")
        }

        // Katakana handakuten (5)
        val kHandakuten = listOf("パ" to "pa","ピ" to "pi","プ" to "pu","ペ" to "pe","ポ" to "po")
        val kHandBase = intArrayOf(200026,200027,200028,200029,200030)
        kHandakuten.forEachIndexed { i, (lit, rom) ->
            db.execSQL("INSERT OR REPLACE INTO kana VALUES(${200067+i},'$lit','katakana','$rom','ha-row',0,NULL,'handakuten',${kHandBase[i]})")
        }

        // Katakana combinations (33)
        val kCombo = listOf("キャ" to "kya","キュ" to "kyu","キョ" to "kyo","シャ" to "sha","シュ" to "shu","ショ" to "sho","チャ" to "cha","チュ" to "chu","チョ" to "cho","ニャ" to "nya","ニュ" to "nyu","ニョ" to "nyo","ヒャ" to "hya","ヒュ" to "hyu","ヒョ" to "hyo","ミャ" to "mya","ミュ" to "myu","ミョ" to "myo","リャ" to "rya","リュ" to "ryu","リョ" to "ryo","ギャ" to "gya","ギュ" to "gyu","ギョ" to "gyo","ジャ" to "ja","ジュ" to "ju","ジョ" to "jo","ビャ" to "bya","ビュ" to "byu","ビョ" to "byo","ピャ" to "pya","ピュ" to "pyu","ピョ" to "pyo")
        kCombo.forEachIndexed { i, (lit, rom) ->
            db.execSQL("INSERT OR REPLACE INTO kana VALUES(${200072+i},'$lit','katakana','$rom','${hComboGroups[i]}',0,NULL,'combination',NULL)")
        }
    }

    private fun seedRadicalData(db: SQLiteDatabase) {
        data class R(val id: Int, val lit: String, val en: String, val jp: String, val strokes: Int, val pos: String?)
        val radicals = listOf(
            R(1,"一","one","いち",1,null),R(2,"丨","line","ぼう",1,null),R(3,"丶","dot","てん",1,null),R(4,"丿","slash","の",1,null),R(5,"乙","second","おつ",1,null),R(6,"亅","hook","はねぼう",1,null),
            R(7,"二","two","に",2,null),R(8,"亠","lid","なべぶた",2,"kanmuri"),R(9,"人","person","ひと",2,"hen"),R(10,"儿","legs","にんにょう",2,"ashi"),R(11,"入","enter","いる",2,null),R(12,"八","eight","はち",2,null),
            R(13,"冂","upside down box","けいがまえ",2,"kamae"),R(14,"冖","cover","わかんむり",2,"kanmuri"),R(15,"冫","ice","にすい",2,"hen"),R(16,"几","table","つくえ",2,null),R(17,"凵","open mouth","うけばこ",2,"kamae"),
            R(18,"刀","sword","かたな",2,"tsukuri"),R(19,"力","power","ちから",2,"tsukuri"),R(20,"勹","wrap","つつみがまえ",2,"kamae"),R(21,"匕","spoon","さじ",2,null),R(22,"匚","box","はこがまえ",2,"kamae"),
            R(23,"匸","hiding","かくしがまえ",2,"kamae"),R(24,"十","ten","じゅう",2,null),R(25,"卜","divination","ぼく",2,null),R(26,"卩","seal","ふしづくり",2,"tsukuri"),R(27,"厂","cliff","がんだれ",2,"tare"),
            R(28,"厶","private","む",2,null),R(29,"又","again","また",2,"tsukuri"),R(30,"口","mouth","くち",3,"hen"),R(31,"囗","enclosure","くにがまえ",3,"kamae"),R(32,"土","earth","つち",3,"hen"),
            R(33,"士","scholar","さむらい",3,"kanmuri"),R(34,"夂","go","ふゆがしら",3,null),R(35,"夊","go slowly","すいにょう",3,"ashi"),R(36,"夕","evening","ゆうべ",3,null),R(37,"大","big","だい",3,null),
            R(38,"女","woman","おんな",3,"hen"),R(39,"子","child","こ",3,"hen"),R(40,"宀","roof","うかんむり",3,"kanmuri"),R(41,"寸","inch","すん",3,"tsukuri"),R(42,"小","small","しょう",3,null),
            R(43,"尢","lame","まげあし",3,"ashi"),R(44,"尸","corpse","しかばね",3,"tare"),R(45,"屮","sprout","てつ",3,null),R(46,"山","mountain","やま",3,"hen"),R(47,"巛","river","まがりがわ",3,null),
            R(48,"工","work","こう",3,null),R(49,"己","oneself","おのれ",3,null),R(50,"巾","towel","はば",3,"hen"),R(51,"干","dry","ほす",3,null),R(52,"幺","short thread","いとがしら",3,null),
            R(53,"广","dotted cliff","まだれ",3,"tare"),R(54,"廴","long stride","えんにょう",3,"nyou"),R(55,"廾","two hands","にじゅうあし",3,"ashi"),R(56,"弋","shoot","しきがまえ",3,null),R(57,"弓","bow","ゆみ",3,"hen"),
            R(58,"彐","snout","けいがしら",3,null),R(59,"彡","bristle","さんづくり",3,"tsukuri"),R(60,"彳","step","ぎょうにんべん",3,"hen"),R(61,"心","heart","こころ",4,"hen"),R(62,"戈","halberd","ほこ",4,"tsukuri"),
            R(63,"戸","door","とだれ",4,"tare"),R(64,"手","hand","て",4,"hen"),R(65,"支","branch","えだ",4,null),R(66,"攴","rap","ぼくづくり",4,"tsukuri"),R(67,"文","script","ぶん",4,null),
            R(68,"斗","dipper","とます",4,"tsukuri"),R(69,"斤","axe","おのづくり",4,"tsukuri"),R(70,"方","square","ほう",4,"hen"),R(71,"无","not","なし",4,null),R(72,"日","sun","にち",4,"hen"),
            R(73,"曰","say","ひらび",4,null),R(74,"月","moon","つき",4,"hen"),R(75,"木","tree","き",4,"hen"),R(76,"欠","lack","あくび",4,"tsukuri"),R(77,"止","stop","とめる",4,"hen"),
            R(78,"歹","death","がつ",4,"hen"),R(79,"殳","weapon","ほこづくり",4,"tsukuri"),R(80,"毋","do not","なかれ",4,null),R(81,"比","compare","くらべる",4,null),R(82,"毛","fur","け",4,null),
            R(83,"氏","clan","うじ",4,null),R(84,"气","steam","きがまえ",4,"kamae"),R(85,"水","water","みず",4,"hen"),R(86,"火","fire","ひ",4,"hen"),R(87,"爪","claw","つめ",4,"kanmuri"),
            R(88,"父","father","ちち",4,null),R(89,"爻","mix","こう",4,null),R(90,"丬","split wood","しょうへん",4,"hen"),R(91,"片","slice","かた",4,"hen"),R(92,"牙","fang","きば",4,null),
            R(93,"牛","cow","うし",4,"hen"),R(94,"犬","dog","いぬ",4,"hen"),R(95,"玄","mysterious","げん",5,null),R(96,"玉","jade","たま",5,"hen"),R(97,"瓜","melon","うり",5,null),
            R(98,"瓦","tile","かわら",5,null),R(99,"甘","sweet","あまい",5,null),R(100,"生","life","うまれる",5,null),R(101,"用","use","もちいる",5,null),R(102,"田","field","た",5,"hen"),
            R(103,"疋","bolt of cloth","ひき",5,null),R(104,"疒","sickness","やまいだれ",5,"tare"),R(105,"癶","dotted tent","はつがしら",5,null),R(106,"白","white","しろ",5,null),R(107,"皮","skin","かわ",5,null),
            R(108,"皿","dish","さら",5,"ashi"),R(109,"目","eye","め",5,"hen"),R(110,"矛","spear","ほこ",5,null),R(111,"矢","arrow","や",5,"hen"),R(112,"石","stone","いし",5,"hen"),
            R(113,"示","spirit","しめす",5,"hen"),R(114,"禸","track","ぐうのあし",5,null),R(115,"禾","grain","のぎ",5,"hen"),R(116,"穴","hole","あな",5,"kanmuri"),R(117,"立","stand","たつ",5,"hen"),
            R(118,"竹","bamboo","たけ",6,"kanmuri"),R(119,"米","rice","こめ",6,"hen"),R(120,"糸","silk","いと",6,"hen"),R(121,"缶","jar","ほとぎ",6,null),R(122,"网","net","あみがしら",6,"kanmuri"),
            R(123,"羊","sheep","ひつじ",6,null),R(124,"羽","feather","はね",6,null),R(125,"老","old","おいる",6,null),R(126,"而","and","しこうして",6,null),R(127,"耒","plow","すき",6,"hen"),
            R(128,"耳","ear","みみ",6,"hen"),R(129,"聿","brush","ふで",6,null),R(130,"肉","meat","にく",6,"hen"),R(131,"臣","minister","しん",6,null),R(132,"自","self","みずから",6,null),
            R(133,"至","arrive","いたる",6,null),R(134,"臼","mortar","うす",6,null),R(135,"舌","tongue","した",6,null),R(136,"舛","oppose","まいあし",6,null),R(137,"舟","boat","ふね",6,"hen"),
            R(138,"艮","stopping","こん",6,null),R(139,"色","color","いろ",6,null),R(140,"艸","grass","くさ",6,"kanmuri"),R(141,"虍","tiger","とらがしら",6,"kanmuri"),R(142,"虫","insect","むし",6,"hen"),
            R(143,"血","blood","ち",6,null),R(144,"行","go","いく",6,"kamae"),R(145,"衣","clothes","ころも",6,"hen"),R(146,"襾","west","にし",6,"kanmuri"),R(147,"見","see","みる",7,null),
            R(148,"角","horn","つの",7,"hen"),R(149,"言","speech","ことば",7,"hen"),R(150,"谷","valley","たに",7,null),R(151,"豆","bean","まめ",7,null),R(152,"豕","pig","いのこ",7,null),
            R(153,"豸","badger","むじなへん",7,"hen"),R(154,"貝","shell","かい",7,"hen"),R(155,"赤","red","あか",7,null),R(156,"走","run","はしる",7,"nyou"),R(157,"足","foot","あし",7,"hen"),
            R(158,"身","body","み",7,"hen"),R(159,"車","car","くるま",7,"hen"),R(160,"辛","bitter","からい",7,null),R(161,"辰","morning","たつ",7,null),R(162,"辶","walk","しんにょう",7,"nyou"),
            R(163,"邑","city","むら",7,"tsukuri"),R(164,"酉","wine","とり",7,"hen"),R(165,"釆","distinguish","のごめ",7,null),R(166,"里","village","さと",7,null),R(167,"金","gold","かね",8,"hen"),
            R(168,"長","long","ながい",8,null),R(169,"門","gate","もん",8,"kamae"),R(170,"阜","mound","こざとへん",8,"hen"),R(171,"隶","slave","れいづくり",8,null),R(172,"隹","short-tailed bird","ふるとり",8,null),
            R(173,"雨","rain","あめ",8,"kanmuri"),R(174,"青","blue","あお",8,null),R(175,"非","wrong","あらず",8,null),R(176,"面","face","めん",9,null),R(177,"革","leather","かわ",9,"hen"),
            R(178,"韋","tanned leather","なめしがわ",9,null),R(179,"韭","leek","にら",9,null),R(180,"音","sound","おと",9,null),R(181,"頁","leaf","おおがい",9,"tsukuri"),R(182,"風","wind","かぜ",9,null),
            R(183,"飛","fly","とぶ",9,null),R(184,"食","eat","しょく",9,"hen"),R(185,"首","head","くび",9,null),R(186,"香","fragrant","かおり",9,null),R(187,"馬","horse","うま",10,"hen"),
            R(188,"骨","bone","ほね",10,"hen"),R(189,"高","tall","たかい",10,null),R(190,"髟","hair","かみがしら",10,"kanmuri"),R(191,"鬥","fight","たたかいがまえ",10,"kamae"),R(192,"鬯","sacrificial wine","ちょう",10,null),
            R(193,"鬲","cauldron","かなえ",10,null),R(194,"鬼","ghost","おに",10,null),R(195,"魚","fish","うお",11,"hen"),R(196,"鳥","bird","とり",11,null),R(197,"鹵","salt","しお",11,null),
            R(198,"鹿","deer","しか",11,null),R(199,"麦","wheat","むぎ",11,null),R(200,"麻","hemp","あさ",11,"tare"),R(201,"黄","yellow","き",12,null),R(202,"黍","millet","きび",12,null),
            R(203,"黒","black","くろ",12,null),R(204,"黹","embroidery","ぬいとり",12,null),R(205,"黽","frog","べんあし",13,null),R(206,"鼎","tripod","かなえ",13,null),R(207,"鼓","drum","つづみ",13,null),
            R(208,"鼠","rat","ねずみ",13,null),R(209,"鼻","nose","はな",14,null),R(210,"齊","even","せい",14,null),R(211,"歯","tooth","は",15,null),R(212,"龍","dragon","りゅう",16,null),
            R(213,"亀","turtle","かめ",16,null),R(214,"龠","flute","やく",17,null)
        )

        for (r in radicals) {
            val posVal = if (r.pos != null) "'${r.pos}'" else "NULL"
            db.execSQL("INSERT OR REPLACE INTO radical VALUES(${300000+r.id},'${r.lit}','${r.en}','${r.jp}',${r.strokes},NULL,0,'[]',$posVal)")
        }

        // Build kanji_radical junction
        db.execSQL("""
            INSERT OR IGNORE INTO kanji_radical(kanji_id, radical_id)
            SELECT k.id, r.id FROM kanji k JOIN radical r ON k.literal = r.literal
        """.trimIndent())
    }

    companion object {
        private const val DB_NAME = "kanjiquest.db"
    }
}
