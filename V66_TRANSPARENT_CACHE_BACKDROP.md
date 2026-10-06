# V66 Transparent Cache Backdrop Validation

Purpose: make the pre-rasterized Home blur cache directly visible and remove legacy foreground surfaces that were masking it.

Changes:
- `AllAppsContent`: full-screen drawer background is now `Color.Transparent`.
- `LauncherRoot`: Search full-screen scrim is disabled.
- `LauncherRoot`: Drawer full-screen gradient scrim is disabled.
- Search/Drawer foreground content and z-order are unchanged.
- Home backdrop cache generation, six blur levels, and gesture mapping are unchanged from V65.

Expected result:
- During Search/Drawer transitions, the blurred Home cache should be plainly visible behind foreground UI.
- If the screen is still a flat purple field, the problem is not the legacy overlay and the cache generation/display path needs further debugging.

This is intentionally a diagnostic baseline. After cache rendering is verified, a low-alpha material tint can be added back separately.
