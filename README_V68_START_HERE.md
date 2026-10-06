# V68：紫色壁纸修复 + 静态/时钟分离缓存

这是基于附件 V67 的源码修复，**没有产出经过编译、安装或真机验证的 APK**。请先在 Android Studio 编译，再按下方流程验收。第一阶段性能目标仍需实测，不能把源码检查当成通过验收。

## 为什么会变成纯紫色

V67 的 `HomeBackdropCache.kt / loadWallpaperImage()` 调用 `WallpaperManager.drawable`，捕获读取失败后，使用 `getWallpaperColors(...).primaryColor` 生成 1×1 不透明图片，再通过 `HomeWallpaperLayer` 拉伸覆盖整个屏幕。

这条路径显示的是壁纸的主色，并不是壁纸。主色是紫色时，桌面就被紫色盖住。主题原本已经启用 `windowShowWallpaper`，真实系统壁纸在窗口后方，但被这张纯色图片挡住。

Android 官方说明：从 Android 13 起，普通应用不能可靠地通过该接口直接取得实际壁纸；Android 14 起此接口会抛出安全异常（特殊权限应用另论）。仅把异常吞掉不能解决图像来源问题。普通 View/Compose 的图层截图也不包含独立的系统壁纸 Surface。

官方参考：https://developer.android.com/reference/android/app/WallpaperManager#getDrawable()

另一个问题是 V67 明确回退到“整屏含时钟”的 V65 基线，`minuteClock` 直接进入 `staticHomeRevision`；它并没有实现你的静态/动态分离方案。

## 这版如何使用

- 默认“使用系统壁纸”：窗口保持透明，显示真实系统壁纸，包括系统动态壁纸。**此模式不模糊系统壁纸本身**，因为没有壁纸像素可供缓存。桌面自身的静态元素和时钟仍独立缓存。
- 长按桌面 → 壁纸 → **选择图片（支持缓存模糊）**：选择同一张壁纸的原图，作为本启动器的桌面背景。只读取用户选择的这一张图片，保存受尺寸限制的私有副本；不需要所有文件访问权限。此操作不修改系统/锁屏壁纸。
- “使用系统壁纸”可随时撤回桌面图片；“打开系统壁纸设置”切回系统模式并保留原系统选择器流程。取消图片选择不会改掉原背景。
- Android 12 / API 31 及以上，预生成 0～5 六档 Gaussian Blur。Android 8～11 保留原工程的降级行为：六个索引复用清晰图像，不提供高斯模糊。本次不引入旧 Android 的模糊后端。

## 实现范围

- 静态 Home 与动态时钟是兄弟绘制层。静态层中仅保留不绘制的时钟布局占位，不用挖空壁纸的方法排除时钟。
- 静态层和时钟分别调用 `CachedHomeSurface`，各自持有六档缓存；直接以 1/2 宽 × 1/2 高进行光栅化，去掉原来的整屏回读和软件复制。
- 时钟缓存是包含透明边缘的局部横向区域；分钟变化只改变它自己的内容版本。动态区域独立叠在静态层上。
- Search / Drawer 同用 0～5 档；去掉 Drawer 原来使它最高只能选到 4 档的 0.78 系数。因此 Drawer 最深一档会比 V67 略强。
- 正常动画只切换缓存纹理，并执行原有位移、缩放、透明度。高斯模糊仅出现在缓存构建函数。
- Home 内容保持组合，不再因切到缓存画面销毁时钟、Pager 或桌面实例。
- 手势/回弹只暂停或恢复构建，不属于缓存内容键；返回 Home 且内容未变时不会重建。无实际页面变化的导航请求序号、resetSignal 和分钟值不进入静态版本键。
- 缓存没有准备好或截图失败时显示原始内容，**不会临时开实时 blur**。一次完整的六档生成完成后才发布；取消的任务不发布半成品。
- 若 Search/Drawer 尚在前台时跨分钟，先保留旧的局部缓存，返回空闲 Home 后只构建时钟的新六档。这是为了不在动画中启动 blur；不是每个动态事件都立即更新纹理。
- 仍保持 V67 禁用 Search/Drawer 额外 scrim 的状态，没有在本次修复中重新改变这些遮罩。

**范围缺口必须说明：** 附件的 `HomeContent` 内 `HorizontalPager` 内容是空的，未实际渲染桌面图标、文件夹或 AppWidgetHostView，也没有天气控件。虽然模型、编辑设置和 Widget 管理代码存在，但不能据此声称这些控件已经接入缓存。这版接入实际已有的壁纸、页码指示器和时钟，并提供独立区域缓存组件。以后添加 Widget 时，每个实例必须放到独立区域、监听自己的内容更新并提供版本键，不能把动态 View 放进静态截图。没有在此次壁纸修复中重建完整桌面组件系统。

