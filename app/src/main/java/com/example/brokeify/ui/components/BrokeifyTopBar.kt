package com.example.brokeify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BrokeifyTopBar(
    onOpenCarMode: () -> Unit,
    onOpenAudioLab: () -> Unit,
    onOpenThemes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleBarBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF090D16))
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(titleBarBrush)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("brokeify_top_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Branding & Status LED
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF00FF66).copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)),
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

                Column {
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
                                .background(Color(0xFF00FF66), shape = CircleShape)
                        )
                    }
                    Text(
                        text = "SELF-HOSTED STEREO RACK",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Action Buttons (Car Mode, Audio Lab, Themes)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Car Mode Button
                IconButton(
                    onClick = onOpenCarMode,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF00FF66).copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp))
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
                        .size(44.dp)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp))
                        .testTag("top_bar_audiolab_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Audio Lab Equalizer",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Themes Button
                IconButton(
                    onClick = onOpenThemes,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp))
                        .testTag("top_bar_themes_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Visual Themes",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Spite Tagline Sub-Bar
        Text(
            text = "Zero subscriptions. Powered by spite and hard drives.",
            color = Color(0xFF64748B),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontFamily = FontFamily.Monospace
        )
    }
}
