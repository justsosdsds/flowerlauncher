package com.flower.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flower.launcher.apps.AppDrawerCatalog
import com.flower.launcher.apps.InstalledApp
import com.flower.launcher.settings.LauncherPreferences
import com.flower.launcher.ui.theme.OnHomePrimary
import com.flower.launcher.ui.theme.OnHomeSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
internal fun AllAppsContent(
    catalog: AppDrawerCatalog,
    showHiddenOnly: Boolean,
    openSheetRequest: Int = 0,
    sortMode: DrawerSortMode,
    preferences: LauncherPreferences,
    gridState: LazyGridState,
    nativeGridScrollEnabled: Boolean = true,
    onSortModeChange: (DrawerSortMode) -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDrawerSettings: () -> Unit,
    onOpenFullAppsSettings: () -> Unit,
    onShowHiddenApps: () -> Unit,
    onShowAllApps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }
    LaunchedEffect(openSheetRequest) {
        if (openSheetRequest > 0) showSheet = true
    }
    var showSortDialog by remember { mutableStateOf(false) }
    var showPrivateSpaceDialog by remember { mutableStateOf(false) }

    // Other modes intentionally retain their previous source order.
    val displayed = if (sortMode == DrawerSortMode.Name) catalog.alphabetical else catalog.original
    val displayedApps = displayed.apps
    val indexLetters = displayed.letters
    val letterStarts = displayed.letterStarts
    val showIndexBar = indexLetters.isNotEmpty()
    val showLetterHeaders = sortMode == DrawerSortMode.Name && preferences.drawerShowLetterHeaders

    // In name mode with section headers on, the grid carries synthetic header rows,
    // so letter -> grid-index lookups must use the combined entry list.
    val gridEntries = remember(displayedApps, indexLetters, letterStarts, showLetterHeaders) {
        buildDrawerGridEntries(displayedApps, indexLetters, letterStarts, showLetterHeaders)
    }
    val entryStarts = remember(gridEntries, letterStarts, showLetterHeaders) {
        if (!showLetterHeaders) {
            letterStarts
        } else {
            buildMap {
                gridEntries.forEachIndexed { index, entry ->
                    if (entry is DrawerGridEntry.Header) put(entry.letter, index)
                }
            }
        }
    }

    var activeLetter by remember { mutableStateOf<String?>(null) }
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    fun jumpToLetter(letter: String) {
        val target = entryStarts[letter] ?: return
        scrollJob?.cancel()
        scrollJob = scope.launch { gridState.scrollToItem(target) }
    }

    val scrolledLetter by remember(gridEntries, indexLetters, letterStarts, showLetterHeaders) {
        derivedStateOf {
            if (indexLetters.isEmpty()) {
                null
            } else {
                val firstVisible = gridState.firstVisibleItemIndex
                var result = indexLetters.first()
                if (showLetterHeaders) {
                    gridEntries.forEachIndexed { index, entry ->
                        if (entry is DrawerGridEntry.Header && index <= firstVisible) result = entry.letter
                    }
                } else {
                    for (letter in indexLetters) {
                        if ((letterStarts[letter] ?: 0) <= firstVisible) result = letter else break
                    }
                }
                result
            }
        }
    }
    val highlightLetter = activeLetter ?: scrolledLetter

    fun onLetterActive(letter: String) {
        if (letter != activeLetter) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        activeLetter = letter
        jumpToLetter(letter)
    }

    val headerCollapsed = gridState.firstVisibleItemIndex > 0
    val headerHeight by animateDpAsState(
        targetValue = if (headerCollapsed) 0.dp else DRAWER_HEADER_HEIGHT,
        animationSpec = tween(durationMillis = DRAWER_HEADER_ANIM_MS),
        label = "drawerHeaderHeight",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .clipToBounds(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DRAWER_HEADER_HEIGHT)
                        .padding(start = 24.dp, end = 18.dp, top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (showHiddenOnly) "显示隐藏应用" else "所有应用",
                        color = OnHomePrimary,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "应用抽屉设置",
                        tint = OnHomePrimary,
                        modifier = Modifier.size(34.dp).clickable { showSheet = true }.padding(5.dp),
                    )
                }
            }

            if (displayedApps.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (showHiddenOnly) "您尚未隐藏任何图标\n要隐藏图标，请长按某个图标然后选择“隐藏”" else "没有应用",
                        color = OnHomeSecondary,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        lineHeight = 28.sp,
                    )
                }
            } else {
                val drawerShape = drawerIconShape(preferences)
                Box(modifier = Modifier.fillMaxSize()) {
                    // Symmetric padding keeps the grid visually centered; the index
                    // bar floats above the right edge instead of reserving a lane.
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(preferences.drawerColumns),
                        state = gridState,
                        userScrollEnabled = nativeGridScrollEnabled,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = DRAWER_GRID_H_PADDING, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(preferences.drawerVerticalSpacingDp.dp),
                    ) {
                        items(
                            items = gridEntries,
                            key = { it.key },
                            span = { entry ->
                                when (entry) {
                                    is DrawerGridEntry.Header -> GridItemSpan(maxLineSpan)
                                    is DrawerGridEntry.App -> GridItemSpan(1)
                                }
                            },
                        ) { entry ->
                            when (entry) {
                                is DrawerGridEntry.Header -> LetterSectionHeader(entry.letter)
                                is DrawerGridEntry.App -> DrawerAppTile(
                                    app = entry.app,
                                    iconSizeDp = preferences.drawerIconSizeDp,
                                    showLabel = preferences.drawerShowLabels,
                                    labelSizeSp = preferences.drawerLabelSizeSp,
                                    allowLabelWrap = preferences.drawerAllowLabelWrap,
                                    iconShape = drawerShape,
                                    onClick = { onLaunchApp(entry.app) },
                                )
                            }
                        }
                    }

                    if (showIndexBar) {
                        AlphabetIndexBar(
                            letters = indexLetters,
                            highlight = highlightLetter,
                            onLetterActive = ::onLetterActive,
                            onDragEnd = { activeLetter = null },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(vertical = 6.dp),
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = activeLetter != null,
                        enter = fadeIn(animationSpec = tween(120)),
                        exit = fadeOut(animationSpec = tween(DRAWER_BUBBLE_FADE_MS)),
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(end = 54.dp)
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xF20B0B0D)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = activeLetter.orEmpty(),
                                color = OnHomePrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }

        if (showSheet) {
            ScaledUiContent(preferences.uiScalePercent.coerceIn(70, 110) / 100f) {
                DrawerSettingsSheet(
                    onDismiss = { showSheet = false },
                    onSwipeUp = {
                        showSheet = false
                        onOpenFullAppsSettings()
                    },
                    onSort = {
                        showSheet = false
                        showSortDialog = true
                    },
                    onPrivateSpace = {
                        showSheet = false
                        showPrivateSpaceDialog = true
                    },
                    onHiddenApps = {
                        showSheet = false
                        if (showHiddenOnly) onShowAllApps() else onShowHiddenApps()
                    },
                    onGrid = {
                        showSheet = false
                        onOpenDrawerSettings()
                    },
                )
            }
        }
    }

    if (showSortDialog) {
        DrawerSortDialog(
            selected = sortMode,
            onDismiss = { showSortDialog = false },
            onSelect = {
                onSortModeChange(it)
                showSortDialog = false
            },
        )
    }

    if (showPrivateSpaceDialog) {
        AlertDialog(
            onDismissRequest = { showPrivateSpaceDialog = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null) },
            title = { Text("创建私密空间") },
            text = {
                Text("私密空间可让您安全地存储应用和数据，从主页和应用中隐藏。您需要先在设备设置中创建此空间，然后才能在 FlowerLauncher 中进行使用。")
            },
            confirmButton = {
                TextButton(onClick = { showPrivateSpaceDialog = false }) { Text("知道了") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerAppTile(
    app: InstalledApp,
    iconSizeDp: Int,
    showLabel: Boolean,
    labelSizeSp: Int,
    allowLabelWrap: Boolean,
    iconShape: Shape,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) DRAWER_TILE_PRESS_SCALE else 1f,
        animationSpec = tween(durationMillis = if (pressed) 80 else 120),
        label = "drawerTileScale",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val icon = app.icon
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = app.label,
                modifier = Modifier.size(iconSizeDp.dp).clip(iconShape),
            )
        } else {
            Box(
                modifier = Modifier.size(iconSizeDp.dp).clip(iconShape).background(Color(0x33FFFFFF)),
                contentAlignment = Alignment.Center,
            ) {
                Text(app.label.take(1), color = OnHomePrimary, fontSize = (labelSizeSp + 8).sp, fontWeight = FontWeight.Bold)
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Text(
                app.label,
                color = OnHomePrimary,
                fontSize = labelSizeSp.sp,
                maxLines = if (allowLabelWrap) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun drawerIconShape(preferences: LauncherPreferences): Shape {
    if (!preferences.drawerUseCustomIconAppearance) return RoundedCornerShape(18.dp)
    if (!preferences.drawerAdaptiveIcons) return RoundedCornerShape(18.dp)
    return when (preferences.drawerIconShape) {
        0 -> RectangleShape
        1 -> RoundedCornerShape(10.dp)
        2 -> RoundedCornerShape(18.dp)
        3 -> RoundedCornerShape(percent = 34)
        else -> CircleShape
    }
}

@Composable
private fun AlphabetIndexBar(
    letters: List<String>,
    highlight: String?,
    onLetterActive: (String) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val count = letters.size
        if (count == 0) return@BoxWithConstraints

        // The visible bar is intentionally plain: no pill background, a small slot
        // per letter, and a wider invisible touch strip so it stays easy to grab.
        val letterHeight = (maxHeight / count).coerceIn(13.dp, 18.dp)
        val barHeight = letterHeight * count

        Column(
            modifier = Modifier
                .width(DRAWER_INDEX_TOUCH_WIDTH)
                .height(barHeight)
                .pointerInput(letters, letterHeight) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        val slot = letterHeight.toPx()
                        fun indexAt(y: Float): Int = (y / slot).toInt().coerceIn(0, count - 1)

                        var index = indexAt(down.position.y)
                        onLetterActive(letters[index])

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            change.consume()
                            if (!change.pressed) break
                            val next = indexAt(change.position.y)
                            if (next != index) {
                                index = next
                                onLetterActive(letters[next])
                            }
                        }
                        onDragEnd()
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            letters.forEach { letter ->
                val isActive = letter == highlight
                Box(
                    modifier = Modifier.height(letterHeight).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(minOf(DRAWER_INDEX_ACTIVE_BUBBLE, letterHeight))
                            .clip(CircleShape)
                            .then(
                                if (isActive) {
                                    Modifier.background(OnHomePrimary.copy(alpha = 0.16f))
                                } else {
                                    Modifier
                                },
                            ),
                    )
                    Text(
                        text = letter,
                        color = OnHomePrimary.copy(alpha = if (isActive) 1f else 0.52f),
                        fontSize = (if (isActive) 11f else 10.5f).sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.graphicsLayer {
                            val activeScale = if (isActive) 1.15f else 1f
                            scaleX = activeScale
                            scaleY = activeScale
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LetterSectionHeader(letter: String) {
    Text(
        text = letter,
        color = OnHomeSecondary.copy(alpha = 0.5f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 4.dp),
    )
}

private sealed interface DrawerGridEntry {
    val key: String

    data class Header(val letter: String) : DrawerGridEntry {
        override val key: String get() = "h:$letter"
    }

    data class App(val app: InstalledApp) : DrawerGridEntry {
        override val key: String get() = "a:${app.componentKey}"
    }
}

private fun buildDrawerGridEntries(
    apps: List<InstalledApp>,
    letters: List<String>,
    letterStarts: Map<String, Int>,
    showHeaders: Boolean,
): List<DrawerGridEntry> {
    if (!showHeaders) return apps.map { DrawerGridEntry.App(it) }
    return buildList {
        var letterIndex = 0
        apps.forEachIndexed { appIndex, app ->
            if (letterIndex < letters.size && letterStarts[letters[letterIndex]] == appIndex) {
                add(DrawerGridEntry.Header(letters[letterIndex]))
                letterIndex++
            }
            add(DrawerGridEntry.App(app))
        }
    }
}

@Composable
private fun DrawerSettingsSheet(
    onDismiss: () -> Unit,
    onSwipeUp: () -> Unit,
    onSort: () -> Unit,
    onPrivateSpace: () -> Unit,
    onHiddenApps: () -> Unit,
    onGrid: () -> Unit,
) {
    var drag by remember { mutableFloatStateOf(0f) }
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0x99000000)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = {})
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { drag = 0f },
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            drag += amount
                        },
                        onDragEnd = {
                            if (drag < -72.dp.toPx()) onSwipeUp()
                            drag = 0f
                        },
                        onDragCancel = { drag = 0f },
                    )
                }
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFF25262B))
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 18.dp),
        ) {
            Box(
                Modifier.width(62.dp).height(5.dp).clip(RoundedCornerShape(99.dp)).background(Color(0x66FFFFFF)).align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("应用网格", color = OnHomePrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Default.Lock, null, tint = OnHomePrimary, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            DrawerSheetRow(Icons.Default.Apps, "启用自动应用排序", onSort)
            DrawerSheetRow(Icons.Default.Sort, "排序", onSort)
            DrawerSheetRow(Icons.Default.Lock, "创建私密空间", onPrivateSpace)
            DrawerSheetRow(Icons.Default.Visibility, "显示隐藏应用", onHiddenApps)
            DrawerSheetRow(Icons.Default.GridView, "应用网格", onGrid)
        }
    }
}

