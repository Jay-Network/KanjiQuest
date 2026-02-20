package com.jworks.kanjiquest.android.ui.navigation

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jworks.kanjiquest.android.ui.achievements.AchievementsScreen
import com.jworks.kanjiquest.android.ui.auth.AuthViewModel
import com.jworks.kanjiquest.android.ui.auth.LoginScreen
import com.jworks.kanjiquest.android.ui.detail.KanjiDetailScreen
import com.jworks.kanjiquest.android.ui.feedback.FeedbackDialog
import com.jworks.kanjiquest.android.ui.feedback.FeedbackFAB
import com.jworks.kanjiquest.android.ui.feedback.FeedbackViewModel
import com.jworks.kanjiquest.android.ui.game.RecognitionScreen
import com.jworks.kanjiquest.android.ui.game.camera.CameraChallengeScreen
import com.jworks.kanjiquest.android.ui.game.kana.KanaRecognitionScreen
import com.jworks.kanjiquest.android.ui.game.kana.KanaWritingScreen
import com.jworks.kanjiquest.android.ui.game.radical.RadicalBuilderScreen
import com.jworks.kanjiquest.android.ui.game.radical.RadicalRecognitionScreen
import com.jworks.kanjiquest.android.ui.game.vocabulary.VocabularyScreen
import com.jworks.kanjiquest.android.ui.game.writing.WritingScreen
import com.jworks.kanjiquest.android.ui.home.HomeScreen
import com.jworks.kanjiquest.android.ui.home.HomeViewModel
import com.jworks.kanjiquest.android.ui.progress.ProgressScreen
import com.jworks.kanjiquest.android.ui.devchat.DevChatScreen
import com.jworks.kanjiquest.android.ui.flashcard.FlashcardScreen
import com.jworks.kanjiquest.android.ui.flashcard.FlashcardStudyScreen
import com.jworks.kanjiquest.android.ui.placement.PlacementTestScreen
import com.jworks.kanjiquest.android.ui.settings.SettingsScreen
import com.jworks.kanjiquest.android.ui.shop.ShopScreen
import com.jworks.kanjiquest.android.ui.subscription.SubscriptionScreen
import com.jworks.kanjiquest.android.ui.worddetail.WordDetailScreen
import androidx.compose.ui.platform.LocalContext
import com.jworks.kanjiquest.core.domain.model.GameMode
import com.jworks.kanjiquest.core.domain.model.KanaType

