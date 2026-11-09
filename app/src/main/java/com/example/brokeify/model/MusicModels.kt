package com.example.brokeify.model

enum class ProviderType(val displayName: String) {
    JELLYFIN("Jellyfin"),
    EMBY("Emby"),
    NAVIDROME("Navidrome"),
    PLEX("Plex"),
    RADIO_BROWSER("Radio-Browser"),
    INTERNET_ARCHIVE("Internet Archive")
}

enum class ProviderStatus {
    CONNECTED,
    DISCONNECTED,
    TESTING,
    ERROR
}

data class ProviderConfig(
    val id: String,
    val type: ProviderType,
    val name: String,
    val enabled: Boolean = true,
    val serverUrl: String = "",
    val username: String = "",
    val token: String = "",
    val userId: String = "",
    val status: ProviderStatus = ProviderStatus.DISCONNECTED,
    val lastMessage: String = "",
    val trackCount: Int = 0,
    val playlistCount: Int = 0
)

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSec: Int,
    val source: ProviderType,
    val sourceName: String,
    val streamUrl: String = "",
    val artworkUrl: String = "",
    val accentColor: Long = 0xFF00FF66,
    val genre: String = "Electronic",
    val bpm: Int = 120,
    val playCount: Int = 0,
    val isLiveStream: Boolean = false,
    val format: String = "FLAC 24-bit"
)

data class Playlist(
    val id: String,
    val name: String,
    val description: String,
    val trackIds: List<String>,
    val coverAccent: Long = 0xFF00FF66,
    val linkedSpotifyUrl: String? = null
)

enum class EQPresetName(val displayName: String) {
    FLAT("Flat"),
    CAR_STEREO("Car Stereo"),
    BASS_BOOST("Bass Boost"),
    ROCK("Rock"),
    VOCAL("Vocal"),
    LATE_NIGHT("Late Night"),
    CUSTOM("Custom")
}

val EQ_PRESET_GAINS = mapOf(
    EQPresetName.FLAT to listOf(0f, 0f, 0f, 0f, 0f),
    EQPresetName.CAR_STEREO to listOf(4.5f, 2.0f, -1.5f, 2.5f, 4.0f),
    EQPresetName.BASS_BOOST to listOf(6.5f, 4.0f, 0f, 0.5f, 1.5f),
    EQPresetName.ROCK to listOf(4.0f, 1.5f, -1.0f, 3.0f, 4.5f),
    EQPresetName.VOCAL to listOf(-2.0f, 1.0f, 4.5f, 3.5f, -1.0f),
    EQPresetName.LATE_NIGHT to listOf(-3.5f, -1.0f, 2.5f, 2.0f, -2.5f)
)

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class VisualTheme(
    val title: String,
    val isDark: Boolean,
    val primaryColor: Long,
    val accentColor: Long,
    val surfaceColor: Long,
    val description: String
) {
    CYBER_DECK("Cyber Deck '99", true, 0xFF070A0F, 0xFF00FF66, 0xFF0D131C, "Obsidian stereo rack with phosphor emerald LCD readouts"),
    CLASSIC_RACK("Classic Stereo '98", true, 0xFF0A0C10, 0xFFFBBF24, 0xFF18181B, "Late-90s charcoal deck with warm gold-tinted meters"),
    MODULAR_BENTO("Modular Studio", true, 0xFF060913, 0xFF38BDF8, 0xFF0F172A, "High-density slate studio with electric cyan"),
    AMBER_CRT("Amber Phosphor", true, 0xFF0C0804, 0xFFF59E0B, 0xFF1E140A, "Warm vintage amber CRT monitor filter for night driving"),
    PLATINUM_LIGHT("Platinum '99", false, 0xFFE2E8F0, 0xFF059669, 0xFFF8FAFC, "Brushed silver desktop skin with dark LCD scopes"),
    SOLAR_LIGHT("Solar Daylight", false, 0xFFF8FAFC, 0xFF0284C7, 0xFFFFFFFF, "Warm ivory & glacier-blue daytime cabin skin")
}

data class AudioPreferences(
    val eqPreset: EQPresetName = EQPresetName.CAR_STEREO,
    val eqGains: List<Float> = EQ_PRESET_GAINS[EQPresetName.CAR_STEREO] ?: listOf(0f, 0f, 0f, 0f, 0f),
    val carVolumeLeveler: Boolean = true,
    val lufsNormalization: Boolean = true,
    val crossfadeSeconds: Int = 4,
    val offlinePreloadCount: Int = 5,
    val smartAntiRepeat: Boolean = true
)

enum class SlskdJobStatus(val label: String) {
    QUEUED("Queued"),
    SEARCHING("Searching Peers"),
    DOWNLOADING("Downloading FLAC"),
    COMPLETED("Ready in Vault"),
    FAILED("Error")
}

data class SlskdJob(
    val id: String,
    val title: String,
    val artist: String,
    val status: SlskdJobStatus,
    val speedKbps: Int = 850,
    val progressPercent: Int = 0,
    val peerUsername: String = "soulseek_master_44"
)

enum class LogLevel {
    INFO,
    WARN,
    ERROR,
    SUCCESS
}

data class DebugLogEntry(
    val id: String,
    val timestamp: String,
    val level: LogLevel,
    val category: String,
    val message: String
)
