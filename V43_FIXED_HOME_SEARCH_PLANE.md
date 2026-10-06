# V43 Fixed Home search plane

## Problem

During one continuous downward gesture from All Apps through Home into Search, the Home layer
could jump downward as soon as the signed vertical transition crossed from the drawer side
(< 0) to the Search side (> 0).

The drawer side already forced Home `translationY = 0`, but the Search side used
`transitionPullOffset`. For a drawer-originated gesture that offset can contain travel
accumulated before reaching Home, so crossing zero applied a large offset in one frame. The
clock made the jump easiest to notice, but the transform affected the whole Home layer.

## Fix

Home is now spatially anchored on both sides of the vertical transition:

- All Apps side: `translationY = 0`
- Search side: `translationY = 0`
- Search return: `translationY = 0`

Home -> Search keeps the existing scale, alpha and blur depth effects. Search remains the
foreground layer responsible for motion.

This means Drawer -> Home -> Search can remain one continuous gesture without carrying the
Drawer travel distance into the Home layer's Y position.
