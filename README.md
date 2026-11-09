# Brokeify (Android Native Edition)

**Because $11.99 a month is highway robbery.**  
*Native Android Self-Hosted Music Aggregator uniting Jellyfin, Navidrome, Emby, Plex, Internet Archive, and Radio-Browser with a Car-Ready Audio Engine & Offline Vault.*

---

## 🎧 What is Brokeify Android?

**Brokeify Android** is a native Android application built with **Kotlin** and **Jetpack Compose**, featuring:
- **Homelab Sources Hub:** Connect directly to Jellyfin, Emby, Navidrome / Subsonic REST APIs, Plex, and public internet streams (Radio-Browser directory & Internet Archive live concerts).
- **Retro '99 Stereo Deck UI:** Late-90s hardware rack interface with real-time animated spectrum analyzer, digital LCD readouts, and 6 switchable theme skins (Cyber Deck, Classic Stereo, Modular Studio, Amber Phosphor, Platinum Light, Solar Daylight).
- **Car Cockpit Driving Mode:** High-contrast, road-bump-friendly driving interface with massive 88dp tactile buttons, speedometer stream indicators, quick driving EQ presets, and road noise leveling.
- **Audio Lab & 5-Band Graphic Equalizer:** 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz biquad bands with presets (Flat, Car Stereo, Bass Boost, Rock, Vocal, Late Night, Custom), EBU R128 LUFS normalization (-14 LUFS target), and configurable crossfade (0–12s).
- **MusicBrainz & Slskd Discovery Engine:** Open metadata resolution and P2P lossless FLAC download queue manager.
- **Offline Vault:** Automatic preloading and caching to protect playback against highway cell tunnel dropouts.
- **Maintainerr Statistics:** Play count analytics, total listening duration, and subscription spite savings tracker ($143.88/yr saved).

---

## 🏗️ Architecture & Stack

- **Target Platform:** Android SDK 35 (Min SDK 26)
- **Language:** Kotlin 2.1
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture:** MVVM with Kotlin Coroutines & StateFlow
- **Build System:** Gradle (Kotlin DSL `.gradle.kts`) with Version Catalog (`libs.versions.toml`)
- **Package:** `com.example.brokeify`
- **Application ID:** `com.aistudio.brokeify.xqkmpz`

---

## 🚀 Building & Running

### Android Studio / Gradle
```bash
gradle :app:assembleDebug
```
Output APK generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

<sub>Built with spite, hard drives, and Google AI Studio · Zero Subscriptions Forever</sub>
