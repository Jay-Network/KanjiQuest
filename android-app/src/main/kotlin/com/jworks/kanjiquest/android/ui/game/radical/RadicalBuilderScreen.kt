package com.jworks.kanjiquest.android.ui.game.radical

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.jworks.kanjiquest.core.engine.GameState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadicalBuilderScreen(
    onBack: () -> Unit,
    viewModel: RadicalBuilderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sessionLength = remember {
        context.getSharedPreferences("kanjiquest_settings", android.content.Context.MODE_PRIVATE)
            .getInt("session_length", 10)
    }

    LaunchedEffect(Unit) {
        viewModel.startGame(questionCount = sessionLength)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Radical Builder") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("\u2190", fontSize = 24.sp) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RadicalColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)
        ) {
            when (val state = uiState.gameState) {
                is GameState.Idle, is GameState.Preparing -> LoadingContent()
                is GameState.AwaitingAnswer -> BuilderQuestionContent(
                    questionText = state.question.questionText,
                    choices = state.question.choices,
                    questionNumber = state.questionNumber,
                    totalQuestions = state.totalQuestions,
                    currentCombo = state.currentCombo,
                    sessionXp = state.sessionXp,
                    selectedAnswer = null, correctAnswer = null,
                    onAnswerClick = { viewModel.submitAnswer(it) }
                )
                is GameState.ShowingResult -> BuilderQuestionContent(
                    questionText = state.question.questionText,
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
                    correctKanji = state.question.kanjiLiteral,
                    kanjiBreakdown = state.question.kanjiBreakdown,
                    onNext = { viewModel.nextQuestion() }
                )
                is GameState.SessionComplete -> SessionCompleteContent(
                    stats = state.stats, sessionResult = uiState.sessionResult,
                    onDone = { viewModel.reset(); onBack() }
                )
                is GameState.Error -> ErrorContent(message = state.message, onBack = onBack)
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Preparing radical questions...", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(color = RadicalColor)
        }
    }
}

@Composable
private fun BuilderQuestionContent(
    questionText: String,
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
    correctKanji: String? = null,
    kanjiBreakdown: List<String> = emptyList(),
    onNext: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("$questionNumber / $totalQuestions", style = MaterialTheme.typography.bodyMedium)
            if (currentCombo > 1) {
                Text("${currentCombo}x combo", style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold, color = RadicalColor)
            }
            Text("$sessionXp XP", style = MaterialTheme.typography.bodyMedium, color = RadicalColor)
        }

        LinearProgressIndicator(
            progress = { questionNumber.toFloat() / totalQuestions },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(4.dp)),
            color = RadicalColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Which kanji contains these radicals?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(16.dp))

        // Radical composition prompt card
        Card(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = questionText,
                    fontSize = 48.sp,
                    textAlign = TextAlign.Center,
                    letterSpacing = 8.sp,
                    color = RadicalColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isCorrect != null) {
            AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isCorrect) "+$xpGained XP" else "Incorrect",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        color = if (isCorrect) RadicalColor else MaterialTheme.colorScheme.error
                    )
                    if (correctKanji != null && !isCorrect) {
                        Text(
                            text = "Answer: $correctKanji",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (kanjiBreakdown.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        kanjiBreakdown.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2x2 kanji choice grid
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (row in choices.chunked(2)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (choice in row) {
                        val buttonColor = when {
                            selectedAnswer == null -> RadicalColor
                            choice == correctAnswer -> Color(0xFF4CAF50)
                            choice == selectedAnswer -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        Button(
                            onClick = { onAnswerClick(choice) },
                            enabled = selectedAnswer == null,
                            modifier = Modifier.weight(1f).height(72.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor,
                                disabledContainerColor = buttonColor.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(text = choice, fontSize = 32.sp, color = Color.White) }
                    }
                }
            }
        }

        if (onNext != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RadicalColor),
                shape = RoundedCornerShape(12.dp)) { Text("Next", fontSize = 18.sp) }
        }
    }
}

@Composable
private fun SessionCompleteContent(
    stats: com.jworks.kanjiquest.core.engine.SessionStats,
    sessionResult: com.jworks.kanjiquest.core.domain.usecase.SessionResult?,
    onDone: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Session Complete!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatRow("Cards Studied", "${stats.cardsStudied}")
                StatRow("Correct", "${stats.correctCount} / ${stats.cardsStudied}")
                StatRow("Accuracy", "${(stats.correctCount.toFloat() / stats.cardsStudied.coerceAtLeast(1) * 100).toInt()}%")
                StatRow("Best Combo", "${stats.comboMax}x")
                StatRow("XP Earned", "+${stats.xpEarned}")
                if (sessionResult != null && sessionResult.leveledUp) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Level Up! -> Level ${sessionResult.newLevel}",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RadicalColor)
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RadicalColor),
            shape = RoundedCornerShape(12.dp)) { Text("Done", fontSize = 18.sp) }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ErrorContent(message: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onBack) { Text("Go Back") }
    }
}
