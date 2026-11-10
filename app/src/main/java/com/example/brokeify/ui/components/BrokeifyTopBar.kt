package com.example.brokeify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brokeify.model.ProviderStatus

@Composable
fun BrokeifyTopBar(
    serverStatus: ProviderStatus,
    serverName: String,
    onOpenCarMode: () -> Unit,
    onOpenAudioLab: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenConnect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val titleBarBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF070A0F))
    )

    val isConnected = serverStatus == ProviderStatus.CONNECTED
    val statusColor = if (isConnected) Color(0xFF00FF66) else Color(0xFFF59E0B)
    val statusText = if (isConnected) {
        if (serverName.isNotBlank()) serverName else "Connected"
    } else {
        "Tap to Link Server (6-Digit / QR)"
    }

    // Outer Column fills behind transparent status bar on full-screen phones (e.g., Pixel 9a)
    // while WindowInsets.safeDrawing pushes interactive content cleanly below the clock, Wi-Fi,
    // battery icons, and camera cutout on both modern edge-to-edge phones and older Android devices.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(titleBarBrush)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
            .testTag("brokeify_top_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Branding & Tappable Server Link Status Indicator
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOpenConnect() }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF00FF66).copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BK",
                        color = Color(0xFF00FF66),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "BROKEIFY",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(statusColor, shape = CircleShape)
                        )
                    }
                    Text(
                        text = statusText,
                        color = if (isConnected) Color(0xFF00FF66) else Color(0xFFF59E0B),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Functional Action Buttons (48dp touch targets, safely below status bar icons)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Connect / QR Pair Shortcut Button
                IconButton(
                    onClick = onOpenConnect,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFA855F7).copy(alpha = 0.18f), shape = RoundedCornerShape(12.dp))
                        .testTag("top_bar_connect_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Connect Android App (6-Digit / QR)",
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Car Mode Button
                IconButton(
                    onClick = onOpenCarMode,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF00FF66).copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                        .testTag("top_bar_car_mode_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Car Driving Mode",
                        tint = Color(0xFF00FF66),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Audio Lab Button
                IconButton(
                    onClick = onOpenAudioLab,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                        .testTag("top_bar_audiolab_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Equalizer",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Themes Button
                IconButton(
                    onClick = onOpenThemes,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                        .testTag("top_bar_themes_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Themes",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
