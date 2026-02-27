package com.jworks.kanjiquest.android.ui.game

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jworks.kanjiquest.android.ui.components.XpPopup
import com.jworks.kanjiquest.core.domain.model.Vocabulary
import com.jworks.kanjiquest.core.engine.DiscoveredKanjiInfo
import com.jworks.kanjiquest.core.engine.GameState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecognitionScreen(
    onBack: () -> Unit,
    targetKanjiId: Int? = null,
    viewModel: RecognitionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDiscoveryOverlay by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val sessionLength = remember {
        context.getSharedPreferences("kanjiquest_settings", android.content.Context.MODE_PRIVATE)
            .getInt("session_length", 10)
    }

    LaunchedEffect(targetKanjiId) {
        if (targetKanjiId != null && uiState.gameState is GameState.Idle) {
            viewModel.startGame(questionCount = 5, targetKanjiId = targetKanjiId)
        } else if (targetKanjiId == null) {
            viewModel.startGame(questionCount = sessionLength)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recognition") },
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
                is GameState.AwaitingAnswer -> {
                    showDiscoveryOverlay = false
                    QuestionContent(
                        kanjiLiteral = state.question.kanjiLiteral,
                        choices = state.question.choices,
                        questionNumber = state.questionNumber,
                        totalQuestions = state.totalQuestions,
                        currentCombo = state.currentCombo,
                        sessionXp = state.sessionXp,
                        selectedAnswer = null,
                        correctAnswer = null,
                        onAnswerClick = { viewModel.submitAnswer(it) },
                        isNewCard = state.question.isNewCard
                    )
                }
                is GameState.ShowingResult -> {
                    // Trigger discovery overlay if a new item was found
                    LaunchedEffect(state.discoveredItems) {
                        if (state.discoveredItems.isNotEmpty()) {
                            showDiscoveryOverlay = true
                        }
                    }
                    QuestionContent(
                        kanjiLiteral = state.question.kanjiLiteral,
                        choices = state.question.choices,
                        questionNumber = state.questionNumber,
                        totalQuestions = state.totalQuestions,
                        currentCombo = state.currentCombo,
                        sessionXp = state.sessionXp,
                        selectedAnswer = state.selectedAnswer,
                        correctAnswer = state.question.correctAnswer,
                        onAnswerClick = {},
                        xpGained = state.xpGained,
                        isCorrect = state.isCorrect,
                        onNext = { viewModel.nextQuestion() },
                        isNewCard = state.question.isNewCard,
                        kunReadings = state.question.kunReadings,
                        onReadings = state.question.onReadings,
                        exampleWords = state.question.exampleWords,
                        kanjiMeaning = state.question.kanjiMeaning
                    )
                    // Discovery overlay
                    val discovered = state.discoveredItems.firstOrNull()
                    if (showDiscoveryOverlay && discovered != null) {
                        DiscoveryOverlay(
                            discoveredItem = discovered,
                            kanjiLiteral = state.question.kanjiLiteral,
                            kanjiMeaning = state.question.questionText.removePrefix("What is the reading of this kanji?").takeIf { it.isNotEmpty() }
                                ?: state.question.kanjiLiteral,
                            onDismiss = { showDiscoveryOverlay = false }
                        )
                    }
                }
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
                text = "Preparing questions...",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator()
        }
    }
}

@Composable
private fun QuestionContent(
    kanjiLiteral: String,
    choices: List<String>,
    questionNumber: Int,
    totalQuestions: Int,
    currentCombo: Int,
    sessionXp: Int,
    selectedAnswer: String?,
    correctAnswer: String?,
    onAnswerClick: (String) -> Unit,
    xpGained: Int = 0,
    isCorrect: Boolean? = null,
    onNext: (() -> Unit)? = null,
    isNewCard: Boolean = false,
    kunReadings: List<String> = emptyList(),
    onReadings: List<String> = emptyList(),
    exampleWords: List<Vocabulary> = emptyList(),
    kanjiMeaning: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
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

        Spacer(modifier = Modifier.height(24.dp))

        // Question prompt
        Text(
            text = "What is the reading?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Large kanji display with NEW badge + XP popup overlay
        Box {
            Card(
                modifier = Modifier.size(200.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    com.jworks.kanjiquest.android.ui.theme.KanjiText(
                        text = kanjiLiteral,
                        fontSize = 96.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            // NEW badge
            if (isNewCard) {
                Text(
                    text = "NEW",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(
                            color = Color(0xFF00BFA5),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            // XP popup overlay
            XpPopup(
                isCorrect = isCorrect,
                xpGained = xpGained,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Result details
        if (isCorrect != null) {

            // Meaning
            if (kanjiMeaning != null) {
                Text(
                    text = kanjiMeaning,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Kun'yomi & On'yomi readings
            if (kunReadings.isNotEmpty()) {
                Text(
                    text = "訓: ${kunReadings.joinToString("、")}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (onReadings.isNotEmpty()) {
                Text(
                    text = "音: ${onReadings.joinToString("、")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Example words
            if (exampleWords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                exampleWords.forEach { vocab ->
                    Text(
                        text = "${vocab.kanjiForm} (${vocab.reading}) ${vocab.primaryMeaning}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 4 answer choices (2x2 grid)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (row in choices.chunked(2)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (choice in row) {
                        val buttonColor = when {
                            selectedAnswer == null -> MaterialTheme.colorScheme.primary
                            choice == correctAnswer -> Color(0xFF4CAF50) // green
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
                                fontSize = 20.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Next button (shown after answer)
        if (onNext != null) {
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
}

@Composable
private fun SessionCompleteContent(
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
                    sessionResult.adaptiveMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // New Discoveries section
        if (stats.newlyCollectedKanji.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF00BFA5).copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "New Discoveries",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00BFA5)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        stats.newlyCollectedKanji.forEach { discovered ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                com.jworks.kanjiquest.android.ui.theme.KanjiText(
                                    text = discovered.literal,
                                    fontSize = 36.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = discovered.meaning,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
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
