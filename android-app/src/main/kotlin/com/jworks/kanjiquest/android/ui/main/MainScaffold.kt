package com.jworks.kanjiquest.android.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jworks.kanjiquest.android.ui.collection.CollectionHubScreen
import com.jworks.kanjiquest.android.ui.games.GamesScreen
import com.jworks.kanjiquest.android.ui.home.HomeScreen
import com.jworks.kanjiquest.android.ui.navigation.NavRoute
import com.jworks.kanjiquest.android.ui.study.StudyScreen
import com.jworks.kanjiquest.core.domain.model.GameMode
import com.jworks.kanjiquest.core.domain.model.KanaType

@Composable
fun MainScaffold(
    rootNavController: NavController,
    onFeedbackClick: () -> Unit,
    onKanjiClick: (Int) -> Unit,
    onRadicalClick: (Int) -> Unit,
    onWordOfDayClick: (Long) -> Unit,
    onShopClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSubscriptionClick: () -> Unit,
    onGameModeClick: (GameMode) -> Unit,
    onKanaModeClick: (KanaType, Boolean) -> Unit,
    onPreviewModeClick: (GameMode) -> Unit,
    onFlashcardsClick: () -> Unit,
    onCollectionClick: () -> Unit,
    onProgressClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onFlashcardStudy: (Long) -> Unit,
    onTestMode: () -> Unit = {}
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                bottomNavController.navigate(item.route) {
                                    popUpTo(bottomNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                fontWeight = if (currentRoute == item.route) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = NavRoute.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoute.Home.route) {
                HomeScreen(
                    onKanjiClick = onKanjiClick,
                    onRadicalClick = onRadicalClick,
                    onWordOfDayClick = onWordOfDayClick,
                    onShopClick = onShopClick,
                    onSettingsClick = onSettingsClick,
                    onSubscriptionClick = onSubscriptionClick,
                    onProgressClick = onProgressClick,
                    onAchievementsClick = onAchievementsClick,
                    onFeedbackClick = onFeedbackClick
                )
            }

            composable(NavRoute.Study.route) {
                StudyScreen(
                    onFeedbackClick = onFeedbackClick,
                    onStartSession = { gameMode, kanaType ->
                        when (gameMode) {
                            GameMode.RECOGNITION -> rootNavController.navigate(NavRoute.Recognition.route)
                            GameMode.WRITING -> rootNavController.navigate(NavRoute.Writing.route)
                            GameMode.VOCABULARY -> rootNavController.navigate(NavRoute.Vocabulary.route)
                            GameMode.CAMERA_CHALLENGE -> rootNavController.navigate(NavRoute.Camera.route)
                            GameMode.KANA_RECOGNITION -> rootNavController.navigate(NavRoute.KanaRecognition.createRoute(kanaType?.name ?: "HIRAGANA"))
                            GameMode.KANA_WRITING -> rootNavController.navigate(NavRoute.KanaWriting.createRoute(kanaType?.name ?: "HIRAGANA"))
                            GameMode.RADICAL_RECOGNITION -> rootNavController.navigate(NavRoute.RadicalRecognition.route)
                            GameMode.RADICAL_BUILDER -> rootNavController.navigate(NavRoute.RadicalBuilder.route)
                        }
                    },
                    onSubscriptionClick = onSubscriptionClick
                )
            }

            composable(NavRoute.Games.route) {
                GamesScreen(
                    onFeedbackClick = onFeedbackClick,
                    onRadicalBuilder = {
                        rootNavController.navigate(NavRoute.RadicalBuilder.route)
                    },
                    onTestMode = onTestMode,
                    onSubscriptionClick = onSubscriptionClick
                )
            }

            composable(NavRoute.CollectionHub.route) {
                CollectionHubScreen(
                    onFeedbackClick = onFeedbackClick,
                    onKanjiClick = onKanjiClick,
                    onRadicalClick = onRadicalClick,
                    onKanaClick = { kanaId, kanaType ->
                        rootNavController.navigate(NavRoute.KanaWritingTargeted.createRoute(kanaId, kanaType))
                    },
                    onFlashcardStudy = onFlashcardStudy,
                    onKanjiDetailClick = onKanjiClick
                )
            }
        }
    }
}
