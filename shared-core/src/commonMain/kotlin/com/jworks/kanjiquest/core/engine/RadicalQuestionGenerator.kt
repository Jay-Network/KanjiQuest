package com.jworks.kanjiquest.core.engine

import com.jworks.kanjiquest.core.domain.model.Radical
import com.jworks.kanjiquest.core.domain.model.SrsState
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import com.jworks.kanjiquest.core.domain.repository.RadicalRepository
import com.jworks.kanjiquest.core.domain.repository.RadicalSrsRepository
import kotlin.random.Random

class RadicalQuestionGenerator(
    private val radicalRepository: RadicalRepository,
    private val radicalSrsRepository: RadicalSrsRepository,
    private val kanjiRepository: KanjiRepository
) {
    private var questionQueue: MutableList<RadicalQueueEntry> = mutableListOf()
    private var distractorPool: List<Radical> = emptyList()

    data class RadicalQueueEntry(
        val radical: Radical,
        val isNew: Boolean,
        val srsState: String = "new"
    )

    /**
     * Prepares a session of radical questions.
     *
     * Priority-based selection ensures beginners see the most common, recognizable
     * components first (priority 1 = essential, covers ~75% of joyo kanji).
     * As users progress, priority 2 (common) and 3 (uncommon) are mixed in.
     *
     * @param playerLevel Used to determine which priority tiers to introduce.
     *   Levels 1-5: Priority 1 only (essential components like 口, 木, 水)
     *   Levels 6-15: Priority 1+2 (add common components)
     *   Levels 16+: All priorities (include rare components)
     */
    suspend fun prepareSession(questionCount: Int, currentTime: Long, playerLevel: Int = 1): Boolean {
        questionQueue.clear()
        val addedIds = mutableSetOf<Int>()

        // Determine max priority tier based on player level
        val maxPriority = when {
            playerLevel <= 5 -> 1   // Only essential radicals
            playerLevel <= 15 -> 2  // Essential + common
            else -> 3               // All radicals
        }

        // 1. Due SRS cards (always review regardless of priority)
        val dueCards = radicalSrsRepository.getDueCards(currentTime)
        for (card in dueCards.take(questionCount)) {
            val radical = radicalRepository.getRadicalById(card.kanjiId) ?: continue
            questionQueue.add(RadicalQueueEntry(radical, isNew = false, srsState = card.state.value))
            addedIds.add(radical.id)
        }

        // 2. Learning cards
        var remaining = questionCount - questionQueue.size
        if (remaining > 0) {
            val learningCards = radicalSrsRepository.getLearningCards(remaining)
            for (card in learningCards) {
                if (card.kanjiId in addedIds) continue
                val radical = radicalRepository.getRadicalById(card.kanjiId) ?: continue
                questionQueue.add(RadicalQueueEntry(radical, isNew = false, srsState = card.state.value))
                addedIds.add(radical.id)
            }
        }

        // 3. New SRS cards
        remaining = questionCount - questionQueue.size
        if (remaining > 0) {
            val newCards = radicalSrsRepository.getNewCards(remaining * 2)
            for (card in newCards) {
                if (questionQueue.size >= questionCount) break
                if (card.kanjiId in addedIds) continue
                val radical = radicalRepository.getRadicalById(card.kanjiId) ?: continue
                // Skip radicals beyond current priority tier for new cards
                if (radical.priority > maxPriority) continue
                questionQueue.add(RadicalQueueEntry(radical, isNew = true))
                addedIds.add(radical.id)
            }
        }

        // 4. Unseen radicals — filtered by priority tier
        remaining = questionCount - questionQueue.size
        if (remaining > 0) {
            val unseen = radicalRepository.getUnseenByPriority(maxPriority, remaining)
            for (radical in unseen) {
                if (radical.id in addedIds) continue
                radicalSrsRepository.ensureCardExists(radical.id)
                questionQueue.add(RadicalQueueEntry(radical, isNew = true))
                addedIds.add(radical.id)
            }
        }

        // Build distractor pool (all radicals for variety in choices)
        distractorPool = radicalRepository.getAllRadicals()

        questionQueue.shuffle()
        return questionQueue.isNotEmpty()
    }

    fun hasNextQuestion(): Boolean = questionQueue.isNotEmpty()
    fun remainingCount(): Int = questionQueue.size

    fun generateRecognitionQuestion(): Question? {
        val entry = questionQueue.removeFirstOrNull() ?: return null
        val radical = entry.radical

        val correctAnswer = radical.meaningEn
        val distractors = generateMeaningDistractors(radical, correctAnswer, 3)
        val choices = (distractors + correctAnswer).shuffled()

        return Question(
            kanjiId = radical.id,
            kanjiLiteral = radical.literal,
            correctAnswer = correctAnswer,
            choices = choices,
            questionText = "What does this radical mean?",
            isNewCard = entry.isNew,
            srsState = entry.srsState,
            radicalNameJp = radical.meaningJp
        )
    }

    suspend fun generateBuilderQuestion(): Question? {
        val entry = questionQueue.removeFirstOrNull() ?: return null
        val radical = entry.radical

        // Find kanji that contain this radical
        val kanjiIds = radicalRepository.getKanjiIdsForRadical(radical.id)
        if (kanjiIds.isEmpty()) {
            // Fall back to recognition for radicals without linked kanji
            questionQueue.add(0, entry)
            return generateRecognitionQuestion()
        }

        // Pick a correct kanji
        val correctKanjiId = kanjiIds.shuffled().first()
        val correctKanji = kanjiRepository.getKanjiById(correctKanjiId.toInt())
            ?: return generateRecognitionQuestion()

        // Get other radicals for this kanji to show as prompt
        val kanjiRadicals = radicalRepository.getRadicalsForKanji(correctKanjiId.toInt())
        val promptRadicals = kanjiRadicals.take(3)
        val promptText = promptRadicals.joinToString(" + ") { it.literal }

        // Get distractor kanji (kanji that only partially match the radicals)
        val distractorKanjiLiterals = mutableListOf<String>()

        // Get kanji that contain SOME but not ALL of the radicals
        val promptRadicalIds = promptRadicals.map { it.id }
        if (promptRadicalIds.size >= 2) {
            val partialMatches = radicalRepository.getKanjiContainingSomeRadicals(
                promptRadicalIds, promptRadicalIds.size, 10
            )
            for (kid in partialMatches.shuffled()) {
                if (distractorKanjiLiterals.size >= 3) break
                if (kid == correctKanjiId) continue
                val k = kanjiRepository.getKanjiById(kid.toInt())
                if (k != null && k.literal != correctKanji.literal) {
                    distractorKanjiLiterals.add(k.literal)
                }
            }
        }

        // Fill remaining distractors from same grade
        if (distractorKanjiLiterals.size < 3) {
            val grade = correctKanji.grade ?: 1
            val gradeKanji = kanjiRepository.getKanjiByGrade(grade).shuffled()
            for (k in gradeKanji) {
                if (distractorKanjiLiterals.size >= 3) break
                if (k.literal != correctKanji.literal && k.literal !in distractorKanjiLiterals) {
                    distractorKanjiLiterals.add(k.literal)
                }
            }
        }

        val choices = (distractorKanjiLiterals.take(3) + correctKanji.literal).shuffled()

        return Question(
            kanjiId = radical.id,
            kanjiLiteral = promptText,
            correctAnswer = correctKanji.literal,
            choices = choices,
            questionText = "Which kanji contains these radicals?",
            isNewCard = entry.isNew,
            srsState = entry.srsState,
            kanjiBreakdown = kanjiRadicals.map {
                val jpName = if (!it.meaningJp.isNullOrBlank()) " (${it.meaningJp})" else ""
                "${it.literal} = ${it.meaningEn}$jpName"
            },
            radicalNameJp = radical.meaningJp
        )
    }

    private fun generateMeaningDistractors(targetRadical: Radical, correctMeaning: String, count: Int): List<String> {
        val distractors = mutableSetOf<String>()

        // Prefer radicals with similar stroke count and same priority tier
        val similarStrokes = distractorPool.filter {
            it.id != targetRadical.id &&
            it.meaningEn != correctMeaning &&
            it.strokeCount in (targetRadical.strokeCount - 2)..(targetRadical.strokeCount + 2)
        }.shuffled()
        for (r in similarStrokes) {
            if (distractors.size >= count) break
            distractors.add(r.meaningEn)
        }

        // Fill from wider pool
        val remaining = distractorPool.filter {
            it.id != targetRadical.id && it.meaningEn != correctMeaning && it.meaningEn !in distractors
        }.shuffled()
        for (r in remaining) {
            if (distractors.size >= count) break
            distractors.add(r.meaningEn)
        }

        return distractors.toList()
    }
}
