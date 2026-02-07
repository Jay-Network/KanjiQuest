package com.jworks.kanjiquest.core.engine

import com.jworks.kanjiquest.core.domain.model.ExampleSentence
import com.jworks.kanjiquest.core.domain.model.Kanji
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
    private val vocabSrsRepository: VocabSrsRepository? = null
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

    suspend fun prepareSession(questionCount: Int, currentTime: Long): Boolean {
        questionQueue.clear()

        // 1. Gather due cards first (SRS review)
        val dueCards = srsRepository.getDueCards(currentTime)
        for (card in dueCards.take(questionCount)) {
            val kanji = kanjiRepository.getKanjiById(card.kanjiId) ?: continue
            questionQueue.add(QueueEntry(kanji, isNew = false, srsState = card.state.value))
        }

        // 2. Fill remainder with new cards (Grade 1 first, then by frequency)
        val remaining = questionCount - questionQueue.size
        if (remaining > 0) {
            val newCards = srsRepository.getNewCards(remaining)
            if (newCards.isEmpty()) {
                // No SRS cards at all - introduce fresh kanji from Grade 1
                val grade1 = kanjiRepository.getKanjiByGrade(1)
                for (kanji in grade1.take(remaining)) {
                    srsRepository.ensureCardExists(kanji.id)
                    questionQueue.add(QueueEntry(kanji, isNew = true))
                }
            } else {
                for (card in newCards) {
                    val kanji = kanjiRepository.getKanjiById(card.kanjiId) ?: continue
                    questionQueue.add(QueueEntry(kanji, isNew = true))
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
            val sentence = kanjiRepository.getExampleSentence(vocab.id)
            vocabQueue.add(VocabQueueEntry(vocab, isNew = false, srsState = card.state.value, exampleSentence = sentence))
            addedIds.add(vocab.id)
        }

        // Fill remainder with new vocab (not yet in SRS or state=new)
        val remaining = questionCount - vocabQueue.size
        if (remaining > 0) {
            for (vocab in studiedVocab) {
                if (vocabQueue.size >= questionCount) break
                if (vocab.id in addedIds) continue

                val existingCard = repo.getCard(vocab.id)
                if (existingCard == null || existingCard.state == SrsState.NEW) {
                    repo.ensureCardExists(vocab.id)
                    val sentence = kanjiRepository.getExampleSentence(vocab.id)
                    vocabQueue.add(VocabQueueEntry(vocab, isNew = true, exampleSentence = sentence))
                    addedIds.add(vocab.id)
                }
            }
        }

        vocabQueue.shuffle()
        return vocabQueue.isNotEmpty()
    }

    suspend fun generateVocabularyQuestion(playerLevel: Int): Question? {
        val entry = vocabQueue.removeFirstOrNull() ?: return null
        val vocab = entry.vocab

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
        val pool = vocabQueue.map { it.vocab } + listOf(vocab)

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
        val distractors = pool
            .filter { it.id != vocab.id && it.primaryMeaning != correct }
            .map { it.primaryMeaning }
            .distinct()
            .shuffled()
            .take(3)
            .toMutableList()
        while (distractors.size < 3) {
            distractors.add("(no match ${distractors.size + 1})")
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
        val distractors = pool
            .filter { it.id != vocab.id && it.reading != correct }
            .map { it.reading }
            .distinct()
            .shuffled()
            .take(3)
            .toMutableList()
        while (distractors.size < 3) {
            val hiragana = "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをん"
            val len = correct.length.coerceIn(2, 4)
            val fake = buildString { repeat(len) { append(hiragana[Random.nextInt(hiragana.length)]) } }
            if (fake != correct && fake !in distractors) distractors.add(fake)
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
        while (distractors.size < 3) {
            distractors.add("?${distractors.size + 1}")
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
        while (distractors.size < 3) {
            distractors.add("--${distractors.size + 1}--")
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
