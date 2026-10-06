# V49 — Search continuous results surface + floating search bar

## Goal
Keep Search as one continuous page while separating four responsibilities:

- page transition: LauncherRoot
- result scrolling / edge rubber-band: SearchContent
- floating search field: independent bottom layer
- IME show/hide: content need + gesture thresholds

## Main changes

### LauncherRoot
- Non-empty query no longer uses V48 mode 3/4 pointer interception.
- While query is non-empty, Search -> Home swipe is disabled by removing the page-level Search pointer modifier.
- Empty query keeps the existing Search -> Home / Search rubber-band behavior.
- Existing Search return animation, drawer gesture state machine, blur and sensitivity parameters are unchanged.

### SearchContent
- Search field moved out of the result scroller into a persistent bottom floating layer.
- `navigationBarsPadding()` + `imePadding()` make the floating layer follow animated IME insets and the bottom safe area.
- Result content owns a single ScrollState.
- A NestedScrollConnection receives only drag distance left over after normal scrolling reaches a boundary.
- Short content therefore enters the same rubber-band immediately; long content scrolls normally first.
- Top and bottom use the same signed rubber-band function/state.

### IME behavior
- Upward drag may hide IME only when `ScrollState.maxValue` exceeds the configured overflow threshold.
- Hiding IME does not switch Search to another page/state; the same result viewport simply gains height.
- When IME is hidden, a downward drag first scrolls results to the top, then enters top rubber-band.
- IME is requested on release only when the visible top rubber-band exceeds the configured reveal threshold.
- Bottom rubber-band never exits Search.

### New Search settings
- IME collapse content overflow: default 80dp, range 0–300dp, step 10dp.
- IME reveal pull distance: default 200dp, range 80–360dp, step 10dp.

## Files changed
- `app/src/main/java/com/flower/launcher/ui/LauncherRoot.kt`
- `app/src/main/java/com/flower/launcher/ui/SearchContent.kt`
- `app/src/main/java/com/flower/launcher/ui/SearchInteractionState.kt` (new)
- `app/src/main/java/com/flower/launcher/ui/SearchSettingsScreen.kt`
- `app/src/main/java/com/flower/launcher/search/SearchPreferencesRepository.kt`

## Build verification
Gradle compile could not run because the wrapper distribution is configured on an external Aliyun Gradle mirror unavailable in the execution environment. Source-level structure/invariant checks were run instead.
