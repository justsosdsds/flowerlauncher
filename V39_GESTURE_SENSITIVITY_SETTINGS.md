# V39 Gesture Sensitivity Settings

新增主页上下滑手势灵敏度设置，并接入现有 LauncherPreferences 持久化。

## 设置路径

设置 -> 手势与热键 -> 滑动手势 -> 主页上下滑灵敏度

## 参数

- 拖动距离：160–480 dp，步进 10 dp，默认 320 dp。越小，同样手指位移产生的页面 progress 越大。
- 切换阈值：15–60%，步进 1%，默认 38%。越小，慢速拖动松手后越容易完成页面切换。
- 甩动速度阈值：300–1800 dp/s，步进 50 dp/s，默认 900 dp/s。越小，短促快速甩动越容易直接触发切换。

三个参数同时用于 Home <-> Search 与 Home <-> All Apps，保持上下方向判定对称。

## 实现

- LauncherPreferencesRepository 增加三个持久化字段。
- LauncherRoot 不再使用硬编码 DRAG_DISTANCE / SETTLE_THRESHOLD / OPEN_VELOCITY。
- pointerInput keys 包含三个运行时参数，设置修改后返回 Launcher 即可对下一次手势生效。
- 设置页提供恢复默认灵敏度按钮。
