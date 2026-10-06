# V40 Compact Search -> Home Return

This revision changes only the visual/settle behavior of the Search -> Home return path.
The Home -> Search entry mapping, gesture sensitivity settings, and drawer hand-off remain intact.

## Target behavior

- Home is a fixed background plane during Search -> Home: no translation, scale, or alpha motion.
- Home Compose blur and wallpaper blur relax progressively to zero across the full return progress.
- Search foreground only moves a small distance instead of flying off-screen.
- Search Bar total upward travel: 28dp.
- Lower Search content total upward travel: 32dp (28dp foreground + 4dp lower-content offset).
- Fast exit begins at returnProgress 0.55.
- Search Bar is fully faded by returnProgress 0.80.
- Search content is fully faded by returnProgress 0.85.
- Foreground scale reaches 0.965 by the Search Bar exit point.
- Search -> Home release settle uses a dedicated 110ms ease-out-quartic completion.
- While the finger is down, position still follows the gesture directly; the 110ms duration applies only after release/confirmation.

## Implementation notes

- Added `returningFromSearch` to `VerticalPageTransitionController` so return visuals are not inferred from stable Search state.
- `SearchContent` receives a dedicated `returnProgress` (0 at Search, 1 at Home).
- Search backdrop/scrim remains screen-fixed and separate from foreground transforms.
- The existing Search root container no longer performs full-page exit translation/scale during return.
- A linear blur-level mapping is used only for Search -> Home so blur does not stay saturated for most of the return.

## Verification

Static delimiter/structure checks pass for the two edited Kotlin files.
Android compilation could not be completed in the execution environment because the Gradle wrapper distribution (Gradle 8.13) is not cached and the configured Aliyun mirror is unreachable from this environment.
