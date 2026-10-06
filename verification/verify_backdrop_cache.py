from pathlib import Path
root = Path(__file__).resolve().parents[1]
launcher = (root/'app/src/main/java/com/flower/launcher/ui/LauncherRoot.kt').read_text()
cache = (root/'app/src/main/java/com/flower/launcher/ui/HomeBackdropCache.kt').read_text()
home = (root/'app/src/main/java/com/flower/launcher/ui/HomeContent.kt').read_text()
checks = {
    'live Home no longer owns RenderEffect': 'renderEffect = homeBlurEffects' not in launcher,
    'six total cache levels': 'HOME_BACKDROP_BLUR_LEVELS = 5' in cache,
    'half resolution cache': 'HOME_BACKDROP_DOWNSAMPLE = 2' in cache,
    'Search/Drawer use shared CachedHomeBackdrop': 'CachedHomeBackdrop(' in launcher,
    'static cache excludes clock bounds': 'excludedClockBounds = clockBounds' in cache,
    'clock has independent cache': 'dynamicClockLevels' in cache and 'clockRevision' in cache,
    'wallpaper changes invalidate static cache': 'ACTION_WALLPAPER_CHANGED' in cache,
    'Home capture uses GraphicsLayer bitmap': 'sourceLayer.toImageBitmap()' in cache,
    'clock bounds are reported separately': 'onClockBoundsChanged' in home,
}
for name, ok in checks.items():
    print(('PASS' if ok else 'FAIL'), name)
if not all(checks.values()):
    raise SystemExit(1)
