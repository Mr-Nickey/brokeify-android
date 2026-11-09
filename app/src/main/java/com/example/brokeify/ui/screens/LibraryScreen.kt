package com.example.brokeify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brokeify.model.Playlist
import com.example.brokeify.model.Track
import com.example.brokeify.viewmodel.BrokeifyViewModel

@Composable
fun LibraryScreen(
    viewModel: BrokeifyViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val subTabs = listOf("Playlists", "Songs", "Albums", "Artists")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A0F))
            .padding(16.dp)
            .testTag("library_screen")
    ) {
        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("library_search_input"),
            placeholder = { Text("Search local library & self-hosted servers...", color = Color(0xFF64748B)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF00FF66)) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00FF66),
                unfocusedBorderColor = Color(0xFF1E293B),
                focusedContainerColor = Color(0xFF0D131C),
                unfocusedContainerColor = Color(0xFF0D131C),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sub Tabs (Playlists, Songs, Albums, Artists)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subTabs.forEachIndexed { index, title ->
                val isSelected = uiState.activeLibraryTab == index
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setLibraryTab(index) },
                    label = {
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00FF66).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF00FF66),
                        containerColor = Color(0xFF0D131C),
                        labelColor = Color(0xFF94A3B8)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) Color(0xFF00FF66) else Color(0xFF1E293B),
                        enabled = true,
                        selected = isSelected
                    ),
                    modifier = Modifier.testTag("library_tab_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content based on tab
        val filteredTracks = remember(uiState.tracks, uiState.searchQuery) {
            if (uiState.searchQuery.isBlank()) uiState.tracks
            else uiState.tracks.filter {
                it.title.contains(uiState.searchQuery, ignoreCase = true) ||
                it.artist.contains(uiState.searchQuery, ignoreCase = true) ||
                it.album.contains(uiState.searchQuery, ignoreCase = true)
            }
        }

        when (uiState.activeLibraryTab) {
            0 -> {
                // Playlists List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.playlists) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onPlay = {
                                val firstTrack = uiState.tracks.find { it.id in playlist.trackIds }
                                if (firstTrack != null) viewModel.playTrack(firstTrack)
                            }
                        )
                    }
                }
            }
            1 -> {
                // Songs List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTracks) { track ->
                        val isCurrent = uiState.currentTrack?.id == track.id
                        TrackRow(
                            track = track,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && uiState.isPlaying,
                            onSelect = { viewModel.playTrack(track) },
                            onInstantMix = { viewModel.generateInstantMix(track) }
                        )
                    }
                }
            }
            2 -> {
                // Albums Grid / List
                val albums = remember(filteredTracks) {
                    filteredTracks.groupBy { it.album }.map { (album, tracks) ->
                        album to tracks
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(albums) { (albumName, tracks) ->
                        AlbumCard(
                            albumName = albumName,
                            artist = tracks.firstOrNull()?.artist ?: "Unknown Artist",
                            trackCount = tracks.size,
                            onPlay = {
                                tracks.firstOrNull()?.let { viewModel.playTrack(it) }
                            }
                        )
                    }
                }
            }
            3 -> {
                // Artists List
                val artists = remember(filteredTracks) {
                    filteredTracks.groupBy { it.artist }.map { (artist, tracks) ->
                        artist to tracks
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(artists) { (artistName, tracks) ->
                        ArtistCard(
                            artistName = artistName,
                            trackCount = tracks.size,
                            onPlay = {
                                tracks.firstOrNull()?.let { viewModel.playTrack(it) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("playlist_card_${playlist.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D131C)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(Color(playlist.coverAccent).copy(alpha = 0.2f), shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = Color(playlist.coverAccent),
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = playlist.description,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${playlist.trackIds.size} tracks • Cross-Resolved",
                    color = Color(0xFF00FF66),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF00FF66).copy(alpha = 0.15f), shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Playlist",
                    tint = Color(0xFF00FF66)
                )
            }
        }
    }
}

@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onInstantMix: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("track_row_${track.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFF0D131C)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (isCurrent) Color(0xFF00FF66) else Color(track.accentColor).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isCurrent) Color(0xFF070A0F) else Color(track.accentColor),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isCurrent) Color(0xFF00FF66) else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist} — ${track.album}",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = track.format,
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(text = "•", color = Color(0xFF475569), fontSize = 10.sp)
                    Text(
                        text = track.sourceName,
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Instant Mix Button
            IconButton(
                onClick = onInstantMix,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Instant Mix",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Duration
            Text(
                text = formatDuration(track.durationSec),
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun AlbumCard(
    albumName: String,
    artist: String,
    trackCount: Int,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D131C)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color(0xFF38BDF8).copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Album, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(26.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = albumName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = artist, color = Color(0xFF94A3B8), fontSize = 12.sp)
                Text(text = "$trackCount tracks • Lossless Ready", color = Color(0xFF00FF66), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            IconButton(onClick = onPlay) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play Album", tint = Color(0xFF00FF66))
            }
        }
    }
}

@Composable
fun ArtistCard(
    artistName: String,
    trackCount: Int,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D131C)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color(0xFFF59E0B).copy(alpha = 0.2f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(26.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = artistName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = "$trackCount tracks across servers", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
            IconButton(onClick = onPlay) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play Artist", tint = Color(0xFF00FF66))
            }
        }
    }
}

private fun formatDuration(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "$m:${s.toString().padStart(2, '0')}"
}
