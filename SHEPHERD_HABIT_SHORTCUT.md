# 搜索「习惯」直达 Shepherd

## 使用方式

必须同时安装更新后的 FlowerLauncher 和 Shepherd。

1. 下拉打开 Launcher 搜索，输入完整的「习惯」。
2. 点击顶部的「Shepherd · 习惯」，或按软键盘上的搜索键。
3. Shepherd 将直接显示「习惯」标签页；即使 Shepherd 已在后台，也应切换到习惯页。

仅对完整的「习惯」（前后空格可忽略）触发，避免干扰普通的应用名称搜索。若 Shepherd 被隐藏/屏蔽而不出现在 Launcher 的应用列表，或尚未安装，入口不会显示。若已安装但版本旧、不支持链接，点击后会提示更新。

## 技术约定

- URI：`shepherd://habit`
- Android 操作：`ACTION_VIEW`
- 目标包名：`com.shepherd.timetracker`（Launcher 使用 `setPackage` 限定接收方）
- Activity 启动标记：`NEW_TASK | CLEAR_TOP | SINGLE_TOP`，复用已有主界面，避免无意义的重复栈。
- Shepherd 通过 Manifest 注册 `VIEW` deep link，`MainActivity` 在 `onCreate` / `onNewIntent` 识别请求，向 `MainScreen` 发送一次性的切换事件。

## 手动检查（有 Android SDK/设备时）

- 两个工程分别执行 `./gradlew :app:assembleDebug --offline --no-daemon --console=plain`。
- 两个 App 安装后验证 Launcher 搜索「习惯」的点击和键盘搜索键。
- 在 Shepherd 当前位于「记录」/「统计」时切回 Launcher，再点快捷入口，确认前台直接落在「习惯」。
- 单独验证深度链接：
  `adb shell am start -a android.intent.action.VIEW -d shepherd://habit -p com.shepherd.timetracker`
- 验证常规搜索与开屏不变：`习`、`习惯养成` 不应触发快捷入口；正常点击 Shepherd 图标应保留原有启动行为。
