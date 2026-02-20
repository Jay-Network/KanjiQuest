package com.jworks.kanjiquest.android.ui.game.kana

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jworks.kanjiquest.android.ui.game.writing.WritingScreen
import com.jworks.kanjiquest.core.domain.model.KanaType

/**
 * Kana Writing screen — reuses the existing WritingScreen composable since the
 * writing mechanic (canvas + strokes) is identical. The GameEngine is already
 * configured to route KANA_WRITING through KanaQuestionGenerator.
 *
 * The ViewModel handles starting the session with the correct GameMode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanaWritingScreen(
    kanaType: KanaType,
    onBack: () -> Unit,
    viewModel: KanaWritingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val accentColor = if (kanaType == KanaType.HIRAGANA) HiraganaColor else KatakanaColor
    val title = if (kanaType == KanaType.HIRAGANA) "Hiragana Writing" else "Katakana Writing"

    LaunchedEffect(kanaType) {
        viewModel.startGame(kanaType)
    }

    // Delegate to the standard WritingScreen which handles canvas, strokes, AI feedback
    // The GameEngine's showNextQuestion() dispatches to kanaQuestionGenerator.generateWritingQuestion()
    WritingScreen(onBack = onBack)
}
