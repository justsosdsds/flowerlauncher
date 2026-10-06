# V33 — Home ↔ All Apps 浮现式双向过渡

本版将 Home ↔ All Apps 从“达到阈值后直接开关抽屉”重构为与 Home ↔ Search 相同的交互模型。

## 动画模型

- Home 上滑时，从第一段拖动开始直接更新 All Apps transition progress。
- All Apps 常驻于屏幕坐标空间内，通过 alpha + scale 浮现，不再从屏幕外整页推入。
- Home 与 All Apps 在首次上滑阶段共享轻微向上的 rubber-band 位移。
- Home 同步做轻微缩放、透明度变化和较轻的高斯模糊。
- All Apps 不使用 Search 的深色渐变蒙版，避免页面切换过重。
- 松手后统一使用现有“搜索切换动画 → 回弹时间”设置。
- 收尾 easing 继续使用 EaseOutQuart：`1 - (1 - t)^4`。

## 双向/可中断

- 拖动期间不启动自动动画，手指移动直接控制 progress。
- Home→All Apps 过程中可立即反向下滑，不必等打开动画完成。
- All Apps 位于列表顶部时，下滑会连续降低 drawer progress，并实时露出 Home。
- 下滑过程中反向上推会立即重新打开 All Apps。
- 列表不在顶部时仍优先正常滚动列表，不误触发返回 Home。

## 模糊

- Search 继续使用完整高斯模糊强度。
- All Apps 复用同一套量化 blur 管线，但强度更轻：
  - Compose Home blur factor: 0.55
  - Wallpaper blur factor: 0.45
- Window dim 仍保持禁用，Home 稳定状态不压暗壁纸。

## 结构

新增 `DrawerTransitionController`，独立维护：

- `progress`
- `transitionPullOffset`
- `targetOpen`
- `layerActive`
- `stableOpen`

All Apps 的下滑手势继续在 `PointerEventPass.Initial` 被动观察，不抢 `LazyVerticalGrid` 的滚动事件。
