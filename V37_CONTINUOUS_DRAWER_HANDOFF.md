# V37 — Continuous Home → All Apps gesture handoff

## Goal

Make one uninterrupted upward gesture behave as one continuous vertical surface:

1. the Home gesture first drives the shared vertical page transition;
2. when `VerticalPage.AllApps` reaches `-1f`, any remaining upward finger delta is handed directly to the app grid;
3. if the finger reverses downward, the grid scroll is unwound first;
4. once the grid reaches its top, remaining downward delta resumes `AllApps -> Home` (and can continue through Home toward Search in the same gesture).

The finger does not need to lift between opening All Apps and scrolling the grid.

## Implementation

- Hoist `LazyGridState` from `AllAppsContent` into `LauncherRoot`.
- Keep the Home-originated pointer stream owned by `LauncherRoot`; Compose cannot reliably add a newly composed drawer child to the hit-test path halfway through an already-running gesture.
- Split each drag delta at the `AllApps = -1f` boundary:
  - transition-consumed portion updates `VerticalPageTransitionController`;
  - overflow portion calls `LazyGridState.dispatchRawDelta()`.
- On reverse drag, consume the delta from the grid first, then feed only the unconsumed remainder back into the page transition.
- Transfer upward release momentum to the drawer after a real handoff using a short decaying scroll mutation.
- Reset the hoisted grid to item 0 only after Home is stable again, preserving the old reopen-at-top behavior.

## Search-layer conflict fix

The Search foreground now owns z-order and its full-screen gesture modifier only while Search is actually on the active side of the signed transition (`position > 0` or target is Search). This prevents a stale `searchLayerActive=true` from sitting above All Apps and consuming drawer gestures during `Home -> AllApps`.

## Debug cleanup

Removed the temporary yellow transition-state overlay from `LauncherRoot`.

## Verification performed here

- Existing transition helper/easing functions and prior blur constants were left unchanged.
- Source braces/parentheses balance was checked.
- Boundary routing was simulated for upward overflow and reverse-downward handoff.
- Android's current Compose API reference confirms `LazyGridState` exposes `dispatchRawDelta`, `canScrollBackward/Forward`, `scroll`, and `ScrollScope.scrollBy`.

A full Gradle compile could not run in this environment because the project's wrapper points to an external Gradle distribution and this runtime cannot download it.
