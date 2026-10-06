# V28 — Search scrim spatial gradient

- Replaced the uniform full-screen SearchScrim fade with the selected “方案 B”.
- The first 10% of Home → Search progress has no dark scrim.
- After that threshold, the scrim fades in and expands downward as a vertical gradient.
- At full Search state the gradient is strongest near the top, softer in the middle, and weakest toward the bottom.
- Scrim animation reads `progress` in `drawBehind`, so frame updates invalidate drawing instead of recomposing `LauncherRoot`.
- Existing Gaussian wallpaper blur, v26 Home dim prevention, and v27b theme attribute fix are preserved.
