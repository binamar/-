package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.i18n.appStrings
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.FocusScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.RoutinesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScreen
import kotlinx.serialization.Serializable

@Serializable data object TodayRoute
@Serializable data object RoutinesRoute
@Serializable data object CalendarRoute
@Serializable data object InsightsRoute
@Serializable data object SettingsRoute
@Serializable data class FocusRoute(val taskId: Long? = null)

private data class TopLevelDestination(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun FlowDayRootShell() {
    val strings = appStrings()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isFocusRoute = currentDestination?.hasRoute<FocusRoute>() == true

    val topDestinations = listOf(
        TopLevelDestination(
            route = TodayRoute,
            label = strings.navToday,
            selectedIcon = Icons.Filled.Today,
            unselectedIcon = Icons.Outlined.Today,
            testTag = "nav_today"
        ),
        TopLevelDestination(
            route = RoutinesRoute,
            label = strings.navRoutines,
            selectedIcon = Icons.Filled.Repeat,
            unselectedIcon = Icons.Outlined.Repeat,
            testTag = "nav_routines"
        ),
        TopLevelDestination(
            route = CalendarRoute,
            label = strings.navCalendar,
            selectedIcon = Icons.Filled.CalendarMonth,
            unselectedIcon = Icons.Outlined.CalendarMonth,
            testTag = "nav_calendar"
        ),
        TopLevelDestination(
            route = InsightsRoute,
            label = strings.navInsights,
            selectedIcon = Icons.Filled.Insights,
            unselectedIcon = Icons.Outlined.Insights,
            testTag = "nav_insights"
        ),
        TopLevelDestination(
            route = SettingsRoute,
            label = strings.navSettings,
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            testTag = "nav_settings"
        )
    )

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp && !isFocusRoute

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail && !isFocusRoute) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        topDestinations.forEach { dest ->
                            val selected = currentDestination?.hasRoute(dest.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        topDestinations.forEach { dest ->
                            val selected = currentDestination?.hasRoute(dest.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = TodayRoute
                    ) {
                        composable<TodayRoute> {
                            TodayScreen(
                                onOpenFocus = { taskId ->
                                    navController.navigate(FocusRoute(taskId))
                                }
                            )
                        }
                        composable<RoutinesRoute> {
                            RoutinesScreen()
                        }
                        composable<CalendarRoute> {
                            CalendarScreen()
                        }
                        composable<InsightsRoute> {
                            InsightsScreen(
                                onOpenFocus = { taskId ->
                                    navController.navigate(FocusRoute(taskId))
                                }
                            )
                        }
                        composable<SettingsRoute> {
                            SettingsScreen()
                        }
                        composable<FocusRoute> { entry ->
                            val args = entry.toRoute<FocusRoute>()
                            FocusScreen(
                                initialTaskId = args.taskId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
