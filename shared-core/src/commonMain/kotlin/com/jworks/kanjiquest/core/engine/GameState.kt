package com.jworks.kanjiquest.core.engine

import com.jworks.kanjiquest.core.domain.model.CollectedItem
import com.jworks.kanjiquest.core.domain.model.GameMode
import com.jworks.kanjiquest.core.domain.model.KanaType

data class Question(
    val kanjiId: Int,
    val kanjiLiteral: String,
    val correctAnswer: String,
    val choices: List<String>,
    val questionText: String,
    val isNewCard: Boolean,
    val strokePaths: List<String> = emptyList(),
    val srsState: String = "new",
    val vocabId: Long? = null,
    val vocabReading: String? = null,
    val vocabQuestionType: String? = null,
    val exampleSentenceJa: String? = null,
    val exampleSentenceEn: String? = null,
    val kanjiBreakdown: List<String> = emptyList(),
    val radicalNameJp: String? = null,
    val kanjiGrade: Int? = null,
    val kanjiFrequency: Int? = null,
    val kanjiStrokeCount: Int = 0
)

data class SessionStats(
    val gameMode: GameMode,
    val cardsStudied: Int,
    val correctCount: Int,
    val comboMax: Int,
    val xpEarned: Int,
    val durationSec: Int,
    val touchedKanjiIds: List<Int> = emptyList(),
    val touchedVocabIds: List<Long> = emptyList()
)

sealed class GameState {
    data object Idle : GameState()

    data class Preparing(
        val gameMode: GameMode
    ) : GameState()

    data class AwaitingAnswer(
        val question: Question,
        val questionNumber: Int,
        val totalQuestions: Int,
        val currentCombo: Int,
        val sessionXp: Int
    ) : GameState()

    data class ShowingResult(
        val question: Question,
        val selectedAnswer: String,
        val isCorrect: Boolean,
        val quality: Int,
        val xpGained: Int,
        val currentCombo: Int,
        val questionNumber: Int,
        val totalQuestions: Int,
        val sessionXp: Int,
        val discoveredItem: CollectedItem? = null,
        val itemLevelUp: Boolean = false
    ) : GameState()

    data class SessionComplete(
        val stats: SessionStats
    ) : GameState()

    data class Error(
        val message: String
    ) : GameState()
}

sealed class GameEvent {
    data class StartSession(val gameMode: GameMode, val questionCount: Int = 10, val targetKanjiId: Int? = null, val kanaType: KanaType? = null) : GameEvent()
    data class SubmitAnswer(val answer: String) : GameEvent()
    data object NextQuestion : GameEvent()
    data object EndSession : GameEvent()
}
