package com.example.brokeify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brokeify.model.SlskdJob
import com.example.brokeify.model.SlskdJobStatus
import com.example.brokeify.viewmodel.BrokeifyViewModel

@Composable
fun DiscoverScreen(
    viewModel: BrokeifyViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: Slskd Queue, 1: MusicBrainz Search, 2: ListenBrainz Playlists

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A0F))
            .padding(16.dp)
            .testTag("discover_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title
        Text(
            text = "DISCOVER // MUSICBRAINZ & SLSKD GRABBER",
            color = Color(0xFFF59E0B),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        // Subtabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Slskd Queue", "Catalog Search", "Discovery Mixes").forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedTab = index },
                    label = { Text(title, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFFF59E0B),
                        containerColor = Color(0xFF0D131C),
                        labelColor = Color(0xFF94A3B8)
                    )
                )
            }
        }

        when (selectedTab) {
            0 -> {
                // Slskd P2P Grab Queue
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE DOWNLOADS (${uiState.slskdJobs.size} JOBS)",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Auto-Organize: Picard Active",
                            color = Color(0xFF00FF66),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.slskdJobs) { job ->
                            SlskdJobCard(job = job)
                        }
                    }
                }
            }
            1 -> {
                // MusicBrainz Open Search
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        modifier = Modifier.fillMaxWidth().testTag("musicbrainz_input"),
                        placeholder = { Text("Search 35M+ releases on MusicBrainz...", color = Color(0xFF64748B)) },
                        leadingIcon = { Icon(Icons.Default.TravelExplore, contentDescription = null, tint = Color(0xFFF59E0B)) },
                        trailingIcon = {
                            if (searchInput.isNotBlank()) {
                                Button(
                                    onClick = {
                                        viewModel.queueDownloadJob(searchInput, "Catalog Artist")
                                        searchInput = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text("Grab FLAC", color = Color(0xFF070A0F), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedContainerColor = Color(0xFF0D131C),
                            unfocusedContainerColor = Color(0xFF0D131C),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D131C)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Zero-Subscription Music Freedom",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Type any song or album title to resolve metadata from MusicBrainz and automatically queue a lossless FLAC download via Slskd or Lidarr.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            2 -> {
                // ListenBrainz Discovery Playlists
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val mixes = listOf(
                        "Weekly Exploration: Darksynth & Retrowave",
                        "Spite Anthems: De-Streamed Classics",
                        "High-Bitrate Lossless Vault Essentials",
                        "San Francisco Underground Relays"
                    )
                    items(mixes) { title ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D131C)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Sensors, contentDescription = null, tint = Color(0xFF38BDF8))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(text = "ListenBrainz AI Curated • 25 Tracks", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                                IconButton(onClick = { viewModel.queueDownloadJob(title, "Various Artists") }) {
                                    Icon(Icons.Default.Download, contentDescription = "Grab All", tint = Color(0xFF00FF66))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SlskdJobCard(job: SlskdJob, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D131C)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = job.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "${job.artist} • Peer: ${job.peerUsername}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }

                Surface(
                    color = when (job.status) {
                        SlskdJobStatus.COMPLETED -> Color(0xFF00FF66).copy(alpha = 0.2f)
                        SlskdJobStatus.DOWNLOADING -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                        SlskdJobStatus.SEARCHING -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                        else -> Color(0xFF1E293B)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = job.status.label,
                        color = when (job.status) {
                            SlskdJobStatus.COMPLETED -> Color(0xFF00FF66)
                            SlskdJobStatus.DOWNLOADING -> Color(0xFF38BDF8)
                            SlskdJobStatus.SEARCHING -> Color(0xFFF59E0B)
                            else -> Color(0xFF94A3B8)
                        },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (job.status == SlskdJobStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { job.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF1E293B)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "${job.progressPercent}% complete", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(text = "${job.speedKbps} KB/s", color = Color(0xFF00FF66), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
