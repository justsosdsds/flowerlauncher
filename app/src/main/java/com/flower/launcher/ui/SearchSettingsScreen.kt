package com.flower.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shortcut
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flower.launcher.apps.InstalledApp
import com.flower.launcher.search.SearchPreferences
import com.flower.launcher.search.SearchTarget
import com.flower.launcher.search.SearchTargetGroup
import com.flower.launcher.search.SearchTargetPresets
import com.flower.launcher.search.SearchTargetPreset
import com.flower.launcher.search.layoutSearchTargets
import com.flower.launcher.ui.theme.OnHomePrimary
import com.flower.launcher.ui.theme.OnHomeSecondary
import java.util.UUID
import kotlin.math.roundToInt

private sealed interface SearchSettingsPage {
    data object Root : SearchSettingsPage
    data object Targets : SearchSettingsPage
    data object Interaction : SearchSettingsPage
    data object Appearance : SearchSettingsPage
    data object Blacklist : SearchSettingsPage
    data object AddTarget : SearchSettingsPage
    data object AddGroup : SearchSettingsPage
    data class EditTarget(val id: String) : SearchSettingsPage
    data class PickApp(val targetId: String) : SearchSettingsPage
    data class PickGroup(val targetId: String) : SearchSettingsPage
}

@Composable
fun SearchSettingsScreen(
    preferences: SearchPreferences,
    targets: List<SearchTarget>,
    groups: List<SearchTargetGroup>,
    apps: List<InstalledApp>,
    onBack: () -> Unit,
    onChange: (SearchPreferences) -> Unit,
    onTargetsChange: (List<SearchTarget>) -> Unit,
    onGroupsChange: (List<SearchTargetGroup>) -> Unit,
    blockedComponentKeys: Set<String> = emptySet(),
    onSetAppBlocked: (InstalledApp, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var page by remember { mutableStateOf<SearchSettingsPage>(SearchSettingsPage.Root) }

    fun replaceTarget(updated: SearchTarget) {
        onTargetsChange(targets.map { if (it.id == updated.id) updated else it })
    }

    fun deleteTarget(id: String) {
        onTargetsChange(targets.filterNot { it.id == id })
    }

    fun renameGroup(group: SearchTargetGroup, label: String) {
        onGroupsChange(groups.map { if (it.id == group.id) it.copy(label = label) else it })
    }

    // Deleting a group only ungroups its members; the targets themselves are kept.
    fun deleteGroup(group: SearchTargetGroup) {
        onTargetsChange(targets.map { if (it.groupId == group.id) it.copy(groupId = null) else it })
        onGroupsChange(groups.filterNot { it.id == group.id })
    }

    fun goBackPage() {
        page = when (val current = page) {
            SearchSettingsPage.Root -> SearchSettingsPage.Root
            SearchSettingsPage.Targets -> SearchSettingsPage.Root
            SearchSettingsPage.Interaction -> SearchSettingsPage.Root
            SearchSettingsPage.Appearance -> SearchSettingsPage.Root
            SearchSettingsPage.Blacklist -> SearchSettingsPage.Root
            SearchSettingsPage.AddTarget -> SearchSettingsPage.Targets
            SearchSettingsPage.AddGroup -> SearchSettingsPage.Targets
            is SearchSettingsPage.EditTarget -> SearchSettingsPage.Targets
            is SearchSettingsPage.PickApp -> SearchSettingsPage.EditTarget(current.targetId)
            is SearchSettingsPage.PickGroup -> SearchSettingsPage.EditTarget(current.targetId)
        }
    }

    BackHandler(enabled = page != SearchSettingsPage.Root) { goBackPage() }

    when (val current = page) {
        SearchSettingsPage.Root -> SearchSettingsRoot(
            preferences = preferences,
            targets = targets,
            onChange = onChange,
            onOpenTargets = { page = SearchSettingsPage.Targets },
            onOpenInteraction = { page = SearchSettingsPage.Interaction },
            onOpenAppearance = { page = SearchSettingsPage.Appearance },
            onOpenBlacklist = { page = SearchSettingsPage.Blacklist },
            blockedAppCount = blockedComponentKeys.size,
            onBack = onBack,
            modifier = modifier,
        )

        SearchSettingsPage.Blacklist -> BlacklistPage(
            apps = apps,
            blockedComponentKeys = blockedComponentKeys,
            onSetAppBlocked = onSetAppBlocked,
            onBack = { page = SearchSettingsPage.Root },
            modifier = modifier,
        )

        SearchSettingsPage.Interaction -> SearchInteractionSettingsPage(
            preferences = preferences,
            onChange = onChange,
            onBack = { page = SearchSettingsPage.Root },
            modifier = modifier,
        )

        SearchSettingsPage.Appearance -> SearchAppearanceSettingsPage(
            preferences = preferences,
            onChange = onChange,
            onBack = { page = SearchSettingsPage.Root },
            modifier = modifier,
        )

        SearchSettingsPage.Targets -> TargetsPage(
            targets = targets,
            groups = groups,
            apps = apps,
            onBack = { page = SearchSettingsPage.Root },
            onAdd = { page = SearchSettingsPage.AddTarget },
            onAddGroup = { page = SearchSettingsPage.AddGroup },
            onToggle = { target, enabled -> replaceTarget(target.copy(enabled = enabled)) },
            onEdit = { target -> page = SearchSettingsPage.EditTarget(target.id) },
            onRenameGroup = ::renameGroup,
            onDeleteGroup = ::deleteGroup,
            modifier = modifier,
        )

        SearchSettingsPage.AddTarget -> AddTargetPage(
            onBack = { page = SearchSettingsPage.Targets },
            onCreate = { preset ->
                val target = SearchTargetPresets.newTarget(
                    label = preset.label,
                    primaryUri = preset.primaryUri,
                    fallbackUri = preset.fallbackUri,
                )
                onTargetsChange(targets + target)
                page = SearchSettingsPage.EditTarget(target.id)
            },
            modifier = modifier,
        )

        SearchSettingsPage.AddGroup -> AddGroupPage(
            onBack = { page = SearchSettingsPage.Targets },
            onCreate = { label ->
                val group = SearchTargetGroup(
                    id = UUID.randomUUID().toString(),
                    label = label,
                    order = (groups.maxOfOrNull { it.order } ?: -1) + 1,
                )
                onGroupsChange(groups + group)
                page = SearchSettingsPage.Targets
            },
            modifier = modifier,
        )

        is SearchSettingsPage.EditTarget -> {
            val target = targets.firstOrNull { it.id == current.id }
            if (target == null) {
                page = SearchSettingsPage.Targets
            } else {
                EditTargetPage(
                    target = target,
                    appLabel = apps.firstOrNull { it.packageName == target.packageName }?.label,
                    groupLabel = groups.firstOrNull { it.id == target.groupId }?.label,
                    duplicatePrefix = !target.prefix.isNullOrBlank() && targets.any { other ->
                        other.id != target.id && other.prefix.equals(target.prefix, ignoreCase = true)
                    },
                    onChange = ::replaceTarget,
                    onBack = { page = SearchSettingsPage.Targets },
                    onPickApp = { page = SearchSettingsPage.PickApp(target.id) },
                    onPickGroup = { page = SearchSettingsPage.PickGroup(target.id) },
                    onDelete = {
                        deleteTarget(target.id)
                        page = SearchSettingsPage.Targets
                    },
                    modifier = modifier,
                )
            }
        }

        is SearchSettingsPage.PickApp -> {
            val target = targets.firstOrNull { it.id == current.targetId }
            if (target == null) {
                page = SearchSettingsPage.Targets
            } else {
                AppPickerPage(
                    apps = apps,
                    selectedPackage = target.packageName,
                    onBack = { page = SearchSettingsPage.EditTarget(target.id) },
                    onSelect = { pkg ->
                        replaceTarget(target.copy(packageName = pkg))
                        page = SearchSettingsPage.EditTarget(target.id)
                    },
                    modifier = modifier,
                )
            }
        }

        is SearchSettingsPage.PickGroup -> {
            val target = targets.firstOrNull { it.id == current.targetId }
            if (target == null) {
                page = SearchSettingsPage.Targets
            } else {
                GroupPickerPage(
                    groups = groups,
                    selectedGroupId = target.groupId,
                    onBack = { page = SearchSettingsPage.EditTarget(target.id) },
                    onSelect = { groupId ->
                        replaceTarget(target.copy(groupId = groupId))
                        page = SearchSettingsPage.EditTarget(target.id)
                    },
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun SearchSettingsRoot(
    preferences: SearchPreferences,
    targets: List<SearchTarget>,
    onChange: (SearchPreferences) -> Unit,
    onOpenTargets: () -> Unit,
    onOpenInteraction: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenBlacklist: () -> Unit,
    blockedAppCount: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF232428))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsHeader(title = "搜索页面", onBack = onBack)

        Divider(color = Color.White.copy(alpha = 0.10f))

        ToggleRow(
            icon = Icons.Default.Visibility,
            title = "在搜索结果中显示隐藏的应用",
            checked = preferences.showHiddenApps,
            onCheckedChange = { onChange(preferences.copy(showHiddenApps = it)) },
        )

        SettingRow(
            icon = Icons.Default.Storefront,
            title = "搜索目标",
            subtitle = targets.filter { it.enabled }.joinToString("、") { it.label }.ifBlank { "无" },
            onClick = onOpenTargets,
            trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnHomeSecondary) },
        )

        SettingRow(
            icon = Icons.Default.Block,
            title = "应用黑名单",
            subtitle = if (blockedAppCount == 0) {
                "黑名单中的应用不会出现在搜索和应用抽屉"
            } else {
                "$blockedAppCount 个应用不会出现在搜索和应用抽屉"
            },
            onClick = onOpenBlacklist,
            trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnHomeSecondary) },
        )

        SectionTitle("搜索启动页")
        StepperRow(
            title = "应用",
            value = preferences.launchAppsCount,
            onMinus = { onChange(preferences.copy(launchAppsCount = (preferences.launchAppsCount - 1).coerceAtLeast(0))) },
            onPlus = { onChange(preferences.copy(launchAppsCount = (preferences.launchAppsCount + 1).coerceAtMost(12))) },
        )

        SettingRow(
            icon = Icons.Default.Settings,
            title = "交互设置",
            subtitle = "输入法阈值、回弹触发与振动反馈",
            onClick = onOpenInteraction,
            trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnHomeSecondary) },
        )

        SettingRow(
            icon = Icons.Default.Palette,
            title = "外观设置",
            subtitle = "底部浮动搜索框的高度、圆角、颜色与透明度",
            onClick = onOpenAppearance,
            trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnHomeSecondary) },
        )

        SectionTitle("搜索结果")
        ToggleRow(Icons.Default.Settings, "设置", null, preferences.settingsEnabled) {
            onChange(preferences.copy(settingsEnabled = it))
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SearchInteractionSettingsPage(
    preferences: SearchPreferences,
    onChange: (SearchPreferences) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF232428))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsHeader(title = "交互设置", onBack = onBack)
        Divider(color = Color.White.copy(alpha = 0.10f))

        SectionTitle("输入法手势")
        StepperRow(
            title = "隐藏输入法内容溢出（dp）",
            value = preferences.imeCollapseOverflowThresholdDp,
            onMinus = {
                onChange(
                    preferences.copy(
                        imeCollapseOverflowThresholdDp =
                            (preferences.imeCollapseOverflowThresholdDp - 10).coerceAtLeast(0),
                    ),
                )
            },
            onPlus = {
                onChange(
                    preferences.copy(
                        imeCollapseOverflowThresholdDp =
                            (preferences.imeCollapseOverflowThresholdDp + 10).coerceAtMost(300),
                    ),
                )
            },
        )
        StepperRow(
            title = "重新显示输入法下拉距离（dp）",
            value = preferences.imeRevealPullThresholdDp,
            onMinus = {
                onChange(
                    preferences.copy(
                        imeRevealPullThresholdDp =
                            (preferences.imeRevealPullThresholdDp - 10).coerceAtLeast(80),
                    ),
                )
            },
            onPlus = {
                onChange(
                    preferences.copy(
                        imeRevealPullThresholdDp =
                            (preferences.imeRevealPullThresholdDp + 10).coerceAtMost(360),
                    ),
                )
            },
        )
        SectionTitle("页面返回")
        StepperRow(
            title = "返回主页上拉距离（dp）",
            value = preferences.homeReturnPullThresholdDp,
            onMinus = {
                onChange(
                    preferences.copy(
                        homeReturnPullThresholdDp =
                            (preferences.homeReturnPullThresholdDp - 10).coerceAtLeast(80),
                    ),
                )
            },
            onPlus = {
                onChange(
                    preferences.copy(
                        homeReturnPullThresholdDp =
                            (preferences.homeReturnPullThresholdDp + 10).coerceAtMost(360),
                    ),
                )
            },
        )
        ToggleRow(
            icon = Icons.Default.Settings,
            title = "达到返回阈值时振动",
            subtitle = "首次越过返回主页上拉阈值时反馈一次",
            checked = preferences.homeReturnHapticEnabled,
            onCheckedChange = { onChange(preferences.copy(homeReturnHapticEnabled = it)) },
        )
        if (preferences.homeReturnHapticEnabled) {
            StepperRow(
                title = "返回振动强度（%）",
                value = preferences.homeReturnHapticStrengthPercent,
                onMinus = {
                    onChange(
                        preferences.copy(
                            homeReturnHapticStrengthPercent =
                                (preferences.homeReturnHapticStrengthPercent - 10).coerceAtLeast(10),
                        ),
                    )
                },
                onPlus = {
                    onChange(
                        preferences.copy(
                            homeReturnHapticStrengthPercent =
                                (preferences.homeReturnHapticStrengthPercent + 10).coerceAtMost(100),
                        ),
                    )
                },
            )
        }

        SectionTitle("输入法阈值振动")
        ToggleRow(
            icon = Icons.Default.Settings,
            title = "达到下拉阈值时振动",
            subtitle = "首次越过重新显示输入法阈值时反馈一次",
            checked = preferences.imeRevealHapticEnabled,
            onCheckedChange = { onChange(preferences.copy(imeRevealHapticEnabled = it)) },
        )
        if (preferences.imeRevealHapticEnabled) {
            StepperRow(
                title = "振动强度（%）",
                value = preferences.imeRevealHapticStrengthPercent,
                onMinus = {
                    onChange(
                        preferences.copy(
                            imeRevealHapticStrengthPercent =
                                (preferences.imeRevealHapticStrengthPercent - 10).coerceAtLeast(10),
                        ),
                    )
                },
                onPlus = {
                    onChange(
                        preferences.copy(
                            imeRevealHapticStrengthPercent =
                                (preferences.imeRevealHapticStrengthPercent + 10).coerceAtMost(100),
                        ),
                    )
                },
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SearchAppearanceSettingsPage(
    preferences: SearchPreferences,
    onChange: (SearchPreferences) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF232428))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsHeader(title = "外观设置", onBack = onBack)
        Divider(color = Color.White.copy(alpha = 0.10f))

        SectionTitle("搜索栏外观")
        StepperRow(
            title = "搜索栏高度（dp）",
            value = preferences.searchFieldHeightDp,
            onMinus = {
                onChange(
                    preferences.copy(
                        searchFieldHeightDp = (preferences.searchFieldHeightDp - 2).coerceAtLeast(36),
                    ),
                )
            },
            onPlus = {
                onChange(
                    preferences.copy(
                        searchFieldHeightDp = (preferences.searchFieldHeightDp + 2).coerceAtMost(64),
                    ),
                )
            },
        )
        StepperRow(
            title = "搜索栏圆角（dp）",
            value = preferences.searchFieldCornerRadiusDp,
            onMinus = {
                onChange(
                    preferences.copy(
                        searchFieldCornerRadiusDp = (preferences.searchFieldCornerRadiusDp - 2).coerceAtLeast(8),
                    ),
                )
            },
            onPlus = {
                onChange(
                    preferences.copy(
                        searchFieldCornerRadiusDp = (preferences.searchFieldCornerRadiusDp + 2).coerceAtMost(32),
                    ),
                )
            },
        )
        SectionTitle("搜索栏颜色")
        SearchBarColorPicker(
            selectedId = preferences.searchBarColorPresetId,
            depthPercent = preferences.searchBarColorDepth,
            opacityPercent = preferences.searchBarOpacityPercent,
            onSelect = { presetId ->
                onChange(preferences.copy(searchBarColorPresetId = presetId))
            },
        )
        SliderRow(
            title = "搜索栏颜色深度",
            value = preferences.searchBarColorDepth,
            valueRange = 0..100,
            valueLabel = "${preferences.searchBarColorDepth}%",
        ) { depth ->
            onChange(preferences.copy(searchBarColorDepth = depth))
        }
        SliderRow(
            title = "搜索栏不透明度",
            value = preferences.searchBarOpacityPercent,
            valueRange = 0..100,
            valueLabel = "${preferences.searchBarOpacityPercent}%",
            startLabel = "透明",
            endLabel = "不透明",
        ) { opacity ->
            onChange(preferences.copy(searchBarOpacityPercent = opacity))
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SearchBarColorPicker(
    selectedId: String,
    depthPercent: Int,
    opacityPercent: Int,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SearchBarColorPresets.chunked(4).forEach { rowPresets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                rowPresets.forEach { preset ->
                    val selected = preset.id == selectedId
                    val preview = searchBarColorFor(
                        presetId = preset.id,
                        depthPercent = depthPercent,
                        opacityPercent = opacityPercent.coerceAtLeast(18),
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(64.dp)
                            .clickable { onSelect(preset.id) },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(preview)
                                .then(
                                    if (selected) {
                                        Modifier.border(2.dp, OnHomePrimary, CircleShape)
                                    } else {
                                        Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    },
                                ),
                        )
                        Text(
                            text = preset.label,
                            color = if (selected) OnHomePrimary else OnHomeSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlacklistPage(
    apps: List<InstalledApp>,
    blockedComponentKeys: Set<String>,
    onSetAppBlocked: (InstalledApp, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf("") }
    val sortedApps = remember(apps) { apps.sortedBy { it.label.lowercase() } }
    val filtered = remember(sortedApps, filter) {
        val needle = filter.trim().lowercase()
        if (needle.isEmpty()) {
            sortedApps
        } else {
            sortedApps.filter {
                it.label.lowercase().contains(needle) || it.packageName.lowercase().contains(needle)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF232428))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        SettingsHeader(title = "应用黑名单", onBack = onBack)
        Divider(color = Color.White.copy(alpha = 0.10f))

        Text(
            text = "黑名单中的应用不会出现在搜索结果和应用抽屉中。",
            color = OnHomeSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        )

        OutlinedTextField(
            value = filter,
            onValueChange = { filter = it },
            label = { Text("搜索应用") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filtered, key = { it.componentKey }) { app ->
                val blocked = app.componentKey in blockedComponentKeys
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetAppBlocked(app, !blocked) }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val icon = app.icon
                    if (icon != null) {
                        Image(
                            bitmap = icon,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF34353A), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(app.label.take(1), color = OnHomePrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(Modifier.weight(1f).padding(start = 16.dp)) {
                        Text(app.label, color = OnHomePrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(app.packageName, color = OnHomeSecondary, fontSize = 12.sp)
                    }
                    Switch(checked = blocked, onCheckedChange = { onSetAppBlocked(app, it) })
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun TargetsPage(
    targets: List<SearchTarget>,
    groups: List<SearchTargetGroup>,
    apps: List<InstalledApp>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onAddGroup: () -> Unit,
    onToggle: (SearchTarget, Boolean) -> Unit,
    onEdit: (SearchTarget) -> Unit,
    onRenameGroup: (SearchTargetGroup, String) -> Unit,
    onDeleteGroup: (SearchTargetGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Settings expansion is temporary UI state; it is deliberately not persisted
    // and never leaks into the Search page's own expand state.
    var expandedGroups by remember { mutableStateOf(emptySet<String>()) }
    var renameTarget by remember { mutableStateOf<SearchTargetGroup?>(null) }
    var renameText by remember { mutableStateOf("") }

    val layout = remember(targets, groups) { layoutSearchTargets(targets, groups, enabledOnly = false) }

    fun appLabelFor(target: SearchTarget): String =
        target.packageName?.let { pkg -> apps.firstOrNull { it.packageName == pkg }?.label } ?: "系统默认"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF232428))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        SettingsHeader(title = "搜索目标", onBack = onBack)
        Divider(color = Color.White.copy(alpha = 0.10f))

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (targets.isEmpty() && groups.isEmpty()) {
                item {
                    Text(
                        text = "还没有搜索目标，点下方“添加目标”。",
                        color = OnHomeSecondary,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(26.dp),
                    )
                }
            }

            items(layout.ungrouped, key = { it.id }) { target ->
                TargetSettingsRow(
                    target = target,
                    appLabel = appLabelFor(target),
                    onClick = { onEdit(target) },
                    onToggle = { enabled -> onToggle(target, enabled) },
                )
            }

            layout.groups.forEach { members ->
                val group = members.group
                val expanded = group.id in expandedGroups
                item(key = "group-${group.id}") {
                    GroupHeaderRow(
                        group = group,
                        memberCount = members.targets.size,
                        expanded = expanded,
                        onToggle = {
                            expandedGroups = if (expanded) {
                                expandedGroups - group.id
                            } else {
                                expandedGroups + group.id
                            }
                        },
                        onRename = {
                            renameTarget = group
                            renameText = group.label
                        },
                        onDelete = { onDeleteGroup(group) },
                    )
                }
                if (expanded) {
                    if (members.targets.isEmpty()) {
                        item(key = "group-${group.id}-empty") {
                            Text(
                                text = "该分类还没有目标",
                                color = OnHomeSecondary.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(start = 52.dp, top = 2.dp, bottom = 12.dp),
                            )
                        }
                    } else {
                        items(members.targets, key = { it.id }) { target ->
                            TargetSettingsRow(
                                target = target,
                                appLabel = appLabelFor(target),
                                onClick = { onEdit(target) },
                                onToggle = { enabled -> onToggle(target, enabled) },
                                indented = true,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }

        AddActionRow(icon = Icons.Default.Add, text = "添加目标", onClick = onAdd)
        AddActionRow(icon = Icons.Default.CreateNewFolder, text = "新建分类", onClick = onAddGroup)
    }

    val renaming = renameTarget
    if (renaming != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名分类") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("取消") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = renameText.trim()
                        if (name.isNotEmpty()) onRenameGroup(renaming, name)
                        renameTarget = null
                    },
                ) { Text("保存") }
            },
        )
    }
}

@Composable
private fun TargetSettingsRow(
    target: SearchTarget,
    appLabel: String,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    indented: Boolean = false,
) {
    val subtitle = buildString {
        append(appLabel)
        target.prefix?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = if (indented) 46.dp else 24.dp, end = 24.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = target.label.ifBlank { "(未命名)" },
                color = if (target.enabled) OnHomePrimary else OnHomeSecondary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(subtitle, color = OnHomeSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(checked = target.enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun GroupHeaderRow(
    group: SearchTargetGroup,
    memberCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "settingsGroupChevron",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = 24.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = OnHomePrimary, modifier = Modifier.size(21.dp))
        }
        Column(Modifier.padding(start = 14.dp, end = 8.dp).weight(1f)) {
            Text(group.label.ifBlank { "(未命名分类)" }, color = OnHomePrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (memberCount == 0) "空分类" else "$memberCount 个目标",
                color = OnHomeSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "分类操作",
                tint = OnHomeSecondary,
                modifier = Modifier
                    .size(34.dp)
                    .clickable { showMenu = true }
                    .padding(6.dp),
            )
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = Color(0xF20B0B0D),
                shape = RoundedCornerShape(18.dp),
            ) {
                DropdownMenuItem(
                    text = { Text("重命名分类", color = OnHomePrimary) },
                    onClick = {
                        showMenu = false
                        onRename()
                    },
                )
                DropdownMenuItem(
                    text = { Text("删除分类", color = Color(0xFFFF6B6B)) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = OnHomeSecondary.copy(alpha = 0.65f),
            modifier = Modifier.size(20.dp).rotate(rotation),
        )
    }
}

@Composable
private fun AddActionRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF5AA7FF), modifier = Modifier.size(22.dp))
        Text(text, color = Color(0xFF5AA7FF), fontSize = 17.sp, modifier = Modifier.padding(start = 14.dp))
    }
}

@Composable
private fun AddGroupPage(
    onBack: () -> Unit,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF232428))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        SettingsHeader(title = "新建分类", onBack = onBack)
        Divider(color = Color.White.copy(alpha = 0.10f))
        Column(Modifier.padding(horizontal = 26.dp, vertical = 18.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("分类名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotEmpty()) onCreate(trimmed)
                },
                enabled = name.isNotBlank(),
            ) { Text("完成") }
        }
    }
}

@Composable
private fun GroupPickerPage(
    groups: List<SearchTargetGroup>,
    selectedGroupId: String?,
    onBack: () -> Unit,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF232428))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        SettingsHeader(title = "所属分类", onBack = onBack)
        Divider(color = Color.White.copy(alpha = 0.10f))
        LazyColumn(modifier = Modifier.weight(1f)) {
            item(key = "none") {
                GroupPickerRow(
                    label = "无分类",
                    selected = selectedGroupId == null,
                    onClick = { onSelect(null) },
                )
            }
            items(groups.sortedWith(compareBy({ it.order }, { it.label })), key = { it.id }) { group ->
                GroupPickerRow(
                    label = group.label.ifBlank { "(未命名分类)" },
                    selected = group.id == selectedGroupId,
                    onClick = { onSelect(group.id) },
                )
            }
        }
    }
}

@Composable
private fun GroupPickerRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = OnHomePrimary, fontSize = 17.sp, modifier = Modifier.weight(1f))
        if (selected) {
            Text("已选", color = Color(0xFF5AA7FF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private const val CUSTOM_PRESET_LABEL = "自定义"

@Composable
private fun AddTargetPage(
    onBack: () -> Unit,
    onCreate: (SearchTargetPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF232428))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        SettingsHeader(title = "添加目标", onBack = onBack)
        Divider(color = Color.White.copy(alpha = 0.10f))
        Column(Modifier.verticalScroll(rememberScrollState())) {
            SearchTargetPresets.all.forEach { preset ->
                SettingRow(
                    icon = iconForUri(preset.primaryUri),
                    title = preset.label,
                    subtitle = preset.primaryUri,
                    onClick = { onCreate(preset) },
                )
            }
            SettingRow(
                icon = Icons.Default.Add,
                title = CUSTOM_PRESET_LABEL,
                subtitle = "自己填写 URI 模板",
                onClick = {
                    onCreate(
                        SearchTargetPreset(
                            label = CUSTOM_PRESET_LABEL,
                            primaryUri = "https://www.google.com/search?q={q}",
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun EditTargetPage(
    target: SearchTarget,
    appLabel: String?,
    groupLabel: String?,
    duplicatePrefix: Boolean,
    onChange: (SearchTarget) -> Unit,
    onBack: () -> Unit,
    onPickApp: () -> Unit,
    onPickGroup: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF232428))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsHeader(title = "编辑目标", onBack = onBack)
        Divider(color = Color.White.copy(alpha = 0.10f))

        Column(Modifier.padding(horizontal = 26.dp, vertical = 18.dp)) {
            OutlinedTextField(
                value = target.label,
                onValueChange = { onChange(target.copy(label = it)) },
                label = { Text("名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = target.actionTemplate,
                onValueChange = { onChange(target.copy(actionTemplate = it)) },
                label = { Text("显示文本") },
                supportingText = { Text("用 {name} 代表目标名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = target.primaryUri,
                onValueChange = { onChange(target.copy(primaryUri = it)) },
                label = { Text("主 URI 模板") },
                supportingText = {
                    Text(if (target.primaryUri.contains(SearchTarget.QUERY_PLACEHOLDER)) "用 {q} 代表搜索词" else "必须包含 {q}")
                },
                isError = !target.primaryUri.contains(SearchTarget.QUERY_PLACEHOLDER),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = target.fallbackUri ?: "",
                onValueChange = { onChange(target.copy(fallbackUri = it.ifBlank { null })) },
                label = { Text("回退 URI 模板（可选）") },
                supportingText = { Text("主 URI 打不开时使用，用 {q}") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(6.dp))
            val prefix = target.prefix ?: ""
            val prefixInvalid = prefix.isNotBlank() && !prefix.startsWith("/")
            OutlinedTextField(
                value = prefix,
                onValueChange = { onChange(target.copy(prefix = it.ifBlank { null })) },
                label = { Text("前缀（可选）") },
                supportingText = {
                    Text(
                        when {
                            prefixInvalid -> "必须以 / 开头"
                            duplicatePrefix -> "该前缀已被其它目标使用"
                            else -> "例如 /gd；输入 “/gd 关键词” 回车即用本目标搜索"
                        },
                    )
                },
                isError = prefixInvalid || duplicatePrefix,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(6.dp))
            SettingRow(
                icon = Icons.Default.PlayArrow,
                title = "打开方式",
                subtitle = appLabel ?: "系统默认",
                onClick = onPickApp,
                trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnHomeSecondary) },
                horizontalPadding = 0.dp,
            )

            SettingRow(
                icon = Icons.Default.CreateNewFolder,
                title = "所属分类",
                subtitle = groupLabel ?: "无分类",
                onClick = onPickGroup,
                trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnHomeSecondary) },
                horizontalPadding = 0.dp,
            )

            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("启用", color = OnHomePrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Switch(checked = target.enabled, onCheckedChange = { onChange(target.copy(enabled = it)) })
            }

            Spacer(Modifier.height(20.dp))
            TextButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFFF6B6B))
                Text("删除目标", color = Color(0xFFFF6B6B), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun AppPickerPage(
    apps: List<InstalledApp>,
    selectedPackage: String?,
    onBack: () -> Unit,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf("") }
    val filtered = remember(apps, filter) {
        val needle = filter.trim().lowercase()
        val list = if (needle.isEmpty()) apps else apps.filter {
            it.label.lowercase().contains(needle) || it.packageName.lowercase().contains(needle)
        }
        list.sortedBy { it.label.lowercase() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF232428))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        SettingsHeader(title = "选择应用", onBack = onBack)
        Divider(color = Color.White.copy(alpha = 0.10f))

        OutlinedTextField(
            value = filter,
            onValueChange = { filter = it },
            label = { Text("搜索应用") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 12.dp),
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                AppPickerRow(
                    label = "系统默认（不限）",
                    packageName = null,
                    icon = null,
                    selected = selectedPackage == null,
                    onClick = { onSelect(null) },
                )
            }

            var lastLetter: Char? = null
            filtered.forEach { app ->
                val letter = app.label.uppercase().firstOrNull() ?: '#'
                if (filter.trim().isEmpty() && letter != lastLetter) {
                    lastLetter = letter
                    item(key = "header-$letter") {
                        Text(
                            text = letter.toString(),
                            color = OnHomeSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 26.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                }
                item(key = app.componentKey) {
                    AppPickerRow(
                        label = app.label,
                        packageName = app.packageName,
                        icon = app.icon,
                        selected = app.packageName == selectedPackage,
                        onClick = { onSelect(app.packageName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppPickerRow(
    label: String,
    packageName: String?,
    icon: androidx.compose.ui.graphics.ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(34.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFF34353A), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Storefront, contentDescription = null, tint = OnHomeSecondary, modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.weight(1f).padding(start = 18.dp)) {
            Text(label, color = OnHomePrimary, fontSize = 17.sp)
            if (packageName != null) {
                Text(packageName, color = OnHomeSecondary, fontSize = 13.sp)
            }
        }
        if (selected) {
            Text("已选", color = Color(0xFF5AA7FF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "返回",
            tint = OnHomePrimary,
            modifier = Modifier
                .size(32.dp)
                .clickable(onClick = onBack),
        )
        Text(
            text = title,
            color = OnHomePrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp),
        )
    }
}

private fun iconForUri(uri: String): ImageVector = when {
    uri.startsWith("market://") -> Icons.Default.PlayArrow
    uri.startsWith("http") -> Icons.Default.Search
    else -> Icons.Default.Storefront
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = OnHomeSecondary,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 6.dp),
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    horizontalPadding: androidx.compose.ui.unit.Dp = 24.dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = horizontalPadding, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = OnHomePrimary, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.padding(start = 16.dp, end = 10.dp).weight(1f)) {
            Text(title, color = OnHomePrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, color = OnHomeSecondary, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
        }
        trailing?.invoke()
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingRow(icon = icon, title = title, subtitle = subtitle, trailing = {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    })
}

@Composable
private fun StepperRow(
    title: String,
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = OnHomePrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onMinus) { Text("−") }
        Text(value.toString(), color = OnHomePrimary, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
        TextButton(onClick = onPlus) { Text("+") }
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Int,
    valueRange: IntRange,
    valueLabel: String,
    startLabel: String = "浅",
    endLabel: String = "深",
    onChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = OnHomePrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(valueLabel, color = OnHomeSecondary, fontSize = 15.sp)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt().coerceIn(valueRange.first, valueRange.last)) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth()) {
            Text(startLabel, color = OnHomeSecondary.copy(alpha = 0.7f), fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text(endLabel, color = OnHomeSecondary.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}
