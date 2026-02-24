package com.jworks.kanjiquest.android.ui.quiz

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

private val TestBlue = Color(0xFF2196F3)
private val TestBlueDark = Color(0xFF1976D2)
private val CorrectGreen = Color(0xFF4CAF50)
private val IncorrectRed = Color(0xFFF44336)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestModeScreen(
    onBack: () -> Unit,
    viewModel: TestModeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Mode") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.phase == TestPhase.SCOPE_SELECTION) {
                            onBack()
                        } else {
                            viewModel.resetToScopeSelection()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TestBlue,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        when (uiState.phase) {
            TestPhase.SCOPE_SELECTION -> ScopeSelectionContent(
                modifier = Modifier.padding(padding),
                isLoading = uiState.isLoading,
                onScopeSelected = { viewModel.selectScope(it) }
            )
            TestPhase.IN_PROGRESS -> QuizContent(
                modifier = Modifier.padding(padding),
                uiState = uiState,
                onAnswer = { viewModel.selectAnswer(it) },
                onNext = { viewModel.nextQuestion() }
            )
            TestPhase.RESULTS -> ResultsContent(
                modifier = Modifier.padding(padding),
                uiState = uiState,
                onTestAgain = { viewModel.retakeTest() },
                onDone = { onBack() }
            )
        }
    }
}

@Composable
private fun ScopeSelectionContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    onScopeSelected: (TestScope) -> Unit
) {
    val categories = TestScope.entries.groupBy { it.category }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Choose what to test",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Pure assessment - no XP or level changes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        categories.forEach { (category, scopes) ->
            Text(
                text = category,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TestBlue
            )
            Spacer(modifier = Modifier.height(8.dp))

            scopes.forEach { scope ->
                Card(
                    onClick = { if (!isLoading) onScopeSelected(scope) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = scope.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "10 Q",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun QuizContent(
    modifier: Modifier = Modifier,
    uiState: TestModeUiState,
    onAnswer: (Int) -> Unit,
    onNext: () -> Unit
) {
    val question = uiState.currentQuestion ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress bar
        LinearProgressIndicator(
            progress = { (uiState.currentIndex + 1).toFloat() / uiState.totalQuestions },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = TestBlue,
            trackColor = TestBlue.copy(alpha = 0.2f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Question counter and timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${uiState.currentIndex + 1} / ${uiState.totalQuestions}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatTime(uiState.elapsedSeconds),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Character display
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(TestBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = question.displayCharacter,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = TestBlue
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = question.prompt,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Answer options
        question.options.forEachIndexed { index, option ->
            val answered = uiState.selectedAnswer != null
            val isSelected = uiState.selectedAnswer == index
            val isCorrectOption = index == question.correctIndex

            val bgColor by animateColorAsState(
                targetValue = when {
                    !answered -> MaterialTheme.colorScheme.surface
                    isCorrectOption -> CorrectGreen.copy(alpha = 0.15f)
                    isSelected -> IncorrectRed.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surface
                },
                label = "optionBg"
            )

            val borderColor = when {
                !answered -> TestBlue.copy(alpha = 0.3f)
                isCorrectOption -> CorrectGreen
                isSelected -> IncorrectRed
                else -> Color.Transparent
            }

            OutlinedButton(
                onClick = { if (!answered) onAnswer(index) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = bgColor
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (answered && (isCorrectOption || isSelected)) 2.dp else 1.dp,
                    color = borderColor
                )
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        answered && isCorrectOption -> CorrectGreen
                        answered && isSelected -> IncorrectRed
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Next button (visible after answering)
        if (uiState.selectedAnswer != null) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TestBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (uiState.currentIndex + 1 >= uiState.totalQuestions) "See Results" else "Next",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ResultsContent(
    modifier: Modifier = Modifier,
    uiState: TestModeUiState,
    onTestAgain: () -> Unit,
    onDone: () -> Unit
) {
    val accuracyPct = (uiState.accuracy * 100).toInt()
    val resultColor = when {
        accuracyPct >= 80 -> CorrectGreen
        accuracyPct >= 50 -> Color(0xFFFFA726)
        else -> IncorrectRed
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = uiState.selectedScope?.displayName ?: "Test",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Results",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Score circle
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(resultColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${uiState.correctCount}/${uiState.totalQuestions}",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = resultColor
                )
                Text(
                    text = "$accuracyPct%",
                    fontSize = 18.sp,
                    color = resultColor
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats row
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Correct", "${uiState.correctCount}")
                StatItem("Wrong", "${uiState.totalQuestions - uiState.correctCount}")
                StatItem("Time", formatTime(uiState.elapsedSeconds))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No XP or level changes were applied",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Action buttons
        Button(
            onClick = onTestAgain,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TestBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Test Again", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Done", fontSize = 16.sp)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}
