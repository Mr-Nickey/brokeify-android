# Brokeify (Android Native Client)

[![GitHub Repository](https://img.shields.io/badge/GitHub-Brokeify_Backend-00FF66?logo=github&style=flat-square)](https://github.com/Mr-Nickey/brokeify)
[![Android](https://img.shields.io/badge/Platform-Android_14_--_15_(API_26+)-3DDC84?logo=android&style=flat-square)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_M3-4285F4?logo=jetpackcompose&style=flat-square)](https://developer.android.com/jetpack/compose)
[![Media3 ExoPlayer](https://img.shields.io/badge/Audio-Media3_ExoPlayer_1.5.1-FF5722?style=flat-square)](https://developer.android.com/media/media3)
[![Room Database](https://img.shields.io/badge/Storage-Room_2.6.1_(KSP)-F59E0B?style=flat-square)](https://developer.android.com/training/data-storage/room)

**Brokeify Android** is the official native companion client for the [Brokeify Self-Hosted Music Server](https://github.com/Mr-Nickey/brokeify). It connects directly to your Brokeify Docker container, streaming your personal lossless FLAC and MP3 library with a tactile retro-modern Hi-Fi deck interface, continuous background playback, road-ready Car Mode, and an offline Room database cache.

---

## 🔗 Connected Backend

The Android client is designed to work hand-in-hand with the **Brokeify Docker Container**:
👉 **Docker Backend & Server Repository:** [https://github.com/Mr-Nickey/brokeify](https://github.com/Mr-Nickey/brokeify)

---

## ⚡ 4 Easy Ways to Connect to Your Brokeify Docker Container

Connecting your phone to your server is simple and follows the seamless pairing experience found in modern media servers like Jellyfin:

### 1. 6-Digit Quick Connect (Recommended)
1. Open your **Brokeify Web UI** in any browser (e.g., `https://music.yourdomain.com` or `http://192.168.1.xxx:3487`).
2. Tap the **"Connect Android"** button in the top navigation bar to open the pairing deck.
3. You will see a 6-digit PIN (e.g. `543 269`).
4. In the **Brokeify Android app**, enter your Server URL and type the 6 digits directly into the tactile number boxes `[ 5 ] [ 4 ] [ 3 ]   [ 2 ] [ 6 ] [ 9 ]`.
5. The app verifies the code against `/api/mobile/quick-connect` and instantly synchronizes your library, playlists, and favorites.

### 2. High-Contrast QR Code Scan
1. In the Brokeify Web UI, click the **"Connect Android"** panel or open the Fullscreen QR Code.
2. In the Android app, tap **"Scan QR"**.
3. Point your camera at the screen. The app automatically parses the server URL and secure temporary token, connects, and brings you straight to your music library.

### 3. Phone-Authorized 6-Digit PIN
1. In the Android app, enter your Server URL and tap **"Generate 6-Digit Code on Phone"**.
2. A 6-digit code will appear on your phone screen.
3. In the Brokeify Web UI, enter that 6-digit code into the **"Authorize 6-Digit Code Shown On Your Phone"** box and click Authorize.
4. The Android app automatically detects the authorization handshake and logs in.

### 4. Direct Credentials & API Token
1. Expand **"Advanced Direct Server Login"** on the Connection screen.
2. Enter your server URL, username, and password or API token.
3. Tap **"Sign In with Credentials"**. Credentials are encrypted on-device via Android `EncryptedSharedPreferences`.

---

## 🛠️ Technical Stack & Architecture

```
┌────────────────────────────────────────────────────────┐
│               BROKEIFY ANDROID CLIENT                  │
│                                                        │
│  ┌──────────────────┐           ┌───────────────────┐  │
│  │ Jetpack Compose  │◄─────────►│ BrokeifyViewModel │  │
│  │ (M3 Retro Hi-Fi) │           │ (StateFlow / UDF) │  │
│  └──────────────────┘           └─────────┬─────────┘  │
│                                           │            │
│                 ┌─────────────────────────┼──────────┐ │
│                 ▼                         ▼          ▼ │
│      ┌─────────────────────┐   ┌──────────────┐ ┌────┴──────┐
│      │ BrokeifyGatewayApi  │   │  Media3      │ │ Local     │
│      │ Retrofit / OkHttp   │   │  ExoPlayer   │ │ Room DB   │
│      └──────────┬──────────┘   │  Service     │ │ (Offline) │
│                 │              └──────────────┘ └───────────┘
└─────────────────┼──────────────────────────────────────┘
                  ▼ HTTP / REST / Streams
┌────────────────────────────────────────────────────────┐
│            BROKEIFY DOCKER CONTAINER                   │
│         https://github.com/Mr-Nickey/brokeify          │
│                                                        │
│   • /api/health              • /api/library/tracks     │
│   • /api/mobile/quick-connect• /api/playlists          │
│   • /api/stream/{trackId}    • /api/cover/{trackId}    │
│   • /api/library/likes/toggle• /api/playlists/favorite │
└────────────────────────────────────────────────────────┘
```

### 1. Local Room Database Caching (`BrokeifyDatabase`)
- **Offline Browsing**: When cell service drops, highway tunnels cut connection, or your server is temporarily offline, the app automatically switches to **Offline Cache Mode**.
- **Metadata Persistence**: Tracks, favorite playlists, recently played history, and play counts are stored in a local SQLite database using **Room 2.6.1 with KSP**.
- **Bidirectional Sync**: Favoriting playlists (`/api/playlists/favorite`) and liking songs (`/api/library/likes/toggle`) are updated locally and synced upstream with the Docker database.

### 2. Continuous, Gapless Playback Engine
- **Native Queue Queuing**: Whole playlists and albums are queued into ExoPlayer (`setMediaItems`), guaranteeing seamless, zero-gap transitions between tracks.
- **Background Persistence**: Powered by `BrokeifyPlaybackService` (Android `MediaSessionService`) holding a CPU partial wake-lock and high-performance Wi-Fi lock so music never cuts out when the screen is turned off.
- **Fail-Safe Streaming**: High buffer tolerance (30s min, 120s max buffer) with automatic error recovery that skips corrupted streams rather than halting playback.
- **Continuous Autoplay**: Automatically queues additional tracks from your library when a playlist finishes so the music never stops.

### 3. Tactile Retro-Hi-Fi UI
- **Playlists Visible First**: Immediate 1-tap access to your playlists upon opening the library.
- **Real-Time LED Spectrum Visualizer**: Animated reactive frequencies responding directly to active ExoPlayer audio.
- **Audio Lab**: 5-band graphic equalizer (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz), EBU R128 LUFS volume leveling, and configurable crossfade (0–12s).
- **Road-Safe Car Mode**: Massive 88dp high-contrast touch targets for safe control while driving.

---

## 🐳 Docker Container Quickstart

To run the Brokeify backend on your server or homelab:

```yaml
# docker-compose.yml
services:
  brokeify:
    image: ghcr.io/mr-nickey/brokeify:latest
    container_name: brokeify
    restart: unless-stopped
    ports:
      - "3487:3487"
    volumes:
      - /path/to/your/music:/music:ro
      - /path/to/brokeify/data:/app/data
    environment:
      - PORT=3487
      - MUSIC_DIR=/music
      - CORS_ORIGIN=*
```

Run container:
```bash
docker compose up -d
```

Open `http://<SERVER_IP>:3487` in your browser, add your music directory, and use the **6-digit Quick Connect code** to link your phone.

---

## 📱 Building the Android App

### Requirements
- Android Studio Ladybug / Meerkat or Gradle 8.8+
- JDK 21
- Android SDK 35 (Min SDK 26)

### Compile & Build APK
```bash
gradle :app:assembleDebug
```

The APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔒 Permissions & Privacy
- `INTERNET`: To stream audio and sync metadata with your Brokeify server.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Required for continuous background playback and notification drawer controls.
- `WAKE_LOCK`: Prevents CPU deep sleep while streaming audio with the screen turned off.
- `CAMERA`: Optional runtime permission used solely for scanning the pairing QR code. Zero photos or videos are stored.
- **Zero Third-Party Ad SDKs or Analytics Trackers**: 100% self-hosted, private, and telemetry-free.

---

<sub>Maintained by [Mr-Nickey/brokeify](https://github.com/Mr-Nickey/brokeify) · Self-Hosted Audio Independence</sub>
