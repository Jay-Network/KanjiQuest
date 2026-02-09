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

        // Seed random with today's date for deterministic daily selection
        val daySeed = today.hashCode().toLong()
        val rng = Random(daySeed)

        val vocabCount = kanjiRepository.getVocabularyCount()
        if (vocabCount == 0L) return null

        val offset = rng.nextLong(vocabCount)
        val word = kanjiRepository.getVocabularyAtOffset(offset)
        cachedWord = word
        cachedDate = today
        return word
    }
}
