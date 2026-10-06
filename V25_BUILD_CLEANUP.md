# V25 — Build cleanup

This release intentionally adds no new launcher features. It folds recurring OpenCode build fixes into the source archive so importing a newer ZIP no longer reintroduces the same errors.

Fixed:

1. `LauncherRoot.kt`: calls to `Animatable.snapTo/animateTo` are no longer invoked directly from the restricted `awaitEachGesture` receiver. Gesture-frame updates are dispatched through the launcher coroutine scope, and the latest drag progress is retained for settling.
2. `LauncherSettingsScreen.kt`: removed the invalid `androidx.compose.foundation.layout.weight` import.
3. `SearchContent.kt`: restored the missing `SearchActionRow` composable and its `ImageVector` import.
4. `LauncherRoot.kt`: `LauncherSettingsScreen(...)` now receives `hiddenComponentKeys` and `onSetAppHidden`.
5. `AllAppsContent.kt`: `RectangleShape` now imports from `androidx.compose.ui.graphics.RectangleShape`.

Validation:

- Searched the source tree for the two known invalid imports; none remain.
- Checked the `LauncherSettingsScreen` call against its required parameters.
- Checked that `SearchActionRow` has a definition and `ImageVector` import.
- Checked the restricted pointer-gesture block so direct animation suspend calls are only inside launched coroutines.
- A full Gradle compile could not run in this environment because the Gradle 8.13 distribution is not locally cached and external download is unavailable.
