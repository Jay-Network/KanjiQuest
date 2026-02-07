package com.jworks.kanjiquest.core.domain.usecase

import com.jworks.kanjiquest.core.domain.model.Vocabulary
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class WordOfTheDayUseCase(
    private val kanjiRepository: KanjiRepository
) {
    suspend fun getWordOfTheDay(): Vocabulary? {
        val vocabCount = kanjiRepository.getVocabularyCount()
        if (vocabCount == 0L) return null

        val dayOfYear = Clock.System.todayIn(TimeZone.currentSystemDefault()).dayOfYear
        val offset = (dayOfYear.toLong() % vocabCount)
        return kanjiRepository.getVocabularyAtOffset(offset)
    }
}
