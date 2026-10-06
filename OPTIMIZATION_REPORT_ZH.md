# Shepherd Launcher 源码优化说明

日期：2026-09-29（北京时间）
基线：本次提供的 shepherd_launcher_source(1).zip。

本版完成了源码层面的性能与异常处理优化。尚未完成 Android 编译、真机视觉回归或功耗测量，不能据此承诺具体降温、续航、帧率或崩溃率提升。请通过下文验收后再作为日常桌面使用。

## 修改内容

| 改动 | 原版开销或风险 | 本版处理 | 适用范围 |
| --- | --- | --- | --- |
| 分钟时钟 | 只显示 HH:mm，但每秒更新 LocalDateTime，后台仍有轮询 | 使用系统分钟、时间、时区、语言广播；只在宿主 STARTED 时监听，恢复前台立即刷新 | 开启时钟且处于首页时；默认关闭时钟时不会产生这项收益 |
| 时钟字体 | 每次时钟更新重新构造 FontFamily | 按字体和字重缓存；日期格式按语言缓存 | 时钟可见时 |
| 抽屉遮罩 | 过渡每帧创建渐变 Brush 及颜色停止点 | drawWithCache 缓存固定渐变，绘制时应用相同 fade | 抽屉进出动画 |
| 壁纸模糊 | 图层开关会重启观察流；匿名回调无法统一撤销 | 图层状态由同一个 snapshotFlow 观察；合并待执行回调，销毁时清理；Android 12 以下和关闭模糊时不观察无效的模糊进度 | 保留 OEM 重绘、下一帧重申和焦点恢复逻辑 |
| 搜索 | 每次输入重复 lowercase、创建 Pair、对结果排序 | 缓存应用搜索字段；应用列表变化时排序一次，查询按原评分分桶 | 查询扫描仍为 O(n)，未加入输入延迟 |
| 搜索附属数据 | 同一列表重复 take、按包名线性查找图标 | 缓存常用应用列表及包名映射；保留同包名第一个活动的图标 | 输入与重组 |
| 应用加载 | Activity 重建会重复扫描和解码图标；取消任务后仍可能继续处理列表 | ViewModel 复用列表；语言、密度、uiMode 改变时重载；只持有 applicationContext；逐应用检查协程取消 | 旋转/重建；不增加磁盘图标缓存 |
| 应用异常 | 标签读取、PackageManager 异常可能终止加载并导致崩溃 | 单应用标签失败回退包名；整体运行时异常记录日志，下一次前台进入可重试 | 异常路径 |
| 重复图标解码 | 先解码再对组件去重 | 解码前按组件去重；仍为原来的 144×144 像素 | 系统返回重复条目时 |
| 重复保存 | 相同设置值仍执行序列化和 SharedPreferences.apply | 值未变直接返回；隐藏/别名/小部件高度也做相同处理 | 特别是滑块连续事件落到同一整数值时 |
| 小部件稳定性 | 分配、查询、配置、删除部分路径未保护；配置期间重建会丢失待处理 ID | 统一异常处理与待处理 ID 清理；保存/恢复 ID 和配置模式；不删除已有小部件；拒绝并发打开多个选择流程 | 系统组件缺失、应用卸载、Activity 重建；进程恢复取决于系统是否提供 savedInstanceState |
| 手势任务 | 切换到新的过渡时可能残留搜索拉伸 snap 任务 | 在开始交互和 settle 时取消旧的搜索 snap 任务 | 快速打断、反向手势 |
| 应用启动 | 回退启动 Intent 的查询也可能抛出异常 | 捕获异常，继续使用原有启动失败提示 | 应用已卸载或系统服务异常 |

## 保持原样的部分

- 动画时长、回弹曲线、滑动阈值、抽屉起始 200px、缩放、透明度、模糊强度及等级。
- 页面层级、手势主流程、搜索前缀、结果评分、同名结果的稳定顺序、空查询返回原顺序。
- 图标分辨率、字号、间距、颜色资源、设置界面、主题和 Manifest。
- Android、Kotlin、Compose 和 Gradle 依赖版本及构建配置；没有通过升级依赖或调整显示刷新率实现优化。

抽屉渐变优化在实数模型中保持 alpha 插值一致。GPU 像素量化、厂商合成器和生命周期时序仍需要真机截图/录像验收；“参数未变”不等于已证明每个像素、每个手势都一致。

搜索背景的动态覆盖高度渐变，以及搜索结果的嵌套网格布局，本次保留原实现。后者在大量匹配结果时仍可能一次布局较多图标；替换为单一懒加载容器需要额外验证滚动、行高、文字换行、菜单锚点和返回位置，不能在未做视觉回归时宣称无感替换。

