# Genga World

Genga World is a premium, modern Android client designed for browsing, discovering, and streaming movies, TV shows, and anime. Built using **Jetpack Compose**, **Material 3**, and **Media3 ExoPlayer**, the client runs a built-in high-performance HTTP server locally on the device to deliver smooth, signed, and ad-free playback experiences.

---

## 🌟 Key Features

*   **Premium Visuals & Shimmer Loaders**: Modern dark theme layout with dynamic shimmer skeleton screens that load content beautifully when the app starts.
*   **Smart Video Player**:
    *   Responsive aspect ratio scaling (resizes properly across tablets and phones).
    *   Transparent custom gesture overlays for instant play/pause and controls toggling.
    *   Dual-mode bottom playback sheets for seamless quality, language dubs, subtitles, and speed selection.
    *   Premium subtitles configured via native canvas rendering (ensures perfect centering) with clean white text and thin black outlines.
*   **Intuitive Search Engine**: Fast search with automatic trending suggestions and auto-collapsing keyboard handling upon selection.
*   **Offline persistence**:
    *   **Favorites Watchlist**: Keep track of titles you want to watch.
    *   **Watch History**: Remembers your exact watch progress down to the millisecond so you can pick up right where you left off.
*   **Multi-Language Audio**: Automatically supports and detects localized dubs (such as Hindi dubs) and disables redundant subtitle tracks by default.

---

## 🛠️ Technology Stack

*   **UI/Framework**: Kotlin, Jetpack Compose, Material 3
*   **Image Loading**: Coil
*   **Media Player**: Android Media3 ExoPlayer (Canvas Subtitle Rendering)
*   **Database**: Room Database (for Watchlist & History persistence)
*   **Networking**: Retrofit 2 & OkHttp
*   **Built-in Server**: Custom Local HTTP Server running directly on-device in Kotlin (`LocalMovieBoxServer`)

---

## 🚀 Setup & Installation

### 1. Build the Android Project
1. Open the project folder in **Android Studio**.
2. Let Gradle sync and resolve dependencies.
3. Select your device or emulator and click **Run**. (No external server setup is required!)

---

## 📦 Generating Signed Release Build

To generate the production APK:
```bash
./gradlew assembleRelease
```
The compiled, signed release package will be located at:
`app/build/outputs/apk/release/app-release.apk`