@Composable
fun KanjiQuestNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Feedback dialog state
    val feedbackViewModel: FeedbackViewModel = hiltViewModel()
    val feedbackUiState by feedbackViewModel.uiState.collectAsState()

    // Show FAB on all screens except Login
    val showFAB = currentRoute != NavRoute.Login.route

    val context = LocalContext.current

    // Helper to navigate after login: placement test if first time, otherwise home
    fun navigateAfterLogin() {
        val prefs = context.getSharedPreferences("kanjiquest_settings", android.content.Context.MODE_PRIVATE)
        val assessmentDone = prefs.getBoolean("assessment_completed", false)
        val destination = if (assessmentDone) NavRoute.Home.route else NavRoute.PlacementTest.route
        navController.navigate(destination) {
            popUpTo(NavRoute.Login.route) { inclusive = true }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (showFAB) {
                FeedbackFAB(
                    onClick = { feedbackViewModel.openDialog() }
                )
            }
        }
    ) { paddingValues ->
        // Feedback dialog
        if (feedbackUiState.isDialogOpen) {
            FeedbackDialog(
                onDismiss = { feedbackViewModel.closeDialog() },
                viewModel = feedbackViewModel
            )
        }

        NavHost(
            navController = navController,
            startDestination = NavRoute.Login.route
        ) {
        composable(NavRoute.Login.route) {
            LoginScreen(
                onLoginSuccess = { navigateAfterLogin() },
                onContinueWithoutAccount = { navigateAfterLogin() }
            )
        }

        composable(NavRoute.Home.route) {
            val homeViewModel: HomeViewModel = hiltViewModel()

            HomeScreen(
                onKanjiClick = { kanjiId ->
                    navController.navigate(NavRoute.KanjiDetail.createRoute(kanjiId))
                },
                onGameModeClick = { mode ->
                    when (mode) {
                        GameMode.RECOGNITION -> navController.navigate(NavRoute.Recognition.route)
                        GameMode.WRITING -> navController.navigate(NavRoute.Writing.route)
                        GameMode.VOCABULARY -> navController.navigate(NavRoute.Vocabulary.route)
                        GameMode.CAMERA_CHALLENGE -> navController.navigate(NavRoute.Camera.route)
                        GameMode.KANA_RECOGNITION -> {} // handled by onKanaModeClick
                        GameMode.KANA_WRITING -> {} // handled by onKanaModeClick
                        GameMode.RADICAL_RECOGNITION -> navController.navigate(NavRoute.RadicalRecognition.route)
                        GameMode.RADICAL_BUILDER -> navController.navigate(NavRoute.RadicalBuilder.route)
                    }
                },
                onKanaModeClick = { kanaType, isWriting ->
                    if (isWriting) {
                        navController.navigate(NavRoute.KanaWriting.createRoute(kanaType.name))
                    } else {
                        navController.navigate(NavRoute.KanaRecognition.createRoute(kanaType.name))
                    }
                },
                onWordOfDayClick = { wordId ->
                    navController.navigate(NavRoute.WordDetail.createRoute(wordId))
                },
                onPreviewModeClick = { mode ->
                    // Consume a preview trial then navigate
                    val success = homeViewModel.usePreviewTrial(mode)
                    if (success) {
                        when (mode) {
                            GameMode.WRITING -> navController.navigate(NavRoute.Writing.route)
                            GameMode.VOCABULARY -> navController.navigate(NavRoute.Vocabulary.route)
                            GameMode.CAMERA_CHALLENGE -> navController.navigate(NavRoute.Camera.route)
                            else -> { /* Recognition is always free */ }
                        }
                    }
                },
                onShopClick = {
                    navController.navigate(NavRoute.Shop.route)
                },
                onProgressClick = {
                    navController.navigate(NavRoute.Progress.route)
                },
                onAchievementsClick = {
                    navController.navigate(NavRoute.Achievements.route)
                },
                onSettingsClick = {
                    navController.navigate(NavRoute.Settings.route)
                },
                onSubscriptionClick = {
                    navController.navigate(NavRoute.Subscription.route)
                },
                onFlashcardsClick = {
                    navController.navigate(NavRoute.Flashcards.route)
                },
                viewModel = homeViewModel
            )
        }

        composable(
            route = NavRoute.KanjiDetail.route,
            arguments = listOf(navArgument("kanjiId") { type = NavType.IntType })
        ) { backStackEntry ->
            val kanjiId = backStackEntry.arguments?.getInt("kanjiId") ?: return@composable
            KanjiDetailScreen(
                kanjiId = kanjiId,
                onBack = { navController.popBackStack() },
                onPracticeWriting = { id ->
                    navController.navigate(NavRoute.WritingTargeted.createRoute(id))
                },
                onPracticeCamera = { id ->
                    navController.navigate(NavRoute.CameraTargeted.createRoute(id))
                }
            )
        }

        composable(NavRoute.Recognition.route) {
            RecognitionScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoute.Writing.route) {
            WritingScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoute.WritingTargeted.route,
            arguments = listOf(navArgument("kanjiId") { type = NavType.IntType })
        ) { backStackEntry ->
            val kanjiId = backStackEntry.arguments?.getInt("kanjiId") ?: return@composable
            WritingScreen(
                onBack = { navController.popBackStack() },
                targetKanjiId = kanjiId
            )
        }

        composable(NavRoute.Vocabulary.route) {
            VocabularyScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoute.Camera.route) {
            CameraChallengeScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoute.RecognitionTargeted.route,
            arguments = listOf(navArgument("kanjiId") { type = NavType.IntType })
        ) { backStackEntry ->
            val kanjiId = backStackEntry.arguments?.getInt("kanjiId") ?: return@composable
            RecognitionScreen(
                onBack = { navController.popBackStack() },
                targetKanjiId = kanjiId
            )
        }

        composable(
            route = NavRoute.VocabularyTargeted.route,
            arguments = listOf(navArgument("kanjiId") { type = NavType.IntType })
        ) { backStackEntry ->
            val kanjiId = backStackEntry.arguments?.getInt("kanjiId") ?: return@composable
            VocabularyScreen(
                onBack = { navController.popBackStack() },
                targetKanjiId = kanjiId
            )
        }

        composable(
            route = NavRoute.CameraTargeted.route,
            arguments = listOf(navArgument("kanjiId") { type = NavType.IntType })
        ) { backStackEntry ->
            val kanjiId = backStackEntry.arguments?.getInt("kanjiId") ?: return@composable
            CameraChallengeScreen(
                onBack = { navController.popBackStack() },
                targetKanjiId = kanjiId
            )
        }

        composable(NavRoute.Shop.route) {
            ShopScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoute.Progress.route) {
            ProgressScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoute.Flashcards.route) {
            FlashcardScreen(
                onBack = { navController.popBackStack() },
                onStudy = { navController.navigate(NavRoute.FlashcardStudy.route) },
                onKanjiClick = { kanjiId ->
                    navController.navigate(NavRoute.KanjiDetail.createRoute(kanjiId))
                }
            )
        }

        composable(NavRoute.FlashcardStudy.route) {
            FlashcardStudyScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoute.PlacementTest.route) {
            PlacementTestScreen(
                onComplete = {
                    navController.navigate(NavRoute.Home.route) {
                        popUpTo(NavRoute.PlacementTest.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = NavRoute.KanaRecognition.route,
            arguments = listOf(navArgument("kanaType") { type = NavType.StringType })
        ) { backStackEntry ->
            val kanaTypeStr = backStackEntry.arguments?.getString("kanaType") ?: "HIRAGANA"
            val kanaType = KanaType.valueOf(kanaTypeStr)
            KanaRecognitionScreen(
                kanaType = kanaType,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoute.KanaWriting.route,
            arguments = listOf(navArgument("kanaType") { type = NavType.StringType })
        ) { backStackEntry ->
            val kanaTypeStr = backStackEntry.arguments?.getString("kanaType") ?: "HIRAGANA"
            val kanaType = KanaType.valueOf(kanaTypeStr)
            KanaWritingScreen(
                kanaType = kanaType,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoute.RadicalRecognition.route) {
            RadicalRecognitionScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoute.RadicalBuilder.route) {
            RadicalBuilderScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoute.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onDevChat = { navController.navigate(NavRoute.DevChat.route) },
                onRetakeAssessment = {
                    navController.navigate(NavRoute.PlacementTest.route)
                }
            )
        }

        composable(NavRoute.DevChat.route) {
            DevChatScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoute.Achievements.route) {
            AchievementsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoute.Subscription.route) {
            SubscriptionScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoute.WordDetail.route,
            arguments = listOf(navArgument("wordId") { type = NavType.LongType })
        ) { backStackEntry ->
            val wordId = backStackEntry.arguments?.getLong("wordId") ?: return@composable
            WordDetailScreen(
                wordId = wordId,
                onBack = { navController.popBackStack() },
                onKanjiClick = { kanjiId ->
                    navController.navigate(NavRoute.KanjiDetail.createRoute(kanjiId))
                }
            )
        }
        }
    }
}
