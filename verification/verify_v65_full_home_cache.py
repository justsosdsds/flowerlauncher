from pathlib import Path
root = Path(__file__).resolve().parents[1]
cache = (root / 'app/src/main/java/com/flower/launcher/ui/HomeBackdropCache.kt').read_text()
launcher = (root / 'app/src/main/java/com/flower/launcher/ui/LauncherRoot.kt').read_text()
home = (root / 'app/src/main/java/com/flower/launcher/ui/HomeContent.kt').read_text()
checks = {
    'single cache levels': 'val levels: List<ImageBitmap>' in cache,
    'six levels': 'HOME_BACKDROP_BLUR_LEVELS = 5' in cache,
    'half resolution': 'HOME_BACKDROP_DOWNSAMPLE = 2' in cache,
    'no dynamic clock cache': 'dynamicClockLevels' not in cache and 'dynamicClockBounds' not in cache,
    'no clock bounds in cache': 'clockBounds' not in cache,
    'no clock crop': 'cropAndDownsample' not in cache,
    'no clock hole': 'clipOutRect' not in cache and 'excludedClockBounds' not in cache,
    'minute participates in full revision': 'resetSignal,\n        minuteClock,' in launcher,
    'launcher no clock bounds plumbing': 'clockBounds' not in launcher and 'clockRevision' not in launcher,
    'home no bounds callback': 'onClockBoundsChanged' not in home and 'boundsInRoot' not in home,
    'hardware bitmap guard': '.ensureSoftware()' in cache and 'Bitmap.Config.HARDWARE' in cache,
}
failed = False
for name, ok in checks.items():
    print(('PASS' if ok else 'FAIL'), name)
    failed |= not ok
raise SystemExit(1 if failed else 0)
