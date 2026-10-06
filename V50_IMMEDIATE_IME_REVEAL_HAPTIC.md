# V50 — Immediate IME reveal + threshold haptic

## Changed behavior

- Search results still scroll back to the top before top-edge pull begins.
- While IME is hidden, the top rubber-band is measured using the existing visual overscroll distance.
- The first frame whose visual pull reaches `imeRevealPullThresholdDp` now immediately:
  1. arms the reveal one-shot guard,
  2. optionally emits one vibration,
  3. requests focus and calls `show()` for the IME,
  4. starts the existing rubber-band rebound.
- Release is no longer required to show the IME. Releasing below threshold still only rebounds.

## Search Interaction settings page

`搜索页面 → 搜索交互` is now a dedicated page containing:

- 隐藏输入法内容溢出（dp）: 0–300, default 80
- 重新显示输入法下拉距离（dp）: 80–360, default 200
- 达到下拉阈值时振动: default on
- 振动强度（%）: 10–100, default 60

Amplitude-capable devices map 10–100% to Android vibration amplitude. Devices without amplitude control fall back to a short duration range.

## Preserved

- `LauncherRoot.kt` is byte-identical to V49.
- Search/Home page transition parameters are unchanged.
- Existing Search result scrolling and top/bottom rubber-band function are unchanged.
- Drawer gesture/blur/home sensitivity logic is unchanged.
