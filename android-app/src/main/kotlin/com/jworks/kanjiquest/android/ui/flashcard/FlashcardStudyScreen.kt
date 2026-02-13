package com.jworks.kanjiquest.android.ui.flashcard

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardStudyScreen(
    onBack: () -> Unit,
    viewModel: FlashcardStudyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study") },
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
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Loading cards...")
                    }
                }
                uiState.cards.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No cards to study")
                    }
                }
                uiState.isComplete -> {
                    StudyCompleteContent(
                        totalStudied = uiState.totalStudied,
                        gradeResults = uiState.gradeResults,
                        onDone = onBack
                    )
                }
                else -> {
                    val card = uiState.currentCard
                    if (card != null) {
                        StudyCardContent(
                            card = card,
                            isFlipped = uiState.isFlipped,
                            progress = uiState.progress,
                            onFlip = { viewModel.flip() },
                            onGrade = { viewModel.grade(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyCardContent(
    card: StudyCard,
    isFlipped: Boolean,
    progress: String,
    onFlip: () -> Unit,
    onGrade: (StudyGrade) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress indicator
        Text(
            text = progress,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Flashcard
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clickable { if (!isFlipped) onFlip() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            AnimatedContent(targetState = isFlipped, label = "flip") { flipped ->
                if (!flipped) {
                    // Front: kanji only
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            com.jworks.kanjiquest.android.ui.theme.KanjiText(
                                text = card.kanji.literal,
                                fontSize = 96.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Tap to reveal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Back: meanings, readings, vocabulary
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        com.jworks.kanjiquest.android.ui.theme.KanjiText(
                            text = card.kanji.literal,
                            fontSize = 48.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = card.kanji.meaningsEn.joinToString(", "),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val onReading = card.kanji.primaryOnReading
                        val kunReading = card.kanji.primaryKunReading
                        if (onReading.isNotEmpty()) {
                            Text(
                                text = "On: $onReading",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (kunReading.isNotEmpty()) {
                            Text(
                                text = "Kun: $kunReading",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        if (card.vocabulary.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            card.vocabulary.forEach { vocab ->
                                Text(
                                    text = "${vocab.kanjiForm} (${vocab.reading}) - ${vocab.primaryMeaning}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Grading buttons (only visible when flipped)
        if (isFlipped) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudyGrade.entries.forEach { grade ->
                    Button(
                        onClick = { onGrade(grade) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (grade) {
                                StudyGrade.AGAIN -> Color(0xFFF44336)
                                StudyGrade.HARD -> Color(0xFFFF9800)
                                StudyGrade.GOOD -> Color(0xFF4CAF50)
                                StudyGrade.EASY -> Color(0xFF2196F3)
                            }
                        )
                    ) {
                        Text(
                            text = grade.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyCompleteContent(
    totalStudied: Int,
    gradeResults: Map<Int, Int>,
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
            text = "Study Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "$totalStudied cards studied",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val again = gradeResults.count { it.value <= 1 }
                val hard = gradeResults.count { it.value == 2 }
                val good = gradeResults.count { it.value in 3..4 }
                val easy = gradeResults.count { it.value >= 5 }

                if (again > 0) GradeRow("Again", again, Color(0xFFF44336))
                if (hard > 0) GradeRow("Hard", hard, Color(0xFFFF9800))
                if (good > 0) GradeRow("Good", good, Color(0xFF4CAF50))
                if (easy > 0) GradeRow("Easy", easy, Color(0xFF2196F3))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Done", fontSize = 16.sp)
        }
    }
}

@Composable
private fun GradeRow(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
