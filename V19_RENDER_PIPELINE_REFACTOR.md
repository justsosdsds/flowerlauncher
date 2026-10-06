# V19 Render Pipeline Refactor

目标：保持 v18 的 Home ↔ Search 视觉和手势行为，降低动画期间 Compose 重组与每帧系统调用，提高高刷新率设备上的帧率稳定性。

## 主要重构

- 新增 `SearchTransitionController`，集中管理 `progress`、第一次下拉位移、Search 二次橡皮筋位移、settle/rebound job 与 layer active 状态。
- Home 与 Search 始终保留在 Composition 中；Search 通过 z-order 激活/隐藏，不再在动画中反复创建/销毁整棵 Search UI。
- Home/Search 的 alpha、scale、translationY 直接在 `graphicsLayer` 中读取 `Animatable`，避免把 `progress.value` 提升到 Composition 层导致 LauncherRoot 每帧重组。
- `SearchContent.transitionProgress` 改为 lambda，在自身 `graphicsLayer` 阶段读取动画值，保留原来的搜索框/结果淡入节奏，同时避免每帧参数变化触发 SearchContent 重组。
- 键盘请求改成 `snapshotFlow + distinctUntilChanged`：只在跨过键盘阈值时改变 Compose state。
- Compose 高斯模糊改成 6 档预创建 `RenderEffect`，动画中只切换已存在的 effect，不再每帧创建新的 BlurEffect。
- 壁纸/窗口 blur 也按 6 档更新，避免每帧修改 `WindowManager.LayoutParams`。
- 保留 v16 的可中断双向手势、v10 的固定 Search backdrop、Search-only rubber-band、动态 settle 时长等行为。

## 预期收益

- 主要动画帧只触发 RenderNode/layer 属性更新，不再需要整个 LauncherRoot/SearchContent 以 60/90/120Hz 重组。
- Blur 对象创建与 WindowManager 写入频率大幅降低。
- Search 打开/关闭期间 Lazy/Grid、应用列表和设置状态不再因 progress 每帧重新参与 Composition。

## 验证限制

当前环境缺少 Gradle 8.13 本地分发包，wrapper 即使使用 `--offline` 仍尝试下载，因此无法完成实际 Kotlin/Android 编译。已做源码结构、括号平衡与差异检查。
