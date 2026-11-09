package com.example.brokeify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brokeify.data.MusicRepository
import com.example.brokeify.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrokeifyUiState(
    val tracks: List<Track> = MusicRepository.initialTracks,
    val playlists: List<Playlist> = MusicRepository.initialPlaylists,
    val providers: List<ProviderConfig> = MusicRepository.initialProviders,
    val slskdJobs: List<SlskdJob> = MusicRepository.initialSlskdJobs,
    val debugLogs: List<DebugLogEntry> = MusicRepository.initialDebugLogs,
    val currentTrack: Track? = MusicRepository.initialTracks.firstOrNull(),
    val isPlaying: Boolean = false,
    val positionSec: Int = 34,
    val queue: List<Track> = MusicRepository.initialTracks,
    val currentIndex: Int = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val audioPrefs: AudioPreferences = AudioPreferences(),
    val activeTheme: VisualTheme = VisualTheme.CYBER_DECK,
    val currentScreen: String = "library",
    val activeLibraryTab: Int = 0, // 0: Playlists, 1: Songs, 2: Albums, 3: Artists
    val searchQuery: String = "",
    val totalListeningMinutes: Int = 412,
    val isCarMode: Boolean = false
)

class BrokeifyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BrokeifyUiState())
    val uiState: StateFlow<BrokeifyUiState> = _uiState.asStateFlow()

    private var playbackTickerJob: Job? = null

    init {
        startPlaybackTicker()
    }

    private fun startPlaybackTicker() {
        playbackTickerJob?.cancel()
        playbackTickerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_uiState.value.isPlaying && _uiState.value.currentTrack != null) {
                    val track = _uiState.value.currentTrack!!
                    val maxDuration = if (track.isLiveStream) 3600 else track.durationSec
                    val nextPos = _uiState.value.positionSec + 1
                    if (nextPos >= maxDuration && !track.isLiveStream) {
                        handleTrackFinished()
                    } else {
                        _uiState.update { it.copy(positionSec = nextPos) }
                    }
                }
            }
        }
    }

    private fun handleTrackFinished() {
        when (_uiState.value.repeatMode) {
            RepeatMode.ONE -> {
                _uiState.update { it.copy(positionSec = 0) }
            }
            RepeatMode.ALL -> {
                skipNext()
            }
            RepeatMode.OFF -> {
                if (_uiState.value.currentIndex < _uiState.value.queue.size - 1) {
                    skipNext()
                } else {
                    _uiState.update { it.copy(isPlaying = false, positionSec = 0) }
                }
            }
        }
    }

    fun togglePlayPause() {
        val nextState = !_uiState.value.isPlaying
        _uiState.update { it.copy(isPlaying = nextState) }
        logEvent(
            LogLevel.INFO,
            "Playback",
            if (nextState) "Playing '${_uiState.value.currentTrack?.title}'" else "Paused playback"
        )
    }

    fun playTrack(track: Track) {
        val index = _uiState.value.queue.indexOfFirst { it.id == track.id }
        val newQueue = if (index >= 0) _uiState.value.queue else listOf(track) + _uiState.value.queue
        val newIndex = if (index >= 0) index else 0

        // Increment play count
        val updatedTracks = _uiState.value.tracks.map {
            if (it.id == track.id) it.copy(playCount = it.playCount + 1) else it
        }

        _uiState.update {
            it.copy(
                tracks = updatedTracks,
                currentTrack = track,
                queue = newQueue,
                currentIndex = newIndex,
                positionSec = 0,
                isPlaying = true
            )
        }
        logEvent(LogLevel.INFO, "Playback", "Switched stream to: ${track.title} [${track.sourceName}]")
    }

    fun skipNext() {
        val queue = _uiState.value.queue
        if (queue.isEmpty()) return

        val nextIndex = if (_uiState.value.shuffleEnabled) {
            (queue.indices).filter { it != _uiState.value.currentIndex }.randomOrNull() ?: 0
        } else {
            (_uiState.value.currentIndex + 1) % queue.size
        }

        val nextTrack = queue[nextIndex]
        _uiState.update {
            it.copy(
                currentIndex = nextIndex,
                currentTrack = nextTrack,
                positionSec = 0,
                isPlaying = true
            )
        }
    }

    fun skipPrevious() {
        val queue = _uiState.value.queue
        if (queue.isEmpty()) return

        if (_uiState.value.positionSec > 3) {
            _uiState.update { it.copy(positionSec = 0) }
            return
        }

        val prevIndex = if (_uiState.value.currentIndex > 0) {
            _uiState.value.currentIndex - 1
        } else {
            queue.size - 1
        }

        val prevTrack = queue[prevIndex]
        _uiState.update {
            it.copy(
                currentIndex = prevIndex,
                currentTrack = prevTrack,
                positionSec = 0,
                isPlaying = true
            )
        }
    }

    fun seekTo(seconds: Int) {
        _uiState.update { it.copy(positionSec = seconds) }
    }

    fun toggleShuffle() {
        val next = !_uiState.value.shuffleEnabled
        _uiState.update { it.copy(shuffleEnabled = next) }
        logEvent(LogLevel.INFO, "AudioEngine", "Smart Anti-Repeat Shuffle: ${if (next) "ACTIVE" else "OFF"}")
    }

    fun cycleRepeat() {
        val next = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _uiState.update { it.copy(repeatMode = next) }
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
        logEvent(LogLevel.INFO, "AudioLab", "Applied 5-band EQ Preset: ${preset.displayName}")
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
        logEvent(LogLevel.INFO, "AudioEngine", "Road Noise Car Volume Leveler: ${if (next) "ON" else "BYPASS"}")
    }

    fun toggleLufsNormalization() {
        val next = !_uiState.value.audioPrefs.lufsNormalization
        _uiState.update {
            it.copy(audioPrefs = it.audioPrefs.copy(lufsNormalization = next))
        }
        logEvent(LogLevel.INFO, "AudioEngine", "EBU R128 LUFS Normalization: ${if (next) "ON (-14 LUFS)" else "OFF"}")
    }

    fun setCrossfadeSeconds(sec: Int) {
        _uiState.update {
            it.copy(audioPrefs = it.audioPrefs.copy(crossfadeSeconds = sec))
        }
    }

    fun setVisualTheme(theme: VisualTheme) {
        _uiState.update { it.copy(activeTheme = theme) }
        logEvent(LogLevel.INFO, "System", "Visual Theme switched to: ${theme.title}")
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

    fun generateInstantMix(seed: Track) {
        val mix = listOf(seed) + _uiState.value.tracks.filter { it.id != seed.id }.shuffled()
        _uiState.update {
            it.copy(
                queue = mix,
                currentTrack = seed,
                currentIndex = 0,
                positionSec = 0,
                isPlaying = true
            )
        }
        logEvent(LogLevel.SUCCESS, "InstantMix", "Smart Instant Mix generated seeded by '${seed.title}' (${mix.size} tracks)")
    }

    fun testProvider(providerId: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    providers = state.providers.map {
                        if (it.id == providerId) it.copy(status = ProviderStatus.TESTING, lastMessage = "Pinging API...") else it
                    }
                )
            }
            delay(800)
            _uiState.update { state ->
                state.copy(
                    providers = state.providers.map {
                        if (it.id == providerId) {
                            it.copy(
                                status = ProviderStatus.CONNECTED,
                                lastMessage = "Verified HTTP 200 OK. Ping 24ms."
                            )
                        } else it
                    }
                )
            }
            logEvent(LogLevel.SUCCESS, "Homelab", "Provider handshake succeeded: $providerId")
        }
    }

    fun toggleProviderEnabled(providerId: String) {
        _uiState.update { state ->
            state.copy(
                providers = state.providers.map {
                    if (it.id == providerId) it.copy(enabled = !it.enabled) else it
                }
            )
        }
    }

    fun queueDownloadJob(title: String, artist: String) {
        val newJob = SlskdJob(
            id = "job-${System.currentTimeMillis()}",
            title = title,
            artist = artist,
            status = SlskdJobStatus.SEARCHING,
            progressPercent = 0
        )
        _uiState.update { it.copy(slskdJobs = listOf(newJob) + it.slskdJobs) }
        logEvent(LogLevel.INFO, "Slskd", "Queued peer search for: $artist - $title")
    }

    private fun logEvent(level: LogLevel, category: String, message: String) {
        val entry = DebugLogEntry(
            id = "log-${System.currentTimeMillis()}",
            timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
            level = level,
            category = category,
            message = message
        )
        _uiState.update { it.copy(debugLogs = listOf(entry) + it.debugLogs.take(50)) }
    }
}
