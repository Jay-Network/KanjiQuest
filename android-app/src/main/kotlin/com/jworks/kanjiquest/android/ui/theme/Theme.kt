package com.jworks.kanjiquest.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Warm, kid-friendly palette
val Orange = Color(0xFFFF8C42)
val OrangeDark = Color(0xFFE07030)
val Teal = Color(0xFF26A69A)
val TealDark = Color(0xFF00897B)
val Gold = Color(0xFFFFD54F)
val GoldDark = Color(0xFFFFC107)
val Cream = Color(0xFFFFF8E1)
val CreamDark = Color(0xFF2C2C2C)

private val LightColors = lightColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    secondary = Teal,
    onSecondary = Color.White,
    tertiary = Gold,
    onTertiary = Color.Black,
    background = Cream,
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
)

private val DarkColors = darkColorScheme(
    primary = OrangeDark,
    onPrimary = Color.White,
    secondary = TealDark,
    onSecondary = Color.White,
    tertiary = GoldDark,
    onTertiary = Color.Black,
    background = CreamDark,
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
)

@Composable
fun KanjiQuestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
