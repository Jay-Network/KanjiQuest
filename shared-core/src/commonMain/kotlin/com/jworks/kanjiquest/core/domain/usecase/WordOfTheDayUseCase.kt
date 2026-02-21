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

        val parts = today.split("-")
        val daySeed = (parts[0].toLong() * 10000 + parts[1].toLong() * 100 + parts[2].toLong()) * 2654435761L
        val rng = Random(daySeed)

        // Use studied vocabulary so WotD is relevant to the user's progress
        val studiedVocab = kanjiRepository.getStudiedKanjiVocabulary()

        val word = if (studiedVocab.isNotEmpty()) {
            studiedVocab[rng.nextInt(studiedVocab.size)]
        } else {
            // Fall back to common vocabulary only (frequency <= 6000, roughly N5-N3)
            val commonCount = kanjiRepository.getCommonVocabularyCount()
            if (commonCount == 0L) return null
            kanjiRepository.getCommonVocabularyAtOffset(rng.nextLong(commonCount))
        }

        cachedWord = word
        cachedDate = today
        return word
    }
}
