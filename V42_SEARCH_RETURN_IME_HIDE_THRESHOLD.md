# V42 Search return IME hide threshold

## Goal

When swiping upward from Search toward Home, do not wait until release to hide the keyboard.
After the return gesture has moved upward by a configurable distance, immediately clear text
focus and request IME hide.

## Behavior

- Default threshold: **8 dp**.
- User range: **0..40 dp**, 1 dp steps.
- 0 dp means hide as soon as an upward Search -> Home return is recognized.
- The distance is measured from the actual reversal/return start point, so interrupted
  Home -> Search -> Home gestures do not include the earlier downward travel.
- Hiding is issued once per return attempt.
- If the user reverses back toward Search and keeps Search open, IME requests are enabled again.

## Settings

Settings -> Gestures & hotkeys -> Swipe gestures -> Home vertical swipe sensitivity:
`返回主页收起输入法阈值`.

The preference is persisted as `search_return_ime_hide_threshold_dp` in the existing
`flower_launcher_settings` SharedPreferences store.

## Implementation

`LauncherRoot` tracks an IME suppression flag for the active Search return. Once the
normalized Search return has travelled the configured physical dp distance, it sets
`keyboardRequested=false`. `SearchContent` already reacts to this state by running
`focusManager.clearFocus()` and `keyboardController.hide()`, so no second keyboard control
path is introduced.
