package com.jworks.kanjiquest.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.jworks.kanjiquest.core.domain.model.GameMode
import com.jworks.kanjiquest.core.domain.model.GradeMastery
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.model.MasteryLevel
import com.jworks.kanjiquest.core.domain.model.UserLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onKanjiClick: (Int) -> Unit,
    onGameModeClick: (GameMode) -> Unit,
    onShopClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onAchievementsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSubscriptionClick: () -> Unit = {},
    onPreviewModeClick: (GameMode) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Refresh profile data (XP, level, streak) when returning from a game session
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("KanjiQuest")
                        // Admin badge
                        if (uiState.isAdmin) {
                            Spacer(modifier = Modifier.padding(start = 8.dp))
                            Text(
                                text = uiState.effectiveLevel.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (uiState.effectiveLevel) {
                                    UserLevel.ADMIN -> Color(0xFFFF6B6B)
                                    UserLevel.PREMIUM -> Color(0xFFFFD700)
                                    UserLevel.FREE -> Color.White.copy(alpha = 0.7f)
                                },
                                modifier = Modifier
                                    .background(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Profile summary with tier info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = uiState.tierNameJp,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.tierName} - Lv.${uiState.displayLevel}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.profile.totalXp} XP",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${uiState.coinBalance.displayBalance} J Coins",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700),
                                modifier = Modifier.clickable(onClick = onShopClick)
                            )
                            if (uiState.coinBalance.needsSync) {
                                Text(
                                    text = "Pending sync...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${uiState.kanjiCount} kanji loaded",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { uiState.profile.xpProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                    )
                    if (uiState.nextTierName != null && uiState.nextTierLevel != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Next: ${uiState.nextTierName} at Lv.${uiState.nextTierLevel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Upgrade banner for free users (not admin)
            if (!uiState.isPremium && !uiState.isAdmin) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSubscriptionClick),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFD700).copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Upgrade to Premium",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB8860B)
                            )
                            Text(
                                text = "Unlock all modes, J Coins & more",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB8860B).copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = "$4.99/mo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB8860B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Shop and Progress buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onShopClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700)
                    )
                ) {
                    Text(
                        text = "J Coin Shop",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = onProgressClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "Progress",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Achievements and Settings buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAchievementsClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        text = "Achievements",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }

            // Grade Mastery Badges
            if (uiState.gradeMasteryList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Grade Mastery",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.gradeMasteryList.forEach { mastery ->
                        GradeMasteryBadge(mastery = mastery)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Word of the Day
            uiState.wordOfTheDay?.let { wotd ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGameModeClick(GameMode.VOCABULARY) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Word of the Day",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = wotd.reading,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = wotd.primaryMeaning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Text(
                            text = wotd.kanjiForm,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Game mode buttons
            Text(
                text = "Study Modes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GameModeButton(
                    label = "Recognition",
                    subtitle = "Free",
                    modifier = Modifier.weight(1f),
                    onClick = { onGameModeClick(GameMode.RECOGNITION) }
                )
                PreviewableGameModeButton(
                    mode = GameMode.WRITING,
                    label = "Writing",
                    isPremium = uiState.isPremium,
                    trialInfo = uiState.previewTrials[GameMode.WRITING],
                    modifier = Modifier.weight(1f),
                    onPremiumClick = { onGameModeClick(GameMode.WRITING) },
                    onPreviewClick = { onPreviewModeClick(GameMode.WRITING) },
                    onUpgradeClick = onSubscriptionClick
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PreviewableGameModeButton(
                    mode = GameMode.VOCABULARY,
                    label = "Vocabulary",
                    isPremium = uiState.isPremium,
                    trialInfo = uiState.previewTrials[GameMode.VOCABULARY],
                    modifier = Modifier.weight(1f),
                    onPremiumClick = { onGameModeClick(GameMode.VOCABULARY) },
                    onPreviewClick = { onPreviewModeClick(GameMode.VOCABULARY) },
                    onUpgradeClick = onSubscriptionClick
                )
                PreviewableGameModeButton(
                    mode = GameMode.CAMERA_CHALLENGE,
                    label = "Camera",
                    isPremium = uiState.isPremium,
                    trialInfo = uiState.previewTrials[GameMode.CAMERA_CHALLENGE],
                    modifier = Modifier.weight(1f),
                    onPremiumClick = { onGameModeClick(GameMode.CAMERA_CHALLENGE) },
                    onPreviewClick = { onPreviewModeClick(GameMode.CAMERA_CHALLENGE) },
                    onUpgradeClick = onSubscriptionClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Kanji grid — shows highest unlocked grade
            Text(
                text = "Grade ${uiState.highestUnlockedGrade} Kanji",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Use a Column with items instead of LazyVerticalGrid inside scrollable
            val kanjiChunks = uiState.gradeOneKanji.chunked(5)
            kanjiChunks.forEach { rowKanji ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowKanji.forEach { kanji ->
                        KanjiGridItem(
                            kanji = kanji,
                            modifier = Modifier.weight(1f),
                            onClick = { onKanjiClick(kanji.id) }
                        )
                    }
                    // Fill remaining space if row is not full
                    repeat(5 - rowKanji.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Bottom padding
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun KanjiGridItem(
    kanji: Kanji,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(64.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = kanji.literal,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GameModeButton(
    label: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(if (subtitle != null) 64.dp else 56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 14.sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * A game mode button that shows preview trial info for free users.
 * - Premium users: normal click
 * - Free users with trials remaining: "Preview (N left)" label, opens in preview mode
 * - Free users with no trials: "No trials left", redirects to subscription
 */
@Composable
private fun PreviewableGameModeButton(
    mode: GameMode,
    label: String,
    isPremium: Boolean,
    trialInfo: PreviewTrialInfo?,
    modifier: Modifier = Modifier,
    onPremiumClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    if (isPremium) {
        GameModeButton(
            label = label,
            modifier = modifier,
            onClick = onPremiumClick
        )
    } else {
        val remaining = trialInfo?.remaining ?: 0
        val hasTrials = remaining > 0

        Button(
            onClick = {
                if (hasTrials) onPreviewClick()
                else onUpgradeClick()
            },
            modifier = modifier.height(64.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (hasTrials)
                    MaterialTheme.colorScheme.secondaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    color = if (hasTrials)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Text(
                    text = if (hasTrials) "Preview ($remaining left)"
                           else "Upgrade to unlock",
                    color = if (hasTrials)
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    else
                        Color(0xFFB8860B),
                    fontSize = 10.sp,
                    fontWeight = if (!hasTrials) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun GradeMasteryBadge(mastery: GradeMastery) {
    val ringColor = when (mastery.masteryLevel) {
        MasteryLevel.BEGINNING -> Color(0xFFE57373)
        MasteryLevel.DEVELOPING -> Color(0xFFFFB74D)
        MasteryLevel.PROFICIENT -> Color(0xFF81C784)
        MasteryLevel.ADVANCED -> Color(0xFFFFD700)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        // Circular progress ring with grade number
        val progress = mastery.masteryScore.coerceIn(0f, 1f)
        val bgColor = MaterialTheme.colorScheme.surfaceVariant

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(56.dp)
        ) {
            Canvas(modifier = Modifier.size(56.dp)) {
                val strokeWidth = 6.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                // Background ring
                drawArc(
                    color = bgColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                // Progress ring
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            Text(
                text = "G${mastery.grade}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = mastery.masteryLevel.label,
            style = MaterialTheme.typography.labelSmall,
            color = ringColor,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp
        )
    }
}
