# V71 — Search entry IME state race fix

V71 builds on V70's unified Search top-pull architecture.

## Goal

Every fresh Home -> Search transition should arrive in the keyboard-present Search input state, rather than occasionally landing in the expanded (IME hidden) state.

## Root cause

LauncherRoot already requests the keyboard when the vertical transition crosses `KEYBOARD_TRIGGER_PROGRESS` (5.5%). However, that request can arrive before `SearchCoordinator` has been promoted from `Hidden` to `Active`. The old `AutoKeyboardRequested` reducer discarded requests while not Active. Because the transition condition then remained `true`, `distinctUntilChanged()` did not emit the request a second time after session activation.

## Fix

`SearchState` now separates:

- `autoKeyboardRequested`: desired automatic keyboard state from the page transition.
- `keyboardRequested`: effective request that SearchContent may execute.

An early automatic request is remembered while Search is still Hidden. When `SessionActiveChanged(true)` activates a new Search session, the pending request is replayed, unless page-return IME suppression is active.

The original 5.5% transition trigger remains unchanged, so the keyboard is not intentionally shown earlier than before.

## Preserved behavior

- V70 empty/non-empty unified top-pull and IME reveal threshold.
- Manual IME hiding while already on Search.
- Down-pull threshold to reveal IME again.
- Search -> Home return suppression and same-pointer reversal logic.
- Search bar / results layout behavior.

## Verification

Run:

```bash
python3 verification/verify_v71_search_entry_ime.py
python3 verification/verify_v70_unified_search_pull.py
python3 verification/verify_v61_gesture_coordinator.py
```

Two unit regressions were added to `SearchCoordinatorTest.kt` for the early-request race and page-return suppression.
