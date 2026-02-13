package com.jworks.kanjiquest.core.domain.usecase

import com.jworks.kanjiquest.core.domain.model.Vocabulary
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.random.Random

class WordOfTheDayUseCase(
    private val kanjiRepository: KanjiRepository
) {
    private var cachedWord: Vocabulary? = null
    private var cachedDate: String? = null

    suspend fun getWordOfTheDay(): Vocabulary? {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

        // Return cached word if same day
        if (cachedDate == today && cachedWord != null) return cachedWord

        // Use studied vocabulary so WotD is relevant to the user's progress
        val studiedVocab = kanjiRepository.getStudiedKanjiVocabulary()

        // Fall back to all vocabulary if nothing studied yet
        val word = if (studiedVocab.isNotEmpty()) {
            // Better hash: multiply date components for more spread between consecutive days
            val parts = today.split("-")
            val daySeed = (parts[0].toLong() * 10000 + parts[1].toLong() * 100 + parts[2].toLong()) * 2654435761L
            val rng = Random(daySeed)
            studiedVocab[rng.nextInt(studiedVocab.size)]
        } else {
            val vocabCount = kanjiRepository.getVocabularyCount()
            if (vocabCount == 0L) return null
            val parts = today.split("-")
            val daySeed = (parts[0].toLong() * 10000 + parts[1].toLong() * 100 + parts[2].toLong()) * 2654435761L
            val rng = Random(daySeed)
            kanjiRepository.getVocabularyAtOffset(rng.nextLong(vocabCount))
        }

        cachedWord = word
        cachedDate = today
        return word
    }
}
