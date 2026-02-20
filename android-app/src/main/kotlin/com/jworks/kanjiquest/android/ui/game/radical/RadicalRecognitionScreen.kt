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

val RadicalColor = Color(0xFF795548)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadicalRecognitionScreen(
    onBack: () -> Unit,
    viewModel: RadicalRecognitionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startGame()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Radical Recognition") },
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
                is GameState.AwaitingAnswer -> QuestionContent(
                    literal = state.question.kanjiLiteral,
                    questionText = state.question.questionText,
                    choices = state.question.choices,
                    questionNumber = state.questionNumber,
                    totalQuestions = state.totalQuestions,
                    currentCombo = state.currentCombo,
                    sessionXp = state.sessionXp,
                    selectedAnswer = null, correctAnswer = null,
                    onAnswerClick = { viewModel.submitAnswer(it) }
                )
                is GameState.ShowingResult -> QuestionContent(
                    literal = state.question.kanjiLiteral,
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
            Text("Preparing questions...", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(color = RadicalColor)
        }
    }
}

@Composable
private fun QuestionContent(
    literal: String,
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

        Spacer(modifier = Modifier.height(24.dp))
        Text(questionText, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.size(200.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = literal, fontSize = 80.sp, textAlign = TextAlign.Center)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isCorrect != null) {
            AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn()) {
                Text(
                    text = if (isCorrect) "+$xpGained XP" else "Incorrect",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    color = if (isCorrect) RadicalColor else MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

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
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor,
                                disabledContainerColor = buttonColor.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(text = choice, fontSize = 16.sp, color = Color.White, maxLines = 2) }
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cards Studied", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${stats.cardsStudied}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold) }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Correct", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${stats.correctCount} / ${stats.cardsStudied}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold) }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("XP Earned", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("+${stats.xpEarned}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RadicalColor),
            shape = RoundedCornerShape(12.dp)) { Text("Done", fontSize = 18.sp) }
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
