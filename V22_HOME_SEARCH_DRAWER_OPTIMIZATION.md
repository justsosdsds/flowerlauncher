# V22 Home / Search / App Drawer optimization

## 1. Stable Home wallpaper fix
- Removed all WindowManager `FLAG_BLUR_BEHIND` / `setBlurBehindRadius` wallpaper blur code.
- Search transition blur is now Compose-only on the Home content layer.
- Search backdrop and foreground force visual progress to 0 whenever Search is inactive, preventing stale scrim alpha from darkening the transparent Home wallpaper.

## 2. Search query restore + select-all
- Search query is now stored as `TextFieldValue`, preserving text and selection.
- When Search is entered again and the previous query is non-empty, the full previous query is selected once when keyboard focus is requested.
- Typing immediately replaces the previous query; tapping/editing the field updates selection normally and is not repeatedly overridden.

## 3. App drawer
- Upward swipe from stable Home opens All Apps.
- Downward swipe in All Apps returns Home.
- All Apps includes a top search shortcut and overflow settings entry.
- Overflow opens an App Grid bottom sheet with shortcut/folder, sorting, private space, hidden apps, and grid entries.
- Upward swipe on the bottom sheet opens the full Apps settings page.
- Grid entry opens Drawer Grid settings.
- Hidden Apps switches to the hidden-app drawer view.
- Sort dialog is implemented; selected mode is persisted. Name sorting is functional. Other sort modes keep current source order until usage/install/manual/color metadata is implemented.
- Private Space explanation dialog is implemented.

## Build note
The environment cannot fetch the Gradle 8.13 distribution, so Android compilation could not be completed here. Kotlin source files were structurally checked for balanced braces/parentheses and the Gradle failure is network/distribution related.
