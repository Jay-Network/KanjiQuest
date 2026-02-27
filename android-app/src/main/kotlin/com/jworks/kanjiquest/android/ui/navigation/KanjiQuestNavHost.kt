package com.jworks.kanjiquest.android.ui.navigation

import android.net.Uri
import android.widget.Toast
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
import com.jworks.kanjiquest.android.ui.auth.LoginScreen
import com.jworks.kanjiquest.android.ui.detail.KanjiDetailScreen
import com.jworks.kanjiquest.android.ui.detail.RadicalDetailScreen
import com.jworks.kanjiquest.android.ui.feedback.FeedbackDialog
import com.jworks.kanjiquest.android.ui.feedback.FeedbackFAB
import com.jworks.kanjiquest.android.ui.feedback.FeedbackViewModel
import com.jworks.kanjiquest.android.ui.game.DiscoveryOverlay
import com.jworks.kanjiquest.android.ui.game.RecognitionScreen
import com.jworks.kanjiquest.android.ui.game.camera.CameraChallengeScreen
import com.jworks.kanjiquest.android.ui.game.camera.FieldJournalScreen
import com.jworks.kanjiquest.android.ui.game.kana.KanaRecognitionScreen
import com.jworks.kanjiquest.android.ui.game.kana.KanaWritingScreen
import com.jworks.kanjiquest.android.ui.game.radical.RadicalBuilderScreen
import com.jworks.kanjiquest.android.ui.game.radical.RadicalRecognitionScreen
import com.jworks.kanjiquest.android.ui.game.vocabulary.VocabularyScreen
import com.jworks.kanjiquest.android.ui.game.writing.WritingScreen
import com.jworks.kanjiquest.android.ui.collection.CollectionScreen
import com.jworks.kanjiquest.android.ui.main.MainScaffold
import com.jworks.kanjiquest.android.ui.progress.ProgressScreen
import com.jworks.kanjiquest.android.ui.devchat.DevChatScreen
import com.jworks.kanjiquest.android.ui.flashcard.FlashcardScreen
import com.jworks.kanjiquest.android.ui.flashcard.FlashcardStudyScreen
import com.jworks.kanjiquest.android.ui.placement.PlacementTestScreen
import com.jworks.kanjiquest.android.ui.quiz.TestModeScreen
import com.jworks.kanjiquest.android.ui.settings.SettingsScreen
import com.jworks.kanjiquest.android.ui.shop.ShopScreen
import com.jworks.kanjiquest.android.ui.subscription.SubscriptionScreen
import com.jworks.kanjiquest.android.ui.worddetail.WordDetailScreen
import androidx.compose.ui.platform.LocalContext
import com.jworks.kanjiquest.core.domain.model.CollectedItem
import com.jworks.kanjiquest.core.domain.model.GameMode
import com.jworks.kanjiquest.core.domain.model.KanaType

private const val MAIN_SCAFFOLD_ROUTE = "main_scaffold"

