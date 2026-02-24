package com.jworks.kanjiquest.android.ui.quiz

import com.jworks.kanjiquest.core.domain.model.Kana
import com.jworks.kanjiquest.core.domain.model.KanaType
import com.jworks.kanjiquest.core.domain.model.KanaVariant
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.model.Radical
import com.jworks.kanjiquest.core.domain.repository.KanaRepository
import com.jworks.kanjiquest.core.domain.repository.KanjiRepository
import com.jworks.kanjiquest.core.domain.repository.RadicalRepository
import javax.inject.Inject

data class QuizQuestion(
    val displayCharacter: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int
)

enum class TestScope(val displayName: String, val category: String) {
    HIRAGANA("Hiragana", "Kana"),
    KATAKANA("Katakana", "Kana"),
    RADICALS("Radicals", "Radicals"),
    GRADE_1("Grade 1", "School Grade"),
    GRADE_2("Grade 2", "School Grade"),
    GRADE_3("Grade 3", "School Grade"),
    GRADE_4("Grade 4", "School Grade"),
    GRADE_5("Grade 5", "School Grade"),
    GRADE_6("Grade 6", "School Grade"),
    JLPT_N5("JLPT N5", "JLPT"),
    JLPT_N4("JLPT N4", "JLPT"),
    JLPT_N3("JLPT N3", "JLPT"),
    JLPT_N2("JLPT N2", "JLPT"),
    JLPT_N1("JLPT N1", "JLPT")
}

class QuizQuestionGenerator @Inject constructor(
    private val kanjiRepository: KanjiRepository,
    private val kanaRepository: KanaRepository,
    private val radicalRepository: RadicalRepository
) {
    suspend fun generateQuestions(scope: TestScope, count: Int = 10): List<QuizQuestion> {
        return when (scope) {
            TestScope.HIRAGANA -> generateKanaQuestions(
                kanaRepository.getKanaByType(KanaType.HIRAGANA), "What is the reading?", count
            )
            TestScope.KATAKANA -> generateKanaQuestions(
                kanaRepository.getKanaByType(KanaType.KATAKANA), "What is the reading?", count
            )
            TestScope.RADICALS -> generateRadicalQuestions(count)
            TestScope.GRADE_1, TestScope.GRADE_2, TestScope.GRADE_3,
            TestScope.GRADE_4, TestScope.GRADE_5, TestScope.GRADE_6 -> {
                val grade = scope.ordinal - TestScope.GRADE_1.ordinal + 1
                generateGradeKanjiQuestions(grade, count)
            }
            TestScope.JLPT_N5, TestScope.JLPT_N4, TestScope.JLPT_N3,
            TestScope.JLPT_N2, TestScope.JLPT_N1 -> {
                val level = 5 - (scope.ordinal - TestScope.JLPT_N5.ordinal)
                generateJlptKanjiQuestions(level, count)
            }
        }
    }

    private fun generateKanaQuestions(
        kanaList: List<Kana>, prompt: String, count: Int
    ): List<QuizQuestion> {
        if (kanaList.size < count) return emptyList()
        val basicKana = kanaList.filter { it.variant == KanaVariant.BASIC }
        val pool = if (basicKana.size >= count) basicKana else kanaList
        val selected = pool.shuffled().take(count)
        val allRomanizations = pool.map { it.romanization }.distinct()

        return selected.map { kana ->
            val correct = kana.romanization
            val distractors = allRomanizations
                .filter { it != correct }
                .shuffled()
                .take(3)
            val options = (distractors + correct).shuffled()
            QuizQuestion(
                displayCharacter = kana.literal,
                prompt = prompt,
                options = options,
                correctIndex = options.indexOf(correct)
            )
        }
    }

    private suspend fun generateRadicalQuestions(count: Int): List<QuizQuestion> {
        val radicals = radicalRepository.getAllRadicals()
        if (radicals.size < count) return emptyList()
        val selected = radicals.shuffled().take(count)
        val allMeanings = radicals.map { it.meaningEn }.distinct()

        return selected.map { radical ->
            val correct = radical.meaningEn
            val distractors = allMeanings
                .filter { it != correct }
                .shuffled()
                .take(3)
            val options = (distractors + correct).shuffled()
            QuizQuestion(
                displayCharacter = radical.literal,
                prompt = "What does this radical mean?",
                options = options,
                correctIndex = options.indexOf(correct)
            )
        }
    }

    private suspend fun generateGradeKanjiQuestions(grade: Int, count: Int): List<QuizQuestion> {
        val gradeKanji = kanjiRepository.getKanjiByGrade(grade)
        return generateKanjiMeaningQuestions(gradeKanji, count)
    }

    private suspend fun generateJlptKanjiQuestions(level: Int, count: Int): List<QuizQuestion> {
        val jlptKanji = kanjiRepository.getKanjiByJlptLevel(level)
        return generateKanjiMeaningQuestions(jlptKanji, count)
    }

    private suspend fun generateKanjiMeaningQuestions(
        kanjiList: List<Kanji>, count: Int
    ): List<QuizQuestion> {
        if (kanjiList.size < count) return emptyList()
        val selected = kanjiList.shuffled().take(count)
        // Build distractor pool from all grades for variety
        val allMeanings = buildMeaningPool()

        return selected.map { kanji ->
            val correctMeaning = kanji.meaningsEn.firstOrNull() ?: "unknown"
            val distractors = allMeanings
                .filter { it !in kanji.meaningsEn }
                .shuffled()
                .take(3)
            val options = (distractors + correctMeaning).shuffled()
            QuizQuestion(
                displayCharacter = kanji.literal,
                prompt = "What does this kanji mean?",
                options = options,
                correctIndex = options.indexOf(correctMeaning)
            )
        }
    }

    private suspend fun buildMeaningPool(): List<String> {
        val meanings = mutableListOf<String>()
        for (grade in 1..6) {
            kanjiRepository.getKanjiByGrade(grade).forEach { kanji ->
                meanings.addAll(kanji.meaningsEn)
            }
        }
        return meanings.distinct()
    }
}
