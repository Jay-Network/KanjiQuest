package com.jworks.kanjiquest.android.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jworks.kanjiquest.android.ui.components.AssetImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(
    onFeedbackClick: () -> Unit = {},
    onRadicalBuilder: () -> Unit,
    onTestMode: () -> Unit = {},
    onSubscriptionClick: () -> Unit = {},
    viewModel: GamesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Play") },
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
            Text(
                text = "Game Modes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Radical Builder — playable
            GameCard(
                title = "Radical Builder",
                description = "Build kanji from radical parts",
                color = Color(0xFF795548),
                imageAsset = "mode-radical-builder.png",
                isPlayable = true,
                onClick = onRadicalBuilder
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Test Mode — playable
            GameCard(
                title = "Test Mode",
                description = "Quiz yourself on grades, JLPT, kana, or radicals",
                color = Color(0xFF2196F3),
                imageAsset = null,
                isPlayable = true,
                onClick = onTestMode
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Speed Challenge — coming soon
            GameCard(
                title = "Speed Challenge",
                description = "Answer as fast as you can before time runs out",
                color = Color(0xFFFF5722),
                imageAsset = null,
                isPlayable = false,
                comingSoonLabel = "Coming Soon"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Battle — coming soon
            GameCard(
                title = "Battle",
                description = "Challenge friends online",
                color = Color(0xFF673AB7),
                imageAsset = null,
                isPlayable = false,
                comingSoonLabel = "Coming Soon"
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GameCard(
    title: String,
    description: String,
    color: Color,
    imageAsset: String?,
    isPlayable: Boolean,
    comingSoonLabel: String? = null,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = { if (isPlayable) onClick() },
        enabled = isPlayable,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlayable) color else color.copy(alpha = 0.3f),
            disabledContainerColor = color.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageAsset != null) {
                AssetImage(
                    filename = imageAsset,
                    contentDescription = title,
                    modifier = Modifier.size(56.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.padding(start = 12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlayable) Color.White else Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = if (isPlayable) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.4f)
                )
            }
            if (comingSoonLabel != null) {
                Text(
                    text = comingSoonLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .background(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
