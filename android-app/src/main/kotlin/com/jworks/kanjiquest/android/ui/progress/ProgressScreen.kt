package com.jworks.kanjiquest.android.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Offset as CanvasOffset
import androidx.compose.ui.geometry.Size as CanvasSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.jworks.kanjiquest.core.domain.model.GradeMastery
import com.jworks.kanjiquest.core.domain.model.MasteryLevel
import com.jworks.kanjiquest.core.domain.model.StudySession
import com.jworks.kanjiquest.core.domain.model.UserProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onBack: () -> Unit,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress & Stats") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading your stats...")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Level & XP Card
                LevelCard(
                    level = uiState.profile.level,
                    totalXp = uiState.profile.totalXp,
                    xpForNextLevel = calculateXpForNextLevel(uiState.profile.level)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Streak Card
                StreakCard(
                    currentStreak = uiState.profile.currentStreak,
                    longestStreak = uiState.profile.longestStreak,
                    dailyGoal = uiState.profile.dailyGoal
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Kanji Mastery Card
                KanjiMasteryCard(
                    masteredCount = uiState.masteredCount,
                    totalInSrs = uiState.totalKanjiInSrs
                )

                // Per-Grade Mastery Breakdown
                if (uiState.gradeMasteryList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    GradeMasteryBreakdownCard(grades = uiState.gradeMasteryList)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Overall Stats Card
                OverallStatsCard(
                    gamesPlayed = uiState.totalGamesPlayed,
                    cardsStudied = uiState.totalCardsStudied,
                    accuracy = uiState.overallAccuracy
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Recent Sessions Card
                if (uiState.recentSessions.isNotEmpty()) {
                    RecentSessionsCard(sessions = uiState.recentSessions)
                }
            }
        }
    }
}

@Composable
private fun LevelCard(level: Int, totalXp: Int, xpForNextLevel: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Level $level",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$totalXp XP",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))

            // XP Progress to next level
            val xpInCurrentLevel = totalXp - calculateTotalXpForLevel(level)
            val progress = (xpInCurrentLevel.toFloat() / xpForNextLevel.toFloat()).coerceIn(0f, 1f)

            Text(
                text = "Progress to Level ${level + 1}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$xpInCurrentLevel / $xpForNextLevel XP",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun StreakCard(currentStreak: Int, longestStreak: Int, dailyGoal: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Study Streak",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Current Streak",
                    value = "$currentStreak days",
                    emoji = "🔥"
                )
                StatItem(
                    label = "Longest Streak",
                    value = "$longestStreak days",
                    emoji = "⭐"
                )
                StatItem(
                    label = "Daily Goal",
                    value = "$dailyGoal XP",
                    emoji = "🎯"
                )
            }
        }
    }
}

@Composable
private fun KanjiMasteryCard(masteredCount: Long, totalInSrs: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Kanji Mastery",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Mastered",
                    value = "$masteredCount",
                    emoji = "✅"
                )
                StatItem(
                    label = "In Progress",
                    value = "${totalInSrs - masteredCount}",
                    emoji = "📚"
                )
                StatItem(
                    label = "Total",
                    value = "$totalInSrs",
                    emoji = "漢"
                )
            }

            if (totalInSrs > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                val masteryPercent = ((masteredCount.toFloat() / totalInSrs.toFloat()) * 100).roundToInt()
                LinearProgressIndicator(
                    progress = { masteredCount.toFloat() / totalInSrs.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$masteryPercent% Mastery Rate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun OverallStatsCard(gamesPlayed: Int, cardsStudied: Int, accuracy: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Overall Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Games Played",
                    value = "$gamesPlayed",
                    emoji = "🎮"
                )
                StatItem(
                    label = "Cards Studied",
                    value = "$cardsStudied",
                    emoji = "📝"
                )
                StatItem(
                    label = "Accuracy",
                    value = "${accuracy.roundToInt()}%",
                    emoji = "🎯"
                )
            }
        }
    }
}

@Composable
private fun RecentSessionsCard(sessions: List<StudySession>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Recent Sessions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            sessions.take(5).forEach { session ->
                SessionItem(session = session)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SessionItem(session: StudySession) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val sessionDate = dateFormat.format(Date(session.startedAt))
    val accuracy = if (session.cardsStudied > 0) {
        ((session.correctCount.toFloat() / session.cardsStudied.toFloat()) * 100).roundToInt()
    } else {
        0
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.gameMode.uppercase(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = sessionDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${session.cardsStudied} cards",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "$accuracy%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    accuracy >= 80 -> MaterialTheme.colorScheme.primary
                    accuracy >= 60 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.error
                }
            )
            Text(
                text = "+${session.xpEarned} XP",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, emoji: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = emoji,
            fontSize = 32.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GradeMasteryBreakdownCard(grades: List<GradeMastery>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Grade Mastery",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            grades.forEach { mastery ->
                GradeMasteryRow(mastery = mastery)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun GradeMasteryRow(mastery: GradeMastery) {
    val levelColor = when (mastery.masteryLevel) {
        MasteryLevel.BEGINNING -> Color(0xFFE57373)
        MasteryLevel.DEVELOPING -> Color(0xFFFFB74D)
        MasteryLevel.PROFICIENT -> Color(0xFF81C784)
        MasteryLevel.ADVANCED -> Color(0xFFFFD700)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mastery ring
        val progress = mastery.masteryScore.coerceIn(0f, 1f)
        val bgColor = MaterialTheme.colorScheme.surfaceVariant
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp)
        ) {
            Canvas(modifier = Modifier.size(40.dp)) {
                val strokeWidth = 4.dp.toPx()
                val arcSize = CanvasSize(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = CanvasOffset(strokeWidth / 2, strokeWidth / 2)
                drawArc(
                    color = bgColor,
                    startAngle = -90f, sweepAngle = 360f,
                    useCenter = false, topLeft = topLeft, size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = levelColor,
                    startAngle = -90f, sweepAngle = 360f * progress,
                    useCenter = false, topLeft = topLeft, size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            Text(
                text = "G${mastery.grade}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Stats
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Grade ${mastery.grade}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = mastery.masteryLevel.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = levelColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { mastery.coverage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = levelColor
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${mastery.studiedCount}/${mastery.totalKanjiInGrade} studied",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${mastery.masteredCount} mastered",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(mastery.averageAccuracy * 100).roundToInt()}% acc",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// XP needed to go from currentLevel to currentLevel+1
private fun calculateXpForNextLevel(currentLevel: Int): Int {
    return UserProfile.xpForLevel(currentLevel + 1) - UserProfile.xpForLevel(currentLevel)
}

// Cumulative XP threshold for reaching a given level
private fun calculateTotalXpForLevel(level: Int): Int {
    return UserProfile.xpForLevel(level)
}
