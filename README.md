# Lumina — Advanced Android Gallery

Lumina is a Jetpack Compose & Material 3 Android gallery application with animated themes, Google Sign-In + Google Drive photo integration, and a built-in photo editor.

## Project Structure

- `app/src/main/java/com/example/MainActivity.kt` — App entry point, adaptive navigation (`NavigationBar` / `NavigationRail`), and viewer/editor overlays
- `app/src/main/java/com/example/ui/theme/` — Animated theme engine (`Obsidian Neon`, `Warm Terracotta`, `Aurora Glass`, `Sakura Mist`, `Material You`)
- `app/src/main/java/com/example/ui/screens/` — Gallery grid, Albums, Google Drive Cloud browser, Photo Viewer/Slideshow, and Photo Editor
- `app/src/main/java/com/example/editor/` — Non-destructive bitmap processor (filters, adjustments, crop/rotate/straighten, brush markup, text overlay, MediaStore export)
- `app/src/main/java/com/example/data/drive/` — Google Drive API v3 client & `AuthorizationClient` integration
- `app/src/main/java/com/example/data/cloud/` — Firebase Firestore user preferences, cloud favorites, and cloud albums sync
- `app/src/main/java/com/example/data/local/` — Room local gallery database & DataStore preferences

## Building an APK on GitHub Actions

This repository includes `.github/workflows/build-apk.yml`:
1. Open the **Actions** tab on GitHub.
2. Select **Build Lumina Android APK** (runs automatically on every push, or click **Run workflow**).
3. Download **`Lumina-debug-apk`** from the workflow run's **Artifacts** section.
