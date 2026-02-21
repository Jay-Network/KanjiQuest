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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.layout.ContentScale
import com.jworks.kanjiquest.android.ui.components.AssetImage
import com.jworks.kanjiquest.android.ui.components.RadicalImage
import androidx.compose.foundation.border
import com.jworks.kanjiquest.core.domain.model.CollectedItem
import com.jworks.kanjiquest.core.domain.model.GameMode
import com.jworks.kanjiquest.core.domain.model.GradeMastery
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.model.KanaType
import com.jworks.kanjiquest.core.domain.model.MasteryLevel
import com.jworks.kanjiquest.core.domain.model.Radical
import com.jworks.kanjiquest.core.domain.model.UserLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onKanjiClick: (Int) -> Unit,
    onRadicalClick: (Int) -> Unit = {},
    onGameModeClick: (GameMode) -> Unit,
    onWordOfDayClick: (Long) -> Unit = {},
    onShopClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onAchievementsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSubscriptionClick: () -> Unit = {},
    onPreviewModeClick: (GameMode) -> Unit = {},
    onFlashcardsClick: () -> Unit = {},
    onKanaModeClick: (KanaType, Boolean) -> Unit = { _, _ -> },
    onCollectionClick: () -> Unit = {},
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

            // Progress and Achievements buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

            // Kana modes (always free)
            Text(
                text = "Kana Practice",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GameModeButton(
                    label = "Hiragana",
                    subtitle = "Recognition",
                    modifier = Modifier.weight(1f),
                    modeColor = Color(0xFFE91E63),
                    imageAsset = "mode-kana-recognition.png",
                    onClick = { onKanaModeClick(KanaType.HIRAGANA, false) }
                )
                GameModeButton(
                    label = "Katakana",
                    subtitle = "Recognition",
                    modifier = Modifier.weight(1f),
                    modeColor = Color(0xFF00BCD4),
                    imageAsset = "mode-kana-writing.png",
                    onClick = { onKanaModeClick(KanaType.KATAKANA, false) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Radical modes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GameModeButton(
                    label = "Radicals",
                    subtitle = "Free",
                    modifier = Modifier.weight(1f),
                    modeColor = Color(0xFF795548),
                    imageAsset = "mode-radical-recognition.png",
                    onClick = { onGameModeClick(GameMode.RADICAL_RECOGNITION) }
                )
                PreviewableGameModeButton(
                    mode = GameMode.RADICAL_BUILDER,
                    label = "Radical Builder",
                    isPremium = uiState.isPremium,
                    trialInfo = uiState.previewTrials[GameMode.RADICAL_BUILDER],
                    modifier = Modifier.weight(1f),
                    modeColor = Color(0xFF795548),
                    imageAsset = "mode-radical-builder.png",
                    onPremiumClick = { onGameModeClick(GameMode.RADICAL_BUILDER) },
                    onPreviewClick = { onPreviewModeClick(GameMode.RADICAL_BUILDER) },
                    onUpgradeClick = onSubscriptionClick
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Kanji game modes
            Text(
                text = "Kanji Study Modes",
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
                    modeColor = Color(0xFF2196F3),
                    imageAsset = "mode-recognition.png",
                    onClick = { onGameModeClick(GameMode.RECOGNITION) }
                )
                PreviewableGameModeButton(
                    mode = GameMode.WRITING,
                    label = "Writing",
                    isPremium = uiState.isPremium,
                    trialInfo = uiState.previewTrials[GameMode.WRITING],
                    modifier = Modifier.weight(1f),
                    modeColor = Color(0xFF4CAF50),
                    imageAsset = "mode-writing.png",
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
                    modeColor = Color(0xFFFF9800),
                    imageAsset = "mode-vocabulary.png",
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
                    modeColor = Color(0xFF9C27B0),
                    imageAsset = "mode-camera.png",
                    onPremiumClick = { onGameModeClick(GameMode.CAMERA_CHALLENGE) },
                    onPreviewClick = { onPreviewModeClick(GameMode.CAMERA_CHALLENGE) },
                    onUpgradeClick = onSubscriptionClick
                )
            }

            // Flashcard deck button (bookmarked kanji)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onFlashcardsClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        text = if (uiState.flashcardDeckCount > 0)
                            "Flashcards (${uiState.flashcardDeckCount})"
                        else
                            "Flashcards",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontSize = 14.sp
                    )
                }
                Button(
                    onClick = onCollectionClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9C27B0).copy(alpha = 0.8f)
                    )
                ) {
                    Text(
                        text = "Collection ${uiState.collectedKanjiCount}/${uiState.totalKanjiInGrades}",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== LAYER 1: Main tabs (Hiragana | Katakana | 部首 | Kanji) =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MainTab.entries.forEach { tab ->
                    val isSelected = tab == uiState.selectedMainTab
                    Text(
                        text = tab.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { viewModel.selectMainTab(tab) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // ===== LAYER 2 + 3: Sub-tabs (only for Kanji tab) =====
            if (uiState.selectedMainTab == MainTab.KANJI) {
                Spacer(modifier = Modifier.height(4.dp))
                // Layer 2: Sort mode tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    KanjiSortMode.entries.forEach { mode ->
                        val isSelected = mode == uiState.kanjiSortMode
                        Text(
                            text = mode.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.tertiary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { viewModel.selectSortMode(mode) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Layer 3: Level selectors
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    when (uiState.kanjiSortMode) {
                        KanjiSortMode.SCHOOL_GRADE -> {
                            uiState.allGrades.forEach { grade ->
                                val isSelected = grade == uiState.selectedGrade
                                val hasCollection = grade in uiState.gradesWithCollection
                                val isEnabled = hasCollection
                                Text(
                                    text = if (grade == 8) "G8+" else "G$grade",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier
                                        .background(
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                !isEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .then(
                                            if (isEnabled) Modifier.clickable { viewModel.selectGrade(grade) }
                                            else Modifier
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        KanjiSortMode.JLPT_LEVEL -> {
                            listOf(5, 4, 3, 2, 1).forEach { level ->
                                val isSelected = level == uiState.selectedJlptLevel
                                Text(
                                    text = "N$level",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .background(
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { viewModel.selectJlptLevel(level) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        KanjiSortMode.STROKES -> {
                            uiState.availableStrokeCounts.forEach { count ->
                                val isSelected = count == uiState.selectedStrokeCount
                                Text(
                                    text = "${count}画",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .background(
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { viewModel.selectStrokeCount(count) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        KanjiSortMode.FREQUENCY -> {
                            HomeViewModel.frequencyLabels.forEachIndexed { index, label ->
                                val isSelected = index == uiState.selectedFrequencyRange
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .background(
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { viewModel.selectFrequencyRange(index) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section title
            Text(
                text = when (uiState.selectedMainTab) {
                    MainTab.HIRAGANA -> "ひらがな Hiragana"
                    MainTab.KATAKANA -> "カタカナ Katakana"
                    MainTab.RADICALS -> "部首 Radicals"
                    MainTab.KANJI -> when (uiState.kanjiSortMode) {
                        KanjiSortMode.SCHOOL_GRADE -> "Grade ${uiState.selectedGrade} Kanji"
                        KanjiSortMode.JLPT_LEVEL -> "JLPT N${uiState.selectedJlptLevel} Kanji"
                        KanjiSortMode.STROKES -> "${uiState.selectedStrokeCount}-Stroke Kanji"
                        KanjiSortMode.FREQUENCY -> "${HomeViewModel.frequencyLabels[uiState.selectedFrequencyRange]} Kanji"
                    }
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (uiState.selectedMainTab) {
                MainTab.HIRAGANA -> {
                    val kanaChunks = uiState.hiraganaList.chunked(5)
                    kanaChunks.forEach { rowKana ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKana.forEach { kana ->
                                val collected = uiState.collectedHiraganaItems[kana.id]
                                KanaGridItem(
                                    kana = kana,
                                    collectedItem = collected,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onKanaModeClick(KanaType.HIRAGANA, false) }
                                )
                            }
                            repeat(5 - rowKana.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                MainTab.KATAKANA -> {
                    val kanaChunks = uiState.katakanaList.chunked(5)
                    kanaChunks.forEach { rowKana ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKana.forEach { kana ->
                                val collected = uiState.collectedKatakanaItems[kana.id]
                                KanaGridItem(
                                    kana = kana,
                                    collectedItem = collected,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onKanaModeClick(KanaType.KATAKANA, false) }
                                )
                            }
                            repeat(5 - rowKana.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                MainTab.RADICALS -> {
                    val radicalChunks = uiState.radicals.chunked(4)
                    radicalChunks.forEach { rowRadicals ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowRadicals.forEach { radical ->
                                val collected = uiState.collectedRadicalItems[radical.id]
                                RadicalGridItem(
                                    radical = radical,
                                    collectedItem = collected,
                                    onClick = { onRadicalClick(radical.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(4 - rowRadicals.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                MainTab.KANJI -> {
                    val kanjiChunks = uiState.gradeOneKanji.chunked(5)
                    kanjiChunks.forEach { rowKanji ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKanji.forEach { kanji ->
                                KanjiGridItem(
                                    kanji = kanji,
                                    practiceCount = uiState.kanjiPracticeCounts[kanji.id] ?: 0,
                                    modeStats = uiState.kanjiModeStats[kanji.id] ?: emptyMap(),
                                    collectedItem = uiState.collectedItems[kanji.id],
                                    modifier = Modifier.weight(1f),
                                    onClick = { onKanjiClick(kanji.id) }
                                )
                            }
                            repeat(5 - rowKanji.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Bottom padding
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun KanjiGridItem(
    kanji: Kanji,
    practiceCount: Int = 0,
    modeStats: Map<String, Int> = emptyMap(),
    collectedItem: CollectedItem? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isCollected = collectedItem != null
    val borderColor = collectedItem?.let { Color(it.rarity.colorValue) }
    val borderMod = if (borderColor != null) {
        Modifier.border(2.dp, borderColor, RoundedCornerShape(12.dp))
    } else Modifier

    Card(
        modifier = modifier
            .height(64.dp)
            .then(borderMod)
            .then(
                if (isCollected) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCollected)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (collectedItem != null) {
                // Collected: show kanji with rarity border
                com.jworks.kanjiquest.android.ui.theme.KanjiText(
                    text = kanji.literal,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
                // Level badge in top-right
                if (collectedItem.itemLevel > 1) {
                    Text(
                        text = "Lv.${collectedItem.itemLevel}",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(collectedItem.rarity.colorValue),
                        modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
                    )
                }

                // 4-corner per-mode badges
                val recCount = modeStats["recognition"] ?: 0
                val vocCount = modeStats["vocabulary"] ?: 0
                val wrtCount = modeStats["writing"] ?: 0
                val camCount = modeStats["camera_challenge"] ?: 0
                if (recCount > 0) {
                    Text(
                        text = "$recCount",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.align(Alignment.TopStart).padding(3.dp)
                    )
                }
                if (vocCount > 0 && collectedItem.itemLevel <= 1) {
                    Text(
                        text = "$vocCount",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.align(Alignment.TopEnd).padding(3.dp)
                    )
                }
                if (wrtCount > 0) {
                    Text(
                        text = "$wrtCount",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.align(Alignment.BottomStart).padding(3.dp)
                    )
                }
                if (camCount > 0) {
                    Text(
                        text = "$camCount",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9C27B0),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp)
                    )
                }
            }
            // Uncollected: empty placeholder — no kanji shown, not clickable
        }
    }
}

@Composable
private fun RadicalGridItem(
    radical: Radical,
    collectedItem: CollectedItem? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isCollected = collectedItem != null
    val borderColor = collectedItem?.let { Color(it.rarity.colorValue) }
    val borderMod = if (borderColor != null) {
        Modifier.border(2.dp, borderColor, RoundedCornerShape(12.dp))
    } else Modifier

    Card(
        modifier = modifier
            .height(72.dp)
            .then(borderMod)
            .then(
                if (isCollected) Modifier.clickable { onClick() }
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCollected)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        )
    ) {
        if (isCollected) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                RadicalImage(
                    radicalId = radical.id,
                    contentDescription = radical.literal,
                    modifier = Modifier.size(36.dp)
                )
                if (!radical.meaningJp.isNullOrBlank()) {
                    Text(
                        text = radical.meaningJp!!,
                        fontSize = 9.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
        // Uncollected: empty placeholder
    }
}

@Composable
private fun KanaGridItem(
    kana: com.jworks.kanjiquest.core.domain.model.Kana,
    collectedItem: CollectedItem? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isCollected = collectedItem != null
    val borderColor = collectedItem?.let { Color(it.rarity.colorValue) }
    val borderMod = if (borderColor != null) {
        Modifier.border(2.dp, borderColor, RoundedCornerShape(12.dp))
    } else Modifier

    Card(
        modifier = modifier
            .height(64.dp)
            .then(borderMod)
            .then(
                if (isCollected) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCollected)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isCollected) {
                Text(
                    text = kana.literal,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
                Text(
                    text = kana.romanization,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
                )
            }
            // Uncollected: empty placeholder
        }
    }
}

@Composable
private fun GameModeButton(
    label: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    modeColor: Color = Color.Unspecified,
    imageAsset: String? = null,
    onClick: () -> Unit
) {
    val containerColor = if (modeColor != Color.Unspecified) modeColor else MaterialTheme.colorScheme.primary
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageAsset != null) {
                AssetImage(
                    filename = imageAsset,
                    contentDescription = label,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.padding(start = 8.dp))
            }
            Column {
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
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
    modeColor: Color = Color.Unspecified,
    imageAsset: String? = null,
    onPremiumClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    if (isPremium) {
        GameModeButton(
            label = label,
            modifier = modifier,
            modeColor = modeColor,
            imageAsset = imageAsset,
            onClick = onPremiumClick
        )
    } else {
        val remaining = trialInfo?.remaining ?: 0
        val hasTrials = remaining > 0

        Card(
            onClick = {
                if (hasTrials) onPreviewClick()
                else onUpgradeClick()
            },
            modifier = modifier.height(80.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (hasTrials)
                    MaterialTheme.colorScheme.secondaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (imageAsset != null) {
                    AssetImage(
                        filename = imageAsset,
                        contentDescription = label,
                        modifier = Modifier.size(48.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                }
                Column {
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
