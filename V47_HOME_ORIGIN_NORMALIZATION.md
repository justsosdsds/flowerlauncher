# V47 — Home-origin normalization for cross-page Search entry

## Problem

During one continuous `Drawer -> Home -> Search` downward drag, the root gesture used the total finger distance accumulated since the gesture began to build `transitionPullOffset`.

When the signed page position crossed `Home = 0`, Search became the foreground, but the offset already contained the entire `Drawer -> Home` travel. Search therefore appeared at a different Y position than it did during a direct `Home -> Search` drag, producing a visible pop.

This is the same class of path-history contamination that previously made Home content jump at the zero crossing.

## Fix

`LauncherRoot` now treats `Home = 0` as the visual origin for the transient pull offset. During the unified Drawer/Home root gesture:

```text
localPageDistancePx = verticalPosition * dragDistancePx
transitionPullOffset = signedRubberBand(localPageDistancePx)
```

The offset is no longer based on total finger travel since gesture start.

Consequences:

- `Home -> Search` keeps the same interactive rubber-band curve it had before.
- `Drawer -> Home -> Search` restarts the Search-side visual offset from zero at Home.
- At a given positive `verticalPosition`, Search has the same translation regardless of the path used to reach it.
- Existing settle behavior is preserved: `transitionPullOffset` still animates back to zero when settling to a stable page.
- Search return, IME behavior, sensitivity settings, Drawer grid handoff, and Search compact-return animation are unchanged.
