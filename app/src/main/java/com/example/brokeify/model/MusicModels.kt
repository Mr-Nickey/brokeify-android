package com.example.brokeify.model

enum class ProviderType(val displayName: String) {
    BROKEIFY_GATEWAY("Brokeify Gateway")
}

enum class ProviderStatus {
    CONNECTED,
    DISCONNECTED,
    TESTING,
    ERROR
}

data class ProviderConfig(
    val id: String = "prov-brokeify-gateway",
    val type: ProviderType = ProviderType.BROKEIFY_GATEWAY,
    val name: String = "Brokeify Native Engine",
    val enabled: Boolean = true,
    val serverUrl: String = "",
    val username: String = "",
    val token: String = "",
    val userId: String = "",
    val status: ProviderStatus = ProviderStatus.DISCONNECTED,
    val lastMessage: String = "",
    val latencyMs: Long = 0,
    val trackCount: Int = 0,
    val albumCount: Int = 0,
    val artistCount: Int = 0,
    val playlistCount: Int = 0,
    val version: String = ""
)

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSec: Int,
    val source: ProviderType = ProviderType.BROKEIFY_GATEWAY,
    val sourceName: String = "Brokeify",
    val streamUrl: String = "",
    val fallbackStreamUrl: String = "",
    val artworkUrl: String = "",
    val accentColor: Long = 0xFF00FF66,
    val genre: String = "",
    val bpm: Int = 0,
    val playCount: Int = 0,
    val isLiveStream: Boolean = false,
    val format: String = "FLAC",
    val bitrateKbps: Int = 0,
    val sampleRate: Int = 0,
    val isLiked: Boolean = false
)

data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val trackIds: List<String> = emptyList(),
    val coverAccent: Long = 0xFF00FF66,
    val isFavorite: Boolean = false
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
    CLASSIC_RACK("Classic Stereo '98", true, 0xFF0A0C10, 0xFFFBBF24, 0xFF18181B, "Charcoal component deck with warm gold-tinted meters"),
    MODULAR_BENTO("Modular Studio", true, 0xFF060913, 0xFF38BDF8, 0xFF0F172A, "Slate studio panels with electric cyan readouts"),
    AMBER_CRT("Amber Phosphor", true, 0xFF0C0804, 0xFFF59E0B, 0xFF1E140A, "Vintage amber CRT monitor filter for night driving"),
    PLATINUM_LIGHT("Platinum '99", false, 0xFFE2E8F0, 0xFF059669, 0xFFF8FAFC, "Brushed silver desktop skin with dark LCD scopes"),
    SOLAR_LIGHT("Solar Daylight", false, 0xFFF8FAFC, 0xFF0284C7, 0xFFFFFFFF, "Ivory & glacier-blue daytime cabin skin")
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

data class TrackPlayStat(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val playCount: Int = 0,
    val totalSeconds: Long = 0L,
    val lastPlayedTime: String = ""
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
