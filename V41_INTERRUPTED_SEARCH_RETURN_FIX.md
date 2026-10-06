# V41 — Interrupted Home→Search reversal uses compact Search return

## Problem
V40 only armed the compact Search→Home return visual when Search had already become the stable page. If Home→Search was still settling (or the same drag reversed before settle) and the user swiped upward, the transition could fall back to the old bidirectional Search visual.

## Fix
- Detect upward reversal on the Search side (`position > 0`) during the live Home gesture.
- Detect the same reversal in the Search foreground pointer path, including when a Search settle is interrupted before `stablePage` becomes Search.
- Snapshot the actual Search-side progress at the reversal point as `searchReturnStartPosition`.
- Normalize compact return progress from that snapshot to Home instead of assuming the return always starts at `+1`.
  - Example: reversal at `+0.45` is return progress `0%` there and `100%` at Home.
- Preserve the blur amount at the reversal point and reduce it continuously to zero.
- If a return is cancelled and motion heads back toward Search, resume the normal blur curve instead of freezing blur at the reversal snapshot.
- Programmatic/interrupted settles whose target was Search also arm the compact return when they are sent back to Home.

## Unchanged
- V40 compact return parameters (28dp / 32dp, 0.55 / 0.80 / 0.85, 0.965, 110ms).
- Home→Search entry visuals.
- Gesture sensitivity settings from V39.
- Drawer continuous hand-off behavior.
