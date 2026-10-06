# V34 — Unified vertical page transition

## Goal

Fix the severe Home vertical gesture conflicts introduced when Search and All Apps were driven by two independent transition controllers.

## Root cause removed

V33 used two independent states:

- Search progress: `0..1`
- All Apps progress: `0..1`

The first drag direction selected one controller and locked the gesture to that mode. Reversing through Home therefore could not continue into the opposite page, and interrupted settle animations could leave both progress values non-zero.

## V34 model

V34 replaces both first-stage controllers with one `VerticalPageTransitionController` and one signed position:

- `-1f` = All Apps
- `0f` = Home
- `+1f` = Search

A finger moving downward increases the value; moving upward decreases it. The value is not direction-locked, so one gesture can cross Home and continue to the other sibling page.

## Interaction behavior

- Home pull down -> Search
- Home push up -> All Apps
- Reverse before release -> immediately retraces the same state
- A single gesture may cross through Home in either direction
- Search -> Home uses the same signed position
- All Apps -> Home uses the same signed position
- Settle animation uses the existing user-configurable rebound duration
- Settle easing remains EaseOutQuart (`1 - (1-t)^4`)

## Search second-stage pull

The fully-open Search page keeps its independent foreground rubber-band pull. This is intentionally separate from the page navigation position:

- Search stable + pull down -> only Search foreground stretches
- Search stable + swipe up -> shared vertical page transition begins

## All Apps list

The app grid keeps normal vertical scrolling. A downward gesture only starts the page-close transition when the grid was already at the top.

## Rendering

Home transform now reads the sign of one position rather than comparing two unrelated progress values. Search and All Apps alpha/scale also derive from the same position, eliminating controller hand-off jumps near Home.

Search retains the stronger blur and spatial gradient scrim. All Apps retains the lighter blur and no Search-style dark gradient.

## Build-cleanup invariants preserved

The V25 fixed compile issues remain fixed:

- no invalid `foundation.layout.weight` import
- `SearchActionRow` remains defined
- `LauncherSettingsScreen` receives hidden-app parameters
- `RectangleShape` uses `androidx.compose.ui.graphics.RectangleShape`
- no direct `Animatable.snapTo/animateTo` call is added inside the restricted `awaitEachGesture` scope; those operations are dispatched through controller-owned coroutine jobs
