# V52 — Fixed Search viewport + IME overlay

## Goal
Remove the one-frame Search result jump seen when a normal-speed upward swipe hides the IME.

## Changes
- Search foreground uses `SOFT_INPUT_ADJUST_NOTHING`; other launcher/settings surfaces restore `SOFT_INPUT_ADJUST_RESIZE`.
- Search Results keep a fixed full-height viewport while IME animates.
- Only the floating Search bar consumes `imePadding()`, so it follows the animated IME top.
- Results no longer use IME-driven layout padding.
- The fixed Search-bar clearance is content-space padding, not viewport-space padding, so the scroll viewport remains stable.
- IME-collapse eligibility no longer depends on `ScrollState.maxValue`. It compares measured real Search content height with the visible area above the floating bar while IME is present.
- No dynamic IME-height spacer is added.

## Preserved behavior
- Non-empty query still blocks page-level Search -> Home swipe.
- Short content still enters Search rubber-band instead of hiding IME when it does not exceed the configured overflow threshold.
- Long content may hide IME on upward input when its overflow exceeds the configured threshold.
- IME reveal threshold, haptic feedback, release-only rebound, and interruptible rebound remain unchanged.
- Home/Search transition constants, Blur, Drawer handoff, and gesture sensitivity are unchanged.
