# Desktop edit panel (v4)

Added on top of the v3 search animation build.

## Interaction
- Long-press the home screen to open a Material 3 bottom sheet.
- The desktop scales down slightly while the sheet is open.
- The sheet includes a layout lock button.

## Functional actions
- Add app: choose an installed app and pin it to the front of the home grid.
- Add folder: name a folder, select apps, then open the folder from the home screen.
- Add widget: opens Android's widget picker, hosts the selected widget with AppWidgetHost, and persists widget IDs.
- Wallpaper: opens the system wallpaper chooser.
- Appearance: toggle the clock and bottom search bar.
- Home settings: choose 3/4/5 grid columns and lock/unlock layout editing.

## Persistence
- Home layout/settings use SharedPreferences in `HomeLayoutRepository`.
- Widget IDs use a separate SharedPreferences store in `MainActivity`.

## Main files
- `ui/DesktopSettingsSheet.kt`
- `home/HomeLayoutRepository.kt`
- `ui/HomeContent.kt`
- `ui/LauncherRoot.kt`
- `MainActivity.kt`

## Build note
A full Gradle compile could not be run in the current environment because the configured Gradle distribution is not cached and network download is unavailable.
