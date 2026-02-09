package com.jworks.kanjiquest.core.domain.usecase

import com.jworks.kanjiquest.core.domain.model.Vocabulary
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import kotlin.random.Random

class WordOfTheDayUseCase(
    private val kanjiRepository: KanjiRepository
) {
    suspend fun getWordOfTheDay(): Vocabulary? {
        val vocabCount = kanjiRepository.getVocabularyCount()
        if (vocabCount == 0L) return null

        val offset = Random.nextLong(vocabCount)
        return kanjiRepository.getVocabularyAtOffset(offset)
    }
}
