# V59 — Same-pointer Search return handoff

## Problem
When a non-empty Search page crossed the bottom-rubber-band Home threshold, V58 immediately started Search -> Home settle and moved SearchCoordinator to `Returning`. If the same finger stayed down and reversed downward, Search nested-scroll consumed the old pointer stream while the page transition could not take ownership. The result could be Home + IME without Search.

## Changes
- Added an always-observing LauncherRoot pointer handoff for non-empty Search gestures.
- Crossing the Home threshold still starts the existing compact Search -> Home settle.
- The threshold-crossing delta is never reused for page movement.
- Any subsequent movement from the same pointer cancels the settle and drives `verticalPosition` interactively from its live value.
- Downward reversal can settle back to Search; continuing upward remains committed to Home.
- Search session teardown is deferred while the threshold-committing pointer remains down.
- `Hidden` / `Returning` Search states cannot trigger IME reveal/hide boundary effects.
- Reopening Search from the same pointer restores Active state, keyboard request, and rebounds residual local overscroll.

## Preserved
- Existing Search -> Home compact animation and timing.
- Home-return distance threshold and haptic settings.
- IME overlay model, Search bar appearance, blur, Drawer gestures, and launcher gesture sensitivity.
