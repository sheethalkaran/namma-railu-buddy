package com.nammarailu.buddy

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity          // ← REQUIRED for locale switching
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.nammarailu.buddy.ui.navigation.*
import com.nammarailu.buddy.ui.screens.alarm.AlarmScreen
import com.nammarailu.buddy.ui.screens.coach.CoachPositionScreen
import com.nammarailu.buddy.ui.screens.home.HomeScreen
import com.nammarailu.buddy.ui.screens.home.StationSelectionScreen
import com.nammarailu.buddy.ui.screens.livestation.LiveStationScreen
import com.nammarailu.buddy.ui.screens.platform.PlatformPingScreen
import com.nammarailu.buddy.ui.screens.settings.SettingsScreen
import com.nammarailu.buddy.ui.theme.NammaRailuTheme
import com.nammarailu.buddy.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val settings by settingsVm.state.collectAsState()
            val openAlarm = intent.getBooleanExtra("open_alarm_screen", false)
            val startDest = if (openAlarm) Screen.Alarm.route else Screen.Home.route

            NammaRailuTheme(darkTheme = settings.isDarkMode) {
                AppNavHost(startDest = startDest)
            }
        }
    }
}

@Composable
fun AppNavHost(startDest: String = Screen.Home.route) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    val showBottomBar = currentRoute in listOf(Screen.Home.route, Screen.Alarm.route, Screen.Settings.route)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { if (showBottomBar) RailuBottomBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToLive = { id -> navController.navigate(Screen.LiveStation.createRoute(id)) },
                    onNavigateToStationSelect = { mode -> navController.navigate("select_station/$mode") }
                )
            }
            composable(
                "select_station/{mode}",
                arguments = listOf(navArgument("mode") { type = NavType.StringType })
            ) { backStackEntry ->
                val mode = backStackEntry.arguments?.getString("mode") ?: "platform"
                val title = if (mode == "platform") stringResource(R.string.platform_ping)
                else stringResource(R.string.coach_map)
                StationSelectionScreen(
                    title = title,
                    onStationSelected = { id -> navController.navigate(Screen.LiveStation.createRoute(id)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                "live/{stationId}",
                arguments = listOf(navArgument("stationId") { type = NavType.StringType })
            ) {
                LiveStationScreen(
                    onNavigateToPlatform = { tId, sId -> navController.navigate(Screen.Platform.createRoute(tId, sId)) },
                    onNavigateToCoach = { tId -> navController.navigate(Screen.CoachPos.createRoute(tId)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                "platform/{trainId}/{stationId}",
                arguments = listOf(
                    navArgument("trainId")   { type = NavType.StringType },
                    navArgument("stationId") { type = NavType.StringType }
                )
            ) { PlatformPingScreen(onBack = { navController.popBackStack() }) }
            composable(
                "coach/{trainId}",
                arguments = listOf(navArgument("trainId") { type = NavType.StringType })
            ) { CoachPositionScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.Alarm.route)    { AlarmScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