@Composable
private fun DrawerSheetAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Color(0xFF3E9BFF), modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, color = OnHomePrimary, fontSize = 15.sp)
    }
}

@Composable
private fun DrawerSheetRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = OnHomePrimary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(22.dp))
        Text(title, color = OnHomePrimary, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DrawerSortDialog(
    selected: DrawerSortMode,
    onDismiss: () -> Unit,
    onSelect: (DrawerSortMode) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("排序") },
        text = {
            Column {
                DrawerSortMode.entries.forEach { mode ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(mode) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mode == selected, onClick = { onSelect(mode) })
                        Spacer(Modifier.width(8.dp))
                        Text(mode.label, fontSize = 17.sp)
                    }
                }
            }
        },
        confirmButton = {},
    )
}

enum class DrawerSortMode(val label: String) {
    Name("按名称排序"),
    Usage("按使用频率排序"),
    InstallTime("以安装时间"),
    Manual("按照用户手动定制"),
    IconColor("按照图标颜色"),
}

// Drawer V2 visual constants.
private val DRAWER_HEADER_HEIGHT = 72.dp
private const val DRAWER_HEADER_ANIM_MS = 180
private val DRAWER_GRID_H_PADDING = 20.dp
private val DRAWER_INDEX_TOUCH_WIDTH = 40.dp
private val DRAWER_INDEX_ACTIVE_BUBBLE = 18.dp
private const val DRAWER_BUBBLE_FADE_MS = 140
private const val DRAWER_TILE_PRESS_SCALE = 0.95f