## 缓存失效与验收

`adb logcat -s HomeCache:D` 可观察 `StaticHomeCache build=N` 和 `DynamicRegionCache.Clock build=N`。日志另含每级尺寸、纹理有效字节数估算和构建耗时；它们不是 GPU 功耗或实际总内存测量。

| 操作 | 预期 |
| --- | --- |
| 新安装，系统静态或动态壁纸 | 正常显示真实壁纸，不能再出现由主色兜底制造的紫色覆盖 |
| 选择图片，回到 Home 等待构建完成 | 生成静态六档；下拉 Search / 上拉 Drawer 能看到同源模糊背景 |
| 连续上下滑动、取消手势、反向拖动、回弹 | 已预热且内容不变时，两个 build 计数都不增加 |
| Search 显示/隐藏键盘 | 不因 IME 事件启动缓存构建；页面和时钟位置不应跳动 |
| 开启时钟，在空闲 Home 等待跨分钟 | 静态计数不变，时钟计数增加；看不到旧时钟残影或矩形缺口 |
| Search 打开时跨分钟，再返回 Home | Search 内保留上次时钟纹理；返回后时钟局部更新，静态计数不变 |
| 更换桌面图片、改变页数/布局/时钟布局尺寸 | 对应静态内容或几何变化，生成新静态缓存；不能继续显示旧壁纸 |
| 修改时钟位置/缩放/颜色，切换桌面页，关闭再开启时钟 | 验证实时与缓存位置一致、没有双重缩放或被裁掉的文字 |
| 旋转屏幕 / 字体或密度变化 / 重启应用 | 尺寸/密度变化或进程重启属于合法重建；不沿用旧尺寸纹理 |
| 超大图片、选图取消、文件解码失败 | 不阻塞主线程解码；失败不以颜色冒充图片，不破坏已提交的壁纸文件 |

先在 Android 12+ 真机编译安装执行：

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -s HomeCache:D
```

GPU/卡顿：使用同一设备、壁纸、亮度、刷新率，在缓存预热后录制相同的 30 秒连续手势，比较 V67/V68 的 Perfetto FrameTimeline、卡顿帧及设备支持的 GPU 计数器/功耗数据。日志中的 build=0 增量只能证明没有重新构建，不等于“功耗已降低”。图形分析中 RenderEffect 应只在首次或内容失效构建期间出现。

内存：仅六档 RGBA 半分辨率静态纹理，1080×2400 屏约 14.8 MiB，1440×3200 屏约 26.4 MiB；还要加局部时钟纹理、选入壁纸、渲染层以及构建时新旧代重叠的峰值。日志按像素估算，真实 PSS/GPU 内存请用 Android Studio Profiler / dumpsys meminfo 测量。没有实现 1/4 缩放、跨档插值或智能淘汰。

## 本环境实际完成的验证

- V61 手势协调器检查：12 项通过。
- 与附件比对：135 个原有文件字节未变，4 个已有源码文件发生预期修改，新增 `HomeWallpaper.kt`。
- `LauncherRoot` 三段 pointerInput 手势处理逻辑字节未变；SearchContent、AllAppsContent、GestureCoordinator、SearchCoordinator、VerticalPageTransitionController、MinuteClock 均未修改。
- 全部 Kotlin 文件通过字符串/注释剥离后的括号平衡检查；**不等同于 Kotlin 编译**。
- Gradle 构建尝试：被环境网络限制阻断，无法下载 `gradle-8.13-bin.zip`。因此没有完成编译、JUnit 执行、APK 安装、视觉、GPU 功耗或帧率验证。
- 旧 `verify_source_invariants.py` / `verify_preserved_ui.py` 是早期版本专用脚本，对当前带顶层目录的附件结构不兼容，运行失败；不能用它们宣称此次通过。旧 V65 脚本更是专门要求“时钟进入整屏版本键”，与本次目标相反。

改动文件：`MainActivity.kt`、`ui/LauncherRoot.kt`、`ui/HomeContent.kt`、`ui/HomeBackdropCache.kt`、新增 `ui/HomeWallpaper.kt`。原有历史说明保留，但以本说明描述 V68 当前行为。`verification/v68_changes.patch` 提供相对于附件源码的修改。
