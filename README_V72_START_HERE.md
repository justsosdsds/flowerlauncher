# V72 — Search Input / Expanded 声明式状态模型

V72 将 V71 的“自动键盘请求 / pending request”方案替换为明确的 Search 展示状态模型。

## 核心状态

- `SearchPresentationMode.Input`
  - Search 的目标是输入状态。
  - 页面达到既有 `KEYBOARD_TRIGGER_PROGRESS = 0.055f` 后，搜索框获得焦点并显示 IME。
- `SearchPresentationMode.Expanded`
  - IME 收起。
  - 搜索栏保持底部浮动。
  - 结果区域继续使用全屏高度。

`SearchImePhase` 仍保留，但只描述实际 IME 的 Hidden / Showing / Visible / Hiding，不再承担“Search 想处于什么状态”的职责。

## 行为

1. 每次从 Home 新进入 Search：目标固定为 `Input`。
2. 5.5% 的原有 IME 触发进度保持不变；它只决定何时兑现 `Input`，不再决定是否进入 Input。
3. 用户手动收起已显示的 IME：进入 `Expanded`。
4. 非空结果上滑触发原有 IME collapse：进入 `Expanded`。
5. Expanded 状态顶部下拉达到原有 IME Reveal 阈值：切回 `Input` 并显示 IME。
6. Search -> Home 页面返回期间的程序性 IME 隐藏只属于导航抑制，不会把 Search 的目标状态误改为 Expanded。
7. 页面返回被取消并回到 Search 时，保留原有“恢复输入态”的体验。

## 架构变化

删除：

- `SearchState.autoKeyboardRequested`
- `SearchState.keyboardRequested`
- `SearchIntent.AutoKeyboardRequested`
- LauncherRoot 中基于 `snapshotFlow` 的一次性自动键盘事件链

新增：

- `SearchPresentationMode.Input / Expanded`
- `SearchIntent.EnterInputMode / EnterExpandedMode`
- LauncherRoot 的声明式 `searchImePresentationReady` 页面进度门
- SearchContent 对“用户隐藏 IME”和“程序隐藏 IME”的区分

## V70 行为保持

V70 的统一顶部下拉继续保留：

- 空查询 / 非空查询顶部下拉共用 SearchContent + SearchCoordinator。
- 空查询下拉只移动结果区域，底部搜索栏固定。
- 空查询向上返回 Home 仍由 LauncherRoot 页面级手势负责。
- 非空查询底部 Return Home handoff 保持 V59-V61 行为。

## 验证

通过：

- `verify_v61_gesture_coordinator.py`
- `verify_v70_unified_search_pull.py`
- `verify_v72_search_presentation_mode.py`（19 项）

Gradle `:app:testDebugUnitTest :app:assembleDebug` 在当前环境无法执行，因为 Wrapper 指向的 `mirrors.aliyun.com` 无法联网下载 Gradle 8.13。该失败发生在 Gradle 启动前，不是 Kotlin/Android 编译错误。
