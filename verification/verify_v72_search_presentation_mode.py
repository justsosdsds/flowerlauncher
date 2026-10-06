#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
ui = root / 'app/src/main/java/com/flower/launcher/ui'
coord = (ui / 'SearchCoordinator.kt').read_text()
content = (ui / 'SearchContent.kt').read_text()
launcher = (ui / 'LauncherRoot.kt').read_text()
test = (root / 'app/src/test/java/com/flower/launcher/ui/SearchCoordinatorTest.kt').read_text()

checks = {
    'explicit Input/Expanded presentation model exists':
        'enum class SearchPresentationMode' in coord and
        'Input,' in coord and 'Expanded,' in coord,
    'SearchState owns presentation target':
        'val presentationMode: SearchPresentationMode = SearchPresentationMode.Input' in coord,
    'legacy pending keyboard event state removed':
        'autoKeyboardRequested' not in coord and 'keyboardRequested' not in coord,
    'legacy auto keyboard intent removed':
        'AutoKeyboardRequested' not in coord and 'AutoKeyboardRequested' not in launcher,
    'fresh Search session deterministically targets Input':
        'presentationMode = SearchPresentationMode.Input' in coord and
        'sessionId = state.sessionId + 1L' in coord,
    'launcher uses declarative page-progress gate':
        'val searchImePresentationReady' in launcher and
        'verticalPosition.value >= KEYBOARD_TRIGGER_PROGRESS' in launcher,
    '5.5 percent keyboard timing is preserved':
        'KEYBOARD_TRIGGER_PROGRESS = 0.055f' in launcher,
    'IME request derives from Input target rather than event latch':
        'searchCoordinator.state.presentationMode == SearchPresentationMode.Input' in launcher and
        'requestKeyboard =' in launcher,
    'manual IME dismissal can enter Expanded':
        'userInitiatedHide' in coord and 'SearchPresentationMode.Expanded' in coord,
    'programmatic hides are distinguished in SearchContent':
        'programmaticImeHidePending' in content and
        '!imeVisible && searchFieldFocused && !programmaticImeHidePending' in content,
    'field focus explicitly returns Search to Input':
        'onFocusStateChanged' in content and 'SearchIntent.EnterInputMode' in content,
    'non-empty overflow collapse targets Expanded':
        'presentationMode = SearchPresentationMode.Expanded' in coord and
        'SearchPreScrollEffect.HideIme' in coord,
    'top pull reveal is available only from Expanded and returns Input':
        'state.presentationMode == SearchPresentationMode.Expanded' in coord and
        'SearchBoundaryEffect.RevealIme' in coord,
    'page return suppression does not overwrite presentation target':
        'state = state.copy(pageReturnImeSuppressed = true)' in coord,
    'same-pointer return reversal explicitly restores Input target':
        'SearchIntent.EnterInputMode' in launcher,
    'new session Input regression test exists':
        'freshSearchSessionTargetsInputPresentation' in test,
    'manual dismiss Expanded regression test exists':
        'userDismissedVisibleImeTransitionsToExpandedPresentation' in test,
    'programmatic hide regression test exists':
        'programmaticImeHideDoesNotChangeInputPresentation' in test,
    'pull reveal mode transition regression test exists':
        'emptyQueryTopPullUsesTheSameImeRevealThreshold' in test and
        'SearchPresentationMode.Input' in test,
}

failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(('PASS' if ok else 'FAIL'), name)
if failed:
    raise SystemExit('V72 verification failed: ' + ', '.join(failed))
