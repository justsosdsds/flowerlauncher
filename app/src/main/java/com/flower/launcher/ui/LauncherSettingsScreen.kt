package com.flower.launcher.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flower.launcher.apps.InstalledApp
import com.flower.launcher.home.CLOCK_FONTS
import com.flower.launcher.home.CLOCK_WEIGHTS
import com.flower.launcher.home.MAX_HOME_PAGES
import com.flower.launcher.home.HomePreferences
import com.flower.launcher.home.addHomePage
import com.flower.launcher.home.removeHomePage
import com.flower.launcher.home.setDefaultHomePage
import com.flower.launcher.search.SearchPreferences
import com.flower.launcher.settings.LauncherPreferences
import com.flower.launcher.search.SearchTarget
import com.flower.launcher.search.SearchTargetGroup
import com.flower.launcher.ui.theme.OnHomePrimary
import com.flower.launcher.ui.theme.OnHomeSecondary

enum class LauncherSettingsSection {
    Home,
    HomeGrid,
    HomeIconAppearance,
    Clock,
    Apps,
    DrawerGrid,
    DrawerIconAppearance,
    Search,
    SearchEngine,
    Gestures,
    HotKeys,
    SingleFinger,
    DoubleFinger,
    SmartDisplay,
    Appearance,
    EditThemeColors,
    DarkMode,
    FontPicker,
    Wallpaper,
    Notifications,
    Folders,
    FolderIcons,
    Widgets,
    Dock,
    CategoryBar,
    Screen,
    Security,
    HiddenApps,
    Backup,
    PageManager,
    MultiDisplay,
    Feed,
    WebPanel,
    GooglePanel,
    Troubleshooting,
    Experimental,
    Developer,
    Premium,
    About,
}

private data class CatalogItem(
    val title: String,
    val detail: String? = null,
    val implemented: Boolean = false,
)

@Composable
fun LauncherSettingsScreen(
    homePreferences: HomePreferences,
    searchPreferences: SearchPreferences,
    launcherPreferences: LauncherPreferences,
    apps: List<InstalledApp>,
    hiddenComponentKeys: Set<String>,
    initialSection: LauncherSettingsSection? = null,
    onClose: () -> Unit,
    onReturnToCaller: (() -> Unit)? = null,
    onHomeChange: (HomePreferences) -> Unit,
    onSearchChange: (SearchPreferences) -> Unit,
    onLauncherChange: (LauncherPreferences) -> Unit,
    searchTargets: List<SearchTarget>,
    onSearchTargetsChange: (List<SearchTarget>) -> Unit,
    searchGroups: List<SearchTargetGroup> = emptyList(),
    onSearchGroupsChange: (List<SearchTargetGroup>) -> Unit = {},
    blockedComponentKeys: Set<String> = emptySet(),
    onSetAppBlocked: (InstalledApp, Boolean) -> Unit = { _, _ -> },
    onSetAppHidden: (InstalledApp, Boolean) -> Unit,
    onOpenWallpaper: () -> Unit,
    onAddFolder: () -> Unit,
    onAddWidget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var section by remember(initialSection) { mutableStateOf(initialSection) }
    var navigationStack by remember(initialSection) { mutableStateOf(emptyList<LauncherSettingsSection>()) }

    fun open(child: LauncherSettingsSection, parent: LauncherSettingsSection? = section) {
        val source = parent ?: section
        if (source != null) navigationStack = navigationStack + source
        section = child
    }

    fun goBack() {
        if (section == null) {
            onClose()
        } else if (navigationStack.isNotEmpty()) {
            section = navigationStack.last()
            navigationStack = navigationStack.dropLast(1)
        } else if (onReturnToCaller != null && section == initialSection) {
            onReturnToCaller()
        } else {
            section = null
        }
    }

    BackHandler { goBack() }

    if (section == LauncherSettingsSection.Search) {
        SearchSettingsScreen(
            preferences = searchPreferences,
            targets = searchTargets,
            groups = searchGroups,
            apps = apps,
            onBack = { goBack() },
            onChange = onSearchChange,
            onTargetsChange = onSearchTargetsChange,
            onGroupsChange = onSearchGroupsChange,
            blockedComponentKeys = blockedComponentKeys,
            onSetAppBlocked = onSetAppBlocked,
            modifier = modifier,
        )
        return
    }

    SettingsPageFrame(
        title = sectionTitle(section),
        onBack = ::goBack,
        modifier = modifier,
    ) {
        when (section) {
            null -> SettingsHome(onOpenSection = { open(it, null) })
            LauncherSettingsSection.Home -> HomeSettingsPage(homePreferences, onHomeChange, ::open)
            LauncherSettingsSection.HomeGrid -> CatalogSettingsPage(homeGridCatalog())
            LauncherSettingsSection.Clock -> ClockSettingsPage(homePreferences, onHomeChange)
            LauncherSettingsSection.HomeIconAppearance -> IconAppearanceSettingsPage(true, ::open)
            LauncherSettingsSection.Apps -> AppsSettingsPage(launcherPreferences, onLauncherChange, ::open)
            LauncherSettingsSection.DrawerGrid -> DrawerGridSettingsPage(launcherPreferences, onLauncherChange)
            LauncherSettingsSection.DrawerIconAppearance -> DrawerIconAppearanceSettingsPage(launcherPreferences, onLauncherChange, ::open)
            LauncherSettingsSection.SearchEngine -> CatalogSettingsPage(searchEngineCatalog())
            LauncherSettingsSection.Gestures -> GesturesSettingsPage(launcherPreferences, onLauncherChange, ::open)
            LauncherSettingsSection.HotKeys -> CatalogSettingsPage(hotKeysCatalog())
            LauncherSettingsSection.SingleFinger -> CatalogSettingsPage(singleFingerCatalog())
            LauncherSettingsSection.DoubleFinger -> CatalogSettingsPage(doubleFingerCatalog())
            LauncherSettingsSection.SmartDisplay -> CatalogSettingsPage(smartDisplayCatalog())
            LauncherSettingsSection.Appearance -> AppearanceSettingsPage(launcherPreferences, onLauncherChange, ::open)
            LauncherSettingsSection.EditThemeColors -> CatalogSettingsPage(themeColorCatalog())
            LauncherSettingsSection.DarkMode -> CatalogSettingsPage(darkModeCatalog())
            LauncherSettingsSection.FontPicker -> CatalogSettingsPage(fontCatalog())
            LauncherSettingsSection.Wallpaper -> WallpaperSettingsPage(onOpenWallpaper)
            LauncherSettingsSection.Notifications -> CatalogSettingsPage(notificationCatalog())
            LauncherSettingsSection.Folders -> FolderSettingsPage(onAddFolder, ::open)
            LauncherSettingsSection.FolderIcons -> CatalogSettingsPage(folderIconsCatalog())
            LauncherSettingsSection.Widgets -> WidgetSettingsPage(onAddWidget)
            LauncherSettingsSection.Dock -> CatalogSettingsPage(dockCatalog())
            LauncherSettingsSection.CategoryBar -> CatalogSettingsPage(categoryBarCatalog())
            LauncherSettingsSection.Screen -> CatalogSettingsPage(screenCatalog())
            LauncherSettingsSection.Security -> SecuritySettingsPage(::open)
            LauncherSettingsSection.HiddenApps -> HiddenAppsSettingsPage(apps, hiddenComponentKeys, onSetAppHidden)
            LauncherSettingsSection.Backup -> CatalogSettingsPage(backupCatalog())
            LauncherSettingsSection.PageManager -> PageManagerSettingsPage(homePreferences, onHomeChange)
            LauncherSettingsSection.MultiDisplay -> CatalogSettingsPage(multiDisplayCatalog())
            LauncherSettingsSection.Feed -> FeedSettingsPage(::open)
            LauncherSettingsSection.WebPanel -> CatalogSettingsPage(webPanelCatalog())
            LauncherSettingsSection.GooglePanel -> CatalogSettingsPage(googlePanelCatalog())
            LauncherSettingsSection.Troubleshooting -> CatalogSettingsPage(troubleshootingCatalog())
            LauncherSettingsSection.Experimental -> ExperimentalSettingsPage(::open)
            LauncherSettingsSection.Developer -> CatalogSettingsPage(developerCatalog())
            LauncherSettingsSection.Premium -> CatalogSettingsPage(premiumCatalog())
            LauncherSettingsSection.About -> AboutSettingsPage()
            LauncherSettingsSection.Search -> Unit
        }
    }
}

