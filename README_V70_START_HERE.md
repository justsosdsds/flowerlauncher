# V70：统一搜索顶部下拉与 IME Reveal

基线为上一版交付的 V69。本版针对一个可稳定复现的问题：搜索框为空、IME 已手动收起时，下拉超过“重新显示输入法”阈值不会重新弹出 IME。

## 根因

V69 之前存在两套顶部下拉路径：

- 空查询：`LauncherRoot` 处理整页 Search rubber-band。
- 非空查询：`SearchContent` + `SearchCoordinator` 处理结果区 rubber-band、IME Reveal 和结果滚动。

因此 IME Reveal 阈值只存在于非空查询路径，空查询无法触发。

## V70 行为

- 空查询与非空查询的**顶部下拉**统一进入 `SearchContent` 的 NestedScroll。
- 两种查询状态共用同一个 `SearchInteractionState`、rubber-band 公式、回弹时长和 `imeRevealPullThresholdDp`。
- 空查询下拉时只移动结果区域，底部浮动搜索栏保持固定。
- IME Hidden 且顶部视觉位移达到用户设置阈值时，空/非空查询都会立即触发相同的振动、`requestFocus()` 与 `keyboardController.show()`。
- 空查询的**上滑 Search -> Home** 仍由 `LauncherRoot` 原页面手势负责，不改变原有页面返回阈值、速度判定、IME hide threshold 和页面 settle 行为。
- 非空查询的结果滚动、底部 rubber-band、达到阈值返回 Home 仍走现有 V54/V59-V61 handoff 路径。

## 架构变化

`SearchCoordinator` 将边界手势明确拆成：

- `onTopBoundaryDrag(...)`：query-agnostic，统一顶部 rubber-band 与 IME Reveal。
- `onBottomBoundaryDrag(...)`：只服务非空查询的底部 Return Home。

`LauncherRoot` 不再持有空查询专用的整页 `searchPullOffset`。对应的 `VerticalPageTransitionController` 本地 Search pull 状态、动画 Job 和相关 API 已删除。

同时保留一个关键边界：当空查询的新上滑页面手势真正取得 ownership 时，会清理上一触摸残留的 Search-local overscroll，避免中断回弹后结果区位移残留到页面过渡中。

## 验证

新增：

- `app/src/test/java/com/flower/launcher/ui/SearchCoordinatorTest.kt`
- `verification/verify_v70_unified_search_pull.py`

源码级检查覆盖：

- 空/非空查询共用顶部 IME Reveal。
- 空查询不会误进入非空查询 bottom Return Home 路径。
- 空查询支持反向消除现有顶部 rubber-band。
- query 从非空切到空时不再强制重置共享顶部 pull 状态。
- LauncherRoot 不再移动整个 Search 平面。
- V61 GestureCoordinator handoff 关键约束仍全部通过。

当前执行环境无法下载工程指定的 Gradle 8.13（`mirrors.aliyun.com` 无网络访问），因此未完成 `assembleDebug` / JVM 单元测试的真实 Gradle 执行。使用本地 Kotlin 编译器做了源码语法扫描，没有发现语法错误或本次新增方法的未解析引用。

最终验收建议重点覆盖：空查询/非空查询 × IME Visible/Hidden、下拉阈值前后、下拉后反向、回弹中再次触摸、空查询直接上滑返回 Home、非空查询滚到底继续上拉返回 Home。
