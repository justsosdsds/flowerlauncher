from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
search = (root/'app/src/main/java/com/flower/launcher/ui/SearchContent.kt').read_text()
launcher = (root/'app/src/main/java/com/flower/launcher/ui/LauncherRoot.kt').read_text()
interaction = (root/'app/src/main/java/com/flower/launcher/ui/SearchInteractionState.kt').read_text()

checks = {
    'Search foreground uses ADJUST_NOTHING': 'SOFT_INPUT_ADJUST_NOTHING' in launcher,
    'Non-Search restores ADJUST_RESIZE': 'SOFT_INPUT_ADJUST_RESIZE' in launcher,
    'Results viewport is measured': 'resultsViewportHeightPx' in search and '.onSizeChanged { resultsViewportHeightPx = it.height }' in search,
    'Real Search content is measured': 'resultsContentHeightPx' in search and '.onSizeChanged { resultsContentHeightPx = it.height }' in search,
    'Collapse uses geometric overflow': 'resultsOverflowAboveImePx > collapseOverflowThresholdPx' in search,
    'Old maxValue collapse check removed': 'scrollState.maxValue > collapseOverflowThresholdPx' not in search,
    'Floating field owns imePadding': '.imePadding()' in search,
    'Root Search box no longer chains nav+ime padding': '.navigationBarsPadding()\n            .imePadding()' not in search,
    'No dynamic IME spacer': 'Spacer(Modifier.height(ime' not in search and 'Spacer(modifier = Modifier.height(ime' not in search,
    'Interruptible rebound preserved': 'cancelAndJoin()' in interaction and 'reboundGeneration' in interaction,
    'IME reveal still immediate': 'focusRequester.requestFocus()\n                    keyboardController?.show()' in search,
}

failed=[]
for name, ok in checks.items():
    print(('PASS' if ok else 'FAIL'), name)
    if not ok: failed.append(name)
if failed:
    raise SystemExit('failed: ' + ', '.join(failed))
