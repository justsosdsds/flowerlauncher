# V26 — Home wallpaper dim fix

## Problem
On some OEM ROMs the stable Home screen still appeared uniformly dark even though blur had been removed. The wallpaper itself remained sharp, indicating window/scrim dimming rather than a blur issue.

## Fix
- Disabled window background dimming in `Theme.FlowerLauncher.Transparent` with `android:backgroundDimEnabled=false` and `android:backgroundDimAmount=0`.
- Added an Activity-side safeguard that clears `FLAG_DIM_BEHIND` and forces `dimAmount=0f` at startup.
- Changed the Search dark scrim from an always-composed alpha-zero layer to a conditionally composed layer that exists only while Search is active/targeted open.
- Stable Home therefore has no SearchScrim composable and no WindowManager dim behind it.

## Expected result
When Home is idle, the system wallpaper is displayed at its original brightness. Darkening is introduced only during the Search transition/search state.
