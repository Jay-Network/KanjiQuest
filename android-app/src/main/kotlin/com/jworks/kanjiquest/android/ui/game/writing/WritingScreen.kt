package com.jworks.kanjiquest.android.ui.game.writing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jworks.kanjiquest.core.engine.GameState
import com.jworks.kanjiquest.core.engine.SessionStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingScreen(
    onBack: () -> Unit,
    viewModel: WritingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startGame()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Writing") },
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
                .padding(padding)
        ) {
            when (val state = uiState.gameState) {
                is GameState.Idle, is GameState.Preparing -> LoadingContent()
                is GameState.AwaitingAnswer -> WritingQuestionContent(
                    question = state,
                    completedStrokes = uiState.completedStrokes,
                    activeStroke = uiState.activeStroke,
                    onCanvasSizeChanged = { viewModel.onCanvasSizeChanged(it) },
                    onDragStart = { viewModel.onDragStart(it) },
                    onDrag = { viewModel.onDrag(it) },
                    onDragEnd = { viewModel.onDragEnd() },
                    onUndo = { viewModel.undoLastStroke() },
                    onClear = { viewModel.clearStrokes() },
                    onSubmit = { viewModel.submitDrawing() }
                )
                is GameState.ShowingResult -> WritingResultContent(
                    state = state,
                    aiFeedback = uiState.aiFeedback,
                    aiLoading = uiState.aiLoading,
                    onNext = { viewModel.nextQuestion() }
                )
                is GameState.SessionComplete -> SessionCompleteContent(
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
                text = "Preparing writing questions...",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator()
        }
    }
}

@Composable
private fun WritingQuestionContent(
    question: GameState.AwaitingAnswer,
    completedStrokes: List<List<androidx.compose.ui.geometry.Offset>>,
    activeStroke: List<androidx.compose.ui.geometry.Offset>,
    onCanvasSizeChanged: (Float) -> Unit,
    onDragStart: (androidx.compose.ui.geometry.Offset) -> Unit,
    onDrag: (androidx.compose.ui.geometry.Offset) -> Unit,
    onDragEnd: () -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit
) {
    val totalRefStrokes = question.question.strokePaths.size
    val currentStrokeIndex = completedStrokes.size

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
                text = "${question.questionNumber} / ${question.totalQuestions}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (question.currentCombo > 1) {
                Text(
                    text = "${question.currentCombo}x combo",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Text(
                text = "${question.sessionXp} XP",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LinearProgressIndicator(
            progress = { question.questionNumber.toFloat() / question.totalQuestions },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(4.dp)),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Question prompt
        Text(
            text = question.question.questionText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Stroke counter
        Text(
            text = "Stroke ${(currentStrokeIndex + 1).coerceAtMost(totalRefStrokes)} / $totalRefStrokes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Drawing canvas
        DrawingCanvas(
            referenceStrokePaths = question.question.strokePaths,
            currentStrokeIndex = currentStrokeIndex,
            completedStrokes = completedStrokes,
            activeStroke = activeStroke,
            onDragStart = onDragStart,
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            srsState = question.question.srsState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .onSizeChanged { size ->
                    onCanvasSizeChanged(size.width.toFloat())
                }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Action buttons: Undo, Clear, Submit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onUndo,
                enabled = completedStrokes.isNotEmpty(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Undo")
            }
            OutlinedButton(
                onClick = onClear,
                enabled = completedStrokes.isNotEmpty(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Clear")
            }
            Button(
                onClick = onSubmit,
                enabled = completedStrokes.isNotEmpty(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Submit")
            }
        }
    }
}

@Composable
private fun WritingResultContent(
    state: GameState.ShowingResult,
    aiFeedback: HandwritingFeedback?,
    aiLoading: Boolean,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Show the correct kanji
        Card(
            modifier = Modifier.size(160.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (state.isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.question.kanjiLiteral,
                    fontSize = 80.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn()
        ) {
            Text(
                text = if (state.isCorrect) "+${state.xpGained} XP" else "Incorrect",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (state.isCorrect) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
            )
        }

        if (state.isCorrect) {
            Text(
                text = "Well drawn!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Keep practicing! The correct kanji is shown above.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Feedback Section
        if (aiLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "AI analyzing your handwriting...",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        aiFeedback?.let { feedback ->
            if (feedback.isAvailable) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AI Feedback",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = buildString {
                                    repeat(feedback.qualityRating) { append("\u2605") }
                                    repeat(5 - feedback.qualityRating) { append("\u2606") }
                                },
                                fontSize = 16.sp
                            )
                        }
                        if (feedback.overallComment.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = feedback.overallComment,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        feedback.strokeFeedback.forEach { tip ->
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "\u2022 $tip",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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

@Composable
private fun SessionCompleteContent(
    stats: SessionStats,
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
                StatRow("Cards Studied", "${stats.cardsStudied}")
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
