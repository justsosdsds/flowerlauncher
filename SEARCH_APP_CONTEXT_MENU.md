# v11 — Search app long-press menu

Added a long-press context menu for apps shown on the Search page.

Implemented actions:
- Share: shares the Play Store URL for the package.
- App info: opens Android's application details page.
- Rename: stores a launcher-local alias and uses it in Home/Search.
- Hide / Unhide: persists by component key. Hidden apps disappear from Home and Search by default.
- Play Store: opens the package details page with web fallback.
- Uninstall: opens Android's package uninstall confirmation UI.

Hide confirmation:
- Tapping Hide first opens a confirmation dialog explaining that the app will disappear from app/search results.
- Search settings > "在搜索结果中显示隐藏的应用" now actually controls whether hidden apps appear in Search.
- When a hidden app is visible through that setting, its long-press menu shows "取消隐藏".

Custom icon:
- The "编辑图标" row is present but disabled for now because this source tree does not yet contain a persistent custom-icon URI/bitmap pipeline.

Implementation:
- New `apps/AppCustomizationRepository.kt` stores hidden component keys and aliases in SharedPreferences.
- `LauncherRoot.kt` applies aliases and hiding before Home/Search consume the app list and wires system intents.
- `SearchContent.kt` adds the long-press menu and rename/hide dialogs.

Build note:
- Gradle compilation could not be run in the sandbox because Gradle 8.13 is not cached and the environment cannot download it.
