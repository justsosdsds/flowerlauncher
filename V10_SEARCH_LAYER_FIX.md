# v10 Search Layer Fix

- Search backdrop is now a fixed full-screen layer in `LauncherRoot`.
- Search-only rubber-band drag moves only the foreground Search content.
- Pulling Search down no longer exposes a hard horizontal seam / different Home region at the top.
- Search pull limit now uses the physical display height, so IME resize does not change rubber-band geometry.
- First-stage Home -> Search transition behavior is otherwise preserved.
