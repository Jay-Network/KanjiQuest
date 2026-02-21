package com.jworks.kanjiquest.android.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jworks.kanjiquest.core.domain.model.CollectedItem
import com.jworks.kanjiquest.core.domain.model.Rarity
import kotlinx.coroutines.delay

@Composable
fun DiscoveryOverlay(
    discoveredItem: CollectedItem,
    kanjiLiteral: String?,
    kanjiMeaning: String?,
    onDismiss: () -> Unit
) {
    val rarityColor = Color(discoveredItem.rarity.colorValue)
    val rarityName = discoveredItem.rarity.label

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    // Pulsing glow effect
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + scaleIn(tween(500, easing = FastOutSlowInEasing)),
        exit = fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                // "NEW!" header
                Text(
                    text = "NEW!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = rarityColor,
                    letterSpacing = 4.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Kanji card with rarity glow
                Card(
                    modifier = Modifier
                        .size(180.dp)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        rarityColor.copy(alpha = glowAlpha),
                                        Color.Transparent
                                    )
                                ),
                                radius = size.maxDimension * 0.7f
                            )
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val displayText = kanjiLiteral ?: discoveredItem.itemId.toString()
                        com.jworks.kanjiquest.android.ui.theme.KanjiText(
                            text = displayText,
                            fontSize = 96.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Meaning
                if (kanjiMeaning != null) {
                    Text(
                        text = kanjiMeaning,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Rarity badge
                val rarityStars = when (discoveredItem.rarity) {
                    Rarity.COMMON -> ""
                    Rarity.UNCOMMON -> "\u2605"
                    Rarity.RARE -> "\u2605\u2605"
                    Rarity.EPIC -> "\u2605\u2605\u2605"
                    Rarity.LEGENDARY -> "\u2605\u2605\u2605\u2605"
                }
                Text(
                    text = "$rarityStars $rarityName",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = rarityColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Source info
                val sourceText = when (discoveredItem.source) {
                    "kanjilens" -> "Discovered via KanjiLens!"
                    "starter" -> "Starter Collection"
                    else -> "Added to Collection"
                }
                Text(
                    text = sourceText,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Tap to continue",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}