@Composable
private fun SettingsHome(onOpenSection: (LauncherSettingsSection) -> Unit) {
    SettingsSectionLabel("主要")
    SettingsNavRow(Icons.Default.Wallpaper, "壁纸", "视差、滚动、随机壁纸与调色板") { onOpenSection(LauncherSettingsSection.Wallpaper) }
    SettingsNavRow(Icons.Default.Palette, "全局外观", "主题颜色、动画、深色模式、字体和图标") { onOpenSection(LauncherSettingsSection.Appearance) }
    SettingsNavRow(Icons.Default.Gesture, "手势与热键", "双击、单指、双指、热键和屏幕锁定") { onOpenSection(LauncherSettingsSection.Gestures) }

    SettingsSectionLabel("页面")
    SettingsNavRow(Icons.Default.Home, "主页", "图标外观、网格、Dock 和高级设置") { onOpenSection(LauncherSettingsSection.Home) }
    SettingsNavRow(Icons.Default.Apps, "应用页面", "文件夹、图标、网格、分类、排序") { onOpenSection(LauncherSettingsSection.Apps) }
    SettingsNavRow(Icons.Default.Search, "搜索页面", "搜索提供者、启动页和搜索结果") { onOpenSection(LauncherSettingsSection.Search) }
    SettingsNavRow(Icons.Default.Settings, "页面管理", "增删、排序 panel 与页面、预设") { onOpenSection(LauncherSettingsSection.PageManager) }

    SettingsSectionLabel("其他")
    SettingsNavRow(Icons.Default.Folder, "文件夹", "沉浸式、颜色、列数、图标和标签") { onOpenSection(LauncherSettingsSection.Folders) }
    SettingsNavRow(Icons.Default.Widgets, "小部件", "主题、圆角和字体") { onOpenSection(LauncherSettingsSection.Widgets) }
    SettingsNavRow(Icons.Default.Lock, "安全与隐私", "PIN、指纹、隐藏应用、数据分享") { onOpenSection(LauncherSettingsSection.Security) }
    SettingsNavRow(Icons.Default.Info, "版本信息", "版本、设备、许可、致谢和系统快捷项") { onOpenSection(LauncherSettingsSection.About) }
}

@Composable
private fun HomeSettingsPage(
    preferences: HomePreferences,
    onChange: (HomePreferences) -> Unit,
    onOpenSection: (LauncherSettingsSection, LauncherSettingsSection?) -> Unit,
) {
    SettingsSectionLabel("主页")
    ToggleSetting("显示时钟", "在主页顶部显示日期和时间", preferences.showClock) { onChange(preferences.copy(showClock = it)) }
    SettingsNavRow(Icons.Default.Settings, "时钟", "字体、颜色与位置") { onOpenSection(LauncherSettingsSection.Clock, LauncherSettingsSection.Home) }
    ToggleSetting("锁定桌面布局", "锁定后禁止从长按菜单添加内容", preferences.layoutLocked) { onChange(preferences.copy(layoutLocked = it)) }
}

