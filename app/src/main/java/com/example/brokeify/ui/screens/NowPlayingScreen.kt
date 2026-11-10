package com.example.brokeify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.brokeify.model.RepeatMode
import com.example.brokeify.ui.components.RetroSpectrumVisualizer
import com.example.brokeify.viewmodel.BrokeifyViewModel

@Composable
fun NowPlayingScreen(
    viewModel: BrokeifyViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val track = uiState.currentTrack
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A0F))
            .verticalScroll(scrollState)
            .padding(14.dp)
            .testTag("now_playing_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (track == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D131C)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No track currently playing",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Select a song from your Library or connect your Brokeify Gateway.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.navigateTo("library") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Open Library", color = Color(0xFF070A0F), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            return
        }

        // Sleek & Compact Hi-Fi Deck Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D131C)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {

                // 1. WHAT IS PLAYING WHERE Header Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF05080E), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF00FF66), CircleShape)
                                )
                                Text(
                                    text = "DOCKER STREAM SOURCE",
                                    color = Color(0xFF00FF66),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = uiState.gatewayConfig.serverUrl.ifBlank { "Local Cache" },
                                color = Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Queue position & Format pill
                        Column(horizontalAlignment = Alignment.End) {
                            val queueTotal = uiState.queue.size
                            val currentIdx = uiState.currentIndex + 1
                            Text(
                                text = "TRACK $currentIdx OF $queueTotal",
                                color = Color(0xFF38BDF8),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(3.dp)
                                ) {
                                    Text(
                                        text = track.format.ifBlank { "FLAC" },
                                        color = Color(0xFF00FF66),
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(3.dp)
                                ) {
                                    Text(
                                        text = "${track.sampleRate}",
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Compact 28dp Spectrum Visualizer
                RetroSpectrumVisualizer(
                    isPlaying = uiState.isPlaying,
                    accentColor = Color(0xFF00FF66)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Compact Vinyl Center Art (88dp height instead of huge 180dp box)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .background(Color(track.accentColor), shape = RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF070A0F), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "♪",
                                color = Color(0xFF00FF66),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = track.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            color = Color(0xFF00FF66),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.album,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Like Button (48dp touch target)
                    IconButton(
                        onClick = { viewModel.toggleLikeTrack(track.id) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("now_playing_like_btn")
                    ) {
                        Icon(
                            imageVector = if (track.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (track.isLiked) "Unlike Track" else "Like Track",
                            tint = if (track.isLiked) Color(0xFFEC4899) else Color(0xFF64748B),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Compact Scrubber Slider & Timing
                val duration = if (track.isLiveStream) 3600 else track.durationSec.coerceAtLeast(1)
                Slider(
                    value = uiState.positionSec.toFloat().coerceIn(0f, duration.toFloat()),
                    onValueChange = { viewModel.seekTo(it.toInt()) },
                    valueRange = 0f..duration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00FF66),
                        activeTrackColor = Color(0xFF00FF66),
                        inactiveTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .testTag("now_playing_scrubber")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(uiState.positionSec),
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = formatDuration(track.durationSec),
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Tactile Transport Controls (48dp touch targets)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(
                        onClick = { viewModel.toggleShuffle() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("now_playing_shuffle_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (uiState.shuffleEnabled) Color(0xFF00FF66) else Color(0xFF64748B),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Skip Previous
                    IconButton(
                        onClick = { viewModel.skipPrevious() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("now_playing_prev_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color(0xFFE2E8F0),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Play/Pause Big Round Button
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFF00FF66), shape = CircleShape)
                            .testTag("now_playing_play_pause_btn")
                    ) {
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                            tint = Color(0xFF070A0F),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Skip Next
                    IconButton(
                        onClick = { viewModel.skipNext() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("now_playing_next_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color(0xFFE2E8F0),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Repeat Mode
                    IconButton(
                        onClick = { viewModel.cycleRepeat() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("now_playing_repeat_btn")
                    ) {
                        Icon(
                            imageVector = when (uiState.repeatMode) {
                                RepeatMode.OFF -> Icons.Default.Repeat
                                RepeatMode.ALL -> Icons.Default.Repeat
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                            },
                            contentDescription = "Repeat",
                            tint = if (uiState.repeatMode != RepeatMode.OFF) Color(0xFF00FF66) else Color(0xFF64748B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 6. Fast 1-Tap Quick EQ Chips directly on Now Playing
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EQ:",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    val quickPresets = listOf(
                        EQPresetName.FLAT,
                        EQPresetName.CAR_STEREO,
                        EQPresetName.BASS_BOOST,
                        EQPresetName.ROCK,
                        EQPresetName.LATE_NIGHT
                    )
                    quickPresets.forEach { preset ->
                        val isSelected = uiState.audioPrefs.eqPreset == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setEqPreset(preset) },
                            label = {
                                Text(
                                    text = preset.displayName,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00FF66).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF00FF66),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%d:%02d", m, s)
}
