#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
ui = root / 'app/src/main/java/com/flower/launcher/ui'
coord = (ui / 'SearchCoordinator.kt').read_text()
content = (ui / 'SearchContent.kt').read_text()
launcher = (ui / 'LauncherRoot.kt').read_text()
test = (root / 'app/src/test/java/com/flower/launcher/ui/SearchCoordinatorTest.kt').read_text()

checks = {
    'serialized IME command model exists':
        'enum class SearchImeCommand' in coord and
        'Show,' in coord and 'Hide,' in coord,
    'coordinator owns IME reconciliation':
        'fun reconcileIme(' in coord and
        'presentationReady: Boolean' in coord and
        'forceHide: Boolean = false' in coord,
    'show advances Hidden to Showing before command':
        'SearchImePhase.Hidden -> {' in coord and
        'imePhase = SearchImePhase.Showing' in coord and
        'SearchImeCommand.Show' in coord,
    'hide advances Visible to Hiding before command':
        'SearchImePhase.Visible -> {' in coord and
        'imePhase = SearchImePhase.Hiding' in coord and
        'SearchImeCommand.Hide' in coord,
    'Showing cannot emit Hide':
        'SearchImePhase.Showing,' in coord and
        'SearchImePhase.Hiding,' in coord,
    'presentation readiness only gates Show':
        'presentationReady &&' in coord and
        'val wantsHide =' in coord and
        'presentationReady' not in coord[coord.index('val wantsHide ='):coord.index('if (wantsHide)')],
    'page return suppression remains explicit hide policy':
        'state.pageReturnImeSuppressed ||' in coord,
    'hidden Search session remains an explicit hide policy':
        'state.phase == SearchPhase.Hidden ||' in coord,
    'late visible acknowledgement preserves newer target':
        'val externalReveal =' in coord and
        'state.imePhase == SearchImePhase.Hidden' in coord,
    'expanded intent no longer pre-sets Hiding':
        'state = state.copy(presentationMode = SearchPresentationMode.Expanded)' in coord,
    'top pull no longer pre-sets Showing':
        'state = state.copy(presentationMode = SearchPresentationMode.Input)' in coord,
    'legacy programmatic-hide boolean removed':
        'programmaticImeHidePending' not in content,
    'legacy requestKeyboard toggle removed':
        'requestKeyboard' not in content and 'requestKeyboard' not in launcher,
    'SearchContent executes commands from reconciler':
        'coordinator.reconcileIme(' in content and
        'SearchImeCommand.Show ->' in content and
        'SearchImeCommand.Hide ->' in content,
    'launcher passes readiness independently from Input target':
        'imePresentationReady = searchImePresentationReady' in launcher,
    'settings use independent force-hide policy':
        'forceHideIme = showSearchSettings' in launcher,
    'entry reversal serialization regression test exists':
        'pageReturnReversalSerializesShowThenHideThenShow' in test,
    'readiness drop no-hide regression test exists':
        'presentationReadinessFallingBelowThresholdDoesNotHideVisibleIme' in test,
    'late visible regression test exists':
        'lateVisibleAcknowledgementCannotUndoNewerExpandedTarget' in test,
    'external reveal from stable Expanded regression test exists':
        'userOrSystemRevealFromStableExpandedStateReturnsToInput' in test,
    'abrupt session end final-hide regression test exists':
        'abruptSessionEndStillSerializesFinalImeHide' in test,
}

failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(('PASS' if ok else 'FAIL'), name)
if failed:
    raise SystemExit('V73 verification failed: ' + ', '.join(failed))
