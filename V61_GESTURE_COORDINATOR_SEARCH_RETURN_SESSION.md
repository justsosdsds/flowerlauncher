# V61 — GestureCoordinator Search Return Session

## Goal

Migrate the complete non-empty Search bottom-return path into one gesture-session owner:

```
Search bottom rubber-band
→ configured Home threshold
→ Search↔Home interactive drag
→ same-pointer reverse
→ release/cancel
→ settle Home/Search
```

This removes the V59/V60 split ownership between SearchCoordinator pointer latches,
LauncherRoot local `returnArmed` / `handoffStarted` flags, and page-transition state.

## Architecture

New `GestureCoordinator.kt` owns the cross-surface pointer session.

Session phases:

- `Idle`
- `SearchBottomCandidate(pointerId, searchSessionId)`
- `SearchToHome(...)`

`SearchToHome` owns:

- source pointer id
- Search session id
- handoff finger origin
- handoff page-position origin
- current page position
- downward reversal distance
- release velocity tracking

`LauncherRoot` only forwards Final-pass pointer frames and applies returned transition commands.

## Ownership boundary

SearchCoordinator still decides when the local bottom rubber-band reaches the configured
`homeReturnPullThresholdDp`. It does **not** itself create a pointer-held latch.

At threshold:

1. Search reports `SearchBoundaryEffect.ReturnHome`.
2. Root asks GestureCoordinator to atomically accept the current candidate pointer.
3. Only after acceptance does Search enter `Returning`, suppress IME, and perform the
   configured threshold haptic.
4. The exact threshold-crossing delta remains Search-local rubber-band motion.
5. The next pixel belongs exclusively to the page-level Search↔Home transition.

If no matching pointer candidate exists, the transfer is rejected and Search remains Active.
This prevents an orphan `Returning` state.

## Release race fix

Threshold commit and pointer-up may occur on the same event. `onSearchPointerFrame(...)`
handles this directly:

```
commit SearchToHome
→ establish origin from current frame
→ pressed == false
→ emit beginInteractive + settle Home in the same frame
```

No `returnArmed`, `handoffStarted`, or `pageReturnPointerHeld` state exists anymore.

## Same-pointer reversal

After handoff:

- further upward motion drives page position toward Home;
- downward reversal immediately drives it back toward Search;
- release target is chosen from reversal distance, current position and velocity;
- settling back to Search restores Search Active state, allows IME again, and rebounds the
  local bottom rubber-band.

## Compile-stability invariant

The OpenCode-reported repeated build fix is preserved permanently:

- `SearchInteractionState.beginInteractive()` is a normal `fun` using `cancel()`.
- `SearchCoordinator.beginInteractive()` is a normal `fun`.
- no `cancelAndJoin` is used by either path.

This is required because Compose restricted pointer scopes cannot call arbitrary suspend
functions.

## Files changed from V60

- `app/src/main/java/com/flower/launcher/ui/GestureCoordinator.kt` — new
- `app/src/main/java/com/flower/launcher/ui/LauncherRoot.kt`
- `app/src/main/java/com/flower/launcher/ui/SearchContent.kt`
- `app/src/main/java/com/flower/launcher/ui/SearchCoordinator.kt`
- `app/src/main/java/com/flower/launcher/ui/SearchInteractionState.kt` — documentation only

## Verification

- legacy handoff latches removed;
- threshold transfer requires an accepted current-pointer candidate;
- same-frame threshold + release resolves to Home;
- same-pointer reversal resolves to Search when reversal criteria are met;
- `beginInteractive()` remains non-suspend;
- Search/Drawer/blur constants and Search settings were not intentionally changed.

A full Gradle build was attempted but the project wrapper could not resolve
`mirrors.aliyun.com` in the execution environment, so no full Android compile is claimed.
The new coordinator and SearchCoordinator were syntax-compiled with minimal Kotlin stubs,
and coordinator session tests passed.
