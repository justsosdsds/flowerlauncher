# V16 — Interruptible Search Gesture

## Goal
Make Home <-> Search switching immediately reversible and make Search -> Home feel faster.

## Changes
- Search opening settle can be interrupted before it reaches 100%.
- A new upward gesture immediately cancels the in-flight open animation and drives the current progress back toward Home.
- A new downward gesture can continue an in-flight transition.
- Fully-open Search still keeps its independent downward rubber-band behavior.
- Settle duration is distance-aware instead of always 310 ms.
- Opening: 90–260 ms depending on remaining distance.
- Closing: 70–180 ms depending on remaining distance.
- Existing LinearThenBrakeEasing is preserved.

## Expected feel
- Quick down -> up can reverse immediately.
- Search -> Home is noticeably faster.
- Small remaining distances finish quickly instead of waiting through a full fixed-duration animation.
