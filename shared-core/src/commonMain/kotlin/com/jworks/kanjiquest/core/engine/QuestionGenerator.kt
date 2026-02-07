package com.jworks.kanjiquest.core.engine

import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.model.SrsCard
import com.jworks.kanjiquest.core.domain.model.SrsState
import com.jworks.kanjiquest.core.domain.model.parseJsonStringArray
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import com.jworks.kanjiquest.core.domain.repository.SrsRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random

class QuestionGenerator(
    private val kanjiRepository: KanjiRepository,
    private val srsRepository: SrsRepository
) {
    private var questionQueue: MutableList<QueueEntry> = mutableListOf()
    private var distractorPool: List<Kanji> = emptyList()

    data class QueueEntry(
        val kanji: Kanji,
        val isNew: Boolean,
        val srsState: String = "new"
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
}
