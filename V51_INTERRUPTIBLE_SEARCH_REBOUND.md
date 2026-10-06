# V51 — Interruptible Search Rebound

## Problems fixed

1. Reaching the IME reveal threshold used to show the IME and immediately start the Search Results rebound while the finger was still down.
2. Once rebound started, the animation and the next drag could both write the overscroll position, making the page feel temporarily non-interactive.

## Behavior

- Crossing the reveal threshold immediately triggers haptic feedback and shows the IME.
- Crossing the threshold does **not** start rebound.
- Search Results continue following the same finger after the IME starts opening.
- Rebound starts only after release/fling.
- A new pointer-down immediately cancels the running rebound at its live position and gives control back to the user.
- A generation guard prevents a cancellation-edge animation frame from overwriting newly acquired user input.

## Scope

Only Search rubber-band ownership changed. Search/Home transitions, Blur, Drawer gestures, search interaction settings, IME thresholds, haptic settings, and existing visual constants are preserved.