## 已完成的检查

- 26 个动画/模糊常量与原版一致。
- 9 个视觉、手势或窗口模糊函数及回弹 Easing 与原版一致。
- 14 个资源、构建、主题、设置相关文件字节一致。
- 4 个修改过的 UI 文件中的 dp/sp 字面尺寸保持一致。
- 202,202 个抽屉 alpha 插值数值对照通过（数学模型，不是 GPU 像素测试）。
- 修改差异检查未发现新增行尾空白。
- ZIP 内容、文件列表和 CRC 完整性检查通过（交付打包时执行）。

这些检查均不代替 Kotlin 编译或真机测试。

## 已编写、尚未运行的回归测试

`app/src/test/java/com/flower/launcher/apps/AppSearchEngineTest.kt` 包含 5 个 JUnit 测试，覆盖空查询原序、评分排序及同名稳定顺序、别名更新、多语言/包名匹配，以及 100 组随机目录 × 30 次查询和原算法的对照。

本环境 Java 17 可以启动，但没有可用的 Android SDK/已缓存 Gradle。启动项目原有 Gradle Wrapper 时，下载 gradle-8.13-bin.zip 返回 `java.net.SocketException: Network is unreachable`，因此 **未完成编译、JUnit、Lint、APK 生成**。没有附带未经验证的 APK。

在具备项目原有依赖和 Android SDK 36 的环境中，从工程根目录执行：

```bash
bash gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Windows 使用 `gradlew.bat`。如原有镜像不能访问，需要在可联网的构建环境解决依赖获取问题。

静态一致性检查可重复执行：

```bash
python3 verification/verify_source_invariants.py '/path/to/shepherd_launcher_source(1).zip'
```

`verification/optimization.patch` 提供相对原工程的源码与 JUnit 测试差异；完整 ZIP 已包含应用后的代码，无须再次应用补丁。

## 真机验收建议

1. 使用同一设备、壁纸、亮度、刷新率、应用列表和设置对比原版/优化版；性能对比使用相同构建类型，避免 debug 与 release 混比。
2. 检查 Home→搜索→Home、Home→抽屉→Home、连续反向滑动、半程松手、快速按 Home、搜索二段拉伸及设置返回。确认时长、轨迹、模糊、键盘时机和点击区域一致。
3. 在原项目重点适配的 vivo 设备上，检查抽屉刚打开、焦点重新获得、返回桌面和切换壁纸时，模糊不延迟、不残留，桌面亮度不变。也覆盖 Android 8–11 无窗口模糊路径和 Android 12+ 路径。
4. 开启时钟，跨分钟/午夜观察；切换时区、语言，锁屏数分钟后返回，确认显示及时更新；关闭时钟时不应注册对应接收器。
5. 搜索中英文、同名应用、包名、前缀；修改别名、显示隐藏应用、多活动同包应用，确认结果和图标一致。
6. 测试小部件取消选择、取消配置、配置页旋转/重建、配置应用被卸载、系统不提供选择器。已有小部件不得因配置失败而被删除。
7. 用 Perfetto/System Trace 对比相同手势场景的帧时、主线程工作、GC；用 Memory Profiler 检查旋转后没有持续积累的 Activity、Bitmap、接收器和回调。观察 `AppListViewModel`/`AppRepository`/`MainActivity` 日志。
8. 功耗/温升用固定时长场景分别测试：时钟开启静置、反复开关抽屉、反复输入搜索、切到其他应用。每组保持初始温度/电量相近，重复多轮记录中位数。不能从“减少代码工作量”直接推导整机耗电降低百分比。

## 边界

应用目录仍没有加入安装/卸载广播的完整增量更新机制；这属于原工程既有缺口。已缓存目录在新装/卸载后可能需要重启进程才能刷新。本版的 ViewModel 缓存不把普通旋转当作刷新方式。后续若要补全，应加入按事件失效和图标增量更新，并单独验收。

本轮主要减少 CPU 重复工作和对象分配，处理已识别的系统调用异常。由于保留实时模糊和原动画，相关 GPU 合成成本仍然存在。没有真实设备数据，不能声称“掉帧减少 X%”或“已经解决所有崩溃”。

## 实现参考

- Android Compose 性能：https://developer.android.com/develop/ui/compose/performance
- Compose 绘制缓存：https://developer.android.com/develop/ui/compose/graphics/draw/modifiers
- 广播的最小生命周期范围：https://developer.android.com/develop/background-work/background-tasks/broadcasts
- Intent 时间广播：https://developer.android.com/reference/android/content/Intent#ACTION_TIME_TICK
