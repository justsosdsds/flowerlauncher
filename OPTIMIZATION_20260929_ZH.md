# 2026-09-29 本次优化说明

基线：本次上传的 `shepherd_launcher_source(2).zip`。这是一轮保守的内部优化，源码已修改；尚未取得 Android 构建及真机验证结果。项目中其他 Vxx 和优化文档是原包保留的历史记录，请以本文件了解本次改动。

## 已完成

1. **减少动画中的无效组合。** 搜索是否拥有前景改为记住的派生状态，组合只观察布尔结果。判断条件和各图层的位移、缩放、透明度、模糊公式保持原样。
2. **让抽屉排序缓存跨开关保留。** 排序结果及字母索引由 LauncherRoot 持有，关闭抽屉不再销毁缓存。应用、别名或隐藏状态变化时才重建；名称排序继续使用原有主／次排序键及稳定顺序，其他排序模式继续维持原行为。
3. **缓存完整的名称排序键。** ICU 转写后的归一化、首字母和排序键一并缓存，比较器只比较已准备好的字符串。初次应用加载时在后台预热名称缓存，避免把这部分首次计算集中到打开抽屉时。转写器调用也纳入串行锁，避免后台预热与 UI 别名编辑并发访问同一对象。此举将一部分工作提前到初始目录加载，不代表冷启动一定变快。
4. **应用目录随包变化刷新。** 接入 LauncherApps.Callback，处理当前用户的安装、卸载、更新及可用性变化；连续事件合并 150ms 后处理。仍查询完整可启动组件列表，以保留原排序与删除失效组件，但仅重新读取变化包及新组件的名称、图标，复用其他条目。
5. **刷新任务防止旧结果覆盖。** 新事件取消旧任务，未成功处理的失效包持续保留；仅最新成功任务发布结果并确认失效记录。注册监听失败时，下次进入前台重试并完整扫描；恢复注册时也重新核对目录。监听随 ViewModel 清理，不持有 Activity。

## 外观与交互的保留范围

本轮没有替换搜索滚动容器或惯性算法，也没有补齐主页小部件、文件夹和未实现的排序功能。

源码对照确认以下部分保持一致：

- 整个页面过渡控制器、壁纸模糊宿主、缓动与辅助函数。
- LauncherRoot 的 5 个绘制／图层块、2 个 pointerInput 处理块。
- 抽屉的 3 个 pointerInput 处理块，以及排序数据准备之后的布局、菜单、字母栏和图标绘制代码。
- 27 个过渡／模糊常量、原有 dp/sp 尺寸。
- SearchContent、HomeContent、所有设置界面、主题、资源、Manifest、构建配置。
- 83 个原始文件中的 78 个逐字节未变；其余 5 个是下列修改文件。

这是源码级保护，不能等同于“真机动效已验证完全一致”。

## 修改文件

| 文件（相对于 app/src/main/java/com/flower/launcher） | 变化 |
| --- | --- |
| ui/LauncherRoot.kt | 派生前景状态；持有抽屉目录缓存 |
| ui/AllAppsContent.kt | 使用已准备的目录，保留布局和交互 |
| apps/AppNameSortKey.kt | 缓存完整名称键，串行访问 ICU |
| apps/AppRepository.kt | 复用未变化条目；后台预热名称键 |
| apps/AppListViewModel.kt | 包变化监听、事件合并、取消及清理 |
| apps/AppDrawerCatalog.kt（新增） | 排序数据与字母索引；纯归一化函数 |

新增 `AppDrawerCatalogTest.kt` 的四个 JUnit 测试，覆盖中文转写结果的首字母分组、数字／符号分组、同名稳定顺序、别名变更、空列表，以及混合名称的随机对照。测试注入转写后的文本，不替代 Android ICU 的设备测试。原 AppSearchEngineTest 保持不变。

## 验证记录

已通过：

```text
python3 verification/verify_source_invariants.py /path/to/shepherd_launcher_source\(2\).zip
python3 verification/verify_preserved_ui.py /path/to/shepherd_launcher_source\(2\).zip
```

完整输出见 `verification/optimization_20260929_checks.txt`。第二个脚本是本轮新增的源码保护检查。

未能运行：

```text
bash gradlew --offline :app:testDebugUnitTest
```

先前 Java 缺失 libjli.so 的启动问题已通过为本次命令指定本机运行库路径解决。随后 Gradle Wrapper 需要下载原项目指定的 Gradle 8.13 分发包，但网络返回 `java.net.SocketException: Network is unreachable`。`--offline` 不能替代尚未安装的 Gradle 本体，因此构建在编译源码前中止。

**没有编译成功记录、JUnit 通过记录、APK 或帧耗时／功耗数据；新增测试是待执行状态。** 本轮未改动依赖版本和仓库地址来绕过限制。

## 在 Android 开发环境中的下一步验证

在已安装 JDK 17、Android SDK 36 且可获取项目依赖的环境中，解压项目，用 Android Studio 打开并执行：

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

随后使用相同设备与设置对照原版：

1. 连续打开／关闭抽屉；主页 → 抽屉 → 主页 → 搜索，途中反向。
2. 搜索键盘弹出／隐藏、快速回主页、不同模糊强度。
3. 应用重命名与隐藏后，核对排序、字母跳转、搜索结果。
4. 启动器运行和退到后台两种情况下安装、卸载、更新应用；确认返回后列表和图标正确。
5. 连续更新两个包，或加载期间旋转／更改配置，确认旧结果不会覆盖新目录。

需要回退时直接使用原 ZIP；本次未修改用户原始压缩包。优化效果应以同设备的重组次数与帧耗时实测为准。
