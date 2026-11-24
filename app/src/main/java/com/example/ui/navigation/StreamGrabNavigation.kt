package com.example.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.QueueScreen
import com.example.ui.screens.ResolverScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.QueueViewModel
import com.example.ui.viewmodel.ResolverViewModel
import com.example.ui.viewmodel.SettingsViewModel
import kotlinx.serialization.Serializable

@Serializable data object ResolverRoute
@Serializable data object QueueRoute
@Serializable data object HistoryRoute
@Serializable data object SettingsRoute

private data class NavTabItem<T : Any>(
    val route: T,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamGrabRootShell(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    resolverViewModel: ResolverViewModel = hiltViewModel(),
    queueViewModel: QueueViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val resolverState by resolverViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.settings.collectAsStateWithLifecycle()
    val queueSummary by queueViewModel.queueState.collectAsStateWithLifecycle()
    val historyItems by historyViewModel.historyItems.collectAsStateWithLifecycle()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Graceful: downloads still run even if user declines notifications */ }
    )

    val ensureNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(resolverState.snackbarMessage) {
        val msg = resolverState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            resolverViewModel.clearSnackbar()
        }
    }

    val tabs = remember {
        listOf(
            NavTabItem(ResolverRoute, "المستخرج", Icons.Rounded.Link, Icons.Outlined.Link, "nav_resolver"),
            NavTabItem(QueueRoute, "الطابور", Icons.Rounded.Download, Icons.Outlined.Download, "nav_queue"),
            NavTabItem(HistoryRoute, "السجل", Icons.Rounded.History, Icons.Outlined.History, "nav_history"),
            NavTabItem(SettingsRoute, "الإعدادات", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination
    val activeBadgeCount = queueSummary.activeCount + queueSummary.queuedCount

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "StreamGrab • 360p Inspector",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                modifier = Modifier.testTag(tab.testTag),
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (tab.route is QueueRoute && activeBadgeCount > 0) {
                                                Badge { Text(activeBadgeCount.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = { Text(tab.label) }
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
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                modifier = Modifier.testTag(tab.testTag),
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (tab.route is QueueRoute && activeBadgeCount > 0) {
                                                Badge { Text(activeBadgeCount.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ResolverRoute,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp)
                    ) {
                        composable<ResolverRoute> {
                            ResolverScreen(
                                uiState = resolverState,
                                settings = settingsState,
                                onModeChange = resolverViewModel::setMode,
                                onSingleUrlChange = resolverViewModel::updateSingleUrl,
                                onBatchUrlsChange = resolverViewModel::updateBatchUrls,
                                onInspectSingle = { resolverViewModel.inspectSingleUrl() },
                                onRunOfflineLab = resolverViewModel::runOfflineLabSample,
                                onSendEpisodesToBatch = resolverViewModel::sendScrapedEpisodesToBatch,
                                onStartAutoBatch = {
                                    ensureNotificationPermission()
                                    resolverViewModel.startAutoBatchGrabber()
                                },
                                onCancelAutoBatch = resolverViewModel::cancelAutoBatch,
                                onPolicyChange = resolverViewModel::updateQualityPolicy,
                                onQueueVideo = { video ->
                                    ensureNotificationPermission()
                                    resolverViewModel.enqueueSingleVideo(video)
                                },
                                onShowDetails = resolverViewModel::openVideoDetails,
                                onToggleDiagnostics = resolverViewModel::setDiagnosticsSheetVisible,
                                onNotifyCopy = resolverViewModel::showMessage
                            )
                        }

                        composable<QueueRoute> {
                            QueueScreen(
                                summary = queueSummary,
                                onPause = queueViewModel::pause,
                                onResume = {
                                    ensureNotificationPermission()
                                    queueViewModel.resume(it)
                                },
                                onRetry = {
                                    ensureNotificationPermission()
                                    queueViewModel.retry(it)
                                },
                                onCancel = queueViewModel::cancel,
                                onDelete = queueViewModel::delete,
                                onPauseAll = queueViewModel::pauseAll,
                                onResumeAll = {
                                    ensureNotificationPermission()
                                    queueViewModel.resumeAll()
                                },
                                onClearFinished = queueViewModel::clearFinished
                            )
                        }

                        composable<HistoryRoute> {
                            HistoryScreen(
                                items = historyItems,
                                onReInspectUrl = { url ->
                                    navController.navigate(ResolverRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    resolverViewModel.inspectSingleUrl(url)
                                },
                                onDeleteItem = historyViewModel::deleteItem,
                                onClearAll = historyViewModel::clearAll
                            )
                        }

                        composable<SettingsRoute> {
                            SettingsScreen(
                                settings = settingsState,
                                onQualityPolicyChange = settingsViewModel::updateQualityPolicy,
                                onMaxConcurrentChange = settingsViewModel::updateMaxConcurrent,
                                onInterDelayChange = settingsViewModel::updateInterDelay,
                                onAutoRetryChange = settingsViewModel::updateAutoRetry,
                                onPreferSubtitlesChange = settingsViewModel::updatePreferSubtitles,
                                onUserAgentChange = settingsViewModel::updateUserAgent,
                                onDarkThemeChange = settingsViewModel::updateDarkTheme
                            )
                        }
                    }
                }
            }
        }
    }
}
