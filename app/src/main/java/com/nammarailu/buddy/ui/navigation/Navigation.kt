package com.nammarailu.buddy.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home        : Screen("home",         "Home",     Icons.Default.Home)
    object LiveStation : Screen("live/{stationId}", "Live",  Icons.Default.Train) {
        fun createRoute(id: String) = "live/$id"
    }
    object CoachPos    : Screen("coach/{trainId}", "Coach", Icons.Default.ViewWeek) {
        fun createRoute(id: String) = "coach/$id"
    }
    object Platform    : Screen("platform/{trainId}/{stationId}", "Ping", Icons.Default.Campaign) {
        fun createRoute(trainId: String, stationId: String) = "platform/$trainId/$stationId"
    }
    object Alarm       : Screen("alarm",        "Alarm",    Icons.Default.NotificationsActive)
    object Settings    : Screen("settings",     "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(Screen.Home, Screen.Alarm, Screen.Settings)

@Composable
fun RailuBottomBar(navController: NavController) {
    val entry by navController.currentBackStackEntryAsState()
    val current = entry?.destination?.route

    NavigationBar {
        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                selected  = current == screen.route,
                onClick   = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
                icon  = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) }
            )
        }
    }
}
