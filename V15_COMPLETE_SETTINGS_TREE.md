# V15 完整设置树 UI

目标：先把 Smart Launcher 反编译文档中的主要设置树完整落到 FlowerLauncher UI，再逐项实现功能。

本版新增/补全：
- 顶级设置：主页、应用页、搜索、Feed、Web、Google/Discover、页面管理、通知、文件夹、小部件、安全、屏幕、多显示、故障排除、实验、开发者、Premium、关于。
- 主页子页：主页网格、主页图标外观、Dock。
- 应用页子页：抽屉网格、应用页图标外观、分类栏、文件夹。
- 搜索子页：搜索提供者；列出 Google/Yahoo/Yandex/Baidu/DuckDuckGo/Bing/Startpage/Naver/Qwant/Ecosia/Brave/Kagi/Perplexity/ChatGPT、自定义提供者、Play Store/YouTube/Maps，以及搜索栏外观条目。
- 手势子页：热键、单指、双指、Smart Display Off。
- 外观子页：主题颜色、深色模式、字体、主页/应用页图标外观。
- 图标外观子页：文件夹图标。
- 安全子页：隐藏应用管理。
- Feed 子页：Web 页面、Google/Discover 面板。
- 实验子页：开发者选项。
- 完整列出壁纸、通知、文件夹、小部件、Dock、分类栏、屏幕、备份、多显示、RSS、Premium 等文档项目。

当前原则：
- 已有底层实现的设置保持可操作。
- 尚未实现的设置完整显示名称、类型/默认值/范围/条件，右侧标记“待接功能”。
- 不使用假开关冒充功能已经实现。

编译说明：当前环境缺少 Gradle 8.13 本地分发包且不能联网，因此无法执行实际 Gradle 编译；代码已做结构级检查。
