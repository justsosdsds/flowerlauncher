from pathlib import Path
root = Path(__file__).resolve().parents[1]
sc = (root/'app/src/main/java/com/flower/launcher/ui/SearchContent.kt').read_text()
settings = (root/'app/src/main/java/com/flower/launcher/ui/SearchSettingsScreen.kt').read_text()
prefs = (root/'app/src/main/java/com/flower/launcher/search/SearchPreferencesRepository.kt').read_text()
manifest = (root/'app/src/main/AndroidManifest.xml').read_text()
checks = {
    'drag-time reveal threshold': 'visual >= revealPullThresholdPx' in sc and 'interactionScope.launch' in sc,
    'release no longer decides reveal': 'visual >= revealPullThresholdPx' not in sc[sc.index('override suspend fun onPreFling'):sc.index('override suspend fun onPostFling')],
    'haptic threshold callback': 'performImeRevealThresholdHaptic(' in sc,
    'single-fire reveal guard': 'imeRevealRequested && available.y > 0f' in sc,
    'haptic switch persisted': 'imeRevealHapticEnabled' in prefs,
    'haptic strength persisted': 'imeRevealHapticStrengthPercent' in prefs,
    'interaction dedicated page': 'data object Interaction' in settings and 'SearchInteractionSettingsPage(' in settings,
    'interaction entry': 'subtitle = "输入法阈值、回弹触发与振动反馈"' in settings,
    'vibrate permission': 'android.permission.VIBRATE' in manifest,
    'compact return constants preserved': all(x in sc for x in [
        'SEARCH_RETURN_BAR_MAX_UP = 28.dp',
        'SEARCH_RETURN_CONTENT_EXTRA_UP = 4.dp',
        'SEARCH_RETURN_FAST_EXIT_START = 0.55f',
        'SEARCH_RETURN_BAR_GONE_PROGRESS = 0.80f',
        'SEARCH_RETURN_CONTENT_GONE_PROGRESS = 0.85f',
        'SEARCH_RETURN_FOREGROUND_SCALE = 0.965f',
    ]),
}
failed = [k for k, v in checks.items() if not v]
for k, v in checks.items():
    print(('PASS' if v else 'FAIL'), k)
if failed:
    raise SystemExit('failed: ' + ', '.join(failed))
