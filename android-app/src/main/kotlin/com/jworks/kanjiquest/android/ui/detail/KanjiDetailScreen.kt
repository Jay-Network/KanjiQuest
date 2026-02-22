package com.jworks.kanjiquest.android.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.jworks.kanjiquest.core.domain.model.ExampleSentence
import com.jworks.kanjiquest.core.domain.model.GameMode
import com.jworks.kanjiquest.core.domain.model.Kanji
import com.jworks.kanjiquest.core.domain.model.Vocabulary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KanjiDetailScreen(
    kanjiId: Int,
    onBack: () -> Unit,
    onPracticeWriting: ((Int) -> Unit)? = null,
    onPracticeCamera: ((Int) -> Unit)? = null,
    viewModel: KanjiDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.kanji?.literal ?: "Kanji Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("\u2190", fontSize = 24.sp)
                    }
                },
                actions = {
                    // Flashcard bookmark toggle
                    IconButton(onClick = { viewModel.toggleFlashcard() }) {
                        Text(
                            text = if (uiState.isInFlashcardDeck) "\u2605" else "\u2606",
                            fontSize = 24.sp,
                            color = if (uiState.isInFlashcardDeck) Color(0xFFFFD700) else MaterialTheme.colorScheme.onPrimary
                        )
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
        val kanji = uiState.kanji

        if (kanji != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large kanji display
                com.jworks.kanjiquest.android.ui.theme.KanjiText(
                    text = kanji.literal,
                    fontSize = 120.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // Info chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    kanji.gradeLabel?.let {
                        AssistChip(onClick = {}, label = { Text(it) })
                    }
                    kanji.jlptLabel?.let {
                        AssistChip(onClick = {}, label = { Text(it) })
                    }
                    AssistChip(onClick = {}, label = { Text("${kanji.strokeCount} strokes") })
                    kanji.frequency?.let {
                        AssistChip(onClick = {}, label = { Text("Freq #$it") })
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Practice stats row
                if (uiState.totalPracticeCount > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${uiState.totalPracticeCount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Practiced",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            uiState.accuracy?.let { acc ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${(acc * 100).toInt()}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            acc >= 0.8f -> Color(0xFF4CAF50)
                                            acc >= 0.6f -> Color(0xFFFF9800)
                                            else -> Color(0xFFF44336)
                                        }
                                    )
                                    Text(
                                        text = "Accuracy",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Practice mode buttons: Writing + Camera
                if (onPracticeWriting != null || onPracticeCamera != null) {
                    Text(
                        text = "Practice:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Writing
                        if (onPracticeWriting != null) {
                            val writingTrial = uiState.modeTrials[GameMode.WRITING]
                            val canPractice = uiState.isPremium || uiState.isAdmin || (writingTrial?.canPractice == true)
                            Button(
                                onClick = {
                                    if (!uiState.isPremium && !uiState.isAdmin) {
                                        viewModel.useModeTrial(GameMode.WRITING)
                                    }
                                    onPracticeWriting(kanjiId)
                                },
                                enabled = canPractice,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50),
                                    disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.3f)
                                )
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Writing", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    if (!uiState.isPremium && !uiState.isAdmin && writingTrial != null) {
                                        Text(
                                            text = if (canPractice) "${writingTrial.trialsRemaining} left" else "No trials",
                                            fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                        // Camera
                        if (onPracticeCamera != null) {
                            val cameraTrial = uiState.modeTrials[GameMode.CAMERA_CHALLENGE]
                            val canPractice = uiState.isPremium || uiState.isAdmin || (cameraTrial?.canPractice == true)
                            Button(
                                onClick = {
                                    if (!uiState.isPremium && !uiState.isAdmin) {
                                        viewModel.useModeTrial(GameMode.CAMERA_CHALLENGE)
                                    }
                                    onPracticeCamera(kanjiId)
                                },
                                enabled = canPractice,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF9C27B0),
                                    disabledContainerColor = Color(0xFF9C27B0).copy(alpha = 0.3f)
                                )
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Camera", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    if (!uiState.isPremium && !uiState.isAdmin && cameraTrial != null) {
                                        Text(
                                            text = if (canPractice) "${cameraTrial.trialsRemaining} left" else "No trials",
                                            fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Meanings
                SectionCard(title = "Meanings") {
                    Text(
                        text = kanji.meaningsEn.joinToString(", "),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // On'yomi readings
                if (kanji.onReadings.isNotEmpty()) {
                    SectionCard(title = "On'yomi (Chinese readings)") {
                        Text(
                            text = kanji.onReadings.joinToString("   "),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Kun'yomi readings
                if (kanji.kunReadings.isNotEmpty()) {
                    SectionCard(title = "Kun'yomi (Japanese readings)") {
                        Text(
                            text = kanji.kunReadings.joinToString("   "),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Related vocabulary with example sentences
                if (uiState.vocabulary.isNotEmpty()) {
                    SectionCard(title = "Related Vocabulary") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.vocabulary.take(10).forEach { vocab ->
                                VocabItem(
                                    vocab = vocab,
                                    sentence = uiState.vocabSentences[vocab.id]
                                )
                            }
                        }
                    }
                }
            }
        } else if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Loading...")
            }
        }
    }

    // Deck chooser dialog
    if (uiState.showDeckChooser) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeckChooser() },
            title = { Text("Add to Deck") },
            text = {
                Column {
                    uiState.deckGroups.forEach { deck ->
                        val isInThisDeck = deck.id in uiState.kanjiInDecks
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isInThisDeck) {
                                        viewModel.removeFromDeck(deck.id)
                                    } else {
                                        viewModel.addToDeck(deck.id)
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isInThisDeck,
                                onCheckedChange = { checked ->
                                    if (checked) viewModel.addToDeck(deck.id)
                                    else viewModel.removeFromDeck(deck.id)
                                }
                            )
                            Text(
                                text = deck.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDeckChooser() }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun VocabItem(vocab: Vocabulary, sentence: ExampleSentence? = null) {
    Column {
        Text(
            text = "${vocab.kanjiForm}  (${vocab.reading})",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = vocab.primaryMeaning,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (sentence != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sentence.japanese,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = sentence.english,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
