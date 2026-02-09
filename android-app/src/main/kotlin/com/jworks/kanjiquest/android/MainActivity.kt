package com.jworks.kanjiquest.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jworks.kanjiquest.android.ui.navigation.KanjiQuestNavHost
import com.jworks.kanjiquest.android.ui.theme.KanjiQuestTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KanjiQuestTheme {
                KanjiQuestNavHost()
            }
        }
    }
}
