package com.jworks.kanjiquest.android.ui.placement

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.jworks.kanjiquest.android.ui.theme.KanjiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacementTestScreen(
    onComplete: () -> Unit,
    viewModel: PlacementTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kanji Assessment") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Preparing assessment...")
                        }
                    }
                }
                uiState.isComplete -> {
                    ResultContent(uiState = uiState, onComplete = onComplete)
                }
                else -> {
                    QuestionContent(uiState = uiState, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun QuestionContent(
    uiState: PlacementUiState,
    viewModel: PlacementTestViewModel
) {
    val question = uiState.question ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Grade and progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Grade ${uiState.currentGrade}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${uiState.questionIndex + 1} / 5",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { (uiState.questionIndex + 1) / 5f },
            modifier = Modifier.fillMaxWidth(),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Kanji display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    KanjiText(
                        text = question.kanji.literal,
                        fontSize = 96.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "What does this kanji mean?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Answer options
        question.options.forEachIndexed { index, option ->
            val isSelected = uiState.selectedAnswer == index
            val isCorrectOption = index == question.correctIndex
            val hasAnswered = uiState.selectedAnswer != null

            val borderColor by animateColorAsState(
                targetValue = when {
                    !hasAnswered -> MaterialTheme.colorScheme.outline
                    isCorrectOption -> Color(0xFF4CAF50)
                    isSelected -> Color(0xFFF44336)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                label = "border"
            )

            val containerColor by animateColorAsState(
                targetValue = when {
                    !hasAnswered && isSelected -> MaterialTheme.colorScheme.primaryContainer
                    hasAnswered && isCorrectOption -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                    hasAnswered && isSelected -> Color(0xFFF44336).copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surface
                },
                label = "container"
            )

            OutlinedButton(
                onClick = { if (!hasAnswered) viewModel.selectAnswer(index) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, borderColor),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = containerColor
                )
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (hasAnswered && isCorrectOption) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        hasAnswered && isCorrectOption -> Color(0xFF4CAF50)
                        hasAnswered && isSelected -> Color(0xFFF44336)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Next button
        if (uiState.selectedAnswer != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.nextQuestion() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (uiState.questionIndex == 4) "Finish Grade" else "Next",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Score so far
        if (uiState.selectedAnswer != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Grade ${uiState.currentGrade}: ${uiState.gradeCorrectCount} / ${uiState.questionIndex + 1} correct",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResultContent(
    uiState: PlacementUiState,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Assessment Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Level assignment card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your Level",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Level ${uiState.assignedLevel}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = uiState.assignedTierName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )

                if (uiState.highestPassedGrade != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You passed up to Grade ${uiState.highestPassedGrade}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Grade breakdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Results by Grade",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                uiState.gradeResults.entries.sortedBy { it.key }.forEach { (grade, result) ->
                    val (correct, total) = result
                    val passed = correct >= 4
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Grade $grade",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$correct/$total",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (passed) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                            Text(
                                text = if (passed) " PASS" else " FAIL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (passed) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Start Learning!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You can retake this assessment from Settings",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
