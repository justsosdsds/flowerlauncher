# v35 — Floating All Apps transition

## Goal
Keep the unified signed vertical navigation introduced in v34, but stop treating All Apps as a mirrored Search transition.

## Home -> All Apps visual mapping
- Home stays physically stable on the drawer side; it no longer translates/scales like Search.
- A dedicated All Apps scrim fades in progressively. It is slightly stronger toward the bottom, matching the drawer's arrival direction.
- Home/Wallpaper Gaussian blur increases with the upward gesture. Drawer blur is intentionally a little lighter than Search.
- All Apps starts visually near 200 px from the top of screen height and approaches the final top edge with `smoothStep` rather than a linear push.
- All Apps also fades/scales in, so it reads as a floating drawer instead of a full page sliding from outside the screen.
- Closing All Apps simply reverses the same progress mapping.

## Gesture architecture
No second transition controller was added. `VerticalPageTransitionController` remains the single source of truth:
- `-1 = All Apps`
- `0 = Home`
- `+1 = Search`

This preserves v34's interruptible/reversible gesture behavior.
