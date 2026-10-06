#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
ui = root / 'app/src/main/java/com/flower/launcher/ui'
sc = (ui / 'SearchCoordinator.kt').read_text()
content = (ui / 'SearchContent.kt').read_text()
launcher = (ui / 'LauncherRoot.kt').read_text()
vertical = (ui / 'VerticalPageTransitionController.kt').read_text()
test = (root / 'app/src/test/java/com/flower/launcher/ui/SearchCoordinatorTest.kt').read_text()


def between(text: str, start: str, end: str) -> str:
    a = text.index(start)
    b = text.index(end, a)
    return text[a:b]


top = between(sc, 'fun onTopBoundaryDrag(', 'fun onBottomBoundaryDrag(')
bottom = between(sc, 'fun onBottomBoundaryDrag(', 'fun beginInteractive()')
pre = between(sc, 'fun onPreScroll(', 'fun onTopBoundaryDrag(')
release = between(sc, 'fun onRelease(', '\n    }\n}\n',)

checks = {
    'empty/non-empty share top boundary handler':
        'if (!state.hasQuery)' not in top and 'interaction.addBoundaryDrag(deltaY)' in top,
    'IME reveal threshold is query agnostic':
        'visual >= revealPullThresholdPx' in top and 'SearchBoundaryEffect.RevealIme' in top,
    'bottom return remains non-empty-only':
        'if (!state.hasQuery) return SearchBoundaryEffect.None' in bottom,
    'IME collapse behavior remains non-empty-only':
        'state.hasQuery &&' in pre and 'SearchPreScrollEffect.HideIme' in pre,
    'shared rebound no longer depends on query':
        'state.hasQuery' not in release and 'interaction.startRebound' in release,
    'SearchContent old query lock removed':
        'queryLocked' not in content,
    'SearchContent routes all downward boundary drag to unified top path':
        'available.y > 0f -> coordinator.onTopBoundaryDrag(' in content,
    'SearchContent keeps non-empty bottom path':
        'hasQuery -> coordinator.onBottomBoundaryDrag(' in content,
    'empty upward nested-scroll stays unconsumed for LauncherRoot':
        'else -> Offset.Zero' in content,
    'empty Search page-level pull no longer translates whole Search plane':
        'searchPullOffset' not in launcher and 'translationY = transitionPullOffset.value' in launcher,
    'stable empty Search downward pull is not consumed by LauncherRoot':
        'mode = if (stableSearch && direction == 1) {' in launcher and '\n                                            0\n' in launcher,
    'page ownership clears stale local Search overscroll':
        'searchCoordinator.clearBoundaryOverscroll()' in launcher,
    'legacy vertical Search-pull controller removed':
        'searchPullOffset' not in vertical and 'beginSearchPull' not in vertical and 'reboundSearchPull' not in vertical,
    'empty-query IME reveal unit regression test added':
        'emptyQueryTopPullUsesTheSameImeRevealThreshold' in test,
    'empty-query bottom return separation unit test added':
        'emptyQueryDoesNotAcquireTheNonEmptyBottomReturnPath' in test,
}

failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(('PASS' if ok else 'FAIL'), name)
if failed:
    raise SystemExit('failed: ' + ', '.join(failed))
