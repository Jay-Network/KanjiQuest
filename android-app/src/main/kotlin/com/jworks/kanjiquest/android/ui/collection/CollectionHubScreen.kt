package com.jworks.kanjiquest.android.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
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
import com.jworks.kanjiquest.android.ui.home.HomeViewModel
import com.jworks.kanjiquest.android.ui.home.MainTab
import com.jworks.kanjiquest.android.ui.home.KanjiSortMode
import com.jworks.kanjiquest.android.ui.components.RadicalImage
import com.jworks.kanjiquest.core.domain.model.CollectedItem
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.model.KanaType
import com.jworks.kanjiquest.core.domain.model.Radical

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionHubScreen(
    onFeedbackClick: () -> Unit = {},
    onKanjiClick: (Int) -> Unit = {},
    onRadicalClick: (Int) -> Unit = {},
    onFlashcardStudy: (Long) -> Unit = {},
    onKanjiDetailClick: (Int) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collect") },
                actions = {
                    IconButton(onClick = onFeedbackClick) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = "Send Feedback",
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
            // Collection stats summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.collectedKanjiCount}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Kanji", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.collectedHiraganaIds.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE91E63)
                        )
                        Text("Hiragana", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.collectedKatakanaIds.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00BCD4)
                        )
                        Text("Katakana", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.collectedRadicalIds.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF795548)
                        )
                        Text("Radicals", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main tabs (Hiragana | Katakana | 部首 | Kanji)
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

            // Sub-tabs for Kanji sorting
            if (uiState.selectedMainTab == MainTab.KANJI) {
                Spacer(modifier = Modifier.height(4.dp))
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

                // Level selectors
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
                                val collected = uiState.perGradeCollectedCounts[grade] ?: 0
                                val total = uiState.perGradeTotalCounts[grade] ?: 0
                                val gradeLabel = if (grade == 8) "G8+" else "G$grade"
                                val labelText = if (total > 0) "$gradeLabel\n$collected/$total" else gradeLabel
                                Text(
                                    text = labelText,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        !hasCollection -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier
                                        .background(
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                !hasCollection -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .then(
                                            if (hasCollection) Modifier.clickable { viewModel.selectGrade(grade) }
                                            else Modifier
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        KanjiSortMode.JLPT_LEVEL -> {
                            listOf(5, 4, 3, 2, 1).forEach { level ->
                                val isSelected = level == uiState.selectedJlptLevel
                                val collected = uiState.perJlptCollectedCounts[level] ?: 0
                                val total = uiState.perJlptTotalCounts[level] ?: 0
                                val labelText = if (total > 0) "N$level\n$collected/$total" else "N$level"
                                Text(
                                    text = labelText,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
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

            // Grid display
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
                                    modifier = Modifier.weight(1f)
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
                                    modifier = Modifier.weight(1f)
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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun KanjiGridItem(
    kanji: Kanji,
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
                com.jworks.kanjiquest.android.ui.theme.KanjiText(
                    text = kanji.literal,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
                if (collectedItem.itemLevel > 1) {
                    Text(
                        text = "Lv.${collectedItem.itemLevel}",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(collectedItem.rarity.colorValue),
                        modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
                    )
                }
                val recCount = modeStats["recognition"] ?: 0
                val vocCount = modeStats["vocabulary"] ?: 0
                val wrtCount = modeStats["writing"] ?: 0
                val camCount = modeStats["camera_challenge"] ?: 0
                if (recCount > 0) {
                    Text("$recCount", fontSize = 7.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3), modifier = Modifier.align(Alignment.TopStart).padding(3.dp))
                }
                if (vocCount > 0 && collectedItem.itemLevel <= 1) {
                    Text("$vocCount", fontSize = 7.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800), modifier = Modifier.align(Alignment.TopEnd).padding(3.dp))
                }
                if (wrtCount > 0) {
                    Text("$wrtCount", fontSize = 7.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50), modifier = Modifier.align(Alignment.BottomStart).padding(3.dp))
                }
                if (camCount > 0) {
                    Text("$camCount", fontSize = 7.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFF9C27B0), modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp))
                }
            }
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
    }
}

@Composable
private fun KanaGridItem(
    kana: com.jworks.kanjiquest.core.domain.model.Kana,
    collectedItem: CollectedItem? = null,
    modifier: Modifier = Modifier
) {
    val isCollected = collectedItem != null
    val borderColor = collectedItem?.let { Color(it.rarity.colorValue) }
    val borderMod = if (borderColor != null) {
        Modifier.border(2.dp, borderColor, RoundedCornerShape(12.dp))
    } else Modifier

    Card(
        modifier = modifier
            .height(64.dp)
            .then(borderMod),
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
        }
    }
}
