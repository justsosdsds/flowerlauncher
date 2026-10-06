# V36 — Drawer 200px start + blur stability

## Changes

- The All Apps floating drawer now starts with its top at approximately **200 raw pixels below the screen top**, instead of 86% of screen height.
- The drawer still follows the unified V34 vertical gesture controller and smoothly approaches its final top position.
- Stabilized Android 12+ wallpaper Gaussian blur:
  - remember the currently desired blur radius;
  - re-apply blur on the next UI frame after enabling it;
  - re-apply the current blur whenever the launcher window regains focus;
  - keep `FLAG_DIM_BEHIND` disabled and `dimAmount = 0`;
  - clear blur on disposal.

This addresses cases where OEM window composition recreates/clears `FLAG_BLUR_BEHIND` while the transition progress remains unchanged, which previously left the drawer/search background unexpectedly sharp.
