# Animation changes v3

- Based on v2 overlay gesture implementation.
- Added release-only overshoot when opening search.
- Search settles slightly past its final state, then springs back:
  - progress peak: 1.045
  - translation overshoot: +6dp
  - scale overshoot: +1.2%
- Home gets a subtle inverse overshoot:
  - extra scale compression: 0.4%
- Dragging remains strictly clamped to 0..1, so the overshoot only happens after release.
- Closing behavior remains fast and controlled without overshoot.
- Early keyboard trigger from v2 remains unchanged.
