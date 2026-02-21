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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.jworks.kanjiquest.core.domain.model.CollectedItem
import com.jworks.kanjiquest.core.domain.model.CollectionItemType
import com.jworks.kanjiquest.core.domain.model.Rarity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onBack: () -> Unit,
    onKanjiClick: (Int) -> Unit = {},
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collection") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Stats summary
            val stats = uiState.stats
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${stats.totalCollected} Total Collected",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RarityStatChip("Common", stats.commonCount, Color(Rarity.COMMON.colorValue))
                        RarityStatChip("Uncommon", stats.uncommonCount, Color(Rarity.UNCOMMON.colorValue))
                        RarityStatChip("Rare", stats.rareCount, Color(Rarity.RARE.colorValue))
                        RarityStatChip("Epic", stats.epicCount, Color(Rarity.EPIC.colorValue))
                        RarityStatChip("Legend", stats.legendaryCount, Color(Rarity.LEGENDARY.colorValue))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CollectionTab("Kanji", stats.kanjiCount, CollectionItemType.KANJI, uiState.selectedTab) {
                    viewModel.selectTab(CollectionItemType.KANJI)
                }
                CollectionTab("Hiragana", stats.hiraganaCount, CollectionItemType.HIRAGANA, uiState.selectedTab) {
                    viewModel.selectTab(CollectionItemType.HIRAGANA)
                }
                CollectionTab("Katakana", stats.katakanaCount, CollectionItemType.KATAKANA, uiState.selectedTab) {
                    viewModel.selectTab(CollectionItemType.KATAKANA)
                }
                CollectionTab("Radicals", stats.radicalCount, CollectionItemType.RADICAL, uiState.selectedTab) {
                    viewModel.selectTab(CollectionItemType.RADICAL)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rarity filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RarityFilterChip("All", null, uiState.selectedRarityFilter) {
                    viewModel.filterByRarity(null)
                }
                Rarity.entries.forEach { rarity ->
                    RarityFilterChip(rarity.label, rarity, uiState.selectedRarityFilter) {
                        viewModel.filterByRarity(rarity)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Item count
            Text(
                text = "${uiState.filteredItems.size} items",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Collection grid
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LinearProgressIndicator()
                }
            } else if (uiState.filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No items found.\nPlay games to discover new items!",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val chunks = uiState.filteredItems.chunked(5)
                chunks.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { item ->
                            CollectionGridItem(
                                item = item,
                                displayText = when (uiState.selectedTab) {
                                    CollectionItemType.KANJI -> uiState.kanjiLiterals[item.itemId] ?: "?"
                                    else -> item.itemId.toString()
                                },
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (uiState.selectedTab == CollectionItemType.KANJI) {
                                        onKanjiClick(item.itemId)
                                    }
                                }
                            )
                        }
                        repeat(5 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RarityStatChip(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 9.sp,
            color = color
        )
    }
}

@Composable
private fun CollectionTab(
    label: String,
    count: Int,
    type: CollectionItemType,
    selectedType: CollectionItemType,
    onClick: () -> Unit
) {
    val isSelected = type == selectedType
    Text(
        text = "$label ($count)",
        fontSize = 12.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun RarityFilterChip(
    label: String,
    rarity: Rarity?,
    selectedRarity: Rarity?,
    onClick: () -> Unit
) {
    val isSelected = rarity == selectedRarity
    val chipColor = if (rarity != null) Color(rarity.colorValue) else MaterialTheme.colorScheme.primary
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) Color.White else chipColor,
        modifier = Modifier
            .background(
                color = if (isSelected) chipColor else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun CollectionGridItem(
    item: CollectedItem,
    displayText: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val rarityColor = Color(item.rarity.colorValue)

    Card(
        modifier = modifier
            .height(72.dp)
            .border(2.dp, rarityColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            com.jworks.kanjiquest.android.ui.theme.KanjiText(
                text = displayText,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )

            // Level badge
            if (item.itemLevel > 0) {
                Text(
                    text = "Lv.${item.itemLevel}",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = rarityColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                )
            }

            // XP progress bar at bottom
            if (!item.isMaxLevel) {
                LinearProgressIndicator(
                    progress = { item.levelProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)),
                    color = rarityColor,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}
