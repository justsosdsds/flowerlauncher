# V67 — Single-source Home backdrop cache

V67 intentionally returns to the V65 full-Home cache baseline and removes the V66 clock-hole/dynamic-clock architecture.

## Core change

The launcher now draws a software snapshot of the current system wallpaper as an actual Compose layer behind Home. The exact same rendered layer is captured together with the complete Home UI (clock included) by `homeCaptureLayer`.

Therefore normal Home and the six cached blur levels share one pixel source:

`HomeWallpaperLayer + HomeContent -> GraphicsLayer capture -> 1/2 downsample -> levels 0..5`

`HomeBackdropCache` no longer re-reads/re-centercrops WallpaperManager during cache construction.

## Validation simplifications

- no clock hole
- no dynamic clock cache
- no clock bounds tracking
- Drawer legacy purple background is transparent
- Search and Drawer legacy full-screen scrims are disabled for cache visibility validation

The minute clock participates in `contentRevision`, so it may rebuild the six half-resolution images once per minute while Home is idle. This is intentional for the baseline.
