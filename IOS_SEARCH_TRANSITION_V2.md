# iOS-style search transition v2

This revision separates the search gesture into two explicit phases.

## 1. Home -> Search (first downward gesture)
- Home and Search occupy the same screen-space coordinates from the first frame.
- Search no longer starts above the physical display.
- Both layers share one resisted downward offset while the finger is down.
- Home blur/opacity/scale are driven only by `progress`.
- On release, `progress` settles while the shared offset returns to zero in the same fixed 310 ms window.
- Result: Home and Search visually rebound together into the stable Search state.

## 2. Search already open -> downward pull
- A new downward gesture may begin anywhere inside the Search layer (search field, result area, blank area).
- This gesture updates only `searchPullOffset`.
- Home translation and Home/wallpaper blur do not change.
- Resistance uses `limit * raw / (limit + raw)`.
- Release always returns Search to zero with the fixed-duration `LinearThenBrakeEasing` curve.
- Upward gestures are not claimed by this handler so Search content can retain its normal upward behavior.

## State separation
- `progress`: Home/Search transition only.
- `transitionPullOffset`: temporary shared offset during the first Home -> Search drag.
- `searchPullOffset`: Search-only rubber-band offset after Search is stably open.
