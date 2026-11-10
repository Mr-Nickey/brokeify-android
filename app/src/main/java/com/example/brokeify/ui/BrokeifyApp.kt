package com.example.brokeify.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brokeify.model.ProviderStatus
import com.example.brokeify.ui.components.BrokeifyTopBar
import com.example.brokeify.ui.components.MiniPlayerBar
import com.example.brokeify.ui.screens.*
import com.example.brokeify.ui.theme.BrokeifyTheme
import com.example.brokeify.viewmodel.BrokeifyViewModel

sealed class NavItem(val route: String, val label: String, val icon: ImageVector) {
    object Library : NavItem("library", "Library", Icons.Default.LibraryMusic)
    object NowPlaying : NavItem("nowplaying", "Player", Icons.Default.PlayCircle)
    object Liked : NavItem("liked", "Liked", Icons.Default.Favorite)
    object Settings : NavItem("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun BrokeifyApp(
    viewModel: BrokeifyViewModel = viewModel(),
    deepLinkPayload: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isConnected = uiState.gatewayConfig.status == ProviderStatus.CONNECTED

    // Automatically process brokeify://pair?... deep link when scanned from camera
    LaunchedEffect(deepLinkPayload) {
        if (!deepLinkPayload.isNullOrBlank()) {
            viewModel.openSetupWizard()
            viewModel.connectWithQrPayload(deepLinkPayload, uiState.gatewayConfig.serverUrl)
            onDeepLinkConsumed()
        }
    }

    BrokeifyTheme(visualTheme = uiState.activeTheme) {
        // Back navigation handler
        BackHandler(enabled = uiState.currentScreen != "library" || uiState.isCarMode || uiState.isReRunningSetup) {
            if (uiState.isCarMode) {
                viewModel.toggleCarMode()
            } else if (uiState.isReRunningSetup) {
                viewModel.closeSetupWizard()
            } else {
                viewModel.navigateTo("library")
            }
        }

        if (uiState.isCarMode) {
            // Fullscreen Car Cockpit Mode
            CarModeScreen(viewModel = viewModel)
        } else if (!isConnected || uiState.isReRunningSetup) {
            // Onboarding Setup Wizard (Connect with 6-digit code or QR code)
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("brokeify_onboarding_scaffold"),
                contentWindowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                ),
                containerColor = Color(0xFF070A0F)
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    SourcesScreen(viewModel = viewModel)
                }
            }
        } else {
            // Main Connected Audio Cockpit
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("brokeify_main_scaffold"),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    BrokeifyTopBar(
                        serverStatus = uiState.gatewayConfig.status,
                        serverName = uiState.gatewayConfig.name,
                        onOpenCarMode = { viewModel.toggleCarMode() },
                        onOpenAudioLab = { viewModel.navigateTo("settings") },
                        onOpenThemes = { viewModel.navigateTo("settings") },
                        onOpenConnect = { viewModel.openSetupWizard() }
                    )
                },
                bottomBar = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Ultra-slim Mini player bar: only shown when not dismissed, not on now playing screen, and when track exists
                        if (!uiState.isMiniPlayerDismissed && uiState.currentScreen != "nowplaying" && uiState.currentTrack != null) {
                            MiniPlayerBar(
                                currentTrack = uiState.currentTrack,
                                isPlaying = uiState.isPlaying,
                                positionSec = uiState.positionSec,
                                onToggleLike = {
                                    uiState.currentTrack?.let { viewModel.toggleLikeTrack(it.id) }
                                },
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onSkipNext = { viewModel.skipNext() },
                                onOpenNowPlaying = { viewModel.navigateTo("nowplaying") },
                                onDismiss = { viewModel.dismissMiniPlayer() }
                            )
                        }

                        // Bottom Navigation Bar (Library, Player, Liked, Settings)
                        NavigationBar(
                            containerColor = Color(0xFF090D16),
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(
                                        WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                                    )
                                )
                                .testTag("brokeify_bottom_nav")
                        ) {
                            val items = listOf(
                                NavItem.Library,
                                NavItem.NowPlaying,
                                NavItem.Liked,
                                NavItem.Settings
                            )

                            items.forEach { item ->
                                val isSelected = when (item) {
                                    NavItem.Library -> uiState.currentScreen == "library" && uiState.activeLibraryTab != 1
                                    NavItem.NowPlaying -> uiState.currentScreen == "nowplaying"
                                    NavItem.Liked -> uiState.currentScreen == "liked" || (uiState.currentScreen == "library" && uiState.activeLibraryTab == 1)
                                    NavItem.Settings -> uiState.currentScreen in listOf("settings", "audiolab", "themes", "stats", "debug")
                                }

                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        when (item) {
                                            NavItem.Liked -> {
                                                viewModel.setLibraryTab(1)
                                                viewModel.navigateTo("library")
                                            }
                                            NavItem.Library -> {
                                                if (uiState.activeLibraryTab == 1) {
                                                    viewModel.setLibraryTab(0)
                                                }
                                                viewModel.navigateTo("library")
                                            }
                                            else -> viewModel.navigateTo(item.route)
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF00FF66),
                                        selectedTextColor = Color(0xFF00FF66),
                                        indicatorColor = Color(0xFF00FF66).copy(alpha = 0.15f),
                                        unselectedIconColor = Color(0xFF64748B),
                                        unselectedTextColor = Color(0xFF64748B)
                                    ),
                                    modifier = Modifier.testTag("nav_item_${item.route}")
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (uiState.currentScreen) {
                        "library" -> LibraryScreen(viewModel = viewModel)
                        "nowplaying" -> NowPlayingScreen(viewModel = viewModel)
                        "liked" -> LibraryScreen(viewModel = viewModel)
                        "settings" -> SettingsScreen(viewModel = viewModel)
                        "audiolab" -> SettingsScreen(viewModel = viewModel)
                        "themes" -> SettingsScreen(viewModel = viewModel)
                        "stats" -> StatsScreen(viewModel = viewModel)
                        "debug" -> DebugLogsScreen(viewModel = viewModel)
                        "sources" -> SourcesScreen(viewModel = viewModel)
                        else -> LibraryScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