@Composable
private fun ClockSettingsPage(
    preferences: HomePreferences,
    onChange: (HomePreferences) -> Unit,
) {
    SettingsSectionLabel("时钟")
    ToggleSetting("显示时钟", "在主页顶部显示日期和时间", preferences.showClock) {
        onChange(preferences.copy(showClock = it))
    }

    SettingsSectionLabel("字体")
    CLOCK_FONTS.forEachIndexed { index, option ->
        RadioSetting(option.label, preferences.clockFont == index) {
            onChange(preferences.copy(clockFont = index))
        }
    }

    SettingsSectionLabel("字体宽度（粗细）")
    CLOCK_WEIGHTS.forEach { option ->
        RadioSetting(option.label, preferences.clockFontWeight == option.weight) {
            onChange(preferences.copy(clockFontWeight = option.weight))
        }
    }

    SettingsSectionLabel("字号与拉伸")
    StepperSetting("字体大小", preferences.clockFontSizeSp, 40, 160, step = 4, suffix = " sp") {
        onChange(preferences.copy(clockFontSizeSp = it))
    }
    StepperSetting("宽度拉伸", preferences.clockScaleXPercent, 50, 200, step = 5, suffix = "%") {
        onChange(preferences.copy(clockScaleXPercent = it))
    }
    StepperSetting("高度拉伸", preferences.clockScaleYPercent, 50, 200, step = 5, suffix = "%") {
        onChange(preferences.copy(clockScaleYPercent = it))
    }

    SettingsSectionLabel("颜色")
    ColorSwatchSetting("时间颜色", preferences.clockTimeColor, clockPalette()) {
        onChange(preferences.copy(clockTimeColor = it))
    }
    ColorSwatchSetting("日期颜色", preferences.clockDateColor, clockPalette()) {
        onChange(preferences.copy(clockDateColor = it))
    }

    SettingsSectionLabel("位置")
    StepperSetting("X 坐标", preferences.clockOffsetX, -300, 300, step = 10, suffix = " dp") {
        onChange(preferences.copy(clockOffsetX = it))
    }
    StepperSetting("Y 坐标", preferences.clockOffsetY, -300, 300, step = 10, suffix = " dp") {
        onChange(preferences.copy(clockOffsetY = it))
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = { onChange(preferences.copy(clockOffsetX = 0, clockOffsetY = 0)) }) {
            Text("重置位置")
        }
        TextButton(onClick = { onChange(preferences.copy(clockScaleXPercent = 100, clockScaleYPercent = 100)) }) {
            Text("重置拉伸")
        }
    }
}

