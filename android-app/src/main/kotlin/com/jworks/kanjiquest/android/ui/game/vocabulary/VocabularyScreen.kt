package com.jworks.kanjiquest.android.ui.game.vocabulary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jworks.kanjiquest.core.engine.GameState
import com.jworks.kanjiquest.core.engine.Question

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    onBack: () -> Unit,
    viewModel: VocabularyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startGame()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vocabulary") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("\u2190", fontSize = 24.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            when (val state = uiState.gameState) {
                is GameState.Idle, is GameState.Preparing -> LoadingContent()
                is GameState.AwaitingAnswer -> VocabQuestionContent(
                    question = state.question,
                    questionNumber = state.questionNumber,
                    totalQuestions = state.totalQuestions,
                    currentCombo = state.currentCombo,
                    sessionXp = state.sessionXp,
                    selectedAnswer = null,
                    correctAnswer = null,
                    onAnswerClick = { viewModel.submitAnswer(it) }
                )
                is GameState.ShowingResult -> VocabQuestionContent(
                    question = state.question,
                    questionNumber = state.questionNumber,
                    totalQuestions = state.totalQuestions,
                    currentCombo = state.currentCombo,
                    sessionXp = state.sessionXp,
                    selectedAnswer = state.selectedAnswer,
                    correctAnswer = state.question.correctAnswer,
                    onAnswerClick = {},
                    xpGained = state.xpGained,
                    isCorrect = state.isCorrect,
                    onNext = { viewModel.nextQuestion() }
                )
                is GameState.SessionComplete -> VocabSessionCompleteContent(
                    stats = state.stats,
                    sessionResult = uiState.sessionResult,
                    onDone = {
                        viewModel.reset()
                        onBack()
                    }
                )
                is GameState.Error -> ErrorContent(
                    message = state.message,
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Preparing vocabulary...",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator()
        }
    }
}

@Composable
private fun VocabQuestionContent(
    question: Question,
    questionNumber: Int,
    totalQuestions: Int,
    currentCombo: Int,
    sessionXp: Int,
    selectedAnswer: String?,
    correctAnswer: String?,
    onAnswerClick: (String) -> Unit,
    xpGained: Int = 0,
    isCorrect: Boolean? = null,
    onNext: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress bar + stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$questionNumber / $totalQuestions",
                style = MaterialTheme.typography.bodyMedium
            )
            if (currentCombo > 1) {
                Text(
                    text = "${currentCombo}x combo",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Text(
                text = "$sessionXp XP",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LinearProgressIndicator(
            progress = { questionNumber.toFloat() / totalQuestions },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(4.dp)),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Question type indicator
        Text(
            text = question.questionText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Main display card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Display depends on question type
                when (question.vocabQuestionType) {
                    "meaning" -> {
                        Text(
                            text = question.kanjiLiteral,
                            fontSize = 48.sp,
                            textAlign = TextAlign.Center
                        )
                        question.vocabReading?.let { reading ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = reading,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    "reading" -> {
                        Text(
                            text = question.kanjiLiteral,
                            fontSize = 48.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = question.choices.firstOrNull { it == question.correctAnswer }?.let {
                                // Show meaning as hint instead of reading
                                question.kanjiLiteral
                            } ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    "kanji_fill", "sentence" -> {
                        Text(
                            text = question.kanjiLiteral,
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp
                        )
                    }
                    else -> {
                        Text(
                            text = question.kanjiLiteral,
                            fontSize = 48.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // XP popup on result
        if (isCorrect != null) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn()
            ) {
                Text(
                    text = if (isCorrect) "+$xpGained XP" else "Incorrect",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 4 answer choices (2x2 grid)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (row in question.choices.chunked(2)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (choice in row) {
                        val buttonColor = when {
                            selectedAnswer == null -> MaterialTheme.colorScheme.primary
                            choice == correctAnswer -> Color(0xFF4CAF50)
                            choice == selectedAnswer -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        Button(
                            onClick = { onAnswerClick(choice) },
                            enabled = selectedAnswer == null,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonColor,
                                disabledContainerColor = buttonColor.copy(alpha = 0.8f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = choice,
                                fontSize = if (choice.length > 8) 14.sp else 18.sp,
                                color = Color.White,
                                maxLines = 2,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Kanji breakdown + example sentence (shown after answer)
        if (selectedAnswer != null) {
            Spacer(modifier = Modifier.height(16.dp))

            if (question.kanjiBreakdown.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Kanji breakdown",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        for (entry in question.kanjiBreakdown) {
                            Text(
                                text = entry,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            question.exampleSentenceJa?.let { sentenceJa ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Example",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sentenceJa,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        question.exampleSentenceEn?.let { en ->
                            Text(
                                text = en,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Next button (shown after answer)
        if (onNext != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Next", fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun VocabSessionCompleteContent(
    stats: com.jworks.kanjiquest.core.engine.SessionStats,
    sessionResult: com.jworks.kanjiquest.core.domain.usecase.SessionResult?,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Session Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatRow("Words Studied", "${stats.cardsStudied}")
                StatRow("Correct", "${stats.correctCount} / ${stats.cardsStudied}")
                StatRow("Accuracy", "${(stats.correctCount.toFloat() / stats.cardsStudied.coerceAtLeast(1) * 100).toInt()}%")
                StatRow("Best Combo", "${stats.comboMax}x")
                StatRow("XP Earned", "+${stats.xpEarned}")
                StatRow("Duration", formatDuration(stats.durationSec))

                if (sessionResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (sessionResult.coinsEarned > 0) {
                        Text(
                            text = "+${sessionResult.coinsEarned} J Coins",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                    if (sessionResult.leveledUp) {
                        Text(
                            text = "Level Up! -> Level ${sessionResult.newLevel}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (sessionResult.streakIncreased) {
                        Text(
                            text = "${sessionResult.currentStreak} day streak!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Done", fontSize = 18.sp)
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ErrorContent(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onBack) {
            Text("Go Back")
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (minutes > 0) "${minutes}m ${secs}s" else "${secs}s"
}
