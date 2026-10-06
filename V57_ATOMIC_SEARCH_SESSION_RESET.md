# V57 — Atomic Search Session Reset

## Fix

V56 reset transient Search gesture latches after `scrollState.scrollTo(0)`. Because `scrollTo` suspends, a long result list at the bottom could let the reset coroutine be cancelled by the next Search session before `homeReturnRequested` was cleared. The next session would then consume all vertical nested-scroll input while clicks still worked.

V57 makes Search session teardown atomic and non-suspending:

- Clear `imeCollapseRequested`, `imeRevealRequested`, and `homeReturnRequested` first.
- Cancel/reset Search overscroll immediately.
- Reset the result `ScrollState` synchronously with `dispatchRawDelta(-value)` while Search is off the foreground.
- Keep V56's independent Home-return threshold haptic switch and strength setting unchanged.

This directly covers the stable reproduction path: non-empty query -> expanded long category -> full-screen -> scroll to bottom -> pull past Home threshold -> release -> re-enter Search.
