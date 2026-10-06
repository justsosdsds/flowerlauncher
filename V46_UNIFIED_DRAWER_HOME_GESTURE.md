# V46 Unified Drawer/Home Gesture

## Problem
V45 removed one late-state dead window, but Drawer -> Home still began in
`AllAppsContent` while Home -> Drawer began in `LauncherRoot`. Rapid reversal
therefore crossed two pointer/scroll owners and could still feel delayed or miss
the first part of the next swipe.

## Change
- `LauncherRoot` is now the single vertical gesture owner for Home and All Apps.
- The root detector stays installed while stable/target pages are not Search, so
  Home <-> All Apps settles can be interrupted immediately from the current frame.
- `LazyVerticalGrid.userScrollEnabled` is disabled in the drawer instance; the root
  routes finger deltas to `LazyGridState.dispatchRawDelta()` while the page is at
  All Apps.
- Downward motion first rewinds a scrolled grid; once it reaches the top, leftover
  distance continues directly into All Apps -> Home -> Search.
- Upward motion first completes Home -> All Apps; leftover distance scrolls the
  grid in the same pointer stream.
- Drawer grid release momentum is preserved in both directions with the existing
  decay loop.
- A new pointer gesture cancels any running manual drawer fling before taking over.
- The old `AllAppsContent.onCloseDrag*` path and its full-screen close detector were
  removed, so there is no second Drawer/Home vertical controller left to re-enter.

## Intentionally unchanged
- Search foreground gesture behavior.
- Gesture sensitivity settings and thresholds.
- Home/Drawer transition rendering and blur curves.
- Drawer app taps, long presses, header controls, settings UI, and alphabet index.

## Verification
- Source invariants checked for single Drawer/Home vertical owner and bidirectional
  grid routing/fling.
- Kotlin parser smoke check found no syntax-token errors (full Android symbols are
  unavailable to standalone kotlinc).
- Android Gradle compile could not run because the wrapper requires Gradle 8.13
  from the configured Aliyun mirror, which is unreachable in this environment.
