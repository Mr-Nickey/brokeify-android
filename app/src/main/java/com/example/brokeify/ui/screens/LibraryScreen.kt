package com.example.brokeify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brokeify.model.Playlist
import com.example.brokeify.model.ProviderStatus
import com.example.brokeify.model.Track
import com.example.brokeify.viewmodel.BrokeifyViewModel

@Composable
fun LibraryScreen(
    viewModel: BrokeifyViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isConnected = uiState.gatewayConfig.status == ProviderStatus.CONNECTED
    val subTabs = listOf(
        "Playlists (${uiState.playlists.size})",
        "Liked (${uiState.likedTrackIds.size + uiState.favoritePlaylistIds.size})",
        "Songs (${uiState.tracks.size})",
        "Albums",
        "Artists"
    )

    var quickServerUrl by remember(uiState.gatewayConfig.serverUrl) {
        mutableStateOf(
            if (uiState.gatewayConfig.serverUrl.isNotBlank()) uiState.gatewayConfig.serverUrl
            else "https://music.flockingreeds.farm"
        )
    }
    var quickPin by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A0F))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("library_screen")
    ) {
        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("library_search_input"),
            placeholder = { Text("Search songs, artists, albums...", color = Color(0xFF64748B)) },
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

        Spacer(modifier = Modifier.height(10.dp))

        // Scrollable Sub Tabs (Songs, Liked, Albums, Artists, Playlists)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
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
                        selectedContainerColor = if (index == 1) Color(0xFFEC4899).copy(alpha = 0.2f) else Color(0xFF00FF66).copy(alpha = 0.2f),
                        selectedLabelColor = if (index == 1) Color(0xFFEC4899) else Color(0xFF00FF66),
                        containerColor = Color(0xFF0D131C),
                        labelColor = Color(0xFF94A3B8)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) {
                            if (index == 1) Color(0xFFEC4899) else Color(0xFF00FF66)
                        } else Color(0xFF1E293B),
                        enabled = true,
                        selected = isSelected
                    ),
                    modifier = Modifier.testTag("library_tab_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Empty State with instant 6-Digit Quick Connect & QR Scanner shortcut when not connected
        if (!isConnected || uiState.tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFA855F7).copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D131C)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(Color(0xFF00FF66).copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = Color(0xFF00FF66),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = if (!isConnected) "Link Your Brokeify Docker Server" else "No music found in library",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (!isConnected) {
                                "Enter your Server URL + the 6-digit Quick Connect code from your Brokeify Web UI, or scan the QR code."
                            } else {
                                "Ensure your Brokeify Docker container has indexed tracks in /music, or tap Refresh in Connect."
                            },
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        if (!isConnected) {
                            OutlinedTextField(
                                value = quickServerUrl,
                                onValueChange = { quickServerUrl = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Server URL") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00FF66),
                                    unfocusedBorderColor = Color(0xFF1E293B),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = Color(0xFF00FF66),
                                    unfocusedLabelColor = Color(0xFF94A3B8)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                            )

                            OutlinedTextField(
                                value = quickPin,
                                onValueChange = { raw ->
                                    val digits = raw.filter { it.isDigit() }.take(6)
                                    quickPin = if (digits.length > 3) "${digits.take(3)} ${digits.drop(3)}" else digits
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("6-Digit Quick Connect Code") },
                                placeholder = {
                                    Text(
                                        "543 269",
                                        color = Color(0xFF475569),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                textStyle = TextStyle(
                                    color = Color(0xFF00FF66),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    letterSpacing = 3.sp
                                ),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFA855F7),
                                    unfocusedBorderColor = Color(0xFF3B0764),
                                    focusedTextColor = Color(0xFF00FF66),
                                    unfocusedTextColor = Color(0xFF00FF66),
                                    focusedLabelColor = Color(0xFFD8B4FE),
                                    unfocusedLabelColor = Color(0xFF94A3B8)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                            )

                            if (!uiState.pairingFeedbackMessage.isNullOrBlank()) {
                                Text(
                                    text = uiState.pairingFeedbackMessage!!,
                                    color = if (uiState.isPairingError) Color(0xFFFCA5A5) else Color(0xFF00FF66),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.connectWithSixDigitCode(quickServerUrl, quickPin)
                                    },
                                    enabled = quickServerUrl.isNotBlank() &&
                                        quickPin.count { it.isDigit() } == 6 &&
                                        !uiState.isSyncing,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    if (uiState.isSyncing) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text(
                                            text = "Link 6-Digit",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = { viewModel.navigateTo("sources") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("library_configure_sources_btn")
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF070A0F), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Scan QR",
                                        color = Color(0xFF070A0F),
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = { viewModel.navigateTo("sources") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(48.dp)
                                    .testTag("library_configure_sources_btn")
                            ) {
                                Text(
                                    text = "Open Server Connection",
                                    color = Color(0xFF070A0F),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
            return
        }

        // Filtered Tracks
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
                // Playlists List (First Tab!)
                val sortedPlaylists = remember(uiState.playlists, uiState.favoritePlaylistIds, uiState.searchQuery) {
                    val base = if (uiState.searchQuery.isBlank()) uiState.playlists
                    else uiState.playlists.filter {
                        it.name.contains(uiState.searchQuery, ignoreCase = true) ||
                            it.description.contains(uiState.searchQuery, ignoreCase = true)
                    }
                    base.sortedByDescending { it.isFavorite || uiState.favoritePlaylistIds.contains(it.id) }
                }

                if (sortedPlaylists.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No playlists found",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Playlists synced from your Brokeify Docker server will appear here.",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(sortedPlaylists, key = { it.id }) { playlist ->
                            val isFav = playlist.isFavorite || uiState.favoritePlaylistIds.contains(playlist.id)
                            PlaylistCard(
                                playlist = playlist,
                                isFavorite = isFav,
                                onToggleFavorite = { viewModel.toggleFavoritePlaylist(playlist.id) },
                                onPlay = {
                                    val playlistTracks = uiState.tracks.filter { it.id in playlist.trackIds }
                                    val firstTrack = playlistTracks.firstOrNull() ?: uiState.tracks.firstOrNull()
                                    if (firstTrack != null) {
                                        viewModel.playTrack(
                                            firstTrack,
                                            playlistTracks.ifEmpty { uiState.tracks }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
            1 -> {
                // Liked / Favorites List (Favorite Playlists + Liked Songs synced with Docker!)
                val favoritePlaylists = remember(uiState.playlists, uiState.favoritePlaylistIds) {
                    uiState.playlists.filter { it.isFavorite || uiState.favoritePlaylistIds.contains(it.id) }
                }
                val likedTracks = remember(filteredTracks, uiState.likedTrackIds) {
                    filteredTracks.filter { it.isLiked || uiState.likedTrackIds.contains(it.id) }
                }

                if (favoritePlaylists.isEmpty() && likedTracks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = Color(0xFFEC4899),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No favorites or liked songs yet",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Favorite playlists and songs with heart/star icons to sync them with your Brokeify Docker database.",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (favoritePlaylists.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "FAVORITE PLAYLISTS (${favoritePlaylists.size})",
                                        color = Color(0xFFFBBF24),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            items(favoritePlaylists, key = { "fav_pl_${it.id}" }) { playlist ->
                                PlaylistCard(
                                    playlist = playlist,
                                    isFavorite = true,
                                    onToggleFavorite = { viewModel.toggleFavoritePlaylist(playlist.id) },
                                    onPlay = {
                                        val playlistTracks = uiState.tracks.filter { it.id in playlist.trackIds }
                                        val firstTrack = playlistTracks.firstOrNull() ?: uiState.tracks.firstOrNull()
                                        if (firstTrack != null) {
                                            viewModel.playTrack(firstTrack, playlistTracks.ifEmpty { uiState.tracks })
                                        }
                                    }
                                )
                            }
                        }

                        if (likedTracks.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "LIKED SONGS (${likedTracks.size})",
                                        color = Color(0xFFEC4899),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            items(likedTracks, key = { it.id }) { track ->
                                val isCurrent = uiState.currentTrack?.id == track.id
                                TrackRow(
                                    track = track,
                                    isCurrent = isCurrent,
                                    isPlaying = isCurrent && uiState.isPlaying,
                                    onSelect = { viewModel.playTrack(track, likedTracks) },
                                    onToggleLike = { viewModel.toggleLikeTrack(track.id) },
                                    isOffline = uiState.offlineTrackIds.contains(track.id),
                                    onToggleOffline = { viewModel.toggleOfflineDownload(track.id) }
                                )
                            }
                        }
                    }
                }
            }
            2 -> {
                // Songs List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTracks, key = { it.id }) { track ->
                        val isCurrent = uiState.currentTrack?.id == track.id
                        TrackRow(
                            track = track,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && uiState.isPlaying,
                            onSelect = { viewModel.playTrack(track, filteredTracks) },
                            onToggleLike = { viewModel.toggleLikeTrack(track.id) },
                            isOffline = uiState.offlineTrackIds.contains(track.id),
                            onToggleOffline = { viewModel.toggleOfflineDownload(track.id) }
                        )
                    }
                }
            }
            3 -> {
                // Albums List
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
                                tracks.firstOrNull()?.let { viewModel.playTrack(it, tracks) }
                            }
                        )
                    }
                }
            }
            4 -> {
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
                                tracks.firstOrNull()?.let { viewModel.playTrack(it, tracks) }
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
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color(playlist.coverAccent).copy(alpha = 0.2f), shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = Color(playlist.coverAccent),
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = playlist.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isFavorite) {
                        Surface(
                            color = Color(0xFFFBBF24).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "★ FAV",
                                color = Color(0xFFFBBF24),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = if (playlist.description.isNotBlank()) {
                        "${playlist.trackIds.size} tracks • ${playlist.description}"
                    } else {
                        "${playlist.trackIds.size} tracks"
                    },
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Favorite Star Button (Syncs with Docker!)
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("playlist_fav_btn_${playlist.id}")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                    tint = if (isFavorite) Color(0xFFFBBF24) else Color(0xFF64748B),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Play Button
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF00FF66).copy(alpha = 0.15f), shape = CircleShape)
                    .testTag("playlist_play_btn_${playlist.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Playlist",
                    tint = Color(0xFF00FF66),
                    modifier = Modifier.size(22.dp)
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
    onToggleLike: () -> Unit = {},
    isOffline: Boolean = false,
    onToggleOffline: () -> Unit = {},
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (isCurrent) Color(0xFF00FF66) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isCurrent) Color(0xFF070A0F) else Color(0xFF00FF66),
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
                    text = "${track.artist} • ${track.album}",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (track.format.isNotBlank()) {
                        Text(
                            text = track.format,
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (track.durationSec > 0) {
                        Text(
                            text = formatDuration(track.durationSec),
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (isOffline) {
                        Text(
                            text = "OFFLINE",
                            color = Color(0xFF00FF66),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Offline Download Button
            IconButton(
                onClick = onToggleOffline,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = if (isOffline) Icons.Default.CheckCircle else Icons.Default.Download,
                    contentDescription = if (isOffline) "Downloaded Offline" else "Download Offline",
                    tint = if (isOffline) Color(0xFF00FF66) else Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Like / Favorite Button synced with Docker server
            IconButton(
                onClick = onToggleLike,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("track_like_${track.id}")
            ) {
                Icon(
                    imageVector = if (track.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (track.isLiked) "Unlike Track" else "Like Track",
                    tint = if (track.isLiked) Color(0xFFEC4899) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
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
                    .size(48.dp)
                    .background(Color(0xFF38BDF8).copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Album, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(26.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = albumName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = "$artist • $trackCount tracks", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
            IconButton(onClick = onPlay, modifier = Modifier.size(48.dp)) {
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
                    .size(48.dp)
                    .background(Color(0xFFF59E0B).copy(alpha = 0.2f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(26.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = artistName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = "$trackCount tracks", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
            IconButton(onClick = onPlay, modifier = Modifier.size(48.dp)) {
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
