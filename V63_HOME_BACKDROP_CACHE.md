# V63 Home Backdrop Cache

## 目标

把 Search / Drawer 手势中的实时 Home 高斯模糊移出主渲染路径，改为 1/2 分辨率的 6 档持久缓存，并让分钟时钟使用独立局部缓存。

## 实现

- 新增 `HomeBackdropCache.kt`。
- Home 正常显示时通过 `GraphicsLayer` 记录当前桌面；缓存生成只在桌面结构或壁纸失效时运行。
- 静态缓存按宽高各 1/2 生成，共 6 个状态：0 + 5 个非零 blur 档。
- Search 与 Drawer 共用同一套静态缓存，根据原有 `blurLevel 0..5` 切换，不再对实时 Home 设置 `RenderEffect`。
- 系统壁纸变化通过 `ACTION_WALLPAPER_CHANGED` 使静态缓存失效。
- 时钟区域使用 `boundsInRoot()` 单独标记；生成静态底图时把该区域从 Home 捕获中排除，让壁纸作为该区域的静态底色。
- 分钟变化只重新生成时钟区域的 6 档局部缓存，不重建 6 张整屏缓存。
- 如果分钟在 Search/Drawer 覆盖期间发生变化，返回 Home 后会触发一次局部时钟缓存刷新。

## 预期收益

- Search/Drawer 手势期间不再实时执行全屏高斯模糊。
- 手势阶段主要成本变成：缓存纹理绘制、scale、alpha、translation。
- 时钟每分钟变化不会触发整屏 6 级重建。
- 1/2 宽高意味着静态缓存每张只有全分辨率约 1/4 像素量。

## 验证

`verification/verify_backdrop_cache.py` 已通过全部静态检查；原有 GestureCoordinator 静态检查也通过。

Gradle 编译在当前环境无法执行完成：Wrapper 需要下载 Gradle 8.13，但当前环境无法解析 `mirrors.aliyun.com`。这不是 Kotlin 编译报错。
