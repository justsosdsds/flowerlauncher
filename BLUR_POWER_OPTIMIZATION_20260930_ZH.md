# Blur / 功耗优化（2026-09-30）

本次只针对 Launcher 的高成本模糊渲染路径，保留现有手势、回弹、Search / Drawer 运动逻辑。

## 1. 模糊改为离散档位

- `BLUR_LEVELS` 从 6 改为 5。
- 实际 RenderEffect 状态现在为 6 个：0（无模糊）+ 5 个非零档位。
- 新增 `homeBlurIndex`（`derivedStateOf`），把模糊档位从每帧 scale / alpha 运动中拆开。
- 外层运动层仍可逐帧跟手；内层 blur 图层只在跨越模糊档位时更新 RenderEffect。

## 2. 删除双重模糊

删除 Android Window 的 wallpaper blur 路径，包括：

- `WallpaperBlurHost`
- `setWallpaperBlurWithoutDim()`
- `FLAG_BLUR_BEHIND`
- 为 OEM blur carrier 设置的 `FLAG_DIM_BEHIND`
- wallpaper blur 的更新链路
- wallpaper blur 的 invalidate / reassert 回调

现在 Search / Drawer 只使用 Compose Home `RenderEffect` blur。

## 3. 删除强制每帧 dirty 的微变化

Drawer 之前使用 `DRAWER_HOME_STATIC_NUDGE` 对 Home 做极小的 progress-driven scale / alpha 变化，以迫使静态图层持续重绘。该逻辑现已删除。

Drawer 下 Home 保持 `scaleX = editScale`、`scaleY = editScale`、`alpha = 1f`，因此静态 Home 可以真正保持静态。

## 验证

- 项目现有 GestureCoordinator 静态检查通过。
- 已检查旧的 WallpaperBlurHost / FLAG_BLUR_BEHIND / DRAWER_HOME_STATIC_NUDGE 等引用均已移除。
- 当前执行环境无法联网下载项目指定的 Gradle 8.13，因此无法在此环境完成 `:app:compileDebugKotlin`。失败原因是 Gradle Wrapper 下载地址 `mirrors.aliyun.com` 网络不可达，并非已观察到的 Kotlin 编译报错。
