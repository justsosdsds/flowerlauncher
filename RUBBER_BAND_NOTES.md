# Search rubber-band v5

The search screen now has a separate pull-down overscroll interaction inspired by the behavior described from iOS Spotlight.

## Drag phase

Visible offset is not linear for the full drag. It uses:

`visible = limit * raw / (limit + raw)`

This gives:
- approximately 1:1 movement at the first instant (`slope = 1` at zero),
- continuously increasing resistance,
- an asymptotic maximum displacement.

`SEARCH_PULL_LIMIT_RATIO = 0.46` makes the theoretical limit about 46% of screen height.

## Release phase

The pull always returns in the same `310 ms`, regardless of distance. Therefore a larger pull naturally has a higher absolute return speed.

The custom `LinearThenBrakeEasing` keeps most of the return close to constant speed, then concentrates deceleration near the resting position.

## Parameters to tune

- `SEARCH_PULL_LIMIT_RATIO`: maximum visual travel / resistance feel.
- `SEARCH_PULL_REBOUND_DURATION_MS`: fixed rebound duration.
- `split` and `splitProgress` inside `LinearThenBrakeEasing`: where and how strongly the final braking happens.
