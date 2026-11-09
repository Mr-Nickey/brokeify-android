package com.example.brokeify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brokeify.model.EQPresetName
import com.example.brokeify.viewmodel.BrokeifyViewModel

@Composable
fun CarModeScreen(
    viewModel: BrokeifyViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val track = uiState.currentTrack

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040609))
            .padding(20.dp)
            .testTag("car_mode_screen"),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cockpit Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D131C), shape = RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFF00FF66).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF00FF66), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = Color(0xFF070A0F),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "CAR DECK // ROAD SAFETY MODE",
                        color = Color(0xFF00FF66),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "High-Tactile Cockpit Controls",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            // Exit Button
            Button(
                onClick = { viewModel.toggleCarMode() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("car_mode_exit_btn")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Exit Car Mode", tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("EXIT", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        // Giant Track Display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            // Speedometer-style Stream Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF00FF66).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Text(
                        text = track?.format ?: "FLAC LOSSLESS",
                        color = Color(0xFF00FF66),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = if (uiState.audioPrefs.carVolumeLeveler) Color(0xFFF59E0B) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (uiState.audioPrefs.carVolumeLeveler) "ROAD NOISE COMP: ON" else "ROAD NOISE COMP: OFF",
                        color = if (uiState.audioPrefs.carVolumeLeveler) Color(0xFF0C0804) else Color(0xFF64748B),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = track?.title ?: "Select Track",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${track?.artist ?: ""} — ${track?.album ?: ""}",
                color = Color(0xFF00FF66),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = track?.sourceName ?: "",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Giant Driving Transport Controls (Massive 80dp play button, 64dp next/prev)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous
            IconButton(
                onClick = { viewModel.skipPrevious() },
                modifier = Modifier
                    .size(68.dp)
                    .background(Color(0xFF0D131C), shape = CircleShape)
                    .border(2.dp, Color(0xFF27272A), CircleShape)
                    .testTag("car_mode_prev_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Track",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Giant Play/Pause
            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier
                    .size(88.dp)
                    .background(Color(0xFF00FF66), shape = CircleShape)
                    .testTag("car_mode_play_pause_btn")
            ) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                    tint = Color(0xFF070A0F),
                    modifier = Modifier.size(52.dp)
                )
            }

            // Next
            IconButton(
                onClick = { viewModel.skipNext() },
                modifier = Modifier
                    .size(68.dp)
                    .background(Color(0xFF0D131C), shape = CircleShape)
                    .border(2.dp, Color(0xFF27272A), CircleShape)
                    .testTag("car_mode_next_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Track",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        // Cockpit Quick Presets
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "DRIVING EQUALIZER PRESET",
                color = Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val carPresets = listOf(
                    EQPresetName.CAR_STEREO,
                    EQPresetName.BASS_BOOST,
                    EQPresetName.ROCK,
                    EQPresetName.LATE_NIGHT
                )
                carPresets.forEach { preset ->
                    val isSelected = uiState.audioPrefs.eqPreset == preset
                    Button(
                        onClick = { viewModel.setEqPreset(preset) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFF00FF66) else Color(0xFF0D131C)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = if (!isSelected) ButtonDefaults.outlinedButtonBorder else null
                    ) {
                        Text(
                            text = preset.displayName,
                            color = if (isSelected) Color(0xFF070A0F) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
