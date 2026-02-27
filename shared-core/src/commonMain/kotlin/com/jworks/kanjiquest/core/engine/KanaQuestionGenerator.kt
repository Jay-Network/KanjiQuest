package com.jworks.kanjiquest.core.engine

import com.jworks.kanjiquest.core.domain.model.Kana
import com.jworks.kanjiquest.core.domain.model.KanaType
import com.jworks.kanjiquest.core.domain.model.SrsCard
import com.jworks.kanjiquest.core.domain.model.SrsState
import com.jworks.kanjiquest.core.domain.repository.KanaRepository
import com.jworks.kanjiquest.core.domain.repository.KanaSrsRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random

class KanaQuestionGenerator(
    private val kanaRepository: KanaRepository,
    private val kanaSrsRepository: KanaSrsRepository
) {
    private var questionQueue: MutableList<KanaQueueEntry> = mutableListOf()
    private var distractorPool: List<Kana> = emptyList()
    private var lastGeneratedKana: Kana? = null

    /** Returns the kana model for the most recently generated question. */
    fun getLastKana(): Kana? = lastGeneratedKana

    data class KanaQueueEntry(
        val kana: Kana,
        val isNew: Boolean,
        val srsState: String = "new"
    )

    suspend fun prepareTargetedSession(targetKanaId: Int, questionCount: Int): Boolean {
        questionQueue.clear()
        val kana = kanaRepository.getKanaById(targetKanaId) ?: return false
        kanaSrsRepository.ensureCardExists(kana.id)
        val card = kanaSrsRepository.getCard(kana.id)
        val srsState = card?.state?.value ?: "new"
        repeat(questionCount) {
            questionQueue.add(KanaQueueEntry(kana, isNew = card == null, srsState = srsState))
        }
        distractorPool = kanaRepository.getKanaByTypeAndVariant(kana.type, "basic")
        return true
    }

    suspend fun prepareSession(questionCount: Int, currentTime: Long, kanaType: KanaType): Boolean {
        questionQueue.clear()
        val addedIds = mutableSetOf<Int>()

        // 1. Due SRS cards
        val dueCards = kanaSrsRepository.getDueCards(currentTime)
        for (card in dueCards.take(questionCount)) {
            val kana = kanaRepository.getKanaById(card.kanjiId) ?: continue
            if (kana.type != kanaType) continue
            questionQueue.add(KanaQueueEntry(kana, isNew = false, srsState = card.state.value))
            addedIds.add(kana.id)
        }

        // 2. Learning cards
        var remaining = questionCount - questionQueue.size
        if (remaining > 0) {
            val learningCards = kanaSrsRepository.getLearningCards(remaining)
            for (card in learningCards) {
                if (card.kanjiId in addedIds) continue
                val kana = kanaRepository.getKanaById(card.kanjiId) ?: continue
                if (kana.type != kanaType) continue
                questionQueue.add(KanaQueueEntry(kana, isNew = false, srsState = card.state.value))
                addedIds.add(kana.id)
            }
        }

        // 3. New SRS cards
        remaining = questionCount - questionQueue.size
        if (remaining > 0) {
            val newCards = kanaSrsRepository.getNewCards(remaining * 2)
            for (card in newCards) {
                if (questionQueue.size >= questionCount) break
                if (card.kanjiId in addedIds) continue
                val kana = kanaRepository.getKanaById(card.kanjiId) ?: continue
                if (kana.type != kanaType) continue
                questionQueue.add(KanaQueueEntry(kana, isNew = true))
                addedIds.add(kana.id)
            }
        }

        // 4. Unseen kana
        remaining = questionCount - questionQueue.size
        if (remaining > 0) {
            val unseen = kanaRepository.getUnseenKana(kanaType, remaining)
            for (kana in unseen) {
                if (kana.id in addedIds) continue
                kanaSrsRepository.ensureCardExists(kana.id)
                questionQueue.add(KanaQueueEntry(kana, isNew = true))
                addedIds.add(kana.id)
            }
        }

        // Build distractor pool from same kana type (basic variant for cleaner distractors)
        distractorPool = kanaRepository.getKanaByTypeAndVariant(kanaType, "basic")

        questionQueue.shuffle()
        return questionQueue.isNotEmpty()
    }

    fun hasNextQuestion(): Boolean = questionQueue.isNotEmpty()
    fun remainingCount(): Int = questionQueue.size

    fun generateRecognitionQuestion(): Question? {
        val entry = questionQueue.removeFirstOrNull() ?: return null
        val kana = entry.kana
        lastGeneratedKana = kana

        val correctAnswer = kana.romanization
        val distractors = generateDistractors(kana, correctAnswer, 3)
        val choices = (distractors + correctAnswer).shuffled()

        return Question(
            kanjiId = kana.id,
            kanjiLiteral = kana.literal,
            correctAnswer = correctAnswer,
            choices = choices,
            questionText = "What is the reading?",
            isNewCard = entry.isNew,
            srsState = entry.srsState
        )
    }

    fun generateWritingQuestion(): Question? {
        val entry = questionQueue.removeFirstOrNull() ?: return null
        val kana = entry.kana
        lastGeneratedKana = kana

        // Parse stroke SVG if available
        val strokePaths = if (!kana.strokeSvg.isNullOrBlank()) {
            try {
                Json.parseToJsonElement(kana.strokeSvg).jsonArray.map { it.jsonPrimitive.content }
            } catch (_: Exception) { emptyList() }
        } else emptyList()

        val prompt = "Write: ${kana.romanization}"

        return Question(
            kanjiId = kana.id,
            kanjiLiteral = kana.literal,
            correctAnswer = kana.literal,
            choices = emptyList(),
            questionText = prompt,
            isNewCard = entry.isNew,
            strokePaths = strokePaths,
            srsState = entry.srsState
        )
    }

    private fun generateDistractors(targetKana: Kana, correctRomanization: String, count: Int): List<String> {
        val distractors = mutableSetOf<String>()

        // Prefer distractors from same group first
        val sameGroup = distractorPool.filter {
            it.id != targetKana.id && it.group == targetKana.group && it.romanization != correctRomanization
        }.shuffled()
        for (kana in sameGroup) {
            if (distractors.size >= count) break
            distractors.add(kana.romanization)
        }

        // Fill from wider pool
        val otherPool = distractorPool.filter {
            it.id != targetKana.id && it.romanization != correctRomanization && it.romanization !in distractors
        }.shuffled()
        for (kana in otherPool) {
            if (distractors.size >= count) break
            distractors.add(kana.romanization)
        }

        // Fallback
        val fallback = listOf("a", "i", "u", "e", "o", "ka", "ki", "ku", "ke", "ko",
            "sa", "shi", "su", "se", "so", "ta", "chi", "tsu", "te", "to",
            "na", "ni", "nu", "ne", "no", "ha", "hi", "fu", "he", "ho",
            "ma", "mi", "mu", "me", "mo", "ya", "yu", "yo", "ra", "ri", "ru", "re", "ro", "wa", "wo", "n")
            .filter { it != correctRomanization && it !in distractors }.shuffled()
        var idx = 0
        while (distractors.size < count && idx < fallback.size) {
            distractors.add(fallback[idx++])
        }

        return distractors.toList()
    }
}
