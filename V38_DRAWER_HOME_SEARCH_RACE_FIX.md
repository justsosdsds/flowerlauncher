# V38 — Drawer → Home → Search fast-swipe race fix

## Symptom

After closing All Apps back to Home, a very fast downward swipe could sometimes fail to open Search and instead make the drawer reappear.

## Root cause

The drawer-owned pull-to-close gesture always reconstructed its transition position from a hard-coded All Apps anchor:

```kotlin
-1f + distance / dragDistancePx
```

and clamped the result to `[-1f, 0f]`.

During `settle(AllApps -> Home)`, the UI can already be visually at/near Home while `stablePage` is still `AllApps` until the settle coroutine executes its final bookkeeping. If a new downward gesture arrives in that short window, the still-active drawer layer can receive it. The old formula then incorrectly teleports the gesture's logical origin back to `-1f`, making the drawer reappear.

## Fix

1. Capture `verticalPosition.value` and `transitionPullOffset.value` when the drawer-originated gesture actually takes ownership.
2. Continue the gesture from that real position instead of assuming `-1f`.
3. Allow the drawer-originated downward gesture to use the full signed range `[-1f, +1f]` so it can continuously travel:

   `AllApps (-1) -> Home (0) -> Search (+1)`

4. On release, use the shared `verticalTargetForRelease(...)` result directly instead of forcibly converting Search to Home.
5. On cancellation, settle from the actual current signed position rather than always reopening All Apps.

## Expected behavior

- Normal All Apps -> Home downward gesture remains unchanged visually.
- A sufficiently long/fast single downward gesture can pass Home and continue into Search.
- If a second downward swipe starts immediately after returning to Home, it opens Search even when the previous settle tail has not yet updated `stablePage`.
- No artificial delay/debounce is introduced.

## Verification performed

- Existing source-invariant verification passed against V37.
- Transition constants, easing, dp/sp dimensions, resources, build files, and shared release-target function were unchanged.
- Android/Gradle device build was not run in this environment because the project wrapper distribution is not locally available.
