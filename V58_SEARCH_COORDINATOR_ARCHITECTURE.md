# V58 Search Coordinator Architecture — 2026-09-30

## Scope

This is phase 1 of the Launcher state architecture refactor. It extracts Search state ownership without changing Search/Home/Drawer visual parameters or adding new user-facing behavior.

## New ownership

`LauncherRoot` now creates one `SearchCoordinator` whose lifetime is above the Search layer composition.

`SearchCoordinator` owns:

- query presence/state
- Search phase: `Hidden / Active / Returning`
- semantic IME phase: `Hidden / Showing / Visible / Hiding`
- session id
- Search-local boundary overscroll through `SearchInteractionState`
- IME threshold single-fire decisions
- Home-return threshold single-fire decision
- page-return keyboard request/suppression state
- synchronous Search session teardown

`SearchContent` now reports Search intents/measurements and executes returned UI effects (hide/show IME, haptic, page-return callback). It no longer owns `homeReturnRequested`, `imeCollapseRequested`, or `imeRevealRequested` booleans.

`LauncherRoot` no longer mirrors `searchHasQuery`, `keyboardRequested`, or `imeSuppressedForSearchReturn`. It reads Search state from the coordinator and only drives the existing page transition when Search requests Home.

## Race reduction

Search session teardown is now triggered from LauncherRoot-level foreground ownership rather than a `LaunchedEffect` inside `SearchContent`. Therefore removing SearchContent from composition cannot skip teardown.

Threshold single-fire uses state transitions instead of independent latches:

- `Visible -> Hiding` for IME collapse
- `Hidden -> Showing` for IME reveal
- `Active -> Returning` for Home return

## Preserved behavior

- fixed full-height Search Results / IME overlay
- floating Search Bar behavior and appearance settings
- top/bottom rubber-band physics
- interruptible rebound
- IME reveal threshold and haptic
- bottom-pull Home threshold and haptic
- existing Search -> Home compact animation
- Home / Drawer continuous gestures
- all existing transition and blur constants

## Verification

- `SearchCoordinator` compiled against isolated Kotlin stubs successfully.
- State transition test passed for session lifecycle, IME single-fire, Home-return single-fire, keyboard suppression, and new-session reset.
- No unexpected files changed outside `LauncherRoot.kt`, `SearchContent.kt`, and new `SearchCoordinator.kt`.
- Existing transition/blur constant declarations are unchanged.
- Full Gradle build cannot run in this environment because the wrapper requires `https://mirrors.aliyun.com/.../gradle-8.13-bin.zip`, which is unreachable here.

## Next architecture phases

Future phases can extract page gesture ownership into `GestureCoordinator`, then Drawer/Home coordination and derived wallpaper blur state. V58 intentionally does not perform those migrations yet.
