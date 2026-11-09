package com.example.brokeify.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import com.example.brokeify.ui.components.BrokeifyTopBar
import com.example.brokeify.ui.components.MiniPlayerBar
import com.example.brokeify.ui.screens.*
import com.example.brokeify.ui.theme.BrokeifyTheme
import com.example.brokeify.viewmodel.BrokeifyViewModel

sealed class NavItem(val route: String, val label: String, val icon: ImageVector) {
    object Library : NavItem("library", "Library", Icons.Default.LibraryMusic)
    object NowPlaying : NavItem("nowplaying", "Player", Icons.Default.PlayCircle)
    object Discover : NavItem("discover", "Discover", Icons.Default.TravelExplore)
    object Sources : NavItem("sources", "Sources", Icons.Default.Dns)
    object AudioLab : NavItem("audiolab", "Audio Lab", Icons.Default.Tune)
    object Stats : NavItem("stats", "Stats", Icons.Default.BarChart)
    object Debug : NavItem("debug", "Debug", Icons.Default.BugReport)
}

@Composable
fun BrokeifyApp(
    viewModel: BrokeifyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BrokeifyTheme(visualTheme = uiState.activeTheme) {
        // Handle Android system back button
        BackHandler(enabled = uiState.currentScreen != "library" || uiState.isCarMode) {
            if (uiState.isCarMode) {
                viewModel.toggleCarMode()
            } else {
                viewModel.navigateTo("library")
            }
        }

        if (uiState.isCarMode) {
            // Fullscreen Car Cockpit Mode
            CarModeScreen(viewModel = viewModel)
        } else {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("brokeify_main_scaffold"),
                topBar = {
                    BrokeifyTopBar(
                        onOpenCarMode = { viewModel.toggleCarMode() },
                        onOpenAudioLab = { viewModel.navigateTo("audiolab") },
                        onOpenThemes = { viewModel.navigateTo("themes") }
                    )
                },
                bottomBar = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Mini player bar only shown when NOT on now playing screen
                        if (uiState.currentScreen != "nowplaying") {
                            MiniPlayerBar(
                                currentTrack = uiState.currentTrack,
                                isPlaying = uiState.isPlaying,
                                positionSec = uiState.positionSec,
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onSkipNext = { viewModel.skipNext() },
                                onOpenNowPlaying = { viewModel.navigateTo("nowplaying") }
                            )
                        }

                        // Bottom Navigation Bar
                        NavigationBar(
                            containerColor = Color(0xFF090D16),
                            tonalElevation = 8.dp,
                            modifier = Modifier.testTag("brokeify_bottom_nav")
                        ) {
                            val items = listOf(
                                NavItem.Library,
                                NavItem.NowPlaying,
                                NavItem.Discover,
                                NavItem.Sources,
                                NavItem.AudioLab,
                                NavItem.Stats
                            )

                            items.forEach { item ->
                                val isSelected = uiState.currentScreen == item.route
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { viewModel.navigateTo(item.route) },
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
                        "discover" -> DiscoverScreen(viewModel = viewModel)
                        "sources" -> SourcesScreen(viewModel = viewModel)
                        "audiolab" -> AudioLabScreen(viewModel = viewModel)
                        "themes" -> ThemesScreen(viewModel = viewModel)
                        "stats" -> StatsScreen(viewModel = viewModel)
                        "debug" -> DebugLogsScreen(viewModel = viewModel)
                        else -> LibraryScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
