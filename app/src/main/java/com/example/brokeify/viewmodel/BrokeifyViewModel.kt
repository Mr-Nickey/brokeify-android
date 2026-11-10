package com.example.brokeify.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.example.brokeify.data.BrokeifyGatewayClient
import com.example.brokeify.data.HealthCheckResult
import com.example.brokeify.data.MusicRepository
import com.example.brokeify.data.local.BrokeifyDatabase
import com.example.brokeify.data.local.MusicCacheRepository
import com.example.brokeify.model.*
import com.example.brokeify.service.BrokeifyPlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class BrokeifyUiState(
    val tracks: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val likedTrackIds: Set<String> = emptySet(),
    val favoritePlaylistIds: Set<String> = emptySet(),
    val gatewayConfig: ProviderConfig = ProviderConfig(),
    val debugLogs: List<DebugLogEntry> = emptyList(),
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val positionSec: Int = 0,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val autoplayEnabled: Boolean = true,
    val audioPrefs: AudioPreferences = AudioPreferences(),
    val activeTheme: VisualTheme = VisualTheme.CYBER_DECK,
    val currentScreen: String = "library",
    val activeLibraryTab: Int = 0, // 0: Playlists, 1: Liked, 2: Songs, 3: Albums, 4: Artists
    val searchQuery: String = "",
    val totalListenSeconds: Long = 0L,
    val playbackHistory: List<TrackPlayStat> = emptyList(),
    val isCarMode: Boolean = false,
    val isSyncing: Boolean = false,
    val pairingFeedbackMessage: String? = null,
    val isPairingError: Boolean = false,
    val generatedPhonePin: String? = null,
    val isPollingPhonePin: Boolean = false,
    val isMiniPlayerDismissed: Boolean = false,
    val isReRunningSetup: Boolean = false,
    val offlineTrackIds: Set<String> = emptySet(),
    val isOfflineMode: Boolean = false,
    val cachedTrackCount: Int = 0,
    val cachedPlaylistCount: Int = 0,
    val recentlyPlayedTracks: List<Track> = emptyList()
) {
    val jellyfinConfig: ProviderConfig get() = gatewayConfig
}

class BrokeifyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository(application)
    private val secureStorage = repository.secureStorage
    private val cacheDatabase = BrokeifyDatabase.getInstance(application)
    val cacheRepository = MusicCacheRepository(cacheDatabase.musicCacheDao())

    private var exoPlayer: ExoPlayer? = null

    private val _uiState = MutableStateFlow(
        BrokeifyUiState(
            gatewayConfig = repository.loadSavedProvider(),
            totalListenSeconds = secureStorage.totalListenSeconds
        )
    )
    val uiState: StateFlow<BrokeifyUiState> = _uiState.asStateFlow()

    private var playbackTickerJob: Job? = null
    private var phonePinPollJob: Job? = null

    init {
        initExoPlayer(application)
        startPlaybackTicker()
        observeRoomCache()

        // If credentials exist, automatically attempt to fetch library from Gateway
        if (secureStorage.serverUrl.isNotBlank()) {
            refreshLibrary()
        }
    }

    private fun observeRoomCache() {
        // Observe local Room cache for offline playback and quick cold-start load
        viewModelScope.launch {
            cacheRepository.allCachedTracks.collect { cachedTracks ->
                if (cachedTracks.isNotEmpty()) {
                    _uiState.update { state ->
                        val shouldPopulate = state.tracks.isEmpty() || state.isOfflineMode ||
                            state.gatewayConfig.status != ProviderStatus.CONNECTED
                        state.copy(
                            tracks = if (shouldPopulate) cachedTracks else state.tracks,
                            cachedTrackCount = cachedTracks.size
                        )
                    }
                } else {
                    _uiState.update { it.copy(cachedTrackCount = 0) }
                }
            }
        }

        viewModelScope.launch {
            cacheRepository.allCachedPlaylists.collect { cachedPlaylists ->
                if (cachedPlaylists.isNotEmpty()) {
                    _uiState.update { state ->
                        val shouldPopulate = state.playlists.isEmpty() || state.isOfflineMode ||
                            state.gatewayConfig.status != ProviderStatus.CONNECTED
                        state.copy(
                            playlists = if (shouldPopulate) cachedPlaylists else state.playlists,
                            cachedPlaylistCount = cachedPlaylists.size
                        )
                    }
                } else {
                    _uiState.update { it.copy(cachedPlaylistCount = 0) }
                }
            }
        }

        viewModelScope.launch {
            cacheRepository.recentlyPlayedTracks.collect { recents ->
                _uiState.update { it.copy(recentlyPlayedTracks = recents) }
            }
        }
    }

    private fun initExoPlayer(app: Application) {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            // High buffer tolerance for weak cell reception, tunnels, and network drops (30s min, 120s max)
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    30_000,   // Min buffer: 30 seconds
                    120_000,  // Max buffer: 120 seconds ahead
                    2_500,    // Buffer for playback start: 2.5s
                    5_000     // Buffer after rebuffer: 5s
                )
                .build()

            exoPlayer = ExoPlayer.Builder(app)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setLoadControl(loadControl)
                .build()
                .apply {
                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _uiState.update { it.copy(isPlaying = isPlaying) }
                            val track = _uiState.value.currentTrack
                            val isLiked = track?.let { _uiState.value.likedTrackIds.contains(it.id) } ?: false
                            BrokeifyPlaybackService.startOrUpdate(app, track, isPlaying, isLiked)
                        }

                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            val trackId = mediaItem?.mediaId ?: return
                            val queue = _uiState.value.queue
                            val nextTrack = queue.find { it.id == trackId } ?: return
                            val newIndex = queue.indexOfFirst { it.id == trackId }

                            _uiState.update {
                                it.copy(
                                    currentTrack = nextTrack,
                                    currentIndex = if (newIndex >= 0) newIndex else it.currentIndex,
                                    positionSec = 0
                                )
                            }
                            val isLiked = _uiState.value.likedTrackIds.contains(nextTrack.id) || nextTrack.isLiked
                            BrokeifyPlaybackService.startOrUpdate(app, nextTrack, _uiState.value.isPlaying, isLiked)

                            viewModelScope.launch(Dispatchers.IO) {
                                cacheRepository.recordTrackPlayed(nextTrack)
                            }
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                handleTrackFinished()
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            logEvent(LogLevel.WARN, "ExoPlayer", "Stream audio error: ${error.localizedMessage}. Advancing to next track...")
                            viewModelScope.launch {
                                delay(300)
                                skipNext()
                            }
                        }
                    })
                }

            // Connect Foreground Service callbacks
            BrokeifyPlaybackService.playerProvider = { exoPlayer }
            BrokeifyPlaybackService.onPlayPause = { togglePlayPause() }
            BrokeifyPlaybackService.onNext = { skipNext() }
            BrokeifyPlaybackService.onPrev = { skipPrevious() }
            BrokeifyPlaybackService.onToggleLike = {
                _uiState.value.currentTrack?.let { toggleLikeTrack(it.id) }
            }
            BrokeifyPlaybackService.isLikedProvider = {
                _uiState.value.currentTrack?.let { _uiState.value.likedTrackIds.contains(it.id) } ?: false
            }
        } catch (e: Exception) {
            logEvent(LogLevel.WARN, "ExoPlayer", "Initialization note: ${e.localizedMessage}")
        }
    }

    private fun startPlaybackTicker() {
        playbackTickerJob?.cancel()
        playbackTickerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val player = exoPlayer
                if (player != null && player.isPlaying) {
                    val currentPosSec = (player.currentPosition / 1000).toInt()
                    val newTotal = _uiState.value.totalListenSeconds + 1
                    secureStorage.totalListenSeconds = newTotal

                    _uiState.update {
                        it.copy(
                            positionSec = currentPosSec,
                            totalListenSeconds = newTotal
                        )
                    }
                } else if (_uiState.value.isPlaying && _uiState.value.currentTrack != null) {
                    val track = _uiState.value.currentTrack!!
                    val maxDuration = if (track.isLiveStream) 3600 else track.durationSec
                    val nextPos = _uiState.value.positionSec + 1
                    val newTotal = _uiState.value.totalListenSeconds + 1
                    secureStorage.totalListenSeconds = newTotal

                    if (nextPos >= maxDuration && !track.isLiveStream && maxDuration > 0) {
                        handleTrackFinished()
                    } else {
                        _uiState.update {
                            it.copy(
                                positionSec = nextPos,
                                totalListenSeconds = newTotal
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleTrackFinished() {
        when (_uiState.value.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0)
                _uiState.value.currentTrack?.let { playTrack(it) }
            }
            RepeatMode.ALL -> skipNext()
            RepeatMode.OFF -> {
                if (_uiState.value.currentIndex < _uiState.value.queue.size - 1) {
                    skipNext()
                } else if (_uiState.value.autoplayEnabled) {
                    // Constant play: find more music from Docker library so it never stops!
                    enqueueAutoplayTracksAndContinue()
                } else {
                    exoPlayer?.pause()
                    _uiState.update { it.copy(isPlaying = false, positionSec = 0) }
                    val track = _uiState.value.currentTrack
                    val isLiked = track?.let { _uiState.value.likedTrackIds.contains(it.id) } ?: false
                    BrokeifyPlaybackService.startOrUpdate(getApplication(), track, false, isLiked)
                }
            }
        }
    }

    private fun enqueueAutoplayTracksAndContinue() {
        val allTracks = _uiState.value.tracks
        if (allTracks.isEmpty()) return

        val currentQueueIds = _uiState.value.queue.map { it.id }.toSet()
        val candidates = allTracks.filter { !currentQueueIds.contains(it.id) }
        val newBatch = if (candidates.isNotEmpty()) {
            candidates.shuffled().take(12)
        } else {
            allTracks.shuffled().take(12)
        }

        if (newBatch.isNotEmpty()) {
            val updatedQueue = _uiState.value.queue + newBatch
            _uiState.update { it.copy(queue = updatedQueue) }
            logEvent(LogLevel.INFO, "Autoplay", "Infinite Playback: Queued ${newBatch.size} more tracks")
            playTrack(newBatch.first(), updatedQueue)
        }
    }

    fun togglePlayPause() {
        if (_uiState.value.currentTrack == null && _uiState.value.tracks.isNotEmpty()) {
            playTrack(_uiState.value.tracks.first())
            return
        }

        val player = exoPlayer
        val track = _uiState.value.currentTrack
        val isLiked = track?.let { _uiState.value.likedTrackIds.contains(it.id) } ?: false

        if (player != null) {
            if (player.isPlaying) {
                player.pause()
                _uiState.update { it.copy(isPlaying = false) }
                BrokeifyPlaybackService.startOrUpdate(getApplication(), track, false, isLiked)
                logEvent(LogLevel.INFO, "Playback", "Playback paused")
            } else {
                if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                    track?.let { t ->
                        if (t.streamUrl.isNotBlank()) {
                            try {
                                val mediaItem = MediaItem.fromUri(t.streamUrl)
                                player.setMediaItem(mediaItem)
                                player.prepare()
                            } catch (e: Exception) {
                                logEvent(LogLevel.WARN, "Playback", "Stream prepare: ${e.localizedMessage}")
                            }
                        }
                    }
                }
                player.play()
                _uiState.update { it.copy(isPlaying = true) }
                BrokeifyPlaybackService.startOrUpdate(getApplication(), track, true, isLiked)
                logEvent(LogLevel.INFO, "Playback", "Playing '${track?.title}'")
            }
        } else {
            val next = !_uiState.value.isPlaying
            _uiState.update { it.copy(isPlaying = next) }
            BrokeifyPlaybackService.startOrUpdate(getApplication(), track, next, isLiked)
            logEvent(
                LogLevel.INFO,
                "Playback",
                if (next) "Playing '${track?.title}'" else "Playback paused"
            )
        }
    }

    fun playPlaylist(playlist: Playlist, customTracks: List<Track>? = null) {
        val playlistTracks = customTracks?.takeIf { it.isNotEmpty() }
            ?: _uiState.value.tracks.filter { it.id in playlist.trackIds }.takeIf { it.isNotEmpty() }
            ?: _uiState.value.tracks

        if (playlistTracks.isEmpty()) return
        logEvent(LogLevel.INFO, "Playlists", "Playing playlist '${playlist.name}' (${playlistTracks.size} tracks)")
        playTrack(playlistTracks.first(), playlistTracks)
    }

    fun playTrack(track: Track, customQueue: List<Track>? = null) {
        val baseQueue = customQueue ?: _uiState.value.queue.ifEmpty { _uiState.value.tracks }
        val index = baseQueue.indexOfFirst { it.id == track.id }
        val newQueue = if (index >= 0) baseQueue else listOf(track) + baseQueue
        val newIndex = if (index >= 0) index else 0

        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val existingStat = _uiState.value.playbackHistory.find { it.trackId == track.id }
        val updatedHistory = if (existingStat != null) {
            _uiState.value.playbackHistory.map {
                if (it.trackId == track.id) {
                    it.copy(
                        playCount = it.playCount + 1,
                        lastPlayedTime = timeStr
                    )
                } else it
            }
        } else {
            listOf(
                TrackPlayStat(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    playCount = 1,
                    lastPlayedTime = timeStr
                )
            ) + _uiState.value.playbackHistory
        }

        // Feed ExoPlayer audio streams with the full queue so it transitions smoothly and gaplessly!
        exoPlayer?.let { player ->
            try {
                val validTracks = newQueue.filter { it.streamUrl.isNotBlank() }
                if (validTracks.isNotEmpty()) {
                    val mediaItems = validTracks.map { t ->
                        MediaItem.Builder()
                            .setMediaId(t.id)
                            .setUri(t.streamUrl)
                            .build()
                    }
                    val startIndex = validTracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                    player.setMediaItems(mediaItems, startIndex, 0L)
                    player.prepare()
                    player.playWhenReady = true
                } else if (track.streamUrl.isNotBlank()) {
                    val singleItem = MediaItem.Builder().setMediaId(track.id).setUri(track.streamUrl).build()
                    player.setMediaItem(singleItem)
                    player.prepare()
                    player.playWhenReady = true
                }
            } catch (e: Exception) {
                logEvent(LogLevel.WARN, "Playback", "Stream init: ${e.localizedMessage}")
            }
        }

        val isLiked = _uiState.value.likedTrackIds.contains(track.id) || track.isLiked

        _uiState.update {
            it.copy(
                currentTrack = track,
                queue = newQueue,
                currentIndex = newIndex,
                positionSec = 0,
                isPlaying = true,
                playbackHistory = updatedHistory,
                isMiniPlayerDismissed = false
            )
        }
        logEvent(LogLevel.INFO, "Playback", "Started: ${track.title} [${track.format}]")

        // Record in local Room database
        viewModelScope.launch(Dispatchers.IO) {
            cacheRepository.recordTrackPlayed(track)
        }

        // Keep foreground service and notification active
        BrokeifyPlaybackService.startOrUpdate(getApplication(), track, true, isLiked)

        // Scrobble play event to Docker backend in background
        val serverUrl = secureStorage.serverUrl
        val token = secureStorage.token
        if (serverUrl.isNotBlank()) {
            viewModelScope.launch {
                BrokeifyGatewayClient.scrobbleTrack(serverUrl, token, track)
            }
        }
    }

    fun toggleLikeTrack(trackId: String) {
        val currentLikes = _uiState.value.likedTrackIds
        val nextLikedState = !currentLikes.contains(trackId)
        val updatedLikes = if (nextLikedState) currentLikes + trackId else currentLikes - trackId

        _uiState.update { state ->
            val updatedTracks = state.tracks.map { t ->
                if (t.id == trackId) t.copy(isLiked = nextLikedState) else t
            }
            val updatedQueue = state.queue.map { t ->
                if (t.id == trackId) t.copy(isLiked = nextLikedState) else t
            }
            val updatedCurrent = state.currentTrack?.let { t ->
                if (t.id == trackId) t.copy(isLiked = nextLikedState) else t
            }
            state.copy(
                likedTrackIds = updatedLikes,
                tracks = updatedTracks,
                queue = updatedQueue,
                currentTrack = updatedCurrent
            )
        }

        _uiState.value.currentTrack?.let { track ->
            val isCurrentLiked = if (track.id == trackId) nextLikedState else updatedLikes.contains(track.id)
            BrokeifyPlaybackService.startOrUpdate(getApplication(), track, _uiState.value.isPlaying, isCurrentLiked)
        }

        // Update local Room database
        viewModelScope.launch(Dispatchers.IO) {
            cacheRepository.setTrackLiked(trackId, nextLikedState)
        }

        val serverUrl = secureStorage.serverUrl
        val token = secureStorage.token
        if (serverUrl.isNotBlank()) {
            viewModelScope.launch {
                val resp = BrokeifyGatewayClient.toggleTrackLike(serverUrl, token, trackId, nextLikedState)
                if (resp != null && resp.ok) {
                    val serverSet = resp.likedTrackIds.toSet()
                    _uiState.update { state ->
                        state.copy(
                            likedTrackIds = serverSet,
                            tracks = state.tracks.map { it.copy(isLiked = serverSet.contains(it.id)) },
                            queue = state.queue.map { it.copy(isLiked = serverSet.contains(it.id)) },
                            currentTrack = state.currentTrack?.let { it.copy(isLiked = serverSet.contains(it.id)) }
                        )
                    }
                }
            }
        }
    }

    fun skipNext() {
        val player = exoPlayer
        if (player != null && player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            return
        }

        val queue = _uiState.value.queue
        if (queue.isEmpty()) return

        if (_uiState.value.shuffleEnabled && queue.size > 1) {
            val nextIndex = (queue.indices).filter { it != _uiState.value.currentIndex }.randomOrNull() ?: 0
            playTrack(queue[nextIndex])
        } else {
            val nextIndex = _uiState.value.currentIndex + 1
            if (nextIndex < queue.size) {
                playTrack(queue[nextIndex])
            } else {
                // Reached end of playlist/queue!
                if (_uiState.value.autoplayEnabled) {
                    enqueueAutoplayTracksAndContinue()
                } else if (_uiState.value.repeatMode == RepeatMode.ALL) {
                    playTrack(queue.first())
                } else {
                    exoPlayer?.pause()
                    _uiState.update { it.copy(isPlaying = false, positionSec = 0) }
                    val track = _uiState.value.currentTrack
                    val isLiked = track?.let { _uiState.value.likedTrackIds.contains(it.id) } ?: false
                    BrokeifyPlaybackService.startOrUpdate(getApplication(), track, false, isLiked)
                }
            }
        }
    }

    fun skipPrevious() {
        val player = exoPlayer
        if (player != null && player.currentPosition > 3000L) {
            player.seekTo(0L)
            _uiState.update { it.copy(positionSec = 0) }
            return
        }
        if (player != null && player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            return
        }

        val queue = _uiState.value.queue
        if (queue.isEmpty()) return

        val prevIndex = if (_uiState.value.currentIndex > 0) {
            _uiState.value.currentIndex - 1
        } else {
            queue.size - 1
        }

        playTrack(queue[prevIndex])
    }

    fun toggleAutoplay() {
        val next = !_uiState.value.autoplayEnabled
        _uiState.update { it.copy(autoplayEnabled = next) }
        logEvent(LogLevel.INFO, "Playback", "Autoplay continuous playback: $next")
    }

    fun openSetupWizard() {
        _uiState.update { it.copy(isReRunningSetup = true, currentScreen = "sources") }
    }

    fun closeSetupWizard() {
        _uiState.update { it.copy(isReRunningSetup = false, currentScreen = "library") }
    }

    fun dismissMiniPlayer() {
        _uiState.update { it.copy(isMiniPlayerDismissed = true) }
    }

    fun toggleOfflineDownload(trackId: String) {
        val currentOffline = _uiState.value.offlineTrackIds
        val nextState = !currentOffline.contains(trackId)
        val updated = if (nextState) currentOffline + trackId else currentOffline - trackId
        _uiState.update { it.copy(offlineTrackIds = updated) }
        logEvent(
            LogLevel.INFO,
            "Offline",
            if (nextState) "Track marked for offline playback" else "Track removed from offline cache"
        )
    }

    fun seekTo(seconds: Int) {
        exoPlayer?.seekTo((seconds * 1000).toLong())
        _uiState.update { it.copy(positionSec = seconds) }
    }

    fun toggleShuffle() {
        val next = !_uiState.value.shuffleEnabled
        _uiState.update { it.copy(shuffleEnabled = next) }
        exoPlayer?.shuffleModeEnabled = next
        logEvent(LogLevel.INFO, "Playback", "Shuffle: ${if (next) "ON" else "OFF"}")
    }

    fun cycleRepeat() {
        val next = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _uiState.update { it.copy(repeatMode = next) }
        exoPlayer?.repeatMode = when (next) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun setEqPreset(preset: EQPresetName) {
        val gains = EQ_PRESET_GAINS[preset] ?: _uiState.value.audioPrefs.eqGains
        _uiState.update {
            it.copy(
                audioPrefs = it.audioPrefs.copy(
                    eqPreset = preset,
                    eqGains = gains
                )
            )
        }
    }

    fun updateEqBand(index: Int, gainDb: Float) {
        val currentGains = _uiState.value.audioPrefs.eqGains.toMutableList()
        if (index in currentGains.indices) {
            currentGains[index] = gainDb
            _uiState.update {
                it.copy(
                    audioPrefs = it.audioPrefs.copy(
                        eqPreset = EQPresetName.CUSTOM,
                        eqGains = currentGains
                    )
                )
            }
        }
    }

    fun toggleCarVolumeLeveler() {
        val next = !_uiState.value.audioPrefs.carVolumeLeveler
        _uiState.update {
            it.copy(audioPrefs = it.audioPrefs.copy(carVolumeLeveler = next))
        }
    }

    fun toggleLufsNormalization() {
        val next = !_uiState.value.audioPrefs.lufsNormalization
        _uiState.update {
            it.copy(audioPrefs = it.audioPrefs.copy(lufsNormalization = next))
        }
    }

    fun setCrossfadeSeconds(sec: Int) {
        _uiState.update {
            it.copy(audioPrefs = it.audioPrefs.copy(crossfadeSeconds = sec))
        }
    }

    fun setVisualTheme(theme: VisualTheme) {
        _uiState.update { it.copy(activeTheme = theme) }
    }

    fun navigateTo(screen: String) {
        _uiState.update { it.copy(currentScreen = screen, isCarMode = (screen == "carmode")) }
    }

    fun toggleCarMode() {
        val next = !_uiState.value.isCarMode
        _uiState.update {
            it.copy(
                isCarMode = next,
                currentScreen = if (next) "carmode" else "nowplaying"
            )
        }
    }

    fun setLibraryTab(tabIndex: Int) {
        _uiState.update { it.copy(activeLibraryTab = tabIndex) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    // --- 6-Digit Quick Connect & QR Code Pairing with Brokeify Docker Server ---

    fun testGatewayHealth(serverUrl: String, onComplete: (HealthCheckResult) -> Unit = {}) {
        viewModelScope.launch {
            val result = BrokeifyGatewayClient.testConnection(serverUrl)
            onComplete(result)
            if (result.success) {
                logEvent(
                    LogLevel.SUCCESS,
                    "Gateway",
                    "Health OK: ${result.serverName} v${result.serverVersion} (${result.totalTracks} tracks, ${result.latencyMs}ms)"
                )
            } else {
                logEvent(LogLevel.ERROR, "Gateway", "Health check failed: ${result.errorMessage}")
            }
        }
    }

    /**
     * Jellyfin-style 6-Digit Quick Connect:
     * Connects to the Brokeify Docker container using Server URL + 6-digit PIN shown in Web UI.
     */
    fun connectWithSixDigitCode(serverUrl: String, sixDigitCode: String) {
        val cleanDigits = sixDigitCode.replace(Regex("\\D"), "")
        if (cleanDigits.length != 6) {
            _uiState.update {
                it.copy(
                    pairingFeedbackMessage = "Please enter all 6 digits of the Quick Connect code (e.g. 543 269).",
                    isPairingError = true
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    pairingFeedbackMessage = "Linking with 6-digit code ${cleanDigits.take(3)} ${cleanDigits.drop(3)}...",
                    isPairingError = false,
                    gatewayConfig = it.gatewayConfig.copy(
                        status = ProviderStatus.TESTING,
                        lastMessage = "Verifying 6-digit Quick Connect code..."
                    )
                )
            }

            val healthRes = BrokeifyGatewayClient.testConnection(serverUrl)
            val authRes = BrokeifyGatewayClient.connectWithQuickConnectOrQr(
                serverUrl = serverUrl,
                sixDigitCode = cleanDigits
            )

            if (!authRes.success) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        pairingFeedbackMessage = authRes.errorMessage ?: "Invalid or expired 6-digit code.",
                        isPairingError = true,
                        gatewayConfig = it.gatewayConfig.copy(
                            status = ProviderStatus.ERROR,
                            lastMessage = authRes.errorMessage ?: "Quick Connect failed"
                        )
                    )
                }
                logEvent(LogLevel.ERROR, "QuickConnect", authRes.errorMessage ?: "6-digit code rejected")
                return@launch
            }

            finalizeGatewayConnection(
                cleanUrl = authRes.serverUrl,
                username = "Quick Connect",
                token = authRes.token,
                userId = authRes.userId,
                serverName = authRes.serverName,
                serverVersion = authRes.serverVersion,
                latencyMs = healthRes.latencyMs,
                methodLabel = "6-Digit Quick Connect (${cleanDigits.take(3)} ${cleanDigits.drop(3)})"
            )
        }
    }

    /**
     * Connects using a scanned QR code or pasted payload (JSON, brokeify://pair?..., or 6-digit code).
     */
    fun connectWithQrPayload(rawPayload: String, fallbackServerUrl: String = "") {
        val parsed = BrokeifyGatewayClient.parsePairingPayload(rawPayload, fallbackServerUrl)
        val targetUrl = parsed.serverUrl.ifBlank {
            parsed.publicUrl.ifBlank {
                parsed.localUrl.ifBlank { BrokeifyGatewayClient.sanitizeUrl(fallbackServerUrl) }
            }
        }

        if (targetUrl.isBlank()) {
            _uiState.update {
                it.copy(
                    pairingFeedbackMessage = "QR payload did not include a Server URL. Enter your Server URL above first.",
                    isPairingError = true
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    pairingFeedbackMessage = "Connecting to $targetUrl via QR payload...",
                    isPairingError = false,
                    gatewayConfig = it.gatewayConfig.copy(
                        status = ProviderStatus.TESTING,
                        lastMessage = "Verifying QR pairing payload..."
                    )
                )
            }

            val healthRes = BrokeifyGatewayClient.testConnection(targetUrl)
            val authRes = BrokeifyGatewayClient.connectWithQuickConnectOrQr(
                serverUrl = targetUrl,
                sixDigitCode = parsed.quickConnectCode,
                tempToken = parsed.tempToken,
                directApiKey = parsed.apiKey
            )

            if (!authRes.success) {
                // If primary URL in QR was unreachable (e.g. Docker internal IP), try publicUrl or fallbackServerUrl
                val altUrl = listOf(parsed.publicUrl, BrokeifyGatewayClient.sanitizeUrl(fallbackServerUrl), parsed.localUrl)
                    .firstOrNull { it.isNotBlank() && it != targetUrl }

                if (altUrl != null) {
                    val altHealth = BrokeifyGatewayClient.testConnection(altUrl)
                    val altAuth = BrokeifyGatewayClient.connectWithQuickConnectOrQr(
                        serverUrl = altUrl,
                        sixDigitCode = parsed.quickConnectCode,
                        tempToken = parsed.tempToken,
                        directApiKey = parsed.apiKey
                    )
                    if (altAuth.success) {
                        finalizeGatewayConnection(
                            cleanUrl = altAuth.serverUrl,
                            username = "QR Paired",
                            token = altAuth.token,
                            userId = altAuth.userId,
                            serverName = altAuth.serverName,
                            serverVersion = altAuth.serverVersion,
                            latencyMs = altHealth.latencyMs,
                            methodLabel = "QR Code Pairing"
                        )
                        return@launch
                    }
                }

                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        pairingFeedbackMessage = authRes.errorMessage ?: "QR pairing failed",
                        isPairingError = true,
                        gatewayConfig = it.gatewayConfig.copy(
                            status = ProviderStatus.ERROR,
                            lastMessage = authRes.errorMessage ?: "QR pairing failed"
                        )
                    )
                }
                logEvent(LogLevel.ERROR, "QRPairing", authRes.errorMessage ?: "QR pairing failed")
                return@launch
            }

            finalizeGatewayConnection(
                cleanUrl = authRes.serverUrl,
                username = "QR Paired",
                token = authRes.token,
                userId = authRes.userId,
                serverName = authRes.serverName,
                serverVersion = authRes.serverVersion,
                latencyMs = healthRes.latencyMs,
                methodLabel = "QR Code Pairing"
            )
        }
    }

    /**
     * Generates a 6-digit code ON the Android phone so the user can enter it into the Web UI's
     * "Authorize 6-Digit Code Shown On Your Phone" box.
     */
    fun generatePhoneSixDigitCode(serverUrl: String) {
        val cleanUrl = BrokeifyGatewayClient.sanitizeUrl(serverUrl)
        if (cleanUrl.isBlank()) {
            _uiState.update {
                it.copy(
                    pairingFeedbackMessage = "Enter your Server URL first to request a 6-digit phone code.",
                    isPairingError = true
                )
            }
            return
        }

        phonePinPollJob?.cancel()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPollingPhonePin = true,
                    pairingFeedbackMessage = "Requesting 6-digit phone code from $cleanUrl...",
                    isPairingError = false
                )
            }

            val initResp = BrokeifyGatewayClient.initiatePhoneCode(cleanUrl)
            if (!initResp.ok || initResp.code.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isPollingPhonePin = false,
                        pairingFeedbackMessage = initResp.error ?: "Could not generate 6-digit phone code",
                        isPairingError = true
                    )
                }
                return@launch
            }

            val rawCode = initResp.code
            val formatted = initResp.formattedCode ?: "${rawCode.take(3)} ${rawCode.drop(3)}"
            val secret = initResp.secret ?: ""

            _uiState.update {
                it.copy(
                    generatedPhonePin = formatted,
                    pairingFeedbackMessage = "Enter $formatted in your Brokeify Web UI under 'Authorize 6-Digit Code Shown On Your Phone'...",
                    isPairingError = false
                )
            }

            phonePinPollJob = viewModelScope.launch {
                repeat(180) { // Poll up to 6 minutes (every 2 seconds)
                    delay(2000)
                    val poll = BrokeifyGatewayClient.pollPhoneCode(cleanUrl, secret, rawCode)
                    if (poll.ok && poll.authorized && !poll.apiKey.isNullOrBlank()) {
                        val health = BrokeifyGatewayClient.testConnection(cleanUrl)
                        _uiState.update {
                            it.copy(
                                isPollingPhonePin = false,
                                generatedPhonePin = null
                            )
                        }
                        finalizeGatewayConnection(
                            cleanUrl = cleanUrl,
                            username = "Phone Code",
                            token = poll.apiKey,
                            userId = "android-phone-pin",
                            serverName = poll.server ?: health.serverName,
                            serverVersion = health.serverVersion,
                            latencyMs = health.latencyMs,
                            methodLabel = "Phone Code ($formatted)"
                        )
                        return@launch
                    }
                }
                _uiState.update {
                    it.copy(
                        isPollingPhonePin = false,
                        generatedPhonePin = null,
                        pairingFeedbackMessage = "Phone code expired. Tap to generate a new 6-digit code.",
                        isPairingError = true
                    )
                }
            }
        }
    }

    fun cancelPhonePinPolling() {
        phonePinPollJob?.cancel()
        _uiState.update {
            it.copy(
                isPollingPhonePin = false,
                generatedPhonePin = null,
                pairingFeedbackMessage = null
            )
        }
    }

    fun connectGateway(serverUrl: String, username: String, passwordOrToken: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    pairingFeedbackMessage = null,
                    isPairingError = false,
                    gatewayConfig = it.gatewayConfig.copy(
                        status = ProviderStatus.TESTING,
                        lastMessage = "Testing gateway /api/health..."
                    )
                )
            }

            val healthRes = BrokeifyGatewayClient.testConnection(serverUrl)
            if (!healthRes.success) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        pairingFeedbackMessage = healthRes.errorMessage ?: "Failed to reach Brokeify server",
                        isPairingError = true,
                        gatewayConfig = it.gatewayConfig.copy(
                            status = ProviderStatus.ERROR,
                            lastMessage = healthRes.errorMessage ?: "Failed to reach Brokeify server"
                        )
                    )
                }
                logEvent(LogLevel.ERROR, "Gateway", healthRes.errorMessage ?: "Connection failed")
                return@launch
            }

            val authRes = BrokeifyGatewayClient.authenticate(serverUrl, username, passwordOrToken)
            if (!authRes.success) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        pairingFeedbackMessage = authRes.errorMessage ?: "Authentication failed",
                        isPairingError = true,
                        gatewayConfig = it.gatewayConfig.copy(
                            status = ProviderStatus.ERROR,
                            lastMessage = authRes.errorMessage ?: "Authentication failed"
                        )
                    )
                }
                logEvent(LogLevel.ERROR, "Gateway", authRes.errorMessage ?: "Authentication failed")
                return@launch
            }

            finalizeGatewayConnection(
                cleanUrl = authRes.serverUrl.ifBlank { BrokeifyGatewayClient.sanitizeUrl(serverUrl) },
                username = username.ifBlank { "admin" },
                token = authRes.token,
                userId = authRes.userId,
                serverName = authRes.serverName,
                serverVersion = authRes.serverVersion,
                latencyMs = healthRes.latencyMs,
                methodLabel = authRes.method
            )
        }
    }

    private suspend fun finalizeGatewayConnection(
        cleanUrl: String,
        username: String,
        token: String,
        userId: String,
        serverName: String,
        serverVersion: String,
        latencyMs: Long,
        methodLabel: String
    ) {
        secureStorage.serverUrl = cleanUrl
        secureStorage.username = username
        secureStorage.token = token
        secureStorage.userId = userId
        secureStorage.serverName = serverName
        secureStorage.serverVersion = serverVersion
        secureStorage.isConnected = true

        _uiState.update {
            it.copy(
                gatewayConfig = it.gatewayConfig.copy(
                    lastMessage = "Syncing library from $cleanUrl..."
                )
            )
        }

        val libraryResult = BrokeifyGatewayClient.fetchLibrary(cleanUrl, token)
        val tracks = libraryResult.tracks
        val playlists = libraryResult.playlists
        val likedIds = libraryResult.likedTrackIds

        val updatedConfig = ProviderConfig(
            id = "prov-brokeify-gateway",
            type = ProviderType.BROKEIFY_GATEWAY,
            name = serverName.ifBlank { "Brokeify Native Engine" },
            enabled = true,
            serverUrl = cleanUrl,
            username = username,
            token = token,
            userId = userId,
            status = ProviderStatus.CONNECTED,
            lastMessage = "Connected via $methodLabel • ${tracks.size} tracks (${latencyMs}ms)",
            latencyMs = latencyMs,
            trackCount = tracks.size,
            albumCount = libraryResult.albums.size,
            artistCount = libraryResult.artists.size,
            playlistCount = playlists.size,
            version = serverVersion
        )

        val favPlaylistIds = playlists.filter { it.isFavorite }.map { it.id }.toSet()

        _uiState.update {
            it.copy(
                isSyncing = false,
                isOfflineMode = false,
                gatewayConfig = updatedConfig,
                tracks = tracks,
                playlists = playlists,
                likedTrackIds = likedIds,
                favoritePlaylistIds = favPlaylistIds,
                queue = tracks,
                currentTrack = it.currentTrack, // Keep null until user picks a song/playlist to play!
                pairingFeedbackMessage = "✅ Connected to $serverName! Synced ${tracks.size} tracks & ${playlists.size} playlists.",
                isPairingError = false,
                isReRunningSetup = false,
                currentScreen = "library",
                activeLibraryTab = 0 // Playlists visible first!
            )
        }

        // Cache all tracks & playlists in local Room database for offline browsing
        viewModelScope.launch(Dispatchers.IO) {
            cacheRepository.cacheTracks(tracks, likedIds)
            cacheRepository.cachePlaylists(playlists, favPlaylistIds)
        }

        logEvent(
            LogLevel.SUCCESS,
            "Gateway",
            "Linked to $serverName ($cleanUrl) via $methodLabel. Loaded ${tracks.size} tracks, ${playlists.size} playlists."
        )
    }

    fun toggleFavoritePlaylist(playlistId: String) {
        val currentFavs = _uiState.value.favoritePlaylistIds
        val nextFavState = !currentFavs.contains(playlistId)
        val updatedFavs = if (nextFavState) currentFavs + playlistId else currentFavs - playlistId

        _uiState.update { state ->
            val updatedPlaylists = state.playlists.map { p ->
                if (p.id == playlistId) p.copy(isFavorite = nextFavState) else p
            }
            state.copy(
                favoritePlaylistIds = updatedFavs,
                playlists = updatedPlaylists
            )
        }
        logEvent(
            LogLevel.INFO,
            "Playlists",
            if (nextFavState) "Favorited playlist '$playlistId'" else "Unfavorited playlist '$playlistId'"
        )

        // Update local Room database
        viewModelScope.launch(Dispatchers.IO) {
            cacheRepository.setPlaylistFavorite(playlistId, nextFavState)
        }

        val serverUrl = secureStorage.serverUrl
        val token = secureStorage.token
        if (serverUrl.isNotBlank()) {
            viewModelScope.launch {
                val resp = BrokeifyGatewayClient.togglePlaylistFavorite(serverUrl, token, playlistId, nextFavState)
                if (resp != null && resp.ok && resp.favoritePlaylistIds.isNotEmpty()) {
                    val serverFavs = resp.favoritePlaylistIds.toSet()
                    _uiState.update { state ->
                        state.copy(
                            favoritePlaylistIds = serverFavs,
                            playlists = state.playlists.map { it.copy(isFavorite = serverFavs.contains(it.id)) }
                        )
                    }
                }
            }
        }
    }

    fun refreshLibrary() {
        val serverUrl = secureStorage.serverUrl
        val token = secureStorage.token

        if (serverUrl.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val health = BrokeifyGatewayClient.testConnection(serverUrl)

            if (!health.success) {
                // Connection lost or unreachable! Switch to Room offline cache mode so library is still browsable
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        isOfflineMode = true,
                        gatewayConfig = it.gatewayConfig.copy(
                            status = ProviderStatus.DISCONNECTED,
                            lastMessage = "Offline Mode • Browsing Room cache (${it.tracks.size} tracks saved)"
                        )
                    )
                }
                logEvent(LogLevel.WARN, "Gateway", "Connection to Docker container lost. Switched to offline Room cache.")
                return@launch
            }

            val libraryResult = BrokeifyGatewayClient.fetchLibrary(serverUrl, token)
            val tracks = libraryResult.tracks
            val playlists = libraryResult.playlists
            val likedIds = libraryResult.likedTrackIds
            val favPlaylistIds = playlists.filter { it.isFavorite }.map { it.id }.toSet()

            _uiState.update {
                it.copy(
                    isSyncing = false,
                    isOfflineMode = false,
                    tracks = if (tracks.isNotEmpty()) tracks else it.tracks,
                    playlists = if (playlists.isNotEmpty()) playlists else it.playlists,
                    likedTrackIds = if (likedIds.isNotEmpty()) likedIds else it.likedTrackIds,
                    favoritePlaylistIds = if (favPlaylistIds.isNotEmpty()) favPlaylistIds else it.favoritePlaylistIds,
                    queue = if (it.queue.isEmpty() && tracks.isNotEmpty()) tracks else it.queue,
                    currentTrack = it.currentTrack, // Keep null until user picks a song!
                    gatewayConfig = it.gatewayConfig.copy(
                        name = health.serverName.ifBlank { it.gatewayConfig.name },
                        status = ProviderStatus.CONNECTED,
                        latencyMs = health.latencyMs,
                        trackCount = if (tracks.isNotEmpty()) tracks.size else health.totalTracks,
                        albumCount = if (libraryResult.albums.isNotEmpty()) libraryResult.albums.size else health.totalAlbums,
                        artistCount = if (libraryResult.artists.isNotEmpty()) libraryResult.artists.size else health.totalArtists,
                        playlistCount = if (playlists.isNotEmpty()) playlists.size else health.totalPlaylists,
                        version = health.serverVersion,
                        lastMessage = "Connected • ${if (tracks.isNotEmpty()) tracks.size else health.totalTracks} tracks (${health.latencyMs}ms)"
                    )
                )
            }

            // Sync Room database with latest Docker library
            viewModelScope.launch(Dispatchers.IO) {
                if (tracks.isNotEmpty()) {
                    cacheRepository.cacheTracks(tracks, likedIds)
                }
                if (playlists.isNotEmpty()) {
                    cacheRepository.cachePlaylists(playlists, favPlaylistIds)
                }
            }
        }
    }

    fun disconnectGateway() {
        phonePinPollJob?.cancel()
        exoPlayer?.stop()
        secureStorage.clearAuth()
        _uiState.update {
            it.copy(
                gatewayConfig = ProviderConfig(
                    status = ProviderStatus.DISCONNECTED,
                    lastMessage = "Disconnected • Tap Sources to Reconnect"
                ),
                isOfflineMode = it.tracks.isNotEmpty(),
                currentTrack = null,
                isPlaying = false,
                positionSec = 0,
                pairingFeedbackMessage = null,
                generatedPhonePin = null,
                isPollingPhonePin = false
            )
        }
        logEvent(LogLevel.INFO, "Gateway", "Server disconnected")
    }

    fun connectJellyfin(serverUrl: String, username: String, passwordOrToken: String) =
        connectGateway(serverUrl, username, passwordOrToken)

    fun disconnectJellyfin() = disconnectGateway()

    private fun logEvent(level: LogLevel, category: String, message: String) {
        val entry = DebugLogEntry(
            id = "log-${System.currentTimeMillis()}",
            timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
            level = level,
            category = category,
            message = message
        )
        _uiState.update { it.copy(debugLogs = listOf(entry) + it.debugLogs.take(40)) }
    }

    override fun onCleared() {
        super.onCleared()
        playbackTickerJob?.cancel()
        phonePinPollJob?.cancel()
        BrokeifyPlaybackService.stop(getApplication())
        try {
            exoPlayer?.release()
            exoPlayer = null
        } catch (_: Exception) {
        }
    }
}
