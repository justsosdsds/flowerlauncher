# V27 — Restore wallpaper Gaussian blur

- Restored Android 12+ wallpaper/background blur during Home -> Search transition and while Search is open.
- Blur remains quantized to 6 levels and follows the existing blur strength setting.
- Window dim is explicitly forced off on every blur update (`FLAG_DIM_BEHIND` cleared, `dimAmount = 0f`) so restored blur does not restore the old dark Home issue.
- Stable Home always requests blur radius 0 and removes `FLAG_BLUR_BEHIND`.
- Compose Home RenderEffect blur remains enabled as before.
