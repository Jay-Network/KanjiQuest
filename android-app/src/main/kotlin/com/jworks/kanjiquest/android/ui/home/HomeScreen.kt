package com.jworks.kanjiquest.android.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import com.jworks.kanjiquest.android.ui.components.AssetImage
import com.jworks.kanjiquest.core.domain.model.GradeMastery
import com.jworks.kanjiquest.core.domain.model.MasteryLevel
import com.jworks.kanjiquest.core.domain.model.UserLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onKanjiClick: (Int) -> Unit = {},
    onRadicalClick: (Int) -> Unit = {},
    onWordOfDayClick: (Long) -> Unit = {},
    onShopClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSubscriptionClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onAchievementsClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {},
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
                actions = {
                    // Feedback icon
                    IconButton(onClick = onFeedbackClick) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = "Send Feedback",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    // J Coin Shop icon
                    IconButton(onClick = onShopClick) {
                        Text(
                            text = "J",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFFFFD700),
                            modifier = Modifier
                                .background(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    // Settings gear icon
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
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

            // Streak card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.profile.currentStreak}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6B35)
                        )
                        Text(
                            text = "Day Streak",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.collectedKanjiCount}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9C27B0)
                        )
                        Text(
                            text = "Collected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.flashcardDeckCount}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Decks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                        .clickable { onWordOfDayClick(wotd.id) },
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

            // Learning Path
            Text(
                text = "Learning Path",
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
                LearningPathCard(
                    title = "Hiragana",
                    subtitle = "ひらがな",
                    progress = uiState.hiraganaProgress,
                    color = Color(0xFFE91E63)
                )
                LearningPathCard(
                    title = "Katakana",
                    subtitle = "カタカナ",
                    progress = uiState.katakanaProgress,
                    color = Color(0xFF00BCD4)
                )
                LearningPathCard(
                    title = "Radicals",
                    subtitle = "部首",
                    progress = uiState.radicalProgress,
                    color = Color(0xFF795548)
                )
                uiState.gradeMasteryList.forEach { mastery ->
                    LearningPathCard(
                        title = "Grade ${mastery.grade}",
                        subtitle = "漢字",
                        progress = mastery.masteryScore,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom padding
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


@Composable
private fun LearningPathCard(
    title: String,
    subtitle: String,
    progress: Float,
    color: Color
) {
    Card(
        modifier = Modifier.size(width = 100.dp, height = 80.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = color
            )
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall,
                color = color, fontSize = 9.sp)
        }
    }
}

@Composable
private fun GradeMasteryBadge(mastery: GradeMastery) {
    val badgeAsset = when (mastery.masteryLevel) {
        MasteryLevel.BEGINNING -> "grade-beginning.png"
        MasteryLevel.DEVELOPING -> "grade-developing.png"
        MasteryLevel.PROFICIENT -> "grade-proficient.png"
        MasteryLevel.ADVANCED -> "grade-advanced.png"
    }

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
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(56.dp)
        ) {
            AssetImage(
                filename = badgeAsset,
                contentDescription = "${mastery.masteryLevel.label} badge",
                modifier = Modifier.size(56.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = "G${mastery.grade}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
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
