package com.jworks.kanjiquest.android.ui.game.vocabulary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import com.jworks.kanjiquest.android.ui.game.DiscoveryOverlay
import com.jworks.kanjiquest.core.engine.GameState
import com.jworks.kanjiquest.core.engine.Question

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    onBack: () -> Unit,
    targetKanjiId: Int? = null,
    viewModel: VocabularyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // Track sequential discovery overlay: which index in the list we're currently showing
    var discoveryQueueIndex by remember { mutableStateOf(0) }
    var lastDiscoveryQuestionNumber by remember { mutableStateOf(-1) }
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
                is GameState.AwaitingAnswer -> {
                    VocabQuestionContent(
                        question = state.question,
                        questionNumber = state.questionNumber,
                        totalQuestions = state.totalQuestions,
                        currentCombo = state.currentCombo,
                        sessionXp = state.sessionXp,
                        selectedAnswer = null,
                        correctAnswer = null,
                        onAnswerClick = { viewModel.submitAnswer(it) }
                    )
                }
                is GameState.ShowingResult -> {
                    // Reset queue index when a new question's result appears
                    if (state.questionNumber != lastDiscoveryQuestionNumber) {
                        discoveryQueueIndex = 0
                        lastDiscoveryQuestionNumber = state.questionNumber
                    }
                    val currentDiscovery = state.discoveredItems.getOrNull(discoveryQueueIndex)
                    VocabQuestionContent(
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
                        onNext = {
                            discoveryQueueIndex = state.discoveredItems.size // skip remaining
                            viewModel.nextQuestion()
                        }
                    )
                    if (currentDiscovery != null) {
                        // Find this kanji's literal from the breakdown (format: "学 = study (Grade 1)")
                        val breakdownEntry = state.question.kanjiBreakdown.firstOrNull { entry ->
                            val literal = entry.substringBefore("=").trim()
                            literal.length == 1 && literal[0].code == currentDiscovery.itemId
                        }
                        val kanjiLiteral = breakdownEntry?.substringBefore("=")?.trim()
                            ?: String(intArrayOf(currentDiscovery.itemId), 0, 1)
                        val kanjiMeaning = breakdownEntry
                            ?.substringAfter("=", "")?.trim()
                            ?.substringBefore("(")?.trim()
                        DiscoveryOverlay(
                            discoveredItem = currentDiscovery,
                            kanjiLiteral = kanjiLiteral,
                            kanjiMeaning = kanjiMeaning,
                            onDismiss = { discoveryQueueIndex++ }
                        )
                    }
                }
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
                VocabularyDecompositionCard(breakdown = question.kanjiBreakdown)
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

/**
 * Visual decomposition card that splits a vocabulary word into component kanji boxes.
 * Each box shows the kanji character and its meaning, connected by "+" symbols.
 * Entries are parsed from the kanjiBreakdown format: "学 = study", "校 = school"
 */
@Composable
private fun VocabularyDecompositionCard(breakdown: List<String>) {
    val components = breakdown.map { entry ->
        val parts = entry.split("=", limit = 2).map { it.trim() }
        val kanji = parts.getOrElse(0) { "?" }
        val meaning = parts.getOrElse(1) { "" }
        kanji to meaning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Composition",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                components.forEachIndexed { index, (kanji, meaning) ->
                    // Kanji component box
                    Box(contentAlignment = Alignment.Center) {
                        Card(
                            modifier = Modifier.size(width = 80.dp, height = 90.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                com.jworks.kanjiquest.android.ui.theme.KanjiText(
                                    text = kanji,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = meaning,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                        // Known indicator badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(20.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "\u2713",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // "+" connector between components
                    if (index < components.size - 1) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
