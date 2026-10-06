# V56 — Search 会话复位 + 上拉返回振动

## 修复
- 修复“有查询内容 → 全屏 Search → bottom rubber-band 超阈值返回 Home → 再次进入 Search 后页面整体偏高且无法交互”。
- 原因是上一 Search 会话遗留 `rawOverscrollPx < 0` 与 `homeReturnRequested = true`；它们不应依赖 SearchContent 是否被销毁来清理。
- 现在由 LauncherRoot 把 Search 前台会话状态传入 SearchContent；页面真正回到 Home 后显式：
  - `resetOverscroll()`
  - `scrollState.scrollTo(0)`
  - `imeCollapseRequested = false`
  - `imeRevealRequested = false`
  - `homeReturnRequested = false`
- 复位发生在 Search 已离开屏幕后，不会让退出动画中途 snap 回原位。

## 新增
- 「交互设置 → 页面返回」新增：
  - 达到返回阈值时振动（默认开启）
  - 返回振动强度（10–100%，默认 60%）
- bottom rubber-band 首次越过“返回主页上拉距离”时振动一次，然后继续走现有 Search → Home 返回动画。
- IME 下拉阈值振动仍为独立开关与独立强度。

## 保持不变
- V55 搜索栏颜色 / 深度 / 不透明度
- V54 bottom rubber-band 返回 Home 阈值
- V52 固定 Results + IME overlay
- V51 可中断 rebound
- Home / Search / Drawer 现有视觉参数与 Blur

## 验证
- 静态检查通过；本次只有 4 个源码文件变化。
- `SearchInteractionState.kt` 与 V55 完全一致。
- Gradle 实编译仍被 Wrapper 的阿里云 Gradle 8.13 地址 DNS 阻断。
