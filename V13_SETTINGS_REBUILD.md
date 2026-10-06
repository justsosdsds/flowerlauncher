# V13 — Settings rebuild

This version rebuilds FlowerLauncher settings using the analyzed Smart Launcher settings tree as a functional specification, while keeping FlowerLauncher independent.

## Implemented and wired

- New top-level Settings home.
- Home: clock, bottom search bar, grid columns, layout lock.
- Search: reuses the existing Search settings page and persisted SearchPreferences.
- Gestures: enable/disable swipe navigation and haptic feedback. The switches affect the actual launcher gesture handlers.
- Global appearance: enable/disable Search background blur and adjust blur strength. This affects both Home RenderEffect blur and wallpaper blur.
- Wallpaper: launches the existing system wallpaper flow.
- Folders: launches the existing Add Folder flow.
- Widgets: launches the existing AppWidget picker flow.
- Security / hidden apps: real hidden-app manager wired to AppCustomizationRepository.
- About: package/version information.
- Desktop long-press sheet now contains an “All settings” entry. Existing “Global appearance” and “Home settings” entries open the matching full settings sections.

## Visible but intentionally disabled

The settings tree also exposes App page, Notifications, Screen and Backup as planned sections, but marks them as not implemented. No fake switches are provided.

## Persistence

Existing Home and Search settings continue using their current repositories. New gesture/appearance settings are persisted by `LauncherPreferencesRepository` in `flower_launcher_settings`.

## Build verification

A Gradle compile was attempted, but the environment cannot download the project Gradle 8.13 distribution. Source-level structure was checked, but this archive has not been APK-compiled in this environment.
