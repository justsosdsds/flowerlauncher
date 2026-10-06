# V55 — Search Bar Color + Opacity

## Scope
V55 is based on V54 and only expands the Search > Appearance settings for the floating search bar.

## Added settings
- Search bar color preset: Neutral, Red, Orange, Green, Cyan, Blue, Purple, Pink.
- Search bar color depth: now controls darkness only; it no longer changes alpha.
- Search bar opacity: independent 0–100% alpha control.

## Migration
If an existing install has no opacity key yet, V55 derives the initial opacity from the previous V54 depth-to-alpha formula so upgrades keep a similar visual weight.

## Preserved behavior
V54 Search gesture behavior is unchanged, including fixed Results viewport / IME overlay, interruptible rubber-band rebound, IME thresholds and haptics, and bottom-pull Search-to-Home threshold.
