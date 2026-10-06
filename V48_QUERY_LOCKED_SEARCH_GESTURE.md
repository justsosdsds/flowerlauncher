# V48 Query-Locked Search Gesture

## Goal

When Search contains a non-empty query, vertical gestures stay inside Search instead of navigating back to Home.

## Behavior

### Query empty

Existing behavior is preserved:

- Swipe up in Search -> compact Search -> Home return.
- Existing return motion, blur, fade, scale, IME-hide threshold and settle timing are unchanged.

### Query non-empty + IME visible

- Swipe up past touch slop.
- Search remains on the Search page.
- `requestKeyboard` becomes false.
- `SearchContent` clears focus and hides the IME.
- The same gesture is consumed as an IME-collapse gesture, so it cannot become Search -> Home.

### Query non-empty + IME hidden / Search fully expanded

- Further upward gestures never navigate to Home.
- Upward movement is left unconsumed by the page gesture layer so SearchContent's own vertical scrolling continues to work.
- A new downward gesture restores focus/IME and is consumed at the Search page layer.

### System navigation

System Home / bottom system navigation continues to leave Search through the existing launcher reset path. Android Back continues to use the existing BackHandler behavior.

## State isolation

V48 adds separate state for this feature:

- `searchHasQuery`
- `searchImeCollapsedByGesture`

It intentionally does not reuse `returningFromSearch` or `imeSuppressedForSearchReturn`, so empty-query Search -> Home behavior is not changed.

## Files changed

- `app/src/main/java/com/flower/launcher/ui/LauncherRoot.kt`
- `app/src/main/java/com/flower/launcher/ui/SearchContent.kt`