@Composable
fun KanjiQuestNavHost(
    deepLinkUri: Uri? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Feedback dialog state
    val feedbackViewModel: FeedbackViewModel = hiltViewModel()
    val feedbackUiState by feedbackViewModel.uiState.collectAsState()

    // Deep link collection state
    var deepLinkCollectedItem by remember { mutableStateOf<CollectedItem?>(null) }
    var deepLinkKanjiLiteral by remember { mutableStateOf<String?>(null) }
    var showDeepLinkOverlay by remember { mutableStateOf(false) }

    // Show FAB on all screens except Login and Splash
    val showFAB = currentRoute != NavRoute.Login.route && currentRoute != NavRoute.Splash.route

    val context = LocalContext.current

    // Handle deep link
    val deepLinkViewModel: DeepLinkCollectionViewModel = hiltViewModel()
    LaunchedEffect(deepLinkUri, currentRoute) {
        if (deepLinkUri == null) return@LaunchedEffect
        if (currentRoute == NavRoute.Splash.route || currentRoute == NavRoute.Login.route || currentRoute == null) {
            return@LaunchedEffect
        }
        val host = deepLinkUri.host
        when (host) {
            "collect" -> {
                val kanjiIdStr = deepLinkUri.getQueryParameter("kanji_id")
                val source = deepLinkUri.getQueryParameter("source") ?: "kanjilens"
                val kanjiId = kanjiIdStr?.toIntOrNull()
                if (kanjiId != null) {
                    val result = deepLinkViewModel.collectFromDeepLink(kanjiId, source)
                    if (result != null) {
                        deepLinkCollectedItem = result.first
                        deepLinkKanjiLiteral = result.second
                        showDeepLinkOverlay = true
                    } else {
                        Toast.makeText(context, "Already in collection!", Toast.LENGTH_SHORT).show()
                    }
                }
                onDeepLinkConsumed()
            }
            "subscription" -> {
                navController.navigate(NavRoute.Subscription.route)
                onDeepLinkConsumed()
            }
            else -> onDeepLinkConsumed()
        }
    }

    // Helper to navigate after login — always go to main screen
    fun navigateAfterLogin() {
        navController.navigate(MAIN_SCAFFOLD_ROUTE) {
            popUpTo(NavRoute.Login.route) { inclusive = true }
        }
    }

    Scaffold { paddingValues ->
        // Feedback dialog
        if (feedbackUiState.isDialogOpen) {
            FeedbackDialog(
                onDismiss = { feedbackViewModel.closeDialog() },
                viewModel = feedbackViewModel
            )
        }

        NavHost(
            navController = navController,
            startDestination = NavRoute.Splash.route
        ) {
        composable(NavRoute.Splash.route) {
            com.jworks.kanjiquest.android.ui.splash.SplashScreen(
                onSplashComplete = {
                    navController.navigate(NavRoute.Login.route) {
                        popUpTo(NavRoute.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoute.Login.route) {
            LoginScreen(
                onLoginSuccess = { navigateAfterLogin() },
                onContinueWithoutAccount = { navigateAfterLogin() }
            )
        }

        // Main scaffold with bottom nav (replaces old Home route)
        composable(MAIN_SCAFFOLD_ROUTE) {
            MainScaffold(
                rootNavController = navController,
                onFeedbackClick = { feedbackViewModel.openDialog() },
                onKanjiClick = { kanjiId ->
                    navController.navigate(NavRoute.KanjiDetail.createRoute(kanjiId))
                },
                onRadicalClick = { radicalId ->
                    navController.navigate(NavRoute.RadicalDetail.createRoute(radicalId))
                },
                onWordOfDayClick = { wordId ->
                    navController.navigate(NavRoute.WordDetail.createRoute(wordId))
                },
                onShopClick = {
                    navController.navigate(NavRoute.Shop.route)
                },
                onSettingsClick = {
                    navController.navigate(NavRoute.Settings.route)
                },
                onSubscriptionClick = {
                    navController.navigate(NavRoute.Subscription.route)
                },
                onGameModeClick = { mode ->
                    when (mode) {
                        GameMode.RECOGNITION -> navController.navigate(NavRoute.Recognition.route)
                        GameMode.WRITING -> navController.navigate(NavRoute.Writing.route)
                        GameMode.VOCABULARY -> navController.navigate(NavRoute.Vocabulary.route)
                        GameMode.CAMERA_CHALLENGE -> navController.navigate(NavRoute.Camera.route)
                        GameMode.KANA_RECOGNITION -> {}
                        GameMode.KANA_WRITING -> {}
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
                onPreviewModeClick = { mode ->
                    when (mode) {
                        GameMode.WRITING -> navController.navigate(NavRoute.Writing.route)
                        GameMode.VOCABULARY -> navController.navigate(NavRoute.Vocabulary.route)
                        GameMode.CAMERA_CHALLENGE -> navController.navigate(NavRoute.Camera.route)
                        else -> {}
                    }
                },
                onFlashcardsClick = {
                    navController.navigate(NavRoute.Flashcards.route)
                },
                onCollectionClick = {
                    navController.navigate(NavRoute.Collection.route)
                },
                onProgressClick = {
                    navController.navigate(NavRoute.Progress.route)
                },
                onAchievementsClick = {
                    navController.navigate(NavRoute.Achievements.route)
                },
                onFlashcardStudy = { deckId ->
                    navController.navigate(NavRoute.FlashcardStudy.createRoute(deckId))
                },
                onTestMode = {
                    navController.navigate(NavRoute.TestMode.route)
                }
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
                onBack = { navController.popBackStack() },
                onJournal = { navController.navigate(NavRoute.FieldJournal.route) }
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
                targetKanjiId = kanjiId,
                onJournal = { navController.navigate(NavRoute.FieldJournal.route) }
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
                onStudy = { deckId ->
                    navController.navigate(NavRoute.FlashcardStudy.createRoute(deckId))
                },
                onKanjiClick = { kanjiId ->
                    navController.navigate(NavRoute.KanjiDetail.createRoute(kanjiId))
                }
            )
        }

        composable(
            route = NavRoute.FlashcardStudy.route,
            arguments = listOf(navArgument("deckId") { type = NavType.LongType })
        ) {
            FlashcardStudyScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoute.PlacementTest.route) {
            PlacementTestScreen(
                onComplete = {
                    navController.navigate(MAIN_SCAFFOLD_ROUTE) {
                        popUpTo(NavRoute.PlacementTest.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoute.TestMode.route) {
            TestModeScreen(
                onBack = { navController.popBackStack() }
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

        composable(
            route = NavRoute.KanaWritingTargeted.route,
            arguments = listOf(
                navArgument("kanaId") { type = NavType.IntType },
                navArgument("kanaType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val kanaId = backStackEntry.arguments?.getInt("kanaId") ?: return@composable
            val kanaTypeStr = backStackEntry.arguments?.getString("kanaType") ?: "HIRAGANA"
            val kanaType = KanaType.valueOf(kanaTypeStr)
            WritingScreen(
                onBack = { navController.popBackStack() },
                targetKanjiId = kanaId,
                gameMode = GameMode.KANA_WRITING,
                kanaType = kanaType
            )
        }

        composable(
            route = NavRoute.RadicalDetail.route,
            arguments = listOf(navArgument("radicalId") { type = NavType.IntType })
        ) { backStackEntry ->
            val radicalId = backStackEntry.arguments?.getInt("radicalId") ?: return@composable
            RadicalDetailScreen(
                radicalId = radicalId,
                onBack = { navController.popBackStack() },
                onKanjiClick = { kanjiId ->
                    navController.navigate(NavRoute.KanjiDetail.createRoute(kanjiId))
                }
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

        composable(NavRoute.FieldJournal.route) {
            FieldJournalScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoute.Collection.route) {
            CollectionScreen(
                onBack = { navController.popBackStack() },
                onKanjiClick = { kanjiId ->
                    navController.navigate(NavRoute.KanjiDetail.createRoute(kanjiId))
                }
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

        // Deep link discovery overlay
        if (showDeepLinkOverlay && deepLinkCollectedItem != null) {
            DiscoveryOverlay(
                discoveredItem = deepLinkCollectedItem!!,
                kanjiLiteral = deepLinkKanjiLiteral,
                kanjiMeaning = null,
                onDismiss = {
                    showDeepLinkOverlay = false
                    deepLinkCollectedItem = null
                    deepLinkKanjiLiteral = null
                }
            )
        }
    }
}
