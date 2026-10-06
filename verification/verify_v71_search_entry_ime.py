from pathlib import Path
root = Path(__file__).resolve().parents[1]
coord = (root/'app/src/main/java/com/flower/launcher/ui/SearchCoordinator.kt').read_text()
rootkt = (root/'app/src/main/java/com/flower/launcher/ui/LauncherRoot.kt').read_text()
test = (root/'app/src/test/java/com/flower/launcher/ui/SearchCoordinatorTest.kt').read_text()
checks = [
    ('pending auto-keyboard state exists', 'val autoKeyboardRequested: Boolean = false' in coord),
    ('auto request is remembered even before Active', 'autoKeyboardRequested = intent.requested' in coord),
    ('effective request still requires Active', 'state.phase == SearchPhase.Active' in coord),
    ('session activation replays pending request', 'state.autoKeyboardRequested && !state.pageReturnImeSuppressed' in coord),
    ('session teardown clears pending request', 'autoKeyboardRequested = false' in coord),
    ('page-return suppression still clears effective request', 'keyboardRequested = false,\n                    pageReturnImeSuppressed = true' in coord),
    ('existing transition trigger remains unchanged', 'verticalPosition.value >= KEYBOARD_TRIGGER_PROGRESS' in rootkt),
    ('existing trigger progress remains 5.5 percent', 'KEYBOARD_TRIGGER_PROGRESS = 0.055f' in rootkt),
    ('regression test covers early request race', 'earlyAutoKeyboardRequestIsReplayedWhenSearchSessionBecomesActive' in test),
    ('regression test covers suppression', 'pendingAutoKeyboardRequestStillHonorsPageReturnSuppression' in test),
]
failed=False
for name, ok in checks:
    print(('PASS' if ok else 'FAIL'), name)
    failed |= not ok
raise SystemExit(1 if failed else 0)
