package com.jworks.kanjiquest.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jworks.kanjiquest.android.ui.detail.KanjiDetailScreen
import com.jworks.kanjiquest.android.ui.game.RecognitionScreen
import com.jworks.kanjiquest.android.ui.game.vocabulary.VocabularyScreen
import com.jworks.kanjiquest.android.ui.game.writing.WritingScreen
import com.jworks.kanjiquest.android.ui.home.HomeScreen
import com.jworks.kanjiquest.android.ui.shop.ShopScreen
import com.jworks.kanjiquest.core.domain.model.GameMode

@Composable
fun KanjiQuestNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoute.Home.route
    ) {
        composable(NavRoute.Home.route) {
            HomeScreen(
                onKanjiClick = { kanjiId ->
                    navController.navigate(NavRoute.KanjiDetail.createRoute(kanjiId))
                },
                onGameModeClick = { mode ->
                    when (mode) {
                        GameMode.RECOGNITION -> navController.navigate(NavRoute.Recognition.route)
                        GameMode.WRITING -> navController.navigate(NavRoute.Writing.route)
                        GameMode.VOCABULARY -> navController.navigate(NavRoute.Vocabulary.route)
                        else -> {}
                    }
                },
                onShopClick = {
                    navController.navigate(NavRoute.Shop.route)
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

        composable(NavRoute.Shop.route) {
            ShopScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
