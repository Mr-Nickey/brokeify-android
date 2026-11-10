package com.example.brokeify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brokeify.model.LogLevel
import com.example.brokeify.model.ProviderStatus
import com.example.brokeify.viewmodel.BrokeifyViewModel

@Composable
fun DebugLogsScreen(
    viewModel: BrokeifyViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val config = uiState.gatewayConfig
    val isConnected = config.status == ProviderStatus.CONNECTED

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A0F))
            .padding(16.dp)
            .testTag("debug_logs_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "SYSTEM & NETWORK DIAGNOSTICS",
                color = Color(0xFF00FF66),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "Live event stream and active server connection diagnostics.",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }

        // Brokeify Gateway Connection Diagnostic Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D131C)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ACTIVE GATEWAY LINK",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (isConnected) {
                        "• Gateway: Connected to ${config.name} (${config.serverUrl})"
                    } else {
                        "• Gateway: Disconnected"
                    },
                    color = if (isConnected) Color(0xFF00FF66) else Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (isConnected && config.latencyMs > 0) {
                    Text(
                        text = "• Ping: ${config.latencyMs} ms latency",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "• Indexed Library: ${uiState.tracks.size} tracks, ${uiState.playlists.size} playlists",
                    color = Color(0xFF00FF66),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Live Log Entries
        if (uiState.debugLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No diagnostic events logged yet.",
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.debugLogs) { log ->
                    val levelColor = when (log.level) {
                        LogLevel.SUCCESS -> Color(0xFF00FF66)
                        LogLevel.INFO -> Color(0xFF38BDF8)
                        LogLevel.WARN -> Color(0xFFF59E0B)
                        LogLevel.ERROR -> Color(0xFFEF4444)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D131C)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = log.timestamp,
                                color = Color(0xFF64748B),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                            Surface(
                                color = levelColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = log.category.uppercase(),
                                    color = levelColor,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = log.message,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
