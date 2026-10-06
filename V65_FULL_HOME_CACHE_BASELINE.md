# V65 Full Home Cache Baseline

## 目标
先验证“6 档预栅格化缓存是否真正接管 Search / Drawer 背景”，暂时移除 V63/V64 的时钟局部缓存复杂度。

## 实现
- 捕获完整 Home UI，时钟包含在 Home 捕获中。
- 使用 WallpaperManager 静态壁纸快照与 Home 透明层合成。
- 合成结果降为 1/2 宽 × 1/2 高。
- 预栅格化 6 个状态：level 0 + 5 个 blur level。
- Search / Drawer 共用同一组缓存。
- 手势期间不生成新的 blur；缓存只在 Home 完全静止时允许重建。
- 分钟变化直接使整套缓存失效；当前阶段接受每分钟最多重建一次。

## V64 删除项
- dynamicClockLevels
- dynamicClockBounds
- clockBounds / boundsInRoot 缓存坐标
- clockRevision 独立重建路径
- 静态缓存的 clock hole / clipOutRect
- clock crop/downsample + 局部 6 档缓存

## 保留的真机修复
- GraphicsLayer.toImageBitmap().asAndroidBitmap() 后调用 ensureSoftware()，防止 HARDWARE Bitmap 被软件 Canvas 绘制导致崩溃。
- HomeContent 保持 internal。

## 验证重点
1. Search / Drawer 中不再出现被放大的巨大时钟。
2. 0~5 档切换时，背景内容位置和比例保持一致，只改变模糊程度。
3. 时钟跳分钟后，仅在回到 Home idle 后重建整套缓存。
4. 手势过程中不应执行 Home cache rebuild。
