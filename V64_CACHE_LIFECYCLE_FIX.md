# V64 Cache Lifecycle Fix

## 目的
修复 V63 中缓存存在但手势期间仍可能重建、时钟局部缓存被过渡坐标拉伸的问题。

## 核心修复
- 仅当 Home 完全静止（position≈0、targetPage=Home、非 Search return）时允许生成缓存。
- Search / Drawer 手势开始后立即冻结缓存；手势期间只读 0..5 级 bitmap。
- `clockBounds` 仅在 Home idle 时接收更新，避免 `boundsInRoot()` 被外层 scale 影响后污染缓存坐标。
- 删除 `homeBlurIndex==0` 时无条件递增的 `cacheRefreshToken`，避免每次回 Home 都触发时钟缓存重建。
- 静态缓存仅在真实 revision / wallpaper / clock bounds / blur strength 改变时重建。
- 时钟缓存仅在分钟/样式/bounds 等真实变化后重建。
- 静态与时钟两条 `GraphicsLayer.toImageBitmap()` 路径都使用 `ensureSoftware()`，兼容 HARDWARE bitmap + software Canvas。
- `HomeContent` 设为 internal，避免暴露 internal `MinuteClockValue`。

## 生命周期
Home idle -> 允许 rebuild（dirty 才执行）
手势开始 -> freeze
Search / Drawer -> 只切换缓存级别
返回 Home -> 等一帧，若 dirty 才 rebuild
