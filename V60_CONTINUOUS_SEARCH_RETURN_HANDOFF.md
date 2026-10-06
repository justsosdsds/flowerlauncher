# V60 — Continuous Search Return Handoff

## Goal
Remove the dead/non-following segment when one pointer performs:

Search bottom overscroll -> Home return threshold -> keeps holding -> reverses toward Search.

## Behavior

- Crossing the Home-return threshold still single-fires the existing haptic and suppresses IME.
- Crossing the threshold no longer calls `closeSearch()` and no automatic settle starts while the pointer is down.
- On the threshold-crossing frame, LauncherRoot immediately switches the same pointer to interactive page ownership and starts the existing compact Search-return visual state.
- The exact threshold-crossing delta belongs only to Search's local rubber-band; every later delta belongs only to the Search <-> Home page transition.
- Reversing direction before release immediately reverses `verticalPosition`; no settle has to be cancelled first.
- Only pointer release chooses the final Home/Search target and starts settle.

## Restricted pointer-scope compile fix retained

The two `beginInteractive()` functions are intentionally non-suspending:

- `SearchInteractionState.beginInteractive()` uses `Job.cancel()` and the existing generation guard.
- `SearchCoordinator.beginInteractive()` is a normal `fun`.

This avoids the restricted-suspension compile failure caused by `cancelAndJoin()` inside Compose pointer scopes.

## Preserved behavior

No changes to Search thresholds, haptic settings, IME overlay, rubber-band physics, Search bar appearance, Drawer, Blur, or existing settle constants.
