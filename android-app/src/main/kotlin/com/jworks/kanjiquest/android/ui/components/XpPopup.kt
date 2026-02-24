package com.jworks.kanjiquest.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun XpPopup(
    isCorrect: Boolean?,
    xpGained: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isCorrect != null,
        enter = fadeIn() + slideInHorizontally { it },
        exit = fadeOut() + slideOutHorizontally { it },
        modifier = modifier
    ) {
        val bgColor = if (isCorrect == true) Color(0xFF4CAF50) else Color(0xFFF44336)
        val label = if (isCorrect == true) "+$xpGained XP" else "Incorrect"

        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .background(color = bgColor, shape = RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