@Composable
private fun PageManagerSettingsPage(
    preferences: HomePreferences,
    onChange: (HomePreferences) -> Unit,
) {
    val count = preferences.pageCount.coerceIn(1, MAX_HOME_PAGES)
    SettingsSectionLabel("页面")
    SettingsTextRow("当前总页数", count.toString())
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Button(
            enabled = count < MAX_HOME_PAGES,
            onClick = { onChange(addHomePage(preferences)) },
        ) { Text("添加页面") }
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
    (0 until count).forEach { index ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("第 ${index + 1} 页", color = OnHomePrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                if (preferences.defaultHomePage == index) {
                    Text("默认主页", color = Color(0xFF8ED9A7), fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
            TextButton(
                enabled = preferences.defaultHomePage != index,
                onClick = { onChange(setDefaultHomePage(preferences, index)) },
            ) { Text("设为默认") }
            TextButton(
                enabled = count > 1,
                onClick = { onChange(removeHomePage(preferences, index)) },
            ) { Text("删除") }
        }
    }
}

private fun clockPalette(): List<Int> = listOf(
    0xFFFFFFFF.toInt(),
    0xE6FFFFFF.toInt(),
    0xFF000000.toInt(),
    0xFFFF5A5A.toInt(),
    0xFFFFB74D.toInt(),
    0xFFFFE066.toInt(),
    0xFF8ED9A7.toInt(),
    0xFF5AA7FF.toInt(),
    0xFFB388FF.toInt(),
)

@Composable
private fun AppsSettingsPage(
    preferences: LauncherPreferences,
    onChange: (LauncherPreferences) -> Unit,
    onOpenSection: (LauncherSettingsSection, LauncherSettingsSection?) -> Unit,
) {
    SettingsSectionLabel("应用网格")
    SettingsNavRow(Icons.Default.Palette, "图标外观", "跟随主页或使用应用网格自定义配置") {
        onOpenSection(LauncherSettingsSection.DrawerIconAppearance, LauncherSettingsSection.Apps)
    }
    SettingsNavRow(Icons.Default.Settings, "网格属性", "列数、图标大小、垂直间距、标签") {
        onOpenSection(LauncherSettingsSection.DrawerGrid, LauncherSettingsSection.Apps)
    }
}

@Composable
private fun DrawerGridSettingsPage(
    preferences: LauncherPreferences,
    onChange: (LauncherPreferences) -> Unit,
) {
    SettingsSectionLabel("网格属性")
    StepperSetting("列数", preferences.drawerColumns, 3, 7) {
        onChange(preferences.copy(drawerColumns = it))
    }
    StepperSetting("图标大小", preferences.drawerIconSizeDp, 40, 96, step = 4, suffix = " dp") {
        onChange(preferences.copy(drawerIconSizeDp = it))
    }
    StepperSetting("垂直间距", preferences.drawerVerticalSpacingDp, 0, 48, step = 4, suffix = " dp") {
        onChange(preferences.copy(drawerVerticalSpacingDp = it))
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
    ToggleSetting("应用名称", "在图标下方显示应用名称", preferences.drawerShowLabels) {
        onChange(preferences.copy(drawerShowLabels = it))
    }
    StepperSetting("应用标签尺寸", preferences.drawerLabelSizeSp, 8, 20, suffix = " pt", enabled = preferences.drawerShowLabels) {
        onChange(preferences.copy(drawerLabelSizeSp = it))
    }
    ToggleSetting("允许标签换行", "有足够空间时，图标标签最多可使用两行", preferences.drawerAllowLabelWrap, enabled = preferences.drawerShowLabels) {
        onChange(preferences.copy(drawerAllowLabelWrap = it))
    }
    ToggleSetting("显示字母分组标题", "按名称排序时，在每组应用上方显示 A / B / C 标题", preferences.drawerShowLetterHeaders) {
        onChange(preferences.copy(drawerShowLetterHeaders = it))
    }
    ToggleSetting("使用自定义填充距", null, preferences.drawerUseCustomPadding) {
        onChange(preferences.copy(drawerUseCustomPadding = it))
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
    SettingsSectionLabel("背景")
    ColorSwatchSetting(
        title = "背景颜色",
        selected = preferences.drawerBackgroundColor,
        palette = drawerBackgroundPalette(),
    ) { color ->
        onChange(preferences.copy(drawerBackgroundColor = color))
    }
    StepperSetting("背景不透明度", preferences.drawerBackgroundOpacityPercent, 0, 100, step = 5, suffix = "%") {
        onChange(preferences.copy(drawerBackgroundOpacityPercent = it))
    }
}

@Composable
private fun DrawerIconAppearanceSettingsPage(
    preferences: LauncherPreferences,
    onChange: (LauncherPreferences) -> Unit,
    onOpenSection: (LauncherSettingsSection, LauncherSettingsSection?) -> Unit,
) {
    SettingsSectionLabel("应用网格")
    RadioSetting("跟随主页配置", !preferences.drawerUseCustomIconAppearance) {
        onChange(preferences.copy(drawerUseCustomIconAppearance = false))
    }
    RadioSetting("使用自定义配置", preferences.drawerUseCustomIconAppearance) {
        onChange(preferences.copy(drawerUseCustomIconAppearance = true))
    }

    if (preferences.drawerUseCustomIconAppearance) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        SettingsTextRow("图标包", "预设")
        ToggleSetting("自适应图标", "自定义图标形状", preferences.drawerAdaptiveIcons) {
            onChange(preferences.copy(drawerAdaptiveIcons = it))
        }
        StepperSetting("形状", preferences.drawerIconShape + 1, 1, 5, enabled = preferences.drawerAdaptiveIcons) { value ->
            onChange(preferences.copy(drawerIconShape = value - 1))
        }
    }
}

@Composable
private fun GesturesSettingsPage(
    preferences: LauncherPreferences,
    onChange: (LauncherPreferences) -> Unit,
    onOpenSection: (LauncherSettingsSection, LauncherSettingsSection?) -> Unit,
) {
    SettingsSectionLabel("滑动手势")
    ToggleSetting("启用滑动手势", "控制主页与搜索页的滑动手势", preferences.enableSwipeGestures) { onChange(preferences.copy(enableSwipeGestures = it)) }
    ToggleSetting("手势震动", "完成手势时提供轻微震动反馈", preferences.vibrationOnGesture, enabled = preferences.enableSwipeGestures) { onChange(preferences.copy(vibrationOnGesture = it)) }

    SettingsSectionLabel("主页上下滑灵敏度")
    Text(
        "数值越小越灵敏：拖动距离控制页面跟手速度；切换阈值控制慢滑松手后是否完成切页；甩动速度阈值控制快速短滑是否直接触发。",
        color = OnHomeSecondary,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
    )
    StepperSetting(
        "拖动距离",
        preferences.gestureDragDistanceDp,
        160,
        480,
        step = 10,
        suffix = " dp",
        enabled = preferences.enableSwipeGestures,
    ) { onChange(preferences.copy(gestureDragDistanceDp = it)) }
    StepperSetting(
        "切换阈值",
        preferences.gestureSettleThresholdPercent,
        15,
        60,
        step = 1,
        suffix = "%",
        enabled = preferences.enableSwipeGestures,
    ) { onChange(preferences.copy(gestureSettleThresholdPercent = it)) }
    StepperSetting(
        "甩动速度阈值",
        preferences.gestureOpenVelocityDpPerSec,
        300,
        1800,
        step = 50,
        suffix = " dp/s",
        enabled = preferences.enableSwipeGestures,
    ) { onChange(preferences.copy(gestureOpenVelocityDpPerSec = it)) }
    StepperSetting(
        "返回主页收起输入法阈值",
        preferences.searchReturnImeHideThresholdDp,
        0,
        40,
        step = 1,
        suffix = " dp",
        enabled = preferences.enableSwipeGestures,
    ) { onChange(preferences.copy(searchReturnImeHideThresholdDp = it)) }
    Text(
        "从搜索页向上返回时，净上滑达到该距离后立即清除输入焦点并收起输入法；0 dp 表示一确认向上返回就立即收起。",
        color = OnHomeSecondary,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(
            enabled = preferences.enableSwipeGestures && (
                preferences.gestureDragDistanceDp != 320 ||
                    preferences.gestureSettleThresholdPercent != 38 ||
                    preferences.gestureOpenVelocityDpPerSec != 900 ||
                    preferences.searchReturnImeHideThresholdDp != 8
                ),
            onClick = {
                onChange(
                    preferences.copy(
                        gestureDragDistanceDp = 320,
                        gestureSettleThresholdPercent = 38,
                        gestureOpenVelocityDpPerSec = 900,
                        searchReturnImeHideThresholdDp = 8,
                    )
                )
            },
        ) {
            Text("恢复默认灵敏度")
        }
    }
}

@Composable
private fun AppearanceSettingsPage(
    preferences: LauncherPreferences,
    onChange: (LauncherPreferences) -> Unit,
    onOpenSection: (LauncherSettingsSection, LauncherSettingsSection?) -> Unit,
) {
    SettingsSectionLabel("搜索切换动画")
    StepperSetting("回弹时间", preferences.searchReboundDurationMs, 160, 600, step = 20, suffix = " ms") {
        onChange(preferences.copy(searchReboundDurationMs = it))
    }
    ToggleSetting("磨砂 / 不透明玻璃", "Smart Launcher 为 PRO；FlowerLauncher 已有搜索背景模糊", preferences.blurEnabled) { onChange(preferences.copy(blurEnabled = it)) }
    StepperSetting("模糊强度", preferences.blurStrengthPercent, 0, 100, step = 10, suffix = "%", enabled = preferences.blurEnabled) { onChange(preferences.copy(blurStrengthPercent = it)) }
    StepperSetting("主页背景压暗", preferences.homeBackgroundDimPercent, 0, 50, step = 5, suffix = "%", enabled = preferences.blurEnabled) {
        onChange(preferences.copy(homeBackgroundDimPercent = it))
    }
    SettingsNavRow(Icons.Default.Palette, "应用页图标外观", "抽屉独立外观") { onOpenSection(LauncherSettingsSection.DrawerIconAppearance, LauncherSettingsSection.Appearance) }
    SettingsSectionLabel("界面缩放")
    StepperSetting("缩放比例", preferences.uiScalePercent, 70, 110, step = 5, suffix = "%") {
        onChange(preferences.copy(uiScalePercent = it))
    }
}

@Composable
private fun IconAppearanceSettingsPage(home: Boolean, onOpenSection: (LauncherSettingsSection, LauncherSettingsSection?) -> Unit) {
    SettingsSectionLabel(if (home) "主页图标外观" else "应用页图标外观")
    CatalogRows(
        listOf(
            CatalogItem("主页 / 应用页独立外观", "开关"),
            CatalogItem("图标包", "Picker"),
            CatalogItem("自适应图标", "开关"),
            CatalogItem("图标背景", "仅自适应图标开启时"),
            CatalogItem("背景色调", "仅自适应图标开启时"),
            CatalogItem("形状", "高级形状为 Premium"),
            CatalogItem("允许厂商图标定制", "条件显示 · 默认关闭"),
            CatalogItem("双击图标", if (home) "仅主页 · 默认关闭" else "仅主页可用"),
        )
    )
    SettingsNavRow(Icons.Default.Folder, "文件夹图标", "文件夹预览与图标样式") { onOpenSection(LauncherSettingsSection.FolderIcons, if (home) LauncherSettingsSection.HomeIconAppearance else LauncherSettingsSection.DrawerIconAppearance) }
}

@Composable
private fun WallpaperSettingsPage(onOpenWallpaper: () -> Unit) {
    SettingsSectionLabel("壁纸")
    Button(onClick = onOpenWallpaper, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) { Text("选择壁纸") }
}

@Composable
private fun FolderSettingsPage(onAddFolder: () -> Unit, onOpenSection: (LauncherSettingsSection, LauncherSettingsSection?) -> Unit) {
    SettingsSectionLabel("文件夹")
    Button(onClick = onAddFolder, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) { Text("添加文件夹") }
}

@Composable
private fun WidgetSettingsPage(onAddWidget: () -> Unit) {
    SettingsSectionLabel("小部件")
    Button(onClick = onAddWidget, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) { Text("添加小部件") }
}

@Composable
private fun SecuritySettingsPage(onOpenSection: (LauncherSettingsSection, LauncherSettingsSection?) -> Unit) {
    SettingsSectionLabel("安全与隐私")
    SettingsNavRow(Icons.Default.Lock, "管理隐藏应用", "应用列表 + 搜索 + 隐藏 / 显示") { onOpenSection(LauncherSettingsSection.HiddenApps, LauncherSettingsSection.Security) }
}

@Composable
private fun HiddenAppsSettingsPage(
    apps: List<InstalledApp>,
    hiddenComponentKeys: Set<String>,
    onSetAppHidden: (InstalledApp, Boolean) -> Unit,
) {
    SettingsSectionLabel("隐藏应用")
    Text("隐藏的应用默认不会出现在主页和搜索结果中；搜索设置可以选择重新显示。", color = OnHomeSecondary, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
    apps.sortedBy { it.label.lowercase() }.forEach { app ->
        val hidden = app.componentKey in hiddenComponentKeys
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconOrFallback(app = app, size = 42.dp, fallbackFontSize = 15.sp)
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(app.label, color = OnHomePrimary, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.packageName, color = OnHomeSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Switch(checked = hidden, onCheckedChange = { onSetAppHidden(app, it) })
        }
    }
}

@Composable
private fun FeedSettingsPage(onOpenSection: (LauncherSettingsSection, LauncherSettingsSection?) -> Unit) {
    SettingsSectionLabel("Your Feed / RSS")
    CatalogRows(feedCatalog())
    SettingsSectionLabel("相关页面")
    SettingsNavRow(Icons.Default.Settings, "Web 页面", "默认 https://news.google.com") { onOpenSection(LauncherSettingsSection.WebPanel, LauncherSettingsSection.Feed) }
    SettingsNavRow(Icons.Default.Settings, "Google / Discover 面板", "主题设置") { onOpenSection(LauncherSettingsSection.GooglePanel, LauncherSettingsSection.Feed) }
}

@Composable
private fun ExperimentalSettingsPage(onOpenSection: (LauncherSettingsSection, LauncherSettingsSection?) -> Unit) {
    SettingsSectionLabel("实验选项")
    CatalogRows(experimentalCatalog())
    SettingsNavRow(Icons.Default.Settings, "开发者选项", "Android 调试选项、开发者微件、新微件选择器、日志") { onOpenSection(LauncherSettingsSection.Developer, LauncherSettingsSection.Experimental) }
}

@Composable
private fun CatalogSettingsPage(items: List<CatalogItem>) {
    SettingsSectionLabel("设置项")
    CatalogRows(items)
}

@Composable
private fun CatalogRows(items: List<CatalogItem>) { items.forEach { CatalogSettingRow(it) } }

@Composable
private fun CatalogSettingRow(item: CatalogItem) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(item.title, color = OnHomePrimary.copy(alpha = if (item.implemented) 1f else 0.90f), fontSize = 17.sp, fontWeight = FontWeight.Medium)
            if (item.detail != null) Text(item.detail, color = OnHomeSecondary, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text(if (item.implemented) "已接入" else "待接功能", color = if (item.implemented) Color(0xFF8ED9A7) else OnHomeSecondary.copy(alpha = 0.65f), fontSize = 12.sp)
    }
}

@Composable
private fun AboutSettingsPage() {
    val context = LocalContext.current
    val version = remember(context) { context.appVersionLabel() }
    SettingsSectionLabel("FlowerLauncher")
    SettingsTextRow("版本", version)
    SettingsTextRow("包名", context.packageName)
    Text("设置结构依据 Smart Launcher 6 反编译设置树重建；FlowerLauncher 使用自己的代码与数据模型。", color = OnHomeSecondary, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp))
}

private fun homeGridCatalog() = listOf(
    CatalogItem("列数", "1–10"), CatalogItem("行数", "2–20"), CatalogItem("图标大小", "20–96dp"),
    CatalogItem("就地旋转", "rotateOnPlace"), CatalogItem("显示标签", "开关"), CatalogItem("标签大小", "80–240"),
    CatalogItem("两行标签", "开关"), CatalogItem("水平内边距", "0–96dp"), CatalogItem("垂直内边距", "0–96dp"),
    CatalogItem("无限滚动", "开关"),
)

private fun drawerGridCatalog() = listOf(
    CatalogItem("列数", "竖屏默认 3"), CatalogItem("横屏列数", "默认 6"), CatalogItem("行数", "布局相关"),
    CatalogItem("图标大小", "20–96dp"), CatalogItem("标签可见性"), CatalogItem("标签大小", "80–240"), CatalogItem("两行标签"),
)

private fun searchEngineCatalog() = listOf(
    CatalogItem("已启用提供者列表", "列表 · 默认按地区"),
    CatalogItem("Google"), CatalogItem("Yahoo"), CatalogItem("Yandex"), CatalogItem("Baidu", "FlowerLauncher 当前默认", true),
    CatalogItem("DuckDuckGo"), CatalogItem("Bing"), CatalogItem("Startpage"), CatalogItem("Naver"), CatalogItem("Qwant"),
    CatalogItem("Ecosia"), CatalogItem("Brave"), CatalogItem("Kagi"), CatalogItem("Perplexity"), CatalogItem("ChatGPT"),
    CatalogItem("添加搜索引擎", "自定义提供者"),
    CatalogItem("Play Store", "固定项"), CatalogItem("YouTube", "固定项"), CatalogItem("Maps", "固定项"),
    CatalogItem("搜索栏背景色", "搜索外观"), CatalogItem("搜索栏文字色", "搜索外观"), CatalogItem("搜索栏圆角", "搜索外观"),
    CatalogItem("描边宽度 / 颜色", "高级"), CatalogItem("阴影", "高级"), CatalogItem("自定义提示文本", "searchBarHint"),
    CatalogItem("文字对齐", "高级"), CatalogItem("点击动作", "Smart Search / Google / Assistant / Sesame"),
)

private fun hotKeysCatalog() = listOf(
    CatalogItem("前台时按主页", "动作 · 默认无"), CatalogItem("主页双击", "动作 · 默认无"), CatalogItem("长按返回", "动作 · 默认无"),
)

private fun singleFingerCatalog() = listOf(
    CatalogItem("左滑", "动作 · 默认无"), CatalogItem("右滑", "动作 · 默认无"),
    CatalogItem("上滑", "动作 · 默认无"), CatalogItem("下滑", "动作 · FlowerLauncher 当前用于搜索", true),
)

private fun doubleFingerCatalog() = listOf(
    CatalogItem("双指左滑", "PRO · 动作 · 默认无"), CatalogItem("双指右滑", "PRO · 动作 · 默认无"),
    CatalogItem("双指上滑", "PRO · 动作 · 默认无"), CatalogItem("双指下滑", "PRO · 动作 · 默认无"),
    CatalogItem("捏合", "PRO · 动作 · 默认无"), CatalogItem("张开", "PRO · 动作 · 默认无"),
    CatalogItem("顺时针旋转", "PRO · 动作 · 默认无"), CatalogItem("逆时针旋转", "PRO · 动作 · 默认无"),
)

private fun smartDisplayCatalog() = listOf(CatalogItem("Smart Display Off", "PRO"), CatalogItem("屏幕关闭方式", "动作 / 系统能力"))

private fun wallpaperCatalog() = listOf(
    CatalogItem("3D 视差", "开关 · PRO · 默认关闭"), CatalogItem("视差强度", "滑块 25–200 · 默认 100 · 仅 3D 视差开启时"),
    CatalogItem("可滚动壁纸", "开关 · 默认开启"), CatalogItem("壁纸获取通知", "开关 · 默认关闭"),
    CatalogItem("使用随机壁纸", "开关 · PRO · 默认关闭"), CatalogItem("随机间隔", "数字 · 默认 28800 秒 · 仅随机壁纸开启时"),
    CatalogItem("壁纸调色板", "模型 / 颜色提取"),
)

private fun notificationCatalog() = listOf(
    CatalogItem("主页通知", "开关 · 默认开启"), CatalogItem("应用页通知", "开关 · PRO · 默认开启"),
    CatalogItem("通知计数", "开关 · 默认开启"), CatalogItem("自适应颜色", "从图标取色 · 默认开启"),
    CatalogItem("徽标颜色", "颜色 · 默认 -1025192"), CatalogItem("徽标大小", "滑块 60–150 · 默认 100"),
    CatalogItem("徽标形状", "子页面"), CatalogItem("徽标圆角", "滑块 0–100 · 默认 -1"),
)

private fun darkModeCatalog() = listOf(
    CatalogItem("激活条件", "单选 · 默认跟随系统；总是 / 深色壁纸 / 夜间 / 跟随系统 / 从不"),
    CatalogItem("AMOLED 黑", "开关 · PRO · 默认关闭"),
)

private fun themeColorCatalog() = listOf(
    CatalogItem("Accent / 强调色", "颜色"), CatalogItem("Background / 背景", "颜色"), CatalogItem("On Background", "颜色"),
    CatalogItem("Surface", "颜色"), CatalogItem("On Surface", "颜色"), CatalogItem("Stroke", "颜色"),
    CatalogItem("Blur radius", "模糊半径"), CatalogItem("Blur vibrancy", "模糊鲜艳度"), CatalogItem("Liquid Glass", "玻璃相关参数"),
    CatalogItem("Widget 颜色组", "widget* 前缀配色"), CatalogItem("壁纸调色板", "从壁纸提取"),
)

private fun fontCatalog() = listOf(
    CatalogItem("标题字体", "titleFont3"), CatalogItem("正文字体", "bodyFont3"), CatalogItem("系统字体"),
    CatalogItem("IBM Plex Sans 预置字体", "preloaded_fonts"), CatalogItem("字体选择器", "FontPicker"),
)

private fun folderCatalog() = listOf(
    CatalogItem("沉浸式文件夹", "开关 · 默认开启"), CatalogItem("自定义文件夹颜色", "开关 · 默认关闭"),
    CatalogItem("文件夹背景色", "颜色 · 仅自定义颜色开启时"), CatalogItem("列数", "下拉 · 默认自动"),
    CatalogItem("主页图标大小", "滑块 60–100 · 默认 100"), CatalogItem("应用页图标大小", "滑块 60–100 · 默认 100"),
    CatalogItem("显示标签", "开关 · 默认开启"), CatalogItem("标签大小", "滑块 80–240 · 默认 140"), CatalogItem("两行标签", "开关 · 默认关闭"),
)

private fun folderIconsCatalog() = listOf(
    CatalogItem("文件夹预览样式", "folderPreviewStyle"), CatalogItem("文件夹图标背景主题", "folderIconBgTheme"),
    CatalogItem("文件夹背景", "folderWithBg"), CatalogItem("图标大小", "主页 / 应用页独立"),
)

private fun widgetCatalog() = listOf(
    CatalogItem("Smart Widgets 主题", "下拉 · 默认 home"), CatalogItem("圆角小部件", "开关 · 默认开启"),
    CatalogItem("圆角半径", "滑块 0–32 · 默认 16/24 · 仅圆角开启时"), CatalogItem("应用自定义字体到小部件", "开关 · 默认关闭"),
)

private fun dockCatalog() = listOf(
    CatalogItem("启用 / 显示 Dock", "开关"), CatalogItem("列数", "2–20"), CatalogItem("行数", "1–10"),
    CatalogItem("图标大小", "20–96dp"), CatalogItem("页数", "1–10"), CatalogItem("无限滚动", "页数 ≥ 2"),
    CatalogItem("标签可见性", "开关"), CatalogItem("标签大小", "数值"), CatalogItem("两行标签", "开关"),
    CatalogItem("背景", "开关 / 颜色"), CatalogItem("边到边 Edge-to-edge", "开关"), CatalogItem("圆角", "数值"),
    CatalogItem("水平溢出", "数值"), CatalogItem("垂直溢出", "数值"), CatalogItem("距网格距离", "数值"),
)

private fun categoryBarCatalog() = listOf(
    CatalogItem("启用分类栏", "开关 · 默认关闭"), CatalogItem("位置", "单选 · 底 / 左 / 右 / 侧 · 默认底"), CatalogItem("分类标签", "开关 · 默认关闭"),
)

private fun screenCatalog() = listOf(
    CatalogItem("屏幕方向", "单选 · 自动 / 竖屏 / 横屏 · 默认自动"), CatalogItem("超沉浸模式", "开关 · PRO · 设备有导航栏时显示"),
    CatalogItem("灰度模式", "开关 · 默认关闭"), CatalogItem("状态栏可见性", "单选 · 默认始终可见"),
    CatalogItem("深色状态栏图标", "单选 · Android 6+"),
)

private fun backupCatalog() = listOf(
    CatalogItem("创建备份", "动作"), CatalogItem("从文件还原", "动作 · 包含 Nova 导入"), CatalogItem("选择备份目录", "SAF 文件夹"),
    CatalogItem("每日备份", "开关 · PRO · 默认关闭"), CatalogItem("备份布局模式", "数字 · 默认 0"),
)

private fun pageManagerCatalog() = listOf(
    CatalogItem("添加页面"), CatalogItem("删除页面"), CatalogItem("页面排序"), CatalogItem("Panel 排序"), CatalogItem("页面预设"),
)

private fun multiDisplayCatalog() = listOf(
    CatalogItem("复制主屏", "镜像"), CatalogItem("独立配置", "每块显示器独立"), CatalogItem("统一配置", "统一 / 镜像模式"),
)

private fun feedCatalog() = listOf(
    CatalogItem("自动抓取", "开关 · 默认开启"), CatalogItem("分组相似文章", "开关 · 默认开启"), CatalogItem("来源栏", "开关 · 默认开启"),
    CatalogItem("打开方式", "下拉 · 默认 0"), CatalogItem("刷新时间", "数字 · 默认 10800 秒"),
    CatalogItem("阅读字体大小", "默认 18"), CatalogItem("阅读行高", "默认 17"), CatalogItem("阅读字距", "默认 1"),
    CatalogItem("阅读字体", "字体选择"), CatalogItem("屏蔽词", "列表"), CatalogItem("主题", "外观"), CatalogItem("管理来源", "列表"),
)

private fun webPanelCatalog() = listOf(CatalogItem("网页 URL", "文本 · 默认 https://news.google.com"))
private fun googlePanelCatalog() = listOf(CatalogItem("Google 面板主题", "下拉 · 默认 0"))

private fun troubleshootingCatalog() = listOf(
    CatalogItem("重启 Launcher"), CatalogItem("重置设置"), CatalogItem("FAQ"), CatalogItem("报告问题"), CatalogItem("社区"), CatalogItem("还原"),
)

private fun experimentalCatalog() = listOf(
    CatalogItem("用户界面实验项", "e2 / f2 / y / z / A / c2 / D / E / F"), CatalogItem("高级选项", "P1 / a2"),
    CatalogItem("强制重同步"), CatalogItem("加入 Beta"), CatalogItem("壁纸滚动", "U / W / p1"), CatalogItem("随机壁纸", "k2 / m2"),
    CatalogItem("SL7 Preview", "实验描述"),
)

private fun developerCatalog() = listOf(
    CatalogItem("Android 调试选项"), CatalogItem("开发者小部件"), CatalogItem("新小部件选择器"), CatalogItem("文件日志", "enableFileLog"),
    CatalogItem("Widget 调试视图", "devViewWidgetDebug"), CatalogItem("系统开发者选项"),
)

private fun premiumCatalog() = listOf(
    CatalogItem("Smart Widgets", "Premium"), CatalogItem("RSS 全文阅读", "Premium"), CatalogItem("磨砂 / 不透明玻璃", "Premium"),
    CatalogItem("自定义分类", "Premium"), CatalogItem("每日备份", "Premium"), CatalogItem("高级自适应图标形状", "Premium"),
    CatalogItem("AMOLED 黑", "Premium"), CatalogItem("超沉浸模式", "Premium"), CatalogItem("应用页内文件夹", "Premium"),
    CatalogItem("应用页通知徽标", "Premium"), CatalogItem("壁纸 3D 视差", "Premium"), CatalogItem("随机壁纸", "Premium"),
    CatalogItem("手动图标排序", "Premium"), CatalogItem("弹出微件", "Premium"), CatalogItem("Smart Screen Off", "Premium"),
    CatalogItem("双指手势", "Premium"), CatalogItem("保护主页更改", "Premium"), CatalogItem("去广告", "Premium"),
    CatalogItem("更多自定义 / 快速启动", "Premium"),
)

@Composable
private fun SettingsPageFrame(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxSize().background(Color(0xFF232428)).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().height(78.dp).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = OnHomePrimary, modifier = Modifier.size(32.dp).clickable(onClick = onBack))
            Text(title, color = OnHomePrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 24.dp))
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        content()
        Spacer(Modifier.height(34.dp))
    }
}

