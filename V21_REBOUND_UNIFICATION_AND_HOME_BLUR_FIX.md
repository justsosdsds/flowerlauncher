# V21 — Rebound unification + Home blur fix

## Fixed

1. Home stable state now forces system wallpaper blur to 0 when Search is inactive.
2. Home rendering also treats stable Home as transition progress 0, preventing stale Compose blur after interrupted gestures.

## Unified rebound timing

The following now all use the same user-configured rebound duration (`160..600 ms`) as a fixed duration:

- Home -> Search settle
- Search -> Home settle
- Search overscroll -> Search resting position

Distance-based duration shortening and the former close-duration multiplier were removed.

## Unified easing

All three rebound paths now use the selected curve #6 from the HTML comparison:

`easeOutQuart(t) = 1 - (1 - t)^4`

This gives a faster initial return and a softer deceleration near the resting position without changing total duration.
