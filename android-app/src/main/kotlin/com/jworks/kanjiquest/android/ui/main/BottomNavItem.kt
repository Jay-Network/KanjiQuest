package com.jworks.kanjiquest.android.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.jworks.kanjiquest.android.ui.navigation.NavRoute

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Main : BottomNavItem(NavRoute.Home.route, "Main", Icons.Default.Home)
    data object Study : BottomNavItem(NavRoute.Study.route, "Study", Icons.Default.Edit)
    data object Play : BottomNavItem(NavRoute.Games.route, "Play", Icons.Default.PlayArrow)
    data object Collect : BottomNavItem(NavRoute.CollectionHub.route, "Collect", Icons.Default.Star)

    companion object {
        val items = listOf(Main, Study, Play, Collect)
    }
}
