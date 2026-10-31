package com.flower.launcher.ui

import kotlinx.coroutines.isActive

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flower.launcher.apps.AppSearchEngine
import com.flower.launcher.apps.InstalledApp
import com.flower.launcher.search.SearchPreferences
import com.flower.launcher.search.ShepherdHabitShortcut
import com.flower.launcher.search.SearchTarget
import com.flower.launcher.search.SearchTargetGroup
import com.flower.launcher.search.layoutSearchTargets
import com.flower.launcher.search.matchSearchTargetPrefix
import com.flower.launcher.search.searchPrefixSuggestions
import com.flower.launcher.settings.LauncherPreferences
import com.flower.launcher.ui.theme.LauncherAccentLight
import com.flower.launcher.ui.theme.OnHomePrimary
import com.flower.launcher.ui.theme.OnHomeSecondary
import com.flower.launcher.ui.theme.SearchFieldBackground
import kotlin.math.ceil

@Composable
fun SearchContent(
    apps: List<InstalledApp>,
    hiddenComponentKeys: Set<String>,
    preferences: SearchPreferences,
    launcherPreferences: LauncherPreferences,
    imePresentationReady: Boolean,
    imeControlEnabled: Boolean = true,
    coordinator: SearchCoordinator,
    reboundDurationMs: Int = 320,
    onReturnHomeThreshold: () -> Boolean = { false },
    transitionProgress: () -> Float = { 1f },
    returnProgress: () -> Float = { 0f },
    onAppSelected: (InstalledApp) -> Unit,
    onShareApp: (InstalledApp) -> Unit,
    onAppInfo: (InstalledApp) -> Unit,
    onRenameApp: (InstalledApp, String?) -> Unit,
    onSetAppHidden: (InstalledApp, Boolean) -> Unit,
    onOpenAppStore: (InstalledApp) -> Unit,
    onUninstallApp: (InstalledApp) -> Unit,
    onOpenSettings: () -> Unit,
    targets: List<SearchTarget>,
    groups: List<SearchTargetGroup> = emptyList(),
    expandedGroupIds: Set<String> = emptySet(),
    onToggleGroup: (String) -> Unit = {},
    onSearchTarget: (SearchTarget, String) -> Unit,
    onOpenShepherdHabits: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var queryValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(coordinator.state.query))
    }
    val query = queryValue.text
    val trimmedQuery = query.trim()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val interactionScope = rememberCoroutineScope()
    val interactionState = coordinator.interaction
    var searchFieldFocused by remember { mutableStateOf(false) }

    // Search keeps one fixed full-height result surface. IME insets only position the
    // floating field; they never resize the result viewport. This removes the scroll-range
    // clamp that used to happen while a normal-speed swipe was hiding the keyboard.
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val navigationBottomPx = WindowInsets.navigationBars.getBottom(density)
    val imeVisible = imeBottomPx > 0
    val floatingBarBottomPadding = 10.dp
    val searchFieldHeight = preferences.searchFieldHeightDp.coerceIn(36, 64).dp
    val searchFieldCornerRadius = preferences.searchFieldCornerRadiusDp.coerceIn(8, 32).dp
    val floatingSearchBarColor = searchBarColorFor(
        presetId = preferences.searchBarColorPresetId,
        depthPercent = preferences.searchBarColorDepth,
        opacityPercent = preferences.searchBarOpacityPercent,
    )
    val resultsBottomPadding = searchFieldHeight + 24.dp
    var resultsViewportHeightPx by remember { mutableIntStateOf(0) }
    var resultsContentHeightPx by remember { mutableIntStateOf(0) }
    val floatingBarHeightPx = with(density) { searchFieldHeight.toPx() }
    val floatingBarBottomPaddingPx = with(density) { floatingBarBottomPadding.toPx() }
    val effectiveImeOcclusionPx = (imeBottomPx - navigationBottomPx).coerceAtLeast(0).toFloat()
    val visibleResultsHeightWithImePx = (
        resultsViewportHeightPx.toFloat() -
            effectiveImeOcclusionPx -
            floatingBarHeightPx -
            floatingBarBottomPaddingPx
        ).coerceAtLeast(0f)
    val resultsOverflowAboveImePx = (
        resultsContentHeightPx.toFloat() - visibleResultsHeightWithImePx
        ).coerceAtLeast(0f)

    val overscrollLimitPx = with(density) {
        (configuration.screenHeightDp.dp * SEARCH_RESULTS_RUBBER_BAND_LIMIT_RATIO).toPx()
    }
    val collapseOverflowThresholdPx = with(density) {
        preferences.imeCollapseOverflowThresholdDp.coerceIn(0, 300).dp.toPx()
    }
    val revealPullThresholdPx = with(density) {
        preferences.imeRevealPullThresholdDp.coerceIn(80, 360).dp.toPx()
    }
    val homeReturnPullThresholdPx = with(density) {
        preferences.homeReturnPullThresholdDp.coerceIn(80, 360).dp.toPx()
    }

    val searchState = coordinator.state

    // rememberSaveable may restore TextFieldValue after process recreation. Keep the
    // coordinator query authoritative for launcher decisions by synchronizing that restore.
    LaunchedEffect(query) {
        if (coordinator.state.query != query) {
            coordinator.dispatch(SearchIntent.QueryChanged(query))
        }
    }

    // IME ownership: Search Settings shares the same window IME. While it is open Search
    // must not observe, reconcile, request focus, or show/hide the keyboard.
    //
    // Entering the overlay performs exactly one ownership handoff (release focus + hide).
    // Leaving the overlay re-enables control; the coordinator then re-derives its target
    // solely from presentationMode. Ownership flips must never reinterpret the overlay's
    // own IME visibility as a Search input/expanded transition.
    LaunchedEffect(imeControlEnabled) {
        coordinator.dispatch(SearchIntent.ImeControlChanged(imeControlEnabled))
        if (!imeControlEnabled) {
            // One-time release only. No further Hide is ever sent while the overlay lives.
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    // Actual IME visibility is only an acknowledgement, and only while Search owns the IME.
    // Programmatic hides are identified by SearchImePhase.Hiding, so no separate boolean can
    // be cleared too early by a newer Show request. A Visible -> Hidden transition while the
    // field remains focused is the only path treated as a user/system dismissal into Expanded.
    LaunchedEffect(imeVisible, imeControlEnabled) {
        if (!imeControlEnabled) return@LaunchedEffect
        coordinator.dispatch(
            SearchIntent.ImeVisibilityChanged(
                visible = imeVisible,
                userInitiatedHide =
                    !imeVisible &&
                        searchFieldFocused &&
                        coordinator.state.imePhase == SearchImePhase.Visible,
            ),
        )
    }

    val prefixMatch = remember(query, targets) { matchSearchTargetPrefix(query, targets) }
    val effectiveQuery = prefixMatch?.remainder ?: trimmedQuery
    val prefixSuggestions = remember(query, targets) { searchPrefixSuggestions(query, targets) }
    val targetLayout = remember(targets, groups) { layoutSearchTargets(targets, groups, enabledOnly = true) }
    val showPrefixMenu = remember(query) {
        val token = query.trimStart()
        token.startsWith("/") && !token.contains(' ')
    }
    val searchIndex = remember(apps) { AppSearchEngine.prepare(apps) }
    val results = remember(searchIndex, effectiveQuery) { searchIndex.search(effectiveQuery) }
    val launchApps = remember(apps, preferences.launchAppsCount) {
        apps.take(preferences.launchAppsCount.coerceAtLeast(0))
    }
    val appsByPackage = remember(apps) {
        buildMap { apps.forEach { app -> if (app.packageName !in this) put(app.packageName, app) } }
    }
    val showShepherdHabitShortcut = prefixMatch == null &&
        ShepherdHabitShortcut.matches(trimmedQuery) &&
        appsByPackage.containsKey(ShepherdHabitShortcut.PACKAGE_NAME)

    // Search result tiles mirror the app drawer's icon appearance and grid metrics.
    val drawerColumns = launcherPreferences.drawerColumns.coerceIn(3, 7)
    val drawerIconSizeDp = launcherPreferences.drawerIconSizeDp
    val drawerIconShape = drawerIconShape(launcherPreferences)
    val drawerShowLabels = launcherPreferences.drawerShowLabels
    val drawerLabelSizeSp = launcherPreferences.drawerLabelSizeSp
    val drawerAllowLabelWrap = launcherPreferences.drawerAllowLabelWrap
    val drawerVerticalSpacingDp = launcherPreferences.drawerVerticalSpacingDp

    // Strictly serialize IME commands. The coordinator advances Hidden -> Showing before
    // Show and Visible -> Hiding before Hide. If the desired target reverses while a command
    // is in flight, the opposite command waits for the corresponding WindowInsets
    // acknowledgement instead of racing it.
    LaunchedEffect(
        imePresentationReady,
        imeControlEnabled,
        searchState.presentationMode,
        searchState.imePhase,
        searchState.phase,
        searchState.imeControlEnabled,
        searchState.pageReturnImeSuppressed,
    ) {
        when (
            coordinator.reconcileIme(
                presentationReady = imePresentationReady,
            )
        ) {
            SearchImeCommand.Show -> {
                if (queryValue.text.isNotEmpty()) {
                    queryValue = queryValue.copy(selection = TextRange(0, queryValue.text.length))
                }
                focusRequester.requestFocus()
                keyboardController?.show()
            }

            SearchImeCommand.Hide -> {
                focusManager.clearFocus()
                keyboardController?.hide()
            }

            SearchImeCommand.None -> Unit
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    val hasQuery = searchState.hasQuery
    val searchNestedScrollConnection = remember(
        coordinator,
        hasQuery,
        imeVisible,
        collapseOverflowThresholdPx,
        resultsOverflowAboveImePx,
        revealPullThresholdPx,
        homeReturnPullThresholdPx,
        reboundDurationMs,
        overscrollLimitPx,
        onReturnHomeThreshold,
        preferences.imeRevealHapticEnabled,
        preferences.imeRevealHapticStrengthPercent,
        preferences.homeReturnHapticEnabled,
        preferences.homeReturnHapticStrengthPercent,
    ) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero

                val (consumedY, effect) = coordinator.onPreScroll(
                    deltaY = available.y,
                    imeVisible = imeVisible,
                    contentOverflowAboveImePx = resultsOverflowAboveImePx,
                    collapseOverflowThresholdPx = collapseOverflowThresholdPx,
                )

                when (effect) {
                    // The coordinator already changed the presentation target to Expanded.
                    // The serialized reconciler above emits Hide at the safe IME phase.
                    SearchPreScrollEffect.HideIme -> Unit
                    SearchPreScrollEffect.ConsumeAll -> return available
                    SearchPreScrollEffect.None -> Unit
                }

                return if (consumedY != 0f) Offset(0f, consumedY) else Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput || available.y == 0f) {
                    return Offset.Zero
                }

                // Downward top-boundary pull is one Search-local interaction for both
                // empty and non-empty queries. Upward empty-Search motion stays
                // unconsumed here so LauncherRoot can preserve the existing page return.
                val effect = when {
                    available.y > 0f -> coordinator.onTopBoundaryDrag(
                        deltaY = available.y,
                        imeVisible = imeVisible,
                        overscrollLimitPx = overscrollLimitPx,
                        revealPullThresholdPx = revealPullThresholdPx,
                    )
                    hasQuery -> coordinator.onBottomBoundaryDrag(
                        deltaY = available.y,
                        overscrollLimitPx = overscrollLimitPx,
                        homeReturnPullThresholdPx = homeReturnPullThresholdPx,
                    )
                    else -> SearchBoundaryEffect.None
                }

                when (effect) {
                    SearchBoundaryEffect.ReturnHome -> {
                        if (onReturnHomeThreshold()) {
                            if (preferences.homeReturnHapticEnabled) {
                                performThresholdHaptic(
                                    vibrator = context.getSystemService(Vibrator::class.java),
                                    strengthPercent = preferences.homeReturnHapticStrengthPercent,
                                )
                            }
                        }
                    }

                    SearchBoundaryEffect.RevealIme -> {
                        if (preferences.imeRevealHapticEnabled) {
                            performThresholdHaptic(
                                vibrator = context.getSystemService(Vibrator::class.java),
                                strengthPercent = preferences.imeRevealHapticStrengthPercent,
                            )
                        }
                        // onTopBoundaryDrag already changed the target to Input.
                        // The serialized reconciler emits Show after any in-flight Hide ends.
                    }

                    SearchBoundaryEffect.ConsumeAll,
                    SearchBoundaryEffect.None,
                    -> Unit
                }

                return when {
                    available.y > 0f -> Offset(0f, available.y)
                    hasQuery -> Offset(0f, available.y)
                    else -> Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (coordinator.state.phase == SearchPhase.Returning) return available
                if (interactionState.rawOverscrollPx == 0f) return Velocity.Zero
                coordinator.onRelease(interactionScope, reboundDurationMs)
                return available
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                coordinator.onRelease(interactionScope, reboundDurationMs)
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            // Observe pointer-down without consuming it. If a release-time rebound is
            // still running, stop it at its live position before nested scrolling or
            // child gestures begin handling this touch.
            .pointerInput(interactionState) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val sessionId = coordinator.state.sessionId
                    coordinator.beginInteractive()
                    try {
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                        } while (event.changes.any { it.pressed })
                    } finally {
                        // An interrupted touch can cancel an existing rebound without
                        // producing a fling. Resume it only for the same live session.
                        if (interactionScope.isActive &&
                            coordinator.state.sessionId == sessionId &&
                            coordinator.state.phase == SearchPhase.Active
                        ) {
                            coordinator.onRelease(interactionScope, reboundDurationMs)
                        }
                    }
                }
            }
            // Keep the Search result viewport stable while IME animates. Navigation-bar
            // padding is static; only the floating field below consumes the IME inset.
            .navigationBarsPadding()
            .graphicsLayer {
                val returnP = returnProgress().coerceIn(0f, 1f)
                val shrinkP = smoothExitWindow(
                    value = returnP,
                    start = SEARCH_RETURN_FAST_EXIT_START,
                    end = SEARCH_RETURN_BAR_GONE_PROGRESS,
                )
                translationY = -SEARCH_RETURN_BAR_MAX_UP.toPx() * returnP
                val exitScale = 1f - (1f - SEARCH_RETURN_FOREGROUND_SCALE) * shrinkP
                scaleX = exitScale
                scaleY = exitScale
                transformOrigin = TransformOrigin(0.5f, 0.10f)
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { resultsViewportHeightPx = it.height }
                .graphicsLayer {
                    val returnP = returnProgress().coerceIn(0f, 1f)
                    val exitFade = smoothExitWindow(
                        value = returnP,
                        start = SEARCH_RETURN_FAST_EXIT_START,
                        end = SEARCH_RETURN_CONTENT_GONE_PROGRESS,
                    )
                    alpha = 1f - exitFade
                    translationY =
                        interactionState.visualOverscrollPx(overscrollLimitPx) -
                            SEARCH_RETURN_CONTENT_EXTRA_UP.toPx() * returnP
                }
                .nestedScroll(searchNestedScrollConnection)
                .verticalScroll(interactionState.scrollState)
                .graphicsLayer {
                    val resultsProgress = ((transitionProgress() - 0.12f) / 0.88f)
                        .coerceIn(0f, 1f)
                    alpha = resultsProgress
                },
        ) {
            // Measure only real Search content. The fixed bottom scroll clearance is kept
            // outside this measurement so it cannot falsely qualify a short result set
            // for IME collapse.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { resultsContentHeightPx = it.height },
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                if (showPrefixMenu) {
                PrefixSuggestionMenu(
                    suggestions = prefixSuggestions,
                    onPick = { target ->
                        val text = target.prefix.orEmpty() + " "
                        queryValue = TextFieldValue(text, selection = TextRange(text.length))
                        coordinator.dispatch(SearchIntent.QueryChanged(text))
                        coordinator.dispatch(SearchIntent.EnterInputMode)
                        focusRequester.requestFocus()
                    },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (trimmedQuery.isEmpty()) {
                if (launchApps.isNotEmpty()) {
                    SectionLabel("应用")
                    AppGrid(
                        apps = launchApps,
                        hiddenComponentKeys = hiddenComponentKeys,
                        columns = drawerColumns,
                        iconSizeDp = drawerIconSizeDp,
                        iconShape = drawerIconShape,
                        showLabels = drawerShowLabels,
                        labelSizeSp = drawerLabelSizeSp,
                        allowLabelWrap = drawerAllowLabelWrap,
                        verticalSpacingDp = drawerVerticalSpacingDp,
                        onAppSelected = onAppSelected,
                        onShareApp = onShareApp,
                        onAppInfo = onAppInfo,
                        onRenameApp = onRenameApp,
                        onSetAppHidden = onSetAppHidden,
                        onOpenAppStore = onOpenAppStore,
                        onUninstallApp = onUninstallApp,
                    )
                }
            } else {
                if (showShepherdHabitShortcut) {
                    SectionLabel("快捷打开")
                    SearchActionRow(
                        icon = if (appsByPackage[ShepherdHabitShortcut.PACKAGE_NAME]?.icon == null) {
                            Icons.Default.CheckCircle
                        } else null,
                        bitmap = appsByPackage[ShepherdHabitShortcut.PACKAGE_NAME]?.icon,
                        text = "Shepherd · 习惯",
                        onClick = onOpenShepherdHabits,
                    )
                }
                if (results.isNotEmpty()) {
                    SectionLabel("所有应用")
                    AppGrid(
                        apps = results,
                        hiddenComponentKeys = hiddenComponentKeys,
                        columns = drawerColumns,
                        iconSizeDp = drawerIconSizeDp,
                        iconShape = drawerIconShape,
                        showLabels = drawerShowLabels,
                        labelSizeSp = drawerLabelSizeSp,
                        allowLabelWrap = drawerAllowLabelWrap,
                        verticalSpacingDp = drawerVerticalSpacingDp,
                        onAppSelected = onAppSelected,
                        onShareApp = onShareApp,
                        onAppInfo = onAppInfo,
                        onRenameApp = onRenameApp,
                        onSetAppHidden = onSetAppHidden,
                        onOpenAppStore = onOpenAppStore,
                        onUninstallApp = onUninstallApp,
                    )
                } else if (!showShepherdHabitShortcut) {
                    Text(
                        text = "没有匹配的应用",
                        color = OnHomeSecondary.copy(alpha = 0.65f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
                    )
                }

                val targetRow: @Composable (SearchTarget, Boolean) -> Unit = { target, indented ->
                    val appIcon = target.packageName?.let { pkg -> appsByPackage[pkg]?.icon }
                    val baseText = target.actionTemplate.replace(SearchTarget.NAME_PLACEHOLDER, target.label)
                    val rowText = if (prefixMatch != null && effectiveQuery.isNotEmpty()) {
                        "$baseText “$effectiveQuery”"
                    } else {
                        baseText
                    }
                    SearchActionRow(
                        icon = if (appIcon == null) iconForTarget(target) else null,
                        bitmap = appIcon,
                        text = rowText,
                        onClick = { onSearchTarget(target, effectiveQuery) },
                        indentDp = if (indented) SEARCH_GROUP_CHILD_INDENT else 0.dp,
                    )
                }

                if (prefixMatch != null) {
                    targetRow(prefixMatch.target, false)
                } else {
                    targetLayout.ungrouped.forEach { target -> targetRow(target, false) }
                    targetLayout.groups.forEach { members ->
                        val expanded = members.group.id in expandedGroupIds
                        SearchGroupRow(
                            label = members.group.label.ifBlank { "(未命名分类)" },
                            expanded = expanded,
                            onToggle = { onToggleGroup(members.group.id) },
                        )
                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically(
                                animationSpec = tween(SEARCH_GROUP_ANIM_MS),
                                expandFrom = Alignment.Top,
                            ) + fadeIn(animationSpec = tween(SEARCH_GROUP_ANIM_MS)),
                            exit = shrinkVertically(
                                animationSpec = tween(SEARCH_GROUP_ANIM_MS),
                                shrinkTowards = Alignment.Top,
                            ) + fadeOut(animationSpec = tween(SEARCH_GROUP_ANIM_MS)),
                        ) {
                            Column {
                                members.targets.forEach { target -> targetRow(target, true) }
                            }
                        }
                    }
                }

                if (prefixMatch == null && preferences.settingsEnabled) {
                    SearchActionRow(
                        icon = Icons.Default.Settings,
                        text = "打开系统设置",
                        onClick = onOpenSystemSettings,
                    )
                }
            }
            }

            // Stable scroll clearance for the floating field when IME is absent. It is
            // intentionally not tied to IME height, so the scroll range stays unchanged
            // throughout keyboard animation.
            Spacer(Modifier.height(resultsBottomPadding + 36.dp))
        }

        // The field is a persistent floating layer. It alone consumes the animated IME
        // inset, so it follows IME top while Results keep the same full-height viewport.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                // Parent consumes navigation bars; this contributes only the remaining
                // animated IME occlusion and moves the field without resizing Results.
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = floatingBarBottomPadding)
                .graphicsLayer {
                    val fieldProgress = transitionProgress().coerceIn(0f, 1f)
                    val exitFade = smoothExitWindow(
                        value = returnProgress().coerceIn(0f, 1f),
                        start = SEARCH_RETURN_FAST_EXIT_START,
                        end = SEARCH_RETURN_BAR_GONE_PROGRESS,
                    )
                    alpha = fieldProgress * (1f - exitFade)
                },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchField(
                    query = query,
                    queryValue = queryValue,
                    onQueryChange = { value ->
                        queryValue = value
                        coordinator.dispatch(SearchIntent.QueryChanged(value.text))
                    },
                    focusRequester = focusRequester,
                    onFocusStateChanged = { focused ->
                        searchFieldFocused = focused
                        if (
                            focused &&
                            coordinator.state.phase == SearchPhase.Active &&
                            !coordinator.state.pageReturnImeSuppressed
                        ) {
                            coordinator.dispatch(SearchIntent.EnterInputMode)
                        }
                    },
                    height = searchFieldHeight,
                    cornerRadius = searchFieldCornerRadius,
                    backgroundColor = floatingSearchBarColor,
                    onClear = {
                        queryValue = TextFieldValue("")
                        coordinator.dispatch(SearchIntent.QueryChanged(""))
                        coordinator.dispatch(SearchIntent.EnterInputMode)
                        focusRequester.requestFocus()
                    },
                    onSearch = {
                        val matched = prefixMatch
                        if (matched != null) {
                            if (effectiveQuery.isNotEmpty()) {
                                coordinator.dispatch(SearchIntent.EnterExpandedMode)
                                focusManager.clearFocus()
                                onSearchTarget(matched.target, effectiveQuery)
                            }
                        } else if (showShepherdHabitShortcut) {
                            coordinator.dispatch(SearchIntent.EnterExpandedMode)
                            focusManager.clearFocus()
                            onOpenShepherdHabits()
                        } else {
                            val bestMatch = results.firstOrNull()
                            if (trimmedQuery.isNotEmpty() && bestMatch != null) {
                                coordinator.dispatch(SearchIntent.EnterExpandedMode)
                                focusManager.clearFocus()
                                onAppSelected(bestMatch)
                            } else if (trimmedQuery.isNotEmpty()) {
                                targets.firstOrNull { it.enabled }?.let { onSearchTarget(it, trimmedQuery) }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                )

                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "搜索设置",
                    tint = OnHomePrimary,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable {
                            onOpenSettings()
                        }
                        .padding(6.dp),
                )
            }
        }
    }
}

