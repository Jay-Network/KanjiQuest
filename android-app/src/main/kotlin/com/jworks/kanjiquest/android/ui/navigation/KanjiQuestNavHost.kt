package com.jworks.kanjiquest.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jworks.kanjiquest.android.ui.achievements.AchievementsScreen
import com.jworks.kanjiquest.android.ui.auth.AuthViewModel
import com.jworks.kanjiquest.android.ui.auth.LoginScreen
import com.jworks.kanjiquest.android.ui.detail.KanjiDetailScreen
import com.jworks.kanjiquest.android.ui.game.RecognitionScreen
import com.jworks.kanjiquest.android.ui.game.camera.CameraChallengeScreen
import com.jworks.kanjiquest.android.ui.game.vocabulary.VocabularyScreen
import com.jworks.kanjiquest.android.ui.game.writing.WritingScreen
import com.jworks.kanjiquest.android.ui.home.HomeScreen
import com.jworks.kanjiquest.android.ui.home.HomeViewModel
import com.jworks.kanjiquest.android.ui.progress.ProgressScreen
import com.jworks.kanjiquest.android.ui.settings.SettingsScreen
import com.jworks.kanjiquest.android.ui.shop.ShopScreen
import com.jworks.kanjiquest.android.ui.subscription.SubscriptionScreen
import com.jworks.kanjiquest.core.domain.model.GameMode

@Composable
fun KanjiQuestNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoute.Login.route
    ) {
        composable(NavRoute.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoute.Home.route) {
                        popUpTo(NavRoute.Login.route) { inclusive = true }
                    }
                },
                onContinueWithoutAccount = {
                    navController.navigate(NavRoute.Home.route) {
                        popUpTo(NavRoute.Login.route) { inclusive = true }
                    }
                }
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
                    }
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
                onBack = { navController.popBackStack() }
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

        composable(NavRoute.Settings.route) {
            SettingsScreen(
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
    }
}
