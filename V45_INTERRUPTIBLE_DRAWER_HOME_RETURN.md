# V45 — Interruptible Drawer → Home Return

## Problem
After swiping down from All Apps to Home, the desktop could already look visually settled while `stablePage` was still `AllApps`. The Home root gesture required both `stablePage` and `targetPage` to be `Home`, leaving a short interval where an immediate upward swipe did nothing.

## Fix
- Treat `AllApps(stable) -> Home(target)` as immediately eligible for the Home/root vertical gesture.
- Keep the drawer visually composed during its closing settle, but temporarily disable its own pull-to-close observer and `LazyVerticalGrid` user scrolling while that settle targets Home.
- The root gesture can therefore cancel the active settle and reverse it immediately back toward All Apps (or continue downward through Home toward Search).

## Behavioral result
`All Apps -> Home -> All Apps` can be reversed before the first settle finishes; there is no intentional gesture dead window at Home.

## Scope
No sensitivity thresholds, page anchors, drawer visuals, Search behavior, or settle duration were changed.