@Composable private fun SettingsSectionLabel(text: String) { Text(text, color = OnHomeSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 6.dp)) }

@Composable
private fun SettingsNavRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    SettingsBaseRow(icon, title, subtitle, enabled = true, onClick = onClick) { Text("›", color = OnHomeSecondary, fontSize = 28.sp) }
}

@Composable
private fun SettingsBaseRow(icon: ImageVector, title: String, subtitle: String?, enabled: Boolean, onClick: () -> Unit, trailing: @Composable () -> Unit) {
    val alpha = if (enabled) 1f else 0.48f
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 24.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(Color.White.copy(alpha = 0.07f * alpha), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = OnHomePrimary.copy(alpha = alpha), modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f).padding(start = 16.dp, end = 10.dp)) {
            Text(title, color = OnHomePrimary.copy(alpha = alpha), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, color = OnHomeSecondary.copy(alpha = alpha), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
        }
        trailing()
    }
}

@Composable
private fun ToggleSetting(title: String, subtitle: String?, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 14.dp)) {
            Text(title, color = OnHomePrimary.copy(alpha = if (enabled) 1f else 0.45f), fontSize = 17.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(subtitle, color = OnHomeSecondary.copy(alpha = if (enabled) 1f else 0.45f), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RadioSetting(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = OnHomePrimary, fontSize = 17.sp, modifier = Modifier.weight(1f))
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun StepperSetting(title: String, value: Int, min: Int, max: Int, step: Int = 1, suffix: String = "", enabled: Boolean = true, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = OnHomePrimary.copy(alpha = if (enabled) 1f else 0.45f), fontSize = 17.sp, modifier = Modifier.weight(1f))
        TextButton(enabled = enabled && value > min, onClick = { onChange((value - step).coerceAtLeast(min)) }) { Text("−") }
        Text("$value$suffix", color = OnHomePrimary.copy(alpha = if (enabled) 1f else 0.45f), fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
        TextButton(enabled = enabled && value < max, onClick = { onChange((value + step).coerceAtMost(max)) }) { Text("+") }
    }
}

@Composable
private fun ColorSwatchSetting(
    title: String,
    selected: Int,
    palette: List<Int>,
    onSelect: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp)) {
        Text(title, color = OnHomePrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            palette.forEach { argb ->
                val isSelected = argb == selected
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(argb))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) OnHomePrimary else Color.White.copy(alpha = 0.20f),
                            shape = CircleShape,
                        )
                        .clickable { onSelect(argb) },
                )
            }
        }
    }
}

