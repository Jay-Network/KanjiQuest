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
                    position TEXT,
                    priority INTEGER NOT NULL DEFAULT 2
                )
            """.trimIndent())

            // Add priority column if upgrading from old schema
            try {
                it.execSQL("ALTER TABLE radical ADD COLUMN priority INTEGER NOT NULL DEFAULT 2")
            } catch (_: Exception) {
                // Column already exists
            }

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

            // Flashcard deck groups (multi-deck support)
            it.execSQL("""
                CREATE TABLE IF NOT EXISTS flashcard_deck_group (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT (datetime('now'))
                )
            """.trimIndent())

            // Insert default deck if empty
            val deckGroupCount = it.rawQuery("SELECT COUNT(*) FROM flashcard_deck_group", null).use { c -> c.moveToFirst(); c.getLong(0) }
            if (deckGroupCount == 0L) {
                it.execSQL("INSERT INTO flashcard_deck_group(id, name) VALUES(1, 'My Flashcards')")
            }

            // Migrate flashcard_deck to support multi-deck (add deck_id column)
            val hasDeckId = try {
                it.rawQuery("SELECT deck_id FROM flashcard_deck LIMIT 1", null).use { c -> c.moveToFirst(); true }
            } catch (_: Exception) { false }

            if (!hasDeckId) {
                // Old schema: kanji_id is single PK. Migrate to composite PK (deck_id, kanji_id)
                it.execSQL("ALTER TABLE flashcard_deck RENAME TO flashcard_deck_old")
                it.execSQL("""
                    CREATE TABLE flashcard_deck (
                        deck_id INTEGER NOT NULL DEFAULT 1,
                        kanji_id INTEGER NOT NULL,
                        added_at TEXT NOT NULL DEFAULT (datetime('now')),
                        last_studied_at TEXT,
                        study_count INTEGER NOT NULL DEFAULT 0,
                        notes TEXT,
                        PRIMARY KEY (deck_id, kanji_id)
                    )
                """.trimIndent())
                it.execSQL("INSERT INTO flashcard_deck(deck_id, kanji_id, added_at, last_studied_at, study_count, notes) SELECT 1, kanji_id, added_at, last_studied_at, study_count, notes FROM flashcard_deck_old")
                it.execSQL("DROP TABLE flashcard_deck_old")
            }

            // Field Journal table (for camera capture gallery)
            it.execSQL("""
                CREATE TABLE IF NOT EXISTS field_journal (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    image_path TEXT NOT NULL,
                    location_label TEXT NOT NULL DEFAULT '',
                    kanji_found TEXT NOT NULL DEFAULT '[]',
                    kanji_count INTEGER NOT NULL DEFAULT 0,
                    captured_at INTEGER NOT NULL
                )
            """.trimIndent())

            it.execSQL("CREATE INDEX IF NOT EXISTS idx_kana_type ON kana(type)")
            it.execSQL("CREATE INDEX IF NOT EXISTS idx_kana_group ON kana(type, kana_group)")
            it.execSQL("CREATE INDEX IF NOT EXISTS idx_radical_strokes ON radical(stroke_count)")
            it.execSQL("CREATE INDEX IF NOT EXISTS idx_kanji_radical_kid ON kanji_radical(kanji_id)")
            it.execSQL("CREATE INDEX IF NOT EXISTS idx_kanji_radical_rid ON kanji_radical(radical_id)")

            // Collection table (Pokémon-style "gotta catch 'em all" system)
            it.execSQL("""
                CREATE TABLE IF NOT EXISTS collection (
                    item_id INTEGER NOT NULL,
                    item_type TEXT NOT NULL,
                    rarity TEXT NOT NULL DEFAULT 'common',
                    item_level INTEGER NOT NULL DEFAULT 1,
                    item_xp INTEGER NOT NULL DEFAULT 0,
                    discovered_at INTEGER NOT NULL,
                    source TEXT NOT NULL DEFAULT 'gameplay',
                    PRIMARY KEY (item_id, item_type)
                )
            """.trimIndent())
            it.execSQL("CREATE INDEX IF NOT EXISTS idx_collection_type ON collection(item_type)")
            it.execSQL("CREATE INDEX IF NOT EXISTS idx_collection_rarity ON collection(item_type, rarity)")

            // Seed kana/radical data if tables are empty (upgrade path)
            val kanaCount = it.rawQuery("SELECT COUNT(*) FROM kana", null).use { c -> c.moveToFirst(); c.getLong(0) }
            if (kanaCount == 0L) {
                seedKanaData(it)
            }
            val radicalCount = it.rawQuery("SELECT COUNT(*) FROM radical", null).use { c -> c.moveToFirst(); c.getLong(0) }
            if (radicalCount < 250L) {
                // Re-seed if empty or using old 214-radical Kangxi data
                // New KRADFILE-based data has ~253 components
                it.execSQL("DELETE FROM radical")
                it.execSQL("DELETE FROM kanji_radical")
                seedRadicalData(it)
            }

            // Migrate existing SRS data into collection table (upgrade path)
            migrateExistingToCollection(it)

            // Seed starter pack for new players (empty collection)
            seedStarterPack(it)
        }
    }

    /**
     * Migrate existing SRS cards into the collection table.
     * Any kanji/kana/radical with an SRS card that has been reviewed at least once
     * gets added to the collection.
     */
    private fun migrateExistingToCollection(db: SQLiteDatabase) {
        // Only migrate if collection is empty (first time running with collection system)
        val collectionCount = db.rawQuery("SELECT COUNT(*) FROM collection", null).use { c -> c.moveToFirst(); c.getLong(0) }
        if (collectionCount > 0L) return

        val now = System.currentTimeMillis() / 1000

        // Migrate kanji SRS cards (reviewed at least once)
        db.execSQL("""
            INSERT OR IGNORE INTO collection (item_id, item_type, rarity, item_level, item_xp, discovered_at, source)
            SELECT
                sc.kanji_id,
                'kanji',
                CASE
                    WHEN k.grade = 8 AND k.stroke_count >= 15 THEN 'legendary'
                    WHEN k.grade >= 6 THEN 'epic'
                    WHEN k.grade IN (4, 5) THEN 'rare'
                    WHEN k.grade IN (2, 3) THEN 'uncommon'
                    ELSE 'common'
                END,
                MIN(10, MAX(1, sc.repetitions / 3 + 1)),
                0,
                $now,
                'gameplay'
            FROM srs_card sc
            LEFT JOIN kanji k ON k.id = sc.kanji_id
            WHERE sc.total_reviews > 0
        """.trimIndent())

        // Migrate kana SRS cards
        db.execSQL("""
            INSERT OR IGNORE INTO collection (item_id, item_type, rarity, item_level, item_xp, discovered_at, source)
            SELECT
                ks.kana_id,
                CASE
                    WHEN ka.type = 'hiragana' THEN 'hiragana'
                    ELSE 'katakana'
                END,
                CASE
                    WHEN ka.variant = 'basic' THEN 'common'
                    WHEN ka.variant IN ('dakuten', 'handakuten') THEN 'uncommon'
                    WHEN ka.variant = 'combination' THEN 'rare'
                    ELSE 'common'
                END,
                MIN(10, MAX(1, ks.repetitions / 3 + 1)),
                0,
                $now,
                'gameplay'
            FROM kana_srs_card ks
            LEFT JOIN kana ka ON ka.id = ks.kana_id
            WHERE ks.total_reviews > 0
        """.trimIndent())

        // Migrate radical SRS cards
        db.execSQL("""
            INSERT OR IGNORE INTO collection (item_id, item_type, rarity, item_level, item_xp, discovered_at, source)
            SELECT
                rs.radical_id,
                'radical',
                CASE
                    WHEN r.priority = 1 THEN 'common'
                    WHEN r.priority = 2 THEN 'uncommon'
                    ELSE 'rare'
                END,
                MIN(10, MAX(1, rs.repetitions / 3 + 1)),
                0,
                $now,
                'gameplay'
            FROM radical_srs_card rs
            LEFT JOIN radical r ON r.id = rs.radical_id
            WHERE rs.total_reviews > 0
        """.trimIndent())
    }

    /**
     * Seeds a starter pack for brand new players to avoid an empty collection.
     * Only runs if the collection table is completely empty.
     */
    private fun seedStarterPack(db: SQLiteDatabase) {
        val collectionCount = db.rawQuery("SELECT COUNT(*) FROM collection", null).use { c -> c.moveToFirst(); c.getLong(0) }
        if (collectionCount > 0L) return

        val now = System.currentTimeMillis() / 1000

        // 5 basic hiragana: あいうえお (IDs 100001-100005)
        for (id in 100001..100005) {
            db.execSQL("INSERT OR IGNORE INTO collection VALUES($id, 'hiragana', 'common', 1, 0, $now, 'starter')")
        }
        // 5 basic katakana: アイウエオ (IDs 200001-200005)
        for (id in 200001..200005) {
            db.execSQL("INSERT OR IGNORE INTO collection VALUES($id, 'katakana', 'common', 1, 0, $now, 'starter')")
        }
        // 3 Grade 1 kanji: 一二三 — look up by literal
        db.execSQL("INSERT OR IGNORE INTO collection SELECT id, 'kanji', 'common', 1, 0, $now, 'starter' FROM kanji WHERE literal IN ('一', '二', '三') LIMIT 3")
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
        // KRADFILE-based 253 visual components with learner-friendly meanings.
        // Priority: 1=essential (freq>=100), 2=common (freq 20-99), 3=uncommon (<20)
        data class R(val id: Int, val lit: String, val en: String, val jp: String, val strokes: Int, val pos: String?, val pri: Int, val freq: Int)
        val radicals = listOf(
            // Priority 1 — Essential (freq >= 100, covers ~75% of kanji)
            R(1,"口","mouth","くち",3,"hen",1,1337),R(2,"一","one","いち",1,null,1,990),R(3,"｜","line","ぼう",1,null,1,646),
            R(4,"ノ","slash","の",1,null,1,626),R(5,"木","tree","き",4,"hen",1,601),R(6,"日","sun","にち",4,"hen",1,593),
            R(7,"ハ","eight","はち",2,null,1,542),R(8,"二","two","に",2,null,1,477),R(9,"土","earth","つち",3,"hen",1,462),
            R(10,"田","rice field","た",5,"hen",1,461),R(11,"亠","lid","なべぶた",2,"kanmuri",1,459),R(12,"十","ten","じゅう",2,null,1,418),
            R(13,"艾","grass top","くさかんむり",4,"kanmuri",1,405),R(14,"并","together","ならぶ",4,null,1,393),R(15,"目","eye","め",5,"hen",1,385),
            R(16,"乞","beg","こう",3,null,1,380),R(17,"汁","water drops","さんずい",3,"hen",1,364),R(18,"小","small","しょう",3,null,1,352),
            R(19,"儿","legs","にんにょう",2,"ashi",1,322),R(20,"杰","fire base","れっか",4,"ashi",1,318),R(21,"月","moon","つき",4,"hen",1,313),
            R(22,"大","big","だい",3,null,1,308),R(23,"化","change","にんべん",4,null,1,285),R(24,"丶","dot","てん",1,null,1,267),
            R(25,"幺","thread","いとがしら",3,null,1,264),R(26,"冂","border","けいがまえ",2,"kamae",1,260),R(27,"勹","wrap","つつみがまえ",2,"kamae",1,252),
            R(28,"个","counter","こ",3,null,1,246),R(29,"人","person","ひと",2,"hen",1,243),R(30,"又","again","また",2,"tsukuri",1,242),
            R(31,"厶","private","む",2,null,1,241),R(32,"冖","cover","わかんむり",2,"kanmuri",1,229),R(33,"扎","hand radical","てへん",3,"hen",1,225),
            R(34,"宀","roof","うかんむり",3,"kanmuri",1,220),R(35,"貝","shell","かい",7,"hen",1,219),R(36,"王","king","おう",4,"hen",1,218),
            R(37,"糸","thread","いと",6,"hen",1,207),R(38,"匕","spoon","さじ",2,null,1,206),R(39,"立","stand","たつ",5,"hen",1,198),
            R(40,"言","say","ことば",7,"hen",1,194),R(41,"厂","cliff","がんだれ",2,"tare",1,188),R(42,"女","woman","おんな",3,"hen",1,171),
            R(43,"止","stop","とめる",4,"hen",1,167),R(44,"亅","hook","はねぼう",1,null,1,162),R(45,"虫","insect","むし",6,"hen",1,161),
            R(46,"心","heart","こころ",4,"ashi",1,161),R(47,"尸","body","しかばね",3,"tare",1,160),R(48,"金","gold","かね",8,"hen",1,160),
            R(49,"山","mountain","やま",3,"hen",1,149),R(50,"戈","halberd","ほこ",4,"tsukuri",1,148),R(51,"込","road","しんにょう",4,"nyou",1,145),
            R(52,"米","rice","こめ",6,"hen",1,134),R(53,"寸","inch","すん",3,"tsukuri",1,133),R(54,"隹","small bird","ふるとり",8,null,1,132),
            R(55,"禾","grain","のぎ",5,"hen",1,132),R(56,"忙","heart radical","りっしんべん",3,"hen",1,132),R(57,"ヨ","katakana yo","ヨ",3,null,1,132),
            R(58,"夂","winter top","ふゆがしら",3,null,1,131),R(59,"冫","ice","にすい",2,"hen",1,127),R(60,"攵","strike","ぼくづくり",4,"tsukuri",1,125),
            R(61,"竹","bamboo","たけ",6,"kanmuri",1,124),R(62,"卜","fortune telling","ぼく",2,null,1,123),R(63,"广","house","まだれ",3,"tare",1,122),
            R(64,"刀","sword","かたな",2,"tsukuri",1,118),R(65,"買","buy","かう",12,null,1,109),R(66,"凵","open box","うけばこ",2,"kamae",1,108),
            R(67,"囗","enclosure","くにがまえ",3,"kamae",1,107),R(68,"白","white","しろ",5,null,1,106),R(69,"尚","esteem","たっとぶ",8,null,1,106),
            R(70,"刈","sword radical","りっとう",2,"tsukuri",1,106),R(71,"几","table","つくえ",2,null,1,106),R(72,"巾","cloth","はば",3,"hen",1,104),
            R(73,"火","fire","ひ",4,"hen",1,103),R(74,"廾","two hands","にじゅうあし",3,"ashi",1,103),R(75,"士","samurai","さむらい",3,"kanmuri",1,103),
            // Priority 2 — Common (freq 20-99)
            R(76,"衣","clothes","ころも",6,"hen",2,97),R(77,"工","craft","こう",3,null,2,92),R(78,"皿","dish","さら",5,"ashi",2,90),
            R(79,"力","power","ちから",2,"tsukuri",2,90),R(80,"鳥","bird","とり",11,null,2,89),R(81,"車","car","くるま",7,"hen",2,88),
            R(82,"爪","claw","つめ",4,"kanmuri",2,84),R(83,"魚","fish","うお",11,"hen",2,81),R(84,"卩","seal","ふしづくり",2,"tsukuri",2,81),
            R(85,"石","stone","いし",5,"hen",2,79),R(86,"羊","sheep","ひつじ",6,null,2,78),R(87,"彡","hair strokes","さんづくり",3,"tsukuri",2,75),
            R(88,"夕","evening","ゆうべ",3,null,2,74),R(89,"阡","hill radical","こざとへん",3,"hen",2,73),R(90,"疔","sickness","やまいだれ",5,"tare",2,73),
            R(91,"水","water","みず",4,"hen",2,73),R(92,"斤","axe","おのづくり",4,"tsukuri",2,73),R(93,"耳","ear","みみ",6,"hen",2,72),
            R(94,"方","direction","ほう",4,"hen",2,72),R(95,"弓","bow","ゆみ",3,"hen",2,72),R(96,"足","foot","あし",7,"hen",2,71),
            R(97,"子","child","こ",3,"hen",2,71),R(98,"彳","step","ぎょうにんべん",3,"hen",2,69),R(99,"頁","page","おおがい",9,"tsukuri",2,67),
            R(100,"門","gate","もん",8,"kamae",2,67),R(101,"匚","box","はこがまえ",2,"kamae",2,67),R(102,"干","dry","ほす",3,null,2,66),
            R(103,"馬","horse","うま",10,"hen",2,65),R(104,"雨","rain","あめ",8,"kanmuri",2,65),R(105,"乙","hook","おつ",1,null,2,65),
            R(106,"里","village","さと",7,null,2,61),R(107,"臼","mortar","うす",6,null,2,61),R(108,"辛","spicy","からい",7,null,2,60),
            R(109,"豆","bean","まめ",7,null,2,60),R(110,"已","already","すでに",3,null,2,59),R(111,"酉","sake jar","とり",7,"hen",2,58),
            R(112,"初","clothes radical","ころもへん",5,"hen",2,58),R(113,"犯","dog radical","けものへん",3,"hen",2,57),R(114,"艮","stubborn","こん",6,null,2,56),
            R(115,"甘","sweet","あまい",5,null,2,54),R(116,"比","compare","くらべる",4,null,2,54),R(117,"欠","yawn","あくび",4,"tsukuri",2,54),
            R(118,"矢","arrow","や",5,"hen",2,51),R(119,"牛","cow","うし",4,"hen",2,51),R(120,"豕","pig","いのこ",7,null,2,50),
            R(121,"殳","weapon","ほこづくり",4,"tsukuri",2,50),R(122,"西","west","にし",6,"kanmuri",2,49),R(123,"穴","hole","あな",5,"kanmuri",2,49),
            R(124,"食","food","しょく",9,"hen",2,47),R(125,"老","old","おいる",6,null,2,47),R(126,"邦","country","くに",7,null,2,46),
            R(127,"示","altar","しめす",5,"hen",2,46),R(128,"虍","tiger","とらがしら",6,"kanmuri",2,45),R(129,"羽","wings","はね",6,null,2,44),
            R(130,"氏","family","うじ",4,null,2,44),R(131,"疋","cloth counter","ひき",5,null,2,43),R(132,"用","use","もちいる",5,null,2,42),
            R(133,"爿","split wood","しょうへん",4,"hen",2,42),R(134,"臣","minister","しん",6,null,2,41),R(135,"勿","must not","なかれ",4,null,2,41),
            R(136,"戸","door","とだれ",4,"tare",2,38),R(137,"礼","spirit radical","しめすへん",4,"hen",2,37),R(138,"見","see","みる",7,null,2,36),
            R(139,"革","leather","かわ",9,"hen",2,35),R(140,"九","nine","きゅう",2,null,2,34),R(141,"釆","divide","のごめ",7,null,2,33),
            R(142,"音","sound","おと",9,null,2,32),R(143,"歹","bare bones","がつ",4,"hen",2,31),R(144,"巛","river","まがりがわ",3,null,2,31),
            R(145,"マ","katakana ma","マ",2,null,2,31),R(146,"舟","boat","ふね",6,"hen",2,30),R(147,"禹","legendary ruler","う",9,null,2,30),
            R(148,"犬","dog","いぬ",4,"hen",2,30),R(149,"手","hand","て",4,"hen",2,30),R(150,"長","long","ながい",8,null,2,28),
            R(151,"至","arrive","いたる",6,null,2,28),R(152,"自","self","みずから",6,null,2,28),R(153,"品","goods","しな",9,null,2,27),
            R(154,"冊","book counter","さつ",5,null,2,27),R(155,"聿","brush","ふで",6,null,2,26),R(156,"弋","arrow","しきがまえ",3,null,2,26),
            R(157,"缶","can","ほとぎ",6,null,2,25),R(158,"癶","footsteps","はつがしら",5,null,2,25),R(159,"鬼","demon","おに",10,null,2,24),
            R(160,"非","not","あらず",8,null,2,24),R(161,"矛","spear","ほこ",5,null,2,23),R(162,"皮","skin","かわ",5,null,2,23),
            R(163,"母","mother","はは",5,null,2,23),R(164,"毋","mother","なかれ",4,null,2,23),R(165,"文","writing","ぶん",4,null,2,23),
            R(166,"廴","long stride","えんにょう",3,"nyou",2,23),R(167,"谷","valley","たに",7,null,2,22),R(168,"而","moreover","しこうして",6,null,2,22),
            R(169,"父","father","ちち",4,null,2,22),R(170,"川","river","かわ",3,null,2,22),R(171,"舌","tongue","した",6,null,2,21),
            R(172,"屮","sprout","てつ",3,null,2,21),R(173,"入","enter","いる",2,null,2,21),R(174,"亡","perish","なくなる",3,null,2,21),
            R(175,"辰","dragon sign","たつ",7,null,2,20),R(176,"角","angle","つの",7,"hen",2,20),R(177,"井","well","い",4,null,2,20),
            // Priority 3 — Uncommon (freq < 20)
            R(178,"黒","black","くろ",12,null,3,19),R(179,"鹿","deer","しか",11,null,3,19),R(180,"髟","long hair","かみがしら",10,"kanmuri",3,19),
            R(181,"隶","servant","れいづくり",8,null,3,19),R(182,"行","go","いく",6,"kamae",3,19),R(183,"生","life","うまれる",5,null,3,19),
            R(184,"瓦","tile","かわら",5,null,3,19),R(185,"巴","comma pattern","ともえ",4,null,3,19),R(186,"毛","fur","け",4,null,3,18),
            R(187,"支","branch","えだ",4,null,3,18),R(188,"青","blue","あお",8,null,3,17),R(189,"豸","animal","むじなへん",7,"hen",3,17),
            R(190,"歯","tooth","は",12,null,3,17),R(191,"尢","bent leg","まげあし",3,"ashi",3,17),R(192,"骨","bone","ほね",10,"hen",3,16),
            R(193,"爻","cross pattern","こう",4,null,3,16),R(194,"高","tall","たかい",10,null,3,15),R(195,"韭","leek","にら",9,null,3,15),
            R(196,"走","run","はしる",7,"nyou",3,15),R(197,"舛","dance step","まいあし",6,null,3,15),R(198,"斗","ladle","とます",4,"tsukuri",3,15),
            R(199,"元","origin","もと",4,null,3,15),R(200,"ユ","katakana yu","ユ",2,null,3,15),R(201,"齊","uniform","せい",14,null,3,14),
            R(202,"牙","fang","きば",4,null,3,14),R(203,"五","five","ご",4,null,3,14),R(204,"世","generation","よ",5,null,3,14),
            R(205,"麻","hemp","あさ",11,"tare",3,13),R(206,"風","wind","かぜ",9,null,3,13),R(207,"身","body","み",7,"hen",3,13),
            R(208,"耒","plow","すき",6,"hen",3,13),R(209,"玄","mysterious","げん",5,null,3,13),R(210,"曰","say","ひらび",4,null,3,13),
            R(211,"免","excuse","まぬがれる",8,null,3,13),R(212,"乃","from","の",2,null,3,13),R(213,"赤","red","あか",7,null,3,12),
            R(214,"彑","pig snout","けいがしら",3,null,3,12),R(215,"黄","yellow","き",12,null,3,11),R(216,"韋","soft leather","なめしがわ",9,null,3,11),
            R(217,"瓜","melon","うり",5,null,3,11),R(218,"无","nothing","なし",4,null,3,10),R(219,"尤","special","もっとも",4,null,3,10),
            R(220,"也","also","なり",3,null,3,10),R(221,"片","one-sided","かた",4,"hen",3,9),R(222,"巨","giant","きょだい",5,null,3,9),
            R(223,"屯","barracks","たむろ",4,null,3,9),R(224,"亀","turtle","かめ",11,null,3,8),R(225,"鼠","mouse","ねずみ",13,null,3,7),
            R(226,"面","face","めん",9,null,3,7),R(227,"無","nothing","む",12,null,3,7),R(228,"滴","drops","しずく",14,null,3,7),
            R(229,"气","steam","きがまえ",4,"kamae",3,7),R(230,"奄","cover","おおう",8,null,3,7),R(231,"及","reach","およぶ",3,null,3,7),
            R(232,"久","long time","ひさしい",3,null,3,7),R(233,"黽","frog","べんあし",13,null,3,6),R(234,"鬲","tripod","かなえ",10,null,3,6),
            R(235,"鬥","fight","たたかいがまえ",10,"kamae",3,6),R(236,"血","blood","ち",6,null,3,6),R(237,"岡","hill","おか",8,null,3,6),
            R(238,"黍","millet","きび",12,null,3,5),R(239,"鹵","salt","しお",11,null,3,5),R(240,"首","neck","くび",9,null,3,5),
            R(241,"斉","equal","せい",8,null,3,5),R(242,"鼻","nose","はな",14,null,3,4),R(243,"鼓","drum","つづみ",13,null,3,4),
            R(244,"麦","wheat","むぎ",11,null,3,4),R(245,"色","color","いろ",6,null,3,4),R(246,"竜","dragon","りゅう",10,null,3,4),
            R(247,"龠","flute","やく",17,null,3,3),R(248,"黹","embroidery","ぬいとり",12,null,3,3),R(249,"香","fragrance","かおり",9,null,3,3),
            R(250,"肉","meat","にく",6,"hen",3,3),R(251,"鬯","herbs","ちょう",10,null,3,2),R(252,"飛","fly","とぶ",9,null,3,2),
            R(253,"鼎","ancient pot","かなえ",13,null,3,1)
        )

        for (r in radicals) {
            val posVal = if (r.pos != null) "'${r.pos}'" else "NULL"
            db.execSQL("INSERT OR REPLACE INTO radical VALUES(${300000+r.id},'${r.lit}','${r.en}','${r.jp}',${r.strokes},NULL,${r.freq},'[]',$posVal,${r.pri})")
        }

        // Build kanji_radical junction from literal matching
        // This is the runtime fallback — the pre-built DB from data pipeline has full KRADFILE links
        db.execSQL("""
            INSERT OR IGNORE INTO kanji_radical(kanji_id, radical_id)
            SELECT k.id, r.id FROM kanji k JOIN radical r ON k.literal = r.literal
        """.trimIndent())
    }

    companion object {
        private const val DB_NAME = "kanjiquest.db"
    }
}
