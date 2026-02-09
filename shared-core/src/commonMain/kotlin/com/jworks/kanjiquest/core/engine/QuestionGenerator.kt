package com.jworks.kanjiquest.core.engine

import com.jworks.kanjiquest.core.domain.model.ExampleSentence
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.model.LevelProgression
import com.jworks.kanjiquest.core.domain.model.SrsCard
import com.jworks.kanjiquest.core.domain.model.SrsState
import com.jworks.kanjiquest.core.domain.model.VocabQuestionType
import com.jworks.kanjiquest.core.domain.model.Vocabulary
import com.jworks.kanjiquest.core.domain.model.parseJsonStringArray
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import com.jworks.kanjiquest.core.domain.repository.SrsRepository
import com.jworks.kanjiquest.core.domain.repository.VocabSrsRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random

class QuestionGenerator(
    private val kanjiRepository: KanjiRepository,
    private val srsRepository: SrsRepository,
    private val vocabSrsRepository: VocabSrsRepository? = null,
    private val gradeMasteryProvider: GradeMasteryProvider? = null
) {
    private var questionQueue: MutableList<QueueEntry> = mutableListOf()
    private var distractorPool: List<Kanji> = emptyList()

    private var vocabQueue: MutableList<VocabQueueEntry> = mutableListOf()
    private var vocabTypeIndex: Int = 0

    data class QueueEntry(
        val kanji: Kanji,
        val isNew: Boolean,
        val srsState: String = "new"
    )

    data class VocabQueueEntry(
        val vocab: Vocabulary,
        val isNew: Boolean,
        val srsState: String = "new",
        val exampleSentence: ExampleSentence? = null
    )

    suspend fun prepareSession(questionCount: Int, currentTime: Long, playerLevel: Int = 1): Boolean {
        questionQueue.clear()
        val addedKanjiIds = mutableSetOf<Int>()

        // 1. Gather due cards first (SRS review)
        val dueCards = srsRepository.getDueCards(currentTime)
        for (card in dueCards.take(questionCount)) {
            val kanji = kanjiRepository.getKanjiById(card.kanjiId) ?: continue
            questionQueue.add(QueueEntry(kanji, isNew = false, srsState = card.state.value))
            addedKanjiIds.add(kanji.id)
        }

        // 2. Fill with learning cards (cards studied before but not yet due — re-review)
        var remaining = questionCount - questionQueue.size
        if (remaining > 0) {
            val learningCards = srsRepository.getLearningCards(remaining)
            for (card in learningCards) {
                if (card.kanjiId in addedKanjiIds) continue
                val kanji = kanjiRepository.getKanjiById(card.kanjiId) ?: continue
                questionQueue.add(QueueEntry(kanji, isNew = false, srsState = card.state.value))
                addedKanjiIds.add(kanji.id)
            }
        }

        // 3. Fill with new SRS cards (state = 'new')
        remaining = questionCount - questionQueue.size
        if (remaining > 0) {
            val newCards = srsRepository.getNewCards(remaining)
            for (card in newCards) {
                if (card.kanjiId in addedKanjiIds) continue
                val kanji = kanjiRepository.getKanjiById(card.kanjiId) ?: continue
                questionQueue.add(QueueEntry(kanji, isNew = true))
                addedKanjiIds.add(kanji.id)
            }
        }

        // 4. Introduce unseen kanji with adaptive grade mixing
        //    Grade gating: only introduce kanji from grades unlocked at the player's level
        remaining = questionCount - questionQueue.size
        if (remaining > 0) {
            val unlockedGrades = LevelProgression.getUnlockedGrades(playerLevel)
            val provider = gradeMasteryProvider

            if (unlockedGrades.size >= 2 && provider != null) {
                // Adaptive mode: check mastery of lower grades to determine new-grade mix
                val highestGrade = unlockedGrades.last()
                val lowerGrades = unlockedGrades.dropLast(1)

                // Use the highest lower grade as the "gatekeeper"
                val gatekeeperGrade = lowerGrades.last()
                val gatekeeperMastery = provider.getGradeMastery(gatekeeperGrade)
                val newGradeRatio = gatekeeperMastery.newGradeRatio

                // Split remaining slots between lower grades and highest grade
                val highestGradeSlots = (remaining * newGradeRatio).toInt()
                val lowerGradeSlots = remaining - highestGradeSlots

                // Fill lower grades first
                var lowerRemaining = lowerGradeSlots
                for (grade in lowerGrades) {
                    if (lowerRemaining <= 0) break
                    val unseen = kanjiRepository.getUnseenKanjiByGrade(grade, lowerRemaining)
                    for (kanji in unseen) {
                        if (kanji.id in addedKanjiIds) continue
                        srsRepository.ensureCardExists(kanji.id)
                        questionQueue.add(QueueEntry(kanji, isNew = true))
                        addedKanjiIds.add(kanji.id)
                        lowerRemaining--
                        if (lowerRemaining <= 0) break
                    }
                }

                // Fill highest grade slots
                var highRemaining = highestGradeSlots + lowerRemaining // carry over unused lower slots
                if (highRemaining > 0) {
                    val unseen = kanjiRepository.getUnseenKanjiByGrade(highestGrade, highRemaining)
                    for (kanji in unseen) {
                        if (kanji.id in addedKanjiIds) continue
                        srsRepository.ensureCardExists(kanji.id)
                        questionQueue.add(QueueEntry(kanji, isNew = true))
                        addedKanjiIds.add(kanji.id)
                        highRemaining--
                        if (highRemaining <= 0) break
                    }
                }
            } else {
                // Single grade or no provider: original behavior
                for (grade in unlockedGrades) {
                    if (remaining <= 0) break
                    val unseen = kanjiRepository.getUnseenKanjiByGrade(grade, remaining)
                    for (kanji in unseen) {
                        if (kanji.id in addedKanjiIds) continue
                        srsRepository.ensureCardExists(kanji.id)
                        questionQueue.add(QueueEntry(kanji, isNew = true))
                        addedKanjiIds.add(kanji.id)
                        remaining--
                        if (remaining <= 0) break
                    }
                }
            }
        }

        // 3. Build distractor pool (kanji from same grade levels)
        val grades = questionQueue.mapNotNull { it.kanji.grade }.distinct()
        val pool = mutableListOf<Kanji>()
        for (grade in grades) {
            pool.addAll(kanjiRepository.getKanjiByGrade(grade))
        }
        if (pool.size < 20) {
            pool.addAll(kanjiRepository.getKanjiByGrade(1))
        }
        distractorPool = pool.distinctBy { it.id }

        // Shuffle the queue for variety
        questionQueue.shuffle()

        return questionQueue.isNotEmpty()
    }

    fun hasNextQuestion(): Boolean = questionQueue.isNotEmpty()

    fun generateRecognitionQuestion(): Question? {
        val entry = questionQueue.removeFirstOrNull() ?: return null
        val kanji = entry.kanji

        // Correct answer: primary on-reading (or kun-reading if no on-reading)
        val correctReading = kanji.primaryOnReading.ifEmpty { kanji.primaryKunReading }
        if (correctReading.isEmpty()) return generateRecognitionQuestion() // skip kanji without readings

        // Generate 3 distractors from the pool
        val distractors = generateDistractors(kanji, correctReading, 3)

        // Combine and shuffle choices
        val choices = (distractors + correctReading).shuffled()

        return Question(
            kanjiId = kanji.id,
            kanjiLiteral = kanji.literal,
            correctAnswer = correctReading,
            choices = choices,
            questionText = "What is the reading of this kanji?",
            isNewCard = entry.isNew,
            srsState = entry.srsState
        )
    }

    fun generateWritingQuestion(): Question? {
        val entry = questionQueue.removeFirstOrNull() ?: return null
        val kanji = entry.kanji

        // Skip kanji without stroke SVG data
        if (kanji.strokeSvg.isNullOrBlank()) {
            return if (questionQueue.isNotEmpty()) generateWritingQuestion() else null
        }

        // Parse stroke SVG JSON array into individual path strings
        val strokePaths = try {
            Json.parseToJsonElement(kanji.strokeSvg).jsonArray.map { it.jsonPrimitive.content }
        } catch (_: Exception) {
            return if (questionQueue.isNotEmpty()) generateWritingQuestion() else null
        }

        if (strokePaths.isEmpty()) {
            return if (questionQueue.isNotEmpty()) generateWritingQuestion() else null
        }

        // Build prompt: "Write: meaning (reading)"
        val reading = kanji.primaryOnReading.ifEmpty { kanji.primaryKunReading }
        val prompt = buildString {
            append("Write: ")
            append(kanji.primaryMeaning)
            if (reading.isNotEmpty()) {
                append(" (")
                append(reading)
                append(")")
            }
        }

        return Question(
            kanjiId = kanji.id,
            kanjiLiteral = kanji.literal,
            correctAnswer = kanji.literal,
            choices = emptyList(),
            questionText = prompt,
            isNewCard = entry.isNew,
            strokePaths = strokePaths,
            srsState = entry.srsState
        )
    }

    private fun generateDistractors(targetKanji: Kanji, correctReading: String, count: Int): List<String> {
        val distractors = mutableSetOf<String>()

        // Prefer readings from kanji in the same grade/JLPT level
        val sameLevel = distractorPool.filter { it.id != targetKanji.id }

        for (kanji in sameLevel.shuffled()) {
            if (distractors.size >= count) break

            val reading = kanji.primaryOnReading.ifEmpty { kanji.primaryKunReading }
            if (reading.isNotEmpty() && reading != correctReading && reading !in distractors) {
                distractors.add(reading)
            }
        }

        // Fallback: generate plausible-looking readings if not enough distractors
        val hiragana = "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをん"
        while (distractors.size < count) {
            val len = correctReading.length.coerceIn(1, 4)
            val fake = buildString {
                repeat(len) { append(hiragana[Random.nextInt(hiragana.length)]) }
            }
            if (fake != correctReading && fake !in distractors) {
                distractors.add(fake)
            }
        }

        return distractors.toList()
    }

    fun remainingCount(): Int = questionQueue.size

    fun vocabRemainingCount(): Int = vocabQueue.size

    suspend fun prepareVocabSession(questionCount: Int, currentTime: Long, playerLevel: Int): Boolean {
        vocabQueue.clear()
        vocabTypeIndex = 0
        val repo = vocabSrsRepository ?: return false

        // Get vocabulary whose component kanji are all studied
        val studiedVocab = kanjiRepository.getStudiedKanjiVocabulary()
        if (studiedVocab.isEmpty()) return false

        // Get due vocab SRS cards first
        val dueCards = repo.getDueCards(currentTime)
        val dueVocabIds = dueCards.map { it.vocabId }.toSet()
        val addedIds = mutableSetOf<Long>()

        for (card in dueCards.take(questionCount)) {
            val vocab = studiedVocab.find { it.id == card.vocabId } ?: continue
            vocabQueue.add(VocabQueueEntry(vocab, isNew = false, srsState = card.state.value))
            addedIds.add(vocab.id)
        }

        // Fill remainder with truly new vocab (not yet reviewed or still in 'new' state)
        // Exclude vocab that has been reviewed and is scheduled for future review
        val remaining = questionCount - vocabQueue.size
        if (remaining > 0) {
            val notDueIds = repo.getNotDueVocabIds(currentTime)
            val candidates = studiedVocab.filter { it.id !in addedIds && it.id !in notDueIds }.take(remaining * 3)
            for (vocab in candidates) {
                if (vocabQueue.size >= questionCount) break

                repo.ensureCardExists(vocab.id)
                vocabQueue.add(VocabQueueEntry(vocab, isNew = true))
                addedIds.add(vocab.id)
            }
        }

        vocabQueue.shuffle()
        return vocabQueue.isNotEmpty()
    }

    suspend fun generateVocabularyQuestion(playerLevel: Int): Question? {
        val rawEntry = vocabQueue.removeFirstOrNull() ?: return null
        val vocab = rawEntry.vocab

        // Lazy-load example sentence if not already fetched
        val entry = if (rawEntry.exampleSentence == null) {
            val sentence = kanjiRepository.getExampleSentence(vocab.id)
            rawEntry.copy(exampleSentence = sentence)
        } else {
            rawEntry
        }

        val availableTypes = VocabQuestionType.availableForLevel(playerLevel)
        if (availableTypes.isEmpty()) return null

        val questionType = availableTypes[vocabTypeIndex % availableTypes.size]
        vocabTypeIndex++

        // Build kanji breakdown
        val kanjiIds = kanjiRepository.getKanjiIdsForVocab(vocab.id)
        val breakdown = mutableListOf<String>()
        for (kid in kanjiIds) {
            val k = kanjiRepository.getKanjiById(kid.toInt())
            if (k != null) {
                val grade = k.gradeLabel ?: ""
                breakdown.add("${k.literal} = ${k.primaryMeaning}${if (grade.isNotEmpty()) " ($grade)" else ""}")
            }
        }

        // Gather the distractor pool from the rest of the vocabQueue + current entry
        // Expand pool from DB when session queue is too small for good distractors
        val sessionPool = vocabQueue.map { it.vocab } + listOf(vocab)
        val pool = if (sessionPool.size < 6) {
            val existingIds = sessionPool.map { it.id }.toSet()
            val extra = mutableListOf<Vocabulary>()
            repeat(20) {
                val v = kanjiRepository.getRandomStudiedVocabulary()
                if (v != null && v.id !in existingIds && v.id !in extra.map { e -> e.id }) {
                    extra.add(v)
                }
            }
            sessionPool + extra
        } else sessionPool

        return when (questionType) {
            VocabQuestionType.MEANING -> buildMeaningQuestion(vocab, entry, pool, breakdown)
            VocabQuestionType.READING -> buildReadingQuestion(vocab, entry, pool, breakdown)
            VocabQuestionType.KANJI_FILL -> buildKanjiFillQuestion(vocab, entry, pool, breakdown, kanjiIds)
            VocabQuestionType.SENTENCE -> buildSentenceQuestion(vocab, entry, pool, breakdown)
        }
    }

    private fun buildMeaningQuestion(
        vocab: Vocabulary, entry: VocabQueueEntry,
        pool: List<Vocabulary>, breakdown: List<String>
    ): Question {
        val correct = vocab.primaryMeaning
        val correctLen = correct.length
        // Prefer distractors with similar word length to avoid length-based guessing
        val candidates = pool
            .filter { it.id != vocab.id && it.primaryMeaning != correct }
            .map { it.primaryMeaning }
            .distinct()
        val similarLength = candidates.filter {
            it.length in (correctLen - correctLen / 3 - 2)..(correctLen + correctLen / 3 + 2)
        }.shuffled()
        val otherLength = candidates.filter {
            it.length !in (correctLen - correctLen / 3 - 2)..(correctLen + correctLen / 3 + 2)
        }.shuffled()
        val distractors = (similarLength + otherLength).take(3).toMutableList()
        // Fallback meanings when pool is exhausted (should rarely happen with expanded pool)
        val fallbackMeanings = listOf(
            "to stand", "to eat", "mountain", "river", "person", "thing",
            "to go", "school", "to read", "flower", "fire", "water",
            "to write", "tree", "stone", "field", "to run", "to see"
        ).filter { it != correct && it !in distractors }.shuffled()
        var fallbackIdx = 0
        while (distractors.size < 3 && fallbackIdx < fallbackMeanings.size) {
            distractors.add(fallbackMeanings[fallbackIdx++])
        }
        val choices = (distractors + correct).shuffled()

        return Question(
            kanjiId = 0,
            kanjiLiteral = vocab.kanjiForm,
            correctAnswer = correct,
            choices = choices,
            questionText = "Pick the meaning",
            isNewCard = entry.isNew,
            srsState = entry.srsState,
            vocabId = vocab.id,
            vocabReading = vocab.reading,
            vocabQuestionType = "meaning",
            exampleSentenceJa = entry.exampleSentence?.japanese,
            exampleSentenceEn = entry.exampleSentence?.english,
            kanjiBreakdown = breakdown
        )
    }

    private fun buildReadingQuestion(
        vocab: Vocabulary, entry: VocabQueueEntry,
        pool: List<Vocabulary>, breakdown: List<String>
    ): Question {
        val correct = vocab.reading
        val correctLen = correct.length
        // Prefer distractors with similar character length (±1) to avoid length-based guessing
        val candidates = pool
            .filter { it.id != vocab.id && it.reading != correct }
            .map { it.reading }
            .distinct()
        val similarLength = candidates.filter { it.length in (correctLen - 1)..(correctLen + 1) }.shuffled()
        val otherLength = candidates.filter { it.length !in (correctLen - 1)..(correctLen + 1) }.shuffled()
        val distractors = (similarLength + otherLength).take(3).toMutableList()
        // Fallback readings: common real Japanese readings instead of random gibberish
        val fallbackReadings = listOf(
            "たべる", "のむ", "はしる", "よむ", "かく", "みる", "いく",
            "くる", "する", "なる", "ある", "おおきい", "ちいさい",
            "あたらしい", "ふるい", "やすい", "たかい", "はやい"
        ).filter { it != correct && it !in distractors }.shuffled()
        var fallbackIdx = 0
        while (distractors.size < 3 && fallbackIdx < fallbackReadings.size) {
            distractors.add(fallbackReadings[fallbackIdx++])
        }
        val choices = (distractors + correct).shuffled()

        return Question(
            kanjiId = 0,
            kanjiLiteral = vocab.kanjiForm,
            correctAnswer = correct,
            choices = choices,
            questionText = "Pick the reading",
            isNewCard = entry.isNew,
            srsState = entry.srsState,
            vocabId = vocab.id,
            vocabReading = vocab.reading,
            vocabQuestionType = "reading",
            exampleSentenceJa = entry.exampleSentence?.japanese,
            exampleSentenceEn = entry.exampleSentence?.english,
            kanjiBreakdown = breakdown
        )
    }

    private suspend fun buildKanjiFillQuestion(
        vocab: Vocabulary, entry: VocabQueueEntry,
        pool: List<Vocabulary>, breakdown: List<String>,
        kanjiIds: List<Long>
    ): Question {
        if (vocab.kanjiForm.length < 2 || kanjiIds.isEmpty()) {
            return buildMeaningQuestion(vocab, entry, pool, breakdown)
        }

        val firstKanjiLiteral = vocab.kanjiForm.first().toString()
        val blanked = "_" + vocab.kanjiForm.drop(1)
        val prompt = "$blanked (${vocab.reading}) ${vocab.primaryMeaning}"

        // Get distractor kanji from studied set
        val distractors = mutableListOf<String>()
        for (kid in kanjiIds.shuffled()) {
            val k = kanjiRepository.getKanjiById(kid.toInt())
            if (k != null && k.literal != firstKanjiLiteral && k.literal !in distractors) {
                distractors.add(k.literal)
            }
            if (distractors.size >= 3) break
        }
        // Fill from pool vocab first characters if needed
        for (v in pool.shuffled()) {
            if (distractors.size >= 3) break
            val ch = v.kanjiForm.first().toString()
            if (ch != firstKanjiLiteral && ch !in distractors) {
                distractors.add(ch)
            }
        }
        // Fallback kanji from common Grade 1-2 kanji
        val fallbackKanji = listOf("山", "川", "田", "林", "森", "火", "水", "土", "金", "木",
            "日", "月", "年", "人", "子", "女", "男", "大", "小", "中")
            .filter { it != firstKanjiLiteral && it !in distractors }.shuffled()
        var fIdx = 0
        while (distractors.size < 3 && fIdx < fallbackKanji.size) {
            distractors.add(fallbackKanji[fIdx++])
        }
        val choices = (distractors.take(3) + firstKanjiLiteral).shuffled()

        return Question(
            kanjiId = 0,
            kanjiLiteral = prompt,
            correctAnswer = firstKanjiLiteral,
            choices = choices,
            questionText = "Pick the missing kanji",
            isNewCard = entry.isNew,
            srsState = entry.srsState,
            vocabId = vocab.id,
            vocabReading = vocab.reading,
            vocabQuestionType = "kanji_fill",
            exampleSentenceJa = entry.exampleSentence?.japanese,
            exampleSentenceEn = entry.exampleSentence?.english,
            kanjiBreakdown = breakdown
        )
    }

    private fun buildSentenceQuestion(
        vocab: Vocabulary, entry: VocabQueueEntry,
        pool: List<Vocabulary>, breakdown: List<String>
    ): Question {
        val sentence = entry.exampleSentence
        if (sentence == null) {
            return buildMeaningQuestion(vocab, entry, pool, breakdown)
        }

        val blankedJa = sentence.japanese.replace(vocab.kanjiForm, "___")
        val prompt = "$blankedJa (${vocab.primaryMeaning})"

        val correct = vocab.kanjiForm
        val distractors = pool
            .filter { it.id != vocab.id && it.kanjiForm != correct }
            .map { it.kanjiForm }
            .distinct()
            .shuffled()
            .take(3)
            .toMutableList()
        // Fallback vocab from pool or common words
        val fallbackWords = pool
            .filter { it.id != vocab.id && it.kanjiForm != correct && it.kanjiForm !in distractors }
            .map { it.kanjiForm }.shuffled()
        var fIdx = 0
        while (distractors.size < 3 && fIdx < fallbackWords.size) {
            distractors.add(fallbackWords[fIdx++])
        }
        val choices = (distractors + correct).shuffled()

        return Question(
            kanjiId = 0,
            kanjiLiteral = prompt,
            correctAnswer = correct,
            choices = choices,
            questionText = "Pick the word for the blank",
            isNewCard = entry.isNew,
            srsState = entry.srsState,
            vocabId = vocab.id,
            vocabReading = vocab.reading,
            vocabQuestionType = "sentence",
            exampleSentenceJa = sentence.japanese,
            exampleSentenceEn = sentence.english,
            kanjiBreakdown = breakdown
        )
    }
}