private fun drawerBackgroundPalette(): List<Int> = listOf(
    0xFF1B0D24.toInt(),
    0xFF101318.toInt(),
    0xFF000000.toInt(),
    0xFF1C1C1E.toInt(),
    0xFFFFFFFF.toInt(),
    0xFF102A43.toInt(),
    0xFF0F3D2E.toInt(),
    0xFF3D0F1A.toInt(),
)

@Composable
private fun SettingsTextRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = OnHomePrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(value, color = OnHomeSecondary, fontSize = 14.sp)
    }
}

private fun sectionTitle(section: LauncherSettingsSection?): String = when (section) {
    null -> "设置"
    LauncherSettingsSection.Home -> "主页"
    LauncherSettingsSection.HomeGrid -> "主页网格"
    LauncherSettingsSection.HomeIconAppearance -> "主页图标外观"
    LauncherSettingsSection.Clock -> "时钟"
    LauncherSettingsSection.Apps -> "应用网格"
    LauncherSettingsSection.DrawerGrid -> "网格属性"
    LauncherSettingsSection.DrawerIconAppearance -> "图标外观"
    LauncherSettingsSection.Search -> "搜索页面"
    LauncherSettingsSection.SearchEngine -> "搜索提供者"
    LauncherSettingsSection.Gestures -> "手势与热键"
    LauncherSettingsSection.HotKeys -> "热键"
    LauncherSettingsSection.SingleFinger -> "单指手势"
    LauncherSettingsSection.DoubleFinger -> "双指手势"
    LauncherSettingsSection.SmartDisplay -> "Smart Display Off"
    LauncherSettingsSection.Appearance -> "全局外观"
    LauncherSettingsSection.EditThemeColors -> "修改主题颜色"
    LauncherSettingsSection.DarkMode -> "深色模式"
    LauncherSettingsSection.FontPicker -> "字体"
    LauncherSettingsSection.Wallpaper -> "壁纸"
    LauncherSettingsSection.Notifications -> "通知"
    LauncherSettingsSection.Folders -> "文件夹"
    LauncherSettingsSection.FolderIcons -> "文件夹图标"
    LauncherSettingsSection.Widgets -> "小部件"
    LauncherSettingsSection.Dock -> "Dock"
    LauncherSettingsSection.CategoryBar -> "分类栏"
    LauncherSettingsSection.Screen -> "屏幕"
    LauncherSettingsSection.Security -> "安全与隐私"
    LauncherSettingsSection.HiddenApps -> "隐藏应用"
    LauncherSettingsSection.Backup -> "备份与还原"
    LauncherSettingsSection.PageManager -> "页面管理"
    LauncherSettingsSection.MultiDisplay -> "多显示设置"
    LauncherSettingsSection.Feed -> "Your Feed"
    LauncherSettingsSection.WebPanel -> "Web 页面"
    LauncherSettingsSection.GooglePanel -> "Google / Discover 面板"
    LauncherSettingsSection.Troubleshooting -> "故障排除"
    LauncherSettingsSection.Experimental -> "实验选项"
    LauncherSettingsSection.Developer -> "开发者选项"
    LauncherSettingsSection.Premium -> "Premium 功能"
    LauncherSettingsSection.About -> "版本信息"
}

@Suppress("DEPRECATION")
private fun Context.appVersionLabel(): String = runCatching {
    val info = if (android.os.Build.VERSION.SDK_INT >= 33) packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L)) else packageManager.getPackageInfo(packageName, 0)
    info.versionName ?: "1.0"
}.getOrDefault("1.0")