private fun smoothExitWindow(value: Float, start: Float, end: Float): Float {
    if (end <= start) return if (value >= end) 1f else 0f
    val t = ((value - start) / (end - start)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private val SEARCH_RETURN_BAR_MAX_UP = 28.dp
private val SEARCH_RETURN_CONTENT_EXTRA_UP = 4.dp
private const val SEARCH_RETURN_FAST_EXIT_START = 0.55f
private const val SEARCH_RETURN_BAR_GONE_PROGRESS = 0.80f
private const val SEARCH_RETURN_CONTENT_GONE_PROGRESS = 0.85f
private const val SEARCH_RETURN_FOREGROUND_SCALE = 0.965f
private val SEARCH_GROUP_CHILD_INDENT = 16.dp
private const val SEARCH_GROUP_ANIM_MS = 180
private fun performThresholdHaptic(
    vibrator: Vibrator?,
    strengthPercent: Int,
) {
    if (vibrator == null || !vibrator.hasVibrator()) return
    val strength = strengthPercent.coerceIn(10, 100)
    if (vibrator.hasAmplitudeControl()) {
        val amplitude = (1 + ((strength - 10) / 90f) * 254f).toInt().coerceIn(1, 255)
        vibrator.vibrate(VibrationEffect.createOneShot(18L, amplitude))
    } else {
        // Devices without amplitude control still get a perceptible strength range
        // through a very short duration adjustment.
        val durationMs = (8f + (strength / 100f) * 18f).toLong().coerceIn(8L, 26L)
        vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

private const val SEARCH_RESULTS_RUBBER_BAND_LIMIT_RATIO = 0.46f

@Composable
private fun SearchField(
    query: String,
    queryValue: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    onFocusStateChanged: (Boolean) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    height: Dp = 44.dp,
    cornerRadius: Dp = 22.dp,
    backgroundColor: Color = Color(0x33000000),
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = OnHomeSecondary,
            modifier = Modifier.size(22.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (query.isEmpty()) {
                Text(
                    text = "搜索",
                    color = OnHomeSecondary.copy(alpha = 0.55f),
                    fontSize = 16.sp,
                )
            }
            BasicTextField(
                value = queryValue,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { onFocusStateChanged(it.isFocused) },
                singleLine = true,
                textStyle = TextStyle(
                    color = OnHomePrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                ),
                cursorBrush = SolidColor(LauncherAccentLight),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            )
        }

        if (query.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "清除",
                tint = OnHomeSecondary,
                modifier = Modifier
                    .size(22.dp)
                    .clickable(onClick = onClear),
            )
        }
    }
}

@Composable
private fun PrefixSuggestionMenu(
    suggestions: List<SearchTarget>,
    onPick: (SearchTarget) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SearchFieldBackground)
            .padding(vertical = 6.dp),
    ) {
        if (suggestions.isEmpty()) {
            Text(
                text = "没有匹配的前缀",
                color = OnHomeSecondary.copy(alpha = 0.7f),
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        } else {
            suggestions.take(6).forEach { target ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(target) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = target.prefix.orEmpty(),
                        color = LauncherAccentLight,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(72.dp),
                    )
                    Text(
                        text = target.label,
                        color = OnHomePrimary,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchActionRow(
    icon: ImageVector? = null,
    bitmap: ImageBitmap? = null,
    text: String,
    onClick: () -> Unit,
    indentDp: Dp = 0.dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 22.dp + indentDp, end = 22.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OnHomeSecondary,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Spacer(modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = text,
            color = OnHomePrimary,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = OnHomeSecondary.copy(alpha = 0.65f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SearchGroupRow(
    label: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = SEARCH_GROUP_ANIM_MS),
        label = "searchGroupChevron",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            tint = OnHomeSecondary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            color = OnHomePrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = OnHomeSecondary.copy(alpha = 0.65f),
            modifier = Modifier.size(20.dp).rotate(rotation),
        )
    }
}

private fun iconForTarget(target: SearchTarget): ImageVector = when {
    target.primaryUri.startsWith("market://") -> Icons.Default.PlayArrow
    target.primaryUri.startsWith("http") -> Icons.Default.Search
    else -> Icons.Default.Storefront
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = OnHomePrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 22.dp, top = 6.dp, bottom = 12.dp),
    )
}

@Composable
private fun AppGrid(
    apps: List<InstalledApp>,
    hiddenComponentKeys: Set<String>,
    columns: Int,
    iconSizeDp: Int,
    iconShape: Shape,
    showLabels: Boolean,
    labelSizeSp: Int,
    allowLabelWrap: Boolean,
    verticalSpacingDp: Int,
    onAppSelected: (InstalledApp) -> Unit,
    onShareApp: (InstalledApp) -> Unit,
    onAppInfo: (InstalledApp) -> Unit,
    onRenameApp: (InstalledApp, String?) -> Unit,
    onSetAppHidden: (InstalledApp, Boolean) -> Unit,
    onOpenAppStore: (InstalledApp) -> Unit,
    onUninstallApp: (InstalledApp) -> Unit,
) {
    val safeColumns = columns.coerceAtLeast(1)
    val rows = ceil(apps.size / safeColumns.toFloat()).toInt().coerceAtLeast(1)
    val density = LocalDensity.current
    val labelLineHeight = with(density) { (labelSizeSp * 1.4f).sp.toDp() }
    val labelLines = if (allowLabelWrap) 2 else 1
    val labelBlock = if (showLabels) 6.dp + labelLineHeight * labelLines else 0.dp
    val cellHeight = iconSizeDp.dp + labelBlock
    val gridHeight = cellHeight * rows +
        verticalSpacingDp.dp * (rows - 1).coerceAtLeast(0) +
        16.dp
    LazyVerticalGrid(
        columns = GridCells.Fixed(safeColumns),
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(verticalSpacingDp.dp),
        userScrollEnabled = false,
        overscrollEffect = null,
    ) {
        items(items = apps, key = InstalledApp::componentKey) { app ->
            SearchResultTile(
                app = app,
                isHidden = app.componentKey in hiddenComponentKeys,
                iconSizeDp = iconSizeDp,
                iconShape = iconShape,
                showLabels = showLabels,
                labelSizeSp = labelSizeSp,
                allowLabelWrap = allowLabelWrap,
                onClick = { onAppSelected(app) },
                onShare = { onShareApp(app) },
                onInfo = { onAppInfo(app) },
                onRename = { name -> onRenameApp(app, name) },
                onSetHidden = { hidden -> onSetAppHidden(app, hidden) },
                onOpenStore = { onOpenAppStore(app) },
                onUninstall = { onUninstallApp(app) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResultTile(
    app: InstalledApp,
    isHidden: Boolean,
    iconSizeDp: Int,
    iconShape: Shape,
    showLabels: Boolean,
    labelSizeSp: Int,
    allowLabelWrap: Boolean,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onInfo: () -> Unit,
    onRename: (String?) -> Unit,
    onSetHidden: (Boolean) -> Unit,
    onOpenStore: () -> Unit,
    onUninstall: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showHideDialog by remember { mutableStateOf(false) }
    var renameText by remember(app.label) { mutableStateOf(app.label) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true },
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconOrFallback(
                app = app,
                size = iconSizeDp.dp,
                fallbackFontSize = (labelSizeSp + 8).sp,
                shape = iconShape,
                fallbackBackground = Color(0x33FFFFFF),
            )
            if (showLabels) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = app.label,
                    color = if (isHidden) OnHomeSecondary.copy(alpha = 0.55f) else OnHomePrimary,
                    fontSize = labelSizeSp.sp,
                    textAlign = TextAlign.Center,
                    maxLines = if (allowLabelWrap) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            containerColor = Color(0xF20B0B0D),
            shape = RoundedCornerShape(24.dp),
        ) {
            AppMenuHeader(
                appName = app.label,
                onShare = {
                    showMenu = false
                    onShare()
                },
                onInfo = {
                    showMenu = false
                    onInfo()
                },
            )
            DropdownMenuItem(
                text = { Text("编辑图标", color = OnHomePrimary) },
                leadingIcon = { Icon(Icons.Default.Edit, null, tint = OnHomePrimary) },
                onClick = { showMenu = false },
                enabled = false,
            )
            DropdownMenuItem(
                text = { Text("重命名", color = OnHomePrimary) },
                leadingIcon = { Icon(Icons.Default.Edit, null, tint = OnHomePrimary) },
                onClick = {
                    showMenu = false
                    renameText = app.label
                    showRenameDialog = true
                },
            )
            DropdownMenuItem(
                text = { Text(if (isHidden) "取消隐藏" else "隐藏", color = OnHomePrimary) },
                leadingIcon = {
                    Icon(
                        if (isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        null,
                        tint = OnHomePrimary,
                    )
                },
                onClick = {
                    showMenu = false
                    if (isHidden) onSetHidden(false) else showHideDialog = true
                },
            )
            DropdownMenuItem(
                text = { Text("Play Store", color = OnHomePrimary) },
                leadingIcon = { Icon(Icons.Default.Storefront, null, tint = OnHomePrimary) },
                onClick = {
                    showMenu = false
                    onOpenStore()
                },
            )
            DropdownMenuItem(
                text = { Text("卸载", color = Color(0xFFFF7A7A)) },
                leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFFF7A7A)) },
                onClick = {
                    showMenu = false
                    onUninstall()
                },
            )
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名应用") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("名称") },
                )
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("取消") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = renameText.trim()
                        onRename(name.ifBlank { null })
                        showRenameDialog = false
                    },
                ) { Text("保存") }
            },
        )
    }

    if (showHideDialog) {
        AlertDialog(
            onDismissRequest = { showHideDialog = false },
            title = { Text("您隐藏了一个应用") },
            text = {
                Column {
                    Text("它将不会出现在你的应用页面或搜索结果中。")
                    Spacer(Modifier.height(10.dp))
                    Text("你可以在搜索页面设置中打开“在搜索结果中显示隐藏的应用”来访问隐藏应用。")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHideDialog = false }) { Text("取消") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSetHidden(true)
                        showHideDialog = false
                    },
                ) { Text("确定") }
            },
        )
    }
}

@Composable
private fun AppMenuHeader(
    appName: String,
    onShare: () -> Unit,
    onInfo: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 10.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = appName,
            color = OnHomePrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "分享",
            tint = OnHomePrimary,
            modifier = Modifier
                .size(38.dp)
                .clickable(onClick = onShare)
                .padding(7.dp),
        )
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "应用信息",
            tint = OnHomePrimary,
            modifier = Modifier
                .size(38.dp)
                .clickable(onClick = onInfo)
                .padding(7.dp),
        )
    }
}
