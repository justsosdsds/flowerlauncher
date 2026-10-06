package com.flower.launcher.ui

import android.appwidget.AppWidgetHost
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.flower.launcher.apps.AppCustomizationRepository
import com.flower.launcher.apps.AppDrawerCatalog
import com.flower.launcher.apps.InstalledApp
import com.flower.launcher.home.HomeFolder
import com.flower.launcher.home.HomePageRequest
import com.flower.launcher.home.visitHomePage
import com.flower.launcher.home.HomePreferences
import com.flower.launcher.search.SearchTarget
import com.flower.launcher.search.SearchTargetGroup
import com.flower.launcher.settings.LauncherPreferences
import com.flower.launcher.ui.theme.SearchScrim
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

@Composable
fun LauncherRoot(
    apps: List<InstalledApp>,
    resetSignal: Int,
    wallpaperSelectionRevision: Int,
    widgetIds: List<Int>,
    widgetHeights: Map<Int, Int>,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    onLaunchApp: (InstalledApp) -> Unit,
    onAddWidget: () -> Unit,
    onConfigureWidget: (Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onSetWidgetHeight: (Int, Int) -> Unit,
    onOpenWallpaper: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val haptic = LocalHapticFeedback.current
    val minuteClock by rememberMinuteClock()
    val homeWallpaper = rememberHomeWallpaperSnapshot(wallpaperSelectionRevision)
    val settingsState = remember(context, scope) { LauncherSettingsState(context, scope) }
    val homePreferences = settingsState.home
    val searchPreferences = settingsState.search
    val searchTargets = settingsState.targets
    val searchGroups = settingsState.groups
    val expandedSearchGroups = settingsState.expandedGroups
    val launcherPreferences = settingsState.launcher
    var currentHomePage by remember { mutableStateOf(homePreferences.defaultHomePage) }
    var homePageRequest by remember { mutableStateOf(HomePageRequest(homePreferences.defaultHomePage)) }
    var showDesktopSheet by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf<DesktopDialog?>(null) }
    val appCustomizationRepository = remember(context) { AppCustomizationRepository(context.applicationContext) }
    var appCustomizations by remember { mutableStateOf(appCustomizationRepository.load()) }
    var showSearchSettings by remember { mutableStateOf(false) }

    DisposableEffect(activity, settingsState) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) settingsState.flush()
        }
        val lifecycleOwner = activity as? LifecycleOwner
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
            settingsState.flush()
        }
    }
    val uiScale = launcherPreferences.uiScalePercent.coerceIn(70, 110) / 100f
    var showLauncherSettings by remember { mutableStateOf(false) }
    var launcherSettingsInitialSection by remember { mutableStateOf<LauncherSettingsSection?>(null) }
    var drawerHiddenOnly by remember { mutableStateOf(false) }
    var launcherSettingsFromDrawerSheet by remember { mutableStateOf(false) }
    var drawerSheetRequest by remember { mutableIntStateOf(0) }
    var drawerSortMode by remember {
        mutableStateOf(
            runCatching { DrawerSortMode.valueOf(launcherPreferences.drawerSortMode) }
                .getOrDefault(DrawerSortMode.Name),
        )
    }
    DisposableEffect(showSearchSettings, showLauncherSettings, showDesktopSheet, activeDialog) {
        onDispose { settingsState.flush() }
    }
    val editScale by animateFloatAsState(
        targetValue = if (showDesktopSheet) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "desktopEditScale",
    )

    fun updateHomePreferences(value: HomePreferences) {
        if (homePreferences == value) return
        if (value.defaultHomePage != settingsState.home.defaultHomePage) {
            homePageRequest = homePageRequest.navigateTo(value.defaultHomePage)
        } else if (value.pageCount != settingsState.home.pageCount) {
            homePageRequest = homePageRequest.navigateTo(value.lastHomePage)
        }
        settingsState.home = value
    }

    fun addAppToHome(app: InstalledApp, page: Int = currentHomePage) {
        if (homePreferences.layoutLocked) return
        val requestedPage = page.coerceIn(0, 8)
        val perPageCapacity = homePreferences.gridColumns.coerceIn(3, 5) * 5
        val requestedPageItemCount = homePreferences.pinnedAppKeys.count {
            (homePreferences.appPageByKey[it] ?: 0) == requestedPage && it != app.componentKey
        } + homePreferences.folders.count {
            (homePreferences.folderPageById[it.id] ?: 0) == requestedPage
        }
        val targetPage = if (requestedPageItemCount >= perPageCapacity && requestedPage < 8) {
            requestedPage + 1
        } else {
            requestedPage
        }
        val keys = listOf(app.componentKey) + homePreferences.pinnedAppKeys.filterNot { it == app.componentKey }
        updateHomePreferences(
            homePreferences.copy(
                pinnedAppKeys = keys,
                removedAppKeys = homePreferences.removedAppKeys - app.componentKey,
                appPageByKey = homePreferences.appPageByKey + (app.componentKey to targetPage),
                pageCount = maxOf(homePreferences.pageCount, targetPage + 1),
                lastHomePage = targetPage,
            ),
        )
        currentHomePage = targetPage
        homePageRequest = homePageRequest.navigateTo(targetPage)
    }

    fun moveHomeApp(app: InstalledApp, direction: Int) {
        if (homePreferences.layoutLocked || direction == 0) return
        val currentPage = homePreferences.appPageByKey[app.componentKey] ?: 0
        val pageKeys = homePreferences.pinnedAppKeys.filter {
            (homePreferences.appPageByKey[it] ?: 0) == currentPage
        }
        val pageIndex = pageKeys.indexOf(app.componentKey)
        val neighborIndex = pageIndex + (if (direction < 0) -1 else 1)
        if (pageIndex >= 0 && neighborIndex in pageKeys.indices) {
            val neighborKey = pageKeys[neighborIndex]
            val full = homePreferences.pinnedAppKeys.toMutableList()
            val a = full.indexOf(app.componentKey)
            val b = full.indexOf(neighborKey)
            if (a >= 0 && b >= 0) {
                val tmp = full[a]
                full[a] = full[b]
                full[b] = tmp
                updateHomePreferences(homePreferences.copy(pinnedAppKeys = full))
            }
            return
        }
        val targetPage = (currentPage + (if (direction < 0) -1 else 1)).coerceIn(0, 8)
        if (targetPage != currentPage) {
            updateHomePreferences(
                homePreferences.copy(
                    appPageByKey = homePreferences.appPageByKey + (app.componentKey to targetPage),
                    pageCount = maxOf(homePreferences.pageCount, targetPage + 1),
                ),
            )
        }
    }

    fun updateSearchPreferences(value: com.flower.launcher.search.SearchPreferences) {
        if (searchPreferences == value) return
        settingsState.search = value
    }

    fun updateSearchTargets(value: List<SearchTarget>) {
        if (searchTargets == value) return
        settingsState.targets = value
    }

    fun updateSearchGroups(value: List<SearchTargetGroup>) {
        if (searchGroups == value) return
        settingsState.groups = value
    }

    fun toggleSearchGroup(groupId: String) {
        settingsState.expandedGroups = if (groupId in settingsState.expandedGroups) {
            settingsState.expandedGroups - groupId
        } else {
            settingsState.expandedGroups + groupId
        }
    }

    fun updateLauncherPreferences(value: LauncherPreferences) {
        if (launcherPreferences == value) return
        settingsState.launcher = value
    }

    fun setAppHidden(app: InstalledApp, hidden: Boolean) {
        appCustomizations = appCustomizationRepository.setHidden(
            current = appCustomizations,
            componentKey = app.componentKey,
            hidden = hidden,
        )
    }

    fun renameApp(app: InstalledApp, newName: String?) {
        appCustomizations = appCustomizationRepository.setAlias(
            current = appCustomizations,
            componentKey = app.componentKey,
            alias = newName,
        )
    }

    fun launchIntent(intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun searchWithTarget(target: SearchTarget, query: String) {
        val encoded = Uri.encode(query)
        fun intentFor(template: String, withPackage: Boolean): Intent? {
            val uri = Uri.parse(template.replace(SearchTarget.QUERY_PLACEHOLDER, encoded))
            val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (withPackage) {
                val pkg = target.packageName ?: return null
                intent.setPackage(pkg)
            }
            return intent
        }

        val attempts = buildList {
            target.fallbackUri?.let { fallback ->
                add(intentFor(fallback, withPackage = true))
                add(intentFor(fallback, withPackage = false))
            }
        }
        val primaryAttempts = listOf(
            intentFor(target.primaryUri, withPackage = true),
            intentFor(target.primaryUri, withPackage = false),
        )

        for (intent in primaryAttempts + attempts) {
            if (intent == null) continue
            if (runCatching { context.startActivity(intent) }.isSuccess) return
        }
        Toast.makeText(context, "找不到可处理该搜索的应用", Toast.LENGTH_SHORT).show()
    }

    fun shareApp(app: InstalledApp) {
        val url = "https://play.google.com/store/apps/details?id=${app.packageName}"
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        launchIntent(Intent.createChooser(share, "分享 ${app.label}"))
    }

    fun openAppInfo(app: InstalledApp) {
        launchIntent(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")))
    }

    fun openAppStore(app: InstalledApp) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${app.packageName}"))
        if (runCatching { context.startActivity(market.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isFailure) {
            launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${app.packageName}")))
        }
    }

    fun uninstallApp(app: InstalledApp) {
        launchIntent(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")))
    }

    val customizedApps = remember(apps, appCustomizations.aliases) {
        apps.map { app ->
            val alias = appCustomizations.aliases[app.componentKey]
            if (alias.isNullOrBlank()) app else app.copy(label = alias)
        }
    }
    val visibleApps = remember(customizedApps, appCustomizations.hiddenComponentKeys) {
        customizedApps.filterNot { it.componentKey in appCustomizations.hiddenComponentKeys }
    }
    val hiddenDrawerApps = remember(customizedApps, appCustomizations.hiddenComponentKeys) {
        customizedApps.filter { it.componentKey in appCustomizations.hiddenComponentKeys }
    }
    val searchApps = if (searchPreferences.showHiddenApps) customizedApps else visibleApps
    // Keep prepared order/index data alive while the drawer itself is uncomposed.
    val visibleDrawerCatalog = remember(visibleApps) { AppDrawerCatalog(visibleApps) }
    val hiddenDrawerCatalog = remember(hiddenDrawerApps) { AppDrawerCatalog(hiddenDrawerApps) }

    // V34: Search, Home and All Apps share one signed vertical position.
    //   -1f = All Apps, 0f = Home, +1f = Search.
    // One gesture may cross Home and reverse direction without switching controllers.
    val verticalTransition = remember(scope) { VerticalPageTransitionController(scope) }
    val verticalPosition = verticalTransition.position
    val transitionPullOffset = verticalTransition.transitionPullOffset
    val searchLayerActive = verticalTransition.searchLayerActive
    val drawerLayerActive = verticalTransition.drawerLayerActive
    val searchCoordinator = rememberSearchCoordinator()
    val gestureCoordinator = rememberGestureCoordinator()
    DisposableEffect(verticalTransition, gestureCoordinator) {
        onDispose {
            gestureCoordinator.reset()
            verticalTransition.dispose()
        }
    }
    val searchOwnsForeground by remember(verticalTransition) {
        derivedStateOf {
            verticalTransition.searchLayerActive && (
                verticalTransition.position.value > 0f ||
                    verticalTransition.targetPage == VerticalPage.Search
                )
        }
    }

    // Search session lifecycle belongs to the coordinator, not SearchContent. Because the
    // coordinator lives at LauncherRoot scope, teardown cannot be skipped if the Search
    // layer is removed from composition during the same settle that reaches Home.
    LaunchedEffect(searchOwnsForeground) {
        searchCoordinator.dispatch(SearchIntent.SessionActiveChanged(searchOwnsForeground))
    }

    // Search uses an overlay IME model: the keyboard must not resize the result surface.
    // Scope ADJUST_NOTHING to the Search foreground only so settings/text editors elsewhere
    // retain the launcher's normal ADJUST_RESIZE behavior.
    LaunchedEffect(activity, searchOwnsForeground, showSearchSettings) {
        activity?.window?.setSoftInputMode(
            if (searchOwnsForeground && !showSearchSettings) {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            } else {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            },
        )
    }
    DisposableEffect(activity) {
        onDispose {
            activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    // Keep the drawer scroll state above AllAppsContent so the Home gesture can hand
    // the same pointer stream to the grid after the page transition reaches -1.
    // AllAppsContent itself may not be in the original hit-test path when the finger
    // went down on Home, so waiting for LazyVerticalGrid to acquire the gesture later
    // cannot provide a seamless one-finger hand-off.
    val drawerGridState = rememberLazyGridState()
    var drawerFlingJob by remember { mutableStateOf<Job?>(null) }

    val dragDistancePx = with(density) { launcherPreferences.gestureDragDistanceDp.dp.toPx() }
    val settleThreshold = launcherPreferences.gestureSettleThresholdPercent.coerceIn(15, 60) / 100f
    val openVelocityPx = with(density) { launcherPreferences.gestureOpenVelocityDpPerSec.dp.toPx() }
    val searchReturnImeHideThresholdPx = with(density) {
        launcherPreferences.searchReturnImeHideThresholdDp.coerceIn(0, 40).dp.toPx()
    }
    val homePullLimitPx = with(density) { (configuration.screenHeightDp.dp * HOME_PULL_LIMIT_RATIO).toPx() }

    fun settleTo(
        page: VerticalPage,
        startValue: Float? = null,
    ) {
        val liveStart = startValue ?: verticalPosition.value
        if (page == VerticalPage.Home && liveStart > VerticalPage.Home.anchor) {
            searchCoordinator.dispatch(SearchIntent.PageReturnStarted)
        } else if (page == VerticalPage.Search) {
            searchCoordinator.dispatch(SearchIntent.ResumeActive)
        }
        if (
            page == VerticalPage.Home &&
            liveStart > VerticalPage.Home.anchor &&
            (verticalTransition.returningFromSearch ||
                verticalTransition.stablePage == VerticalPage.Search ||
                verticalTransition.targetPage == VerticalPage.Search)
        ) {
            // Also covers an interrupted Search settle (including Back / a new
            // upward gesture before stablePage has caught up with the pixels).
            verticalTransition.beginSearchReturn(liveStart)
        }
        val isSearchReturnSettle =
            page == VerticalPage.Home &&
                liveStart > VerticalPage.Home.anchor &&
                verticalTransition.returningFromSearch
        verticalTransition.settle(
            page = page,
            startValue = startValue,
            reboundDurationMs = launcherPreferences.searchReboundDurationMs,
            durationOverrideMs = if (isSearchReturnSettle) SEARCH_RETURN_SETTLE_MS else null,
        )
    }

    fun openSearch() = settleTo(VerticalPage.Search)
    fun closeSearch() = settleTo(VerticalPage.Home)
    fun closeDrawer() = settleTo(VerticalPage.Home)

    // Backing out of the full-screen settings that were opened from the drawer's
    // half-screen sheet returns to that drawer sheet instead of the settings home.
    fun returnToDrawerSheet() {
        showLauncherSettings = false
        launcherSettingsInitialSection = null
        launcherSettingsFromDrawerSheet = false
        drawerHiddenOnly = false
        drawerSheetRequest++
        settleTo(VerticalPage.AllApps)
    }

    fun resetDrawerImmediately() {
        drawerFlingJob?.cancel()
        scope.launch { verticalTransition.reset() }
        drawerHiddenOnly = false
    }

    LaunchedEffect(resetSignal) {
        if (resetSignal > 0) {
            gestureCoordinator.reset()
            homePageRequest = homePageRequest.navigateTo(settingsState.home.defaultHomePage)
            verticalTransition.reset()
            drawerHiddenOnly = false
            // The system Home gesture (the bottom "pill"/white bar) delivers a new
            // HOME intent while we are already the home activity. Treat it as
            // "go home": dismiss every transient overlay so the launcher returns to
            // the actual home screen instead of staying on a settings page.
            showLauncherSettings = false
            launcherSettingsInitialSection = null
            launcherSettingsFromDrawerSheet = false
            showSearchSettings = false
            showDesktopSheet = false
            activeDialog = null
            searchCoordinator.dispatch(SearchIntent.LauncherReset)
            gestureCoordinator.reset()
        }
    }

    // Preserve the old behavior where closing the drawer destroyed its local
    // LazyGridState: once Home is truly stable again, prepare the next drawer open
    // at the first item without making the closing animation jump.
    LaunchedEffect(verticalTransition.stablePage, drawerLayerActive) {
        if (verticalTransition.stablePage == VerticalPage.Home && !drawerLayerActive) {
            drawerFlingJob?.cancel()
            drawerGridState.requestScrollToItem(0)
        }
    }

    BackHandler(
        enabled = !showLauncherSettings &&
            (verticalTransition.stablePage == VerticalPage.AllApps ||
                verticalTransition.targetPage == VerticalPage.AllApps),
    ) {
        closeDrawer()
    }

    BackHandler(enabled = showSearchSettings) {
        showSearchSettings = false
    }

    BackHandler(
        enabled = !showSearchSettings &&
            (verticalTransition.stablePage == VerticalPage.Search ||
                verticalTransition.targetPage == VerticalPage.Search),
    ) {
        closeSearch()
    }

    // Search presentation is declarative: every fresh Search session targets Input,
    // while this page-progress gate decides when that target may be realized as an IME.
    // Because this is derived state rather than a one-shot event, activation ordering
    // cannot drop the request.
    val searchImePresentationReady by remember(verticalTransition, searchCoordinator) {
        derivedStateOf {
            verticalPosition.value >= KEYBOARD_TRIGGER_PROGRESS &&
                verticalTransition.searchLayerActive &&
                searchCoordinator.state.phase == SearchPhase.Active &&
                !searchCoordinator.state.pageReturnImeSuppressed
        }
    }

    // A completed return should not poison the next Search entry. If a return
    // was cancelled and Search settles open again, the IME is allowed to return.
    LaunchedEffect(
        verticalTransition.stablePage,
        verticalTransition.searchLayerActive,
    ) {
        if (
            verticalTransition.stablePage == VerticalPage.Home &&
            !verticalTransition.searchLayerActive
        ) {
            searchCoordinator.dispatch(SearchIntent.AllowImeAfterPageReturn)
        } else if (verticalTransition.stablePage == VerticalPage.Search) {
            searchCoordinator.dispatch(SearchIntent.AllowImeAfterPageReturn)
            searchCoordinator.dispatch(SearchIntent.ResumeActive)
        }
    }

    // Home <-> All Apps uses one continuous vertical gesture owner. Keeping the
    // detector on LauncherRoot while either Home or All Apps is stable (and while
    // they settle between each other) avoids the hand-off gap that used to exist
    // between AllAppsContent's close gesture and Home's root gesture. Search keeps
    // its own foreground gesture path once it becomes the committed target.
    val drawerHomeGestureReady =
        verticalTransition.stablePage != VerticalPage.Search &&
            verticalTransition.targetPage != VerticalPage.Search

    val gestureModifier = if (
        !drawerHomeGestureReady ||
        showDesktopSheet || activeDialog != null ||
        showSearchSettings || showLauncherSettings || !launcherPreferences.enableSwipeGestures
    ) {
        Modifier
    } else {
        Modifier.pointerInput(
            dragDistancePx,
            settleThreshold,
            openVelocityPx,
            homePullLimitPx,
            searchReturnImeHideThresholdPx,
        ) {
            var velocityTracker = VelocityTracker()
            var latestPosition = verticalPosition.value.coerceIn(-1f, 1f)
            var gestureStartPage = verticalTransition.stablePage
            var drawerScrollEngaged = false

            fun snapInteractivePosition(position: Float) {
                latestPosition = position.coerceIn(-1f, 1f)
                // Home=0 is the visual origin for both sibling pages. Derive the
                // transient pull offset from the signed page position itself instead
                // of from the total finger distance travelled since the gesture began.
                // This prevents Drawer -> Home travel from leaking into Search the
                // instant the gesture crosses 0, while preserving the exact same
                // Home -> Search rubber-band curve as a direct gesture.
                val localPageDistancePx = latestPosition * dragDistancePx
                val liveOffset = signedRubberBandDistance(
                    localPageDistancePx,
                    homePullLimitPx,
                ).coerceIn(-homePullLimitPx, homePullLimitPx)
                verticalTransition.snapTo(
                    positionValue = latestPosition,
                    pullOffset = liveOffset,
                )
            }

            fun consumeTransitionDelta(fingerDy: Float): Float {
                if (fingerDy == 0f) return 0f
                val before = latestPosition
                val requested = before + fingerDy / dragDistancePx
                val after = requested.coerceIn(-1f, 1f)
                val consumedFingerDy = (after - before) * dragDistancePx
                if (consumedFingerDy != 0f) {
                    snapInteractivePosition(after)
                }
                return consumedFingerDy
            }

            fun maybeHideImeForSearchReturn() {
                if (!verticalTransition.returningFromSearch || searchCoordinator.state.pageReturnImeSuppressed) return
                val returnedDistancePx = (
                    (verticalTransition.searchReturnStartPosition - latestPosition) * dragDistancePx
                ).coerceAtLeast(0f)
                if (returnedDistancePx + 0.5f >= searchReturnImeHideThresholdPx) {
                    searchCoordinator.dispatch(SearchIntent.SuppressImeForPageReturn)
                    // Suppression is the hide policy. The 5.5% presentation gate only
                    // controls when Input may show, so crossing it in reverse cannot race
                    // an in-flight IME Show with an unrelated Hide.
                }
            }

            var interactionOwner: Long? = null
            try {
                detectVerticalDragGestures(
                    onDragStart = {
                        drawerFlingJob?.cancel()
                        interactionOwner = verticalTransition.beginInteractive()
                        velocityTracker = VelocityTracker()
                        latestPosition = verticalPosition.value.coerceIn(-1f, 1f)
                        gestureStartPage = verticalTransition.stablePage
                        drawerScrollEngaged = false
                    },
                    onVerticalDrag = drag@{ change, dragAmount ->
                        val owned = interactionOwner ?: return@drag
                        if (!verticalTransition.ownsInteraction(owned)) return@drag
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        change.consume()

                        // V41: a Home -> Search drag may reverse before Search ever becomes
                        // the stable page. The compact return visual must start at the
                        // direction reversal itself, not after the Search settle finishes.
                        if (dragAmount < 0f && latestPosition > VerticalPage.Home.anchor) {
                            verticalTransition.beginSearchReturn(latestPosition)
                        }

                        var remainingDy = dragAmount

                        if (remainingDy < 0f) {
                            // Finger up: the page transition owns the movement until All Apps
                            // is fully open. Any overflow then becomes forward grid scrolling.
                            if (latestPosition > VerticalPage.AllApps.anchor) {
                                remainingDy -= consumeTransitionDelta(remainingDy)
                            }
                            if (remainingDy < 0f && latestPosition <= VerticalPage.AllApps.anchor) {
                                val consumedScroll = drawerGridState.dispatchRawDelta(-remainingDy)
                                if (consumedScroll > 0f) drawerScrollEngaged = true
                            }
                        } else if (remainingDy > 0f) {
                            // Finger down while the drawer is fully open: unwind the grid first.
                            // Once it reaches the top, feed the unconsumed distance back into
                            // All Apps -> Home (and, if the finger keeps going, Home -> Search).
                            if (latestPosition <= VerticalPage.AllApps.anchor && drawerGridState.canScrollBackward) {
                                val requestedScroll = -remainingDy
                                val consumedScroll = drawerGridState.dispatchRawDelta(requestedScroll)
                                val consumedFingerDy = -consumedScroll
                                remainingDy = (remainingDy - consumedFingerDy).coerceAtLeast(0f)
                                if (kotlin.math.abs(consumedScroll) > 0f) drawerScrollEngaged = true
                            }
                            if (remainingDy > 0f && !drawerGridState.canScrollBackward) {
                                consumeTransitionDelta(remainingDy)
                            }
                        }

                        if (dragAmount < 0f) {
                            maybeHideImeForSearchReturn()
                        }
                    },
                    onDragCancel = {
                        val owned = interactionOwner
                        interactionOwner = null
                        if (owned != null && scope.isActive && verticalTransition.ownsInteraction(owned)) {
                            val page = if (drawerGridState.canScrollBackward && latestPosition <= VerticalPage.AllApps.anchor) {
                                VerticalPage.AllApps
                            } else {
                                verticalTargetForRelease(
                                    position = latestPosition,
                                    velocityY = 0f,
                                    velocityThreshold = openVelocityPx,
                                    settleThreshold = settleThreshold,
                                )
                            }
                            if (page == VerticalPage.Search) searchCoordinator.dispatch(SearchIntent.AllowImeAfterPageReturn)
                            settleTo(page, startValue = latestPosition)
                        }
                    },
                    onDragEnd = end@{
                        val owned = interactionOwner ?: return@end
                        interactionOwner = null
                        if (!scope.isActive || !verticalTransition.ownsInteraction(owned)) return@end
                        val velocityY = runCatching {
                            velocityTracker.calculateVelocity().y
                        }.getOrDefault(0f)

                        val drawerOwnsRelease =
                            latestPosition <= VerticalPage.AllApps.anchor && drawerGridState.canScrollBackward
                        val page = if (drawerOwnsRelease) {
                            VerticalPage.AllApps
                        } else {
                            verticalTargetForRelease(
                                position = latestPosition,
                                velocityY = velocityY,
                                velocityThreshold = openVelocityPx,
                                settleThreshold = settleThreshold,
                            )
                        }

                        if (launcherPreferences.vibrationOnGesture && page != gestureStartPage) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        if (page == VerticalPage.Search) searchCoordinator.dispatch(SearchIntent.AllowImeAfterPageReturn)
                        settleTo(page, startValue = latestPosition)

                        // V46: the root detector owns normal drawer scrolling too, so preserve
                        // release momentum in both directions. A finger-up fling scrolls the
                        // grid forward; a finger-down fling scrolls it backward toward the top.
                        // Once the grid hits an edge the fling ends instead of leaking into a
                        // second page transition after the finger has already been released.
                        val drawerFlingTriggerPx = (openVelocityPx * 0.35f)
                            .coerceAtLeast(DRAWER_HANDOFF_FLING_STOP_VELOCITY_PX * 2f)
                        val initialGridVelocity = -velocityY
                        val canFlingGrid = if (initialGridVelocity > 0f) {
                            drawerGridState.canScrollForward
                        } else {
                            drawerGridState.canScrollBackward
                        }
                        if (
                            page == VerticalPage.AllApps &&
                            drawerScrollEngaged &&
                            kotlin.math.abs(initialGridVelocity) >= drawerFlingTriggerPx &&
                            canFlingGrid
                        ) {
                            drawerFlingJob?.cancel()
                            drawerFlingJob = scope.launch {
                                drawerGridState.scroll {
                                    var velocity = initialGridVelocity
                                    var lastFrameNanos = System.nanoTime()
                                    while (
                                        kotlin.math.abs(velocity) > DRAWER_HANDOFF_FLING_STOP_VELOCITY_PX &&
                                        (if (velocity > 0f) {
                                            drawerGridState.canScrollForward
                                        } else {
                                            drawerGridState.canScrollBackward
                                        })
                                    ) {
                                        delay(DRAWER_HANDOFF_FLING_FRAME_MS)
                                        val now = System.nanoTime()
                                        val dtSeconds = ((now - lastFrameNanos) / 1_000_000_000.0)
                                            .toFloat()
                                            .coerceIn(0f, 0.05f)
                                        lastFrameNanos = now
                                        val requested = velocity * dtSeconds
                                        val consumed = scrollBy(requested)
                                        if (kotlin.math.abs(requested - consumed) > 0.5f) break
                                        val decay = kotlin.math.exp(
                                            (-DRAWER_HANDOFF_FLING_FRICTION * dtSeconds).toDouble(),
                                        ).toFloat()
                                        velocity *= decay
                                    }
                                }
                            }
                        }
                        },
                )
            } finally {
                val owned = interactionOwner
                interactionOwner = null
                if (owned != null && scope.isActive && verticalTransition.ownsInteraction(owned)) {
                    val page = verticalTargetForRelease(
                        position = latestPosition,
                        velocityY = 0f,
                        velocityThreshold = openVelocityPx,
                        settleThreshold = settleThreshold,
                    )
                    if (page == VerticalPage.Search) searchCoordinator.dispatch(SearchIntent.AllowImeAfterPageReturn)
                    settleTo(page, startValue = latestPosition)
                }
            }
        }
    }

    // V61: Search's local bottom rubber-band owns motion only until its configured
    // Home threshold is crossed. GestureCoordinator then becomes the single owner of
    // that exact pointer stream through page drag, reversal, release and settle target.
    // LauncherRoot forwards pointer frames and applies page-transition commands only.
    val searchReturnHandoffEnabled =
        !showDesktopSheet && activeDialog == null &&
            !showSearchSettings && !showLauncherSettings && launcherPreferences.enableSwipeGestures
    val searchReturnHandoffModifier = if (!searchReturnHandoffEnabled) {
        Modifier
    } else {
        Modifier.pointerInput(
            gestureCoordinator,
            searchCoordinator,
            verticalTransition,
            dragDistancePx,
            settleThreshold,
            openVelocityPx,
        ) {
            val touchSlop = viewConfiguration.touchSlop
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val searchSessionId = searchCoordinator.state.sessionId
                val eligibleAtDown =
                    searchCoordinator.state.hasQuery &&
                        searchCoordinator.state.phase == SearchPhase.Active

                val pointerGeneration = gestureCoordinator.beginSearchBottomPointer(
                    pointerId = down.id.value,
                    searchSessionId = searchSessionId,
                    eligible = eligibleAtDown,
                )
                if (pointerGeneration == null) return@awaitEachGesture

                var settled = false
                var interactionOwner: Long? = null

                fun applyFrame(
                    frame: SearchReturnGestureFrame,
                    consume: (() -> Unit)? = null,
                ) {
                    frame.beginInteractiveAt?.let { start ->
                        interactionOwner = verticalTransition.beginInteractive()
                        verticalTransition.beginSearchReturn(start)
                    }
                    if (frame.consumePointer) consume?.invoke()
                    frame.dragPosition?.let { position ->
                        // Once the threshold transfers ownership, all later motion is
                        // page-space motion. The Search-local rubber-band remains local
                        // and rebounds only if this same gesture ultimately returns to Search.
                        verticalTransition.snapTo(
                            positionValue = position,
                            pullOffset = 0f,
                        )
                    }

                    val target = frame.finishTarget ?: return
                    val latestPosition =
                        frame.finishPosition ?: verticalPosition.value.coerceIn(0f, 1f)
                    val page = when (target) {
                        SearchReturnSettleTarget.Home -> VerticalPage.Home
                        SearchReturnSettleTarget.Search -> VerticalPage.Search
                    }

                    if (page == VerticalPage.Search) {
                        searchCoordinator.dispatch(SearchIntent.ResumeActive)
                        searchCoordinator.dispatch(SearchIntent.AllowImeAfterPageReturn)
                        // Preserve the existing same-pointer reversal experience:
                        // returning to Search explicitly targets Input again.
                        searchCoordinator.dispatch(SearchIntent.EnterInputMode)
                        searchCoordinator.onRelease(
                            scope = scope,
                            reboundDurationMs = launcherPreferences.searchReboundDurationMs,
                        )
                    }
                    settleTo(page, startValue = latestPosition)
                    settled = true
                }

                try {
                    while (!settled) {
                        // Final pass lets SearchContent commit the rubber-band threshold
                        // first. GestureCoordinator can therefore observe a threshold commit
                        // and a pointer-up in the same event without any armed/release race.
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        if (interactionOwner?.let { !verticalTransition.ownsInteraction(it) } == true) break
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val frame = gestureCoordinator.onSearchPointerFrame(
                            pointerId = down.id.value,
                            searchSessionId = searchSessionId,
                            position = change.position,
                            uptimeMillis = change.uptimeMillis,
                            pressed = change.pressed,
                            currentPagePosition = verticalPosition.value.coerceIn(0f, 1f),
                            dragDistancePx = dragDistancePx,
                            touchSlopPx = touchSlop,
                            settleThreshold = settleThreshold,
                            openVelocityPx = openVelocityPx,
                        )
                        applyFrame(frame) { change.consume() }
                        if (!change.pressed) break
                    }
                } finally {
                    // Cancellation of awaitPointerEvent skips ordinary code after the loop.
                    // Clear ownership even when the UI scope itself is already cancelled.
                    val frame = gestureCoordinator.cancelSearchPointer(
                        pointerId = down.id.value,
                        searchSessionId = searchSessionId,
                        currentPagePosition = verticalPosition.value.coerceIn(0f, 1f),
                        generation = pointerGeneration,
                    )
                    val stillOwnsPage = interactionOwner?.let(verticalTransition::ownsInteraction) ?: true
                    if (!settled && scope.isActive && !verticalTransition.isDisposed &&
                        stillOwnsPage && searchCoordinator.state.sessionId == searchSessionId
                    ) {
                        applyFrame(frame)
                    }
                }
            }
        }
    }

    val blurStrength = if (launcherPreferences.blurEnabled) {
        launcherPreferences.blurStrengthPercent.coerceIn(0, 100) / 100f
    } else {
        0f
    }
    // Search and Drawer share six pre-rasterized backdrop levels. Blur is no longer
    // evaluated on the live Home layer during a gesture.
    val homeBlurIndex by remember(verticalTransition, blurStrength) {
        derivedStateOf {
            val signed = verticalPosition.value.coerceIn(-1f, 1f)
            if (signed < 0f) {
                composeBlurLevel(-signed, blurStrength)
            } else {
                // Search follows the existing Drawer curve, including the return trip.
                composeBlurLevel(signed, blurStrength)
            }
        }
    }

    val homeCacheUpdatesEnabled by remember(verticalTransition) {
        derivedStateOf {
            kotlin.math.abs(verticalPosition.value) < 0.001f &&
                verticalTransition.targetPage == VerticalPage.Home &&
                !verticalTransition.returningFromSearch
        }
    }

    // Background dim follows the same gesture progress as blur. It is a cheap
    // compositor overlay, not part of the cached textures. The strength is a
    // global appearance setting (0..50%).
    val homeBackgroundDimFactor =
        launcherPreferences.homeBackgroundDimPercent.coerceIn(0, 50) / 100f
    val homeBackgroundDim by remember(verticalPosition, homeBackgroundDimFactor) {
        derivedStateOf {
            (kotlin.math.abs(verticalPosition.value).coerceIn(0f, 1f) * homeBackgroundDimFactor)
        }
    }
    val staticHomeRevision = remember(
        homePreferences,
        currentHomePage,
        apps,
        widgetIds,
        widgetHeights,
        homeWallpaper.revision,
    ) { Any() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(gestureModifier)
            .then(searchReturnHandoffModifier),
    ) {
        HomeWallpaperLayer(homeWallpaper, Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val signed = verticalPosition.value.coerceIn(-1f, 1f)
                    if (signed < 0f) {
                        // All Apps behaves like a floating drawer. Home stays
                        // physically steady; only the quantized Compose blur and drawer
                        // scrim provide depth. Do not perturb scale/alpha merely to keep
                        // this layer dirty: a static Home should be allowed to stay static.
                        scaleX = editScale
                        scaleY = editScale
                        translationY = 0f
                        alpha = 1f
                        transformOrigin = TransformOrigin.Center
                    } else {
                        // Match Drawer background compositing. Fading this cache would
                        // reveal the sharp wallpaper underneath and weaken Search blur.
                        // Search foreground animations remain on their own layer below.
                        scaleX = editScale
                        scaleY = editScale
                        translationY = 0f
                        alpha = 1f
                        transformOrigin = TransformOrigin.Center
                    }
                },
        ) {
            HomeScene(
                wallpaper = homeWallpaper,
                staticRevision = staticHomeRevision,
                preferences = homePreferences,
                pageRequest = homePageRequest,
                currentPage = currentHomePage,
                clockValue = minuteClock,
                blurStrength = blurStrength,
                blurLevel = homeBlurIndex,
                backgroundDimAmount = homeBackgroundDim,
                updatesEnabled = homeCacheUpdatesEnabled && !showDesktopSheet &&
                    !showLauncherSettings && !showSearchSettings && activeDialog == null,
                onLongPressEmptyArea = {
                    if (verticalTransition.targetPage == VerticalPage.Home &&
                        kotlin.math.abs(verticalPosition.value) < 0.01f) showDesktopSheet = true
                },
                onCurrentPageChanged = { page ->
                    currentHomePage = page
                    settingsState.home = visitHomePage(settingsState.home, page)
                },
            )
        }

        run {
            // Search may stay composed while a cancelled/interactive transition cleans up,
            // but it must not sit above All Apps unless Search is actually on the active
            // side of the signed transition. This prevents an invisible Search layer from
            // consuming drawer gestures during Home -> All Apps.
            val emptySearchPageGestureModifier = if (
                searchOwnsForeground &&
                !searchCoordinator.state.hasQuery &&
                !showSearchSettings &&
                !showLauncherSettings &&
                launcherPreferences.enableSwipeGestures
            ) {
                Modifier.pointerInput(
                    homePullLimitPx,
                    dragDistancePx,
                    settleThreshold,
                    openVelocityPx,
                    searchReturnImeHideThresholdPx,
                ) {
                    val touchSlop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var lastY = down.position.y
                        var accumulatedY = 0f
                        var direction = 0 // 1 = down, -1 = up
                        // Stable empty-Search downward pulls now belong to SearchContent.
                        // Mode 2 preserves the existing page-level Search <-> Home path.
                        var mode = 0
                        var gestureStartPosition = verticalPosition.value.coerceIn(-1f, 1f)
                        var latestPosition = gestureStartPosition
                        var gestureStartTransitionOffset = transitionPullOffset.value
                        var gestureStartPage = verticalTransition.stablePage
                        val velocityTracker = VelocityTracker().also {
                            it.addPosition(down.uptimeMillis, down.position)
                        }

                        var interactionOwner: Long? = null
                        var finished = false
                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (interactionOwner?.let { !verticalTransition.ownsInteraction(it) } == true) {
                                    mode = 0
                                    break
                                }
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break

                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                val dy = change.position.y - lastY
                                lastY = change.position.y
                                accumulatedY += dy

                                if (direction == 0) {
                                    direction = when {
                                        accumulatedY > touchSlop -> 1
                                        accumulatedY < -touchSlop -> -1
                                        else -> 0
                                    }
                                    if (direction != 0) {
                                        gestureStartPosition = verticalPosition.value.coerceIn(-1f, 1f)
                                        latestPosition = gestureStartPosition
                                        gestureStartTransitionOffset = transitionPullOffset.value
                                        gestureStartPage = verticalTransition.stablePage

                                        val stableSearch =
                                            gestureStartPage == VerticalPage.Search &&
                                                gestureStartPosition >= SEARCH_STABLE_PROGRESS &&
                                                kotlin.math.abs(gestureStartTransitionOffset) < 1f

                                        // Do not consume a stable Search downward pull here.
                                        // SearchContent's NestedScroll owns the result-only
                                        // rubber-band and the unified IME reveal threshold.
                                        mode = if (stableSearch && direction == 1) {
                                            0
                                        } else {
                                            if (direction < 0 && gestureStartPosition > VerticalPage.Home.anchor) {
                                                verticalTransition.beginSearchReturn(gestureStartPosition)
                                            }
                                            // A new page-level gesture supersedes any
                                            // Search-local rebound left from the previous touch.
                                            searchCoordinator.clearBoundaryOverscroll()
                                            interactionOwner = verticalTransition.beginInteractive()
                                            2
                                        }
                                    }
                                }

                                if (mode == 2) {
                                    change.consume()
                                    if (dy < 0f && latestPosition > VerticalPage.Home.anchor) {
                                        verticalTransition.beginSearchReturn(latestPosition)
                                    }
                                    latestPosition = (
                                        gestureStartPosition + accumulatedY / dragDistancePx
                                    ).coerceIn(-1f, 1f)
                                    if (
                                        dy < 0f &&
                                        verticalTransition.returningFromSearch &&
                                        !searchCoordinator.state.pageReturnImeSuppressed
                                    ) {
                                        val returnedDistancePx = (
                                            (verticalTransition.searchReturnStartPosition - latestPosition) *
                                                dragDistancePx
                                        ).coerceAtLeast(0f)
                                        if (returnedDistancePx + 0.5f >= searchReturnImeHideThresholdPx) {
                                            searchCoordinator.dispatch(SearchIntent.SuppressImeForPageReturn)
                                        }
                                    }
                                    val liveOffset = (
                                        gestureStartTransitionOffset +
                                            signedRubberBandDistance(accumulatedY, homePullLimitPx)
                                    ).coerceIn(-homePullLimitPx, homePullLimitPx)
                                    verticalTransition.snapTo(
                                        positionValue = latestPosition,
                                        pullOffset = liveOffset,
                                    )
                                }
                            }

                            if (mode == 2) {
                                val velocityY = runCatching {
                                    velocityTracker.calculateVelocity().y
                                }.getOrDefault(0f)
                                val page = verticalTargetForRelease(
                                    position = latestPosition,
                                    velocityY = velocityY,
                                    velocityThreshold = openVelocityPx,
                                    settleThreshold = settleThreshold,
                                )
                                if (launcherPreferences.vibrationOnGesture && page != gestureStartPage) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                if (page == VerticalPage.Search) {
                                    searchCoordinator.dispatch(SearchIntent.AllowImeAfterPageReturn)
                                    searchCoordinator.dispatch(SearchIntent.EnterInputMode)
                                }
                                settleTo(page, startValue = latestPosition)
                            }
                            finished = true
                        } finally {
                            if (!finished && scope.isActive && !verticalTransition.isDisposed) {
                                val owned = interactionOwner
                                if (owned != null && verticalTransition.ownsInteraction(owned)) {
                                    val page = verticalTargetForRelease(
                                        position = latestPosition,
                                        velocityY = 0f,
                                        velocityThreshold = openVelocityPx,
                                        settleThreshold = settleThreshold,
                                    )
                                    if (page == VerticalPage.Search) {
                                        searchCoordinator.dispatch(SearchIntent.AllowImeAfterPageReturn)
                                        searchCoordinator.dispatch(SearchIntent.EnterInputMode)
                                    }
                                    settleTo(page, startValue = latestPosition)
                                }
                            }
                        }
                    }
                }
            } else {
                Modifier
            }

            // Search backdrop remains fixed to the physical screen. Search-local
            // result rubber-band motion is owned inside SearchContent.
            if (false && searchLayerActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                        .drawBehind {
                            val p = verticalPosition.value.coerceIn(0f, 1f)
                            val delayed = ((p - 0.10f) / 0.90f).coerceIn(0f, 1f)
                            val fade = delayed * delayed * (3f - 2f * delayed)
                            if (fade > 0f) {
                                val coverageProgress = easeOutCubic(fade)
                                val coverage = 0.32f + 0.78f * coverageProgress
                                val brush = Brush.verticalGradient(
                                    0f to SearchScrim.copy(alpha = 0.58f * fade),
                                    0.58f to SearchScrim.copy(alpha = 0.44f * fade),
                                    0.82f to SearchScrim.copy(alpha = 0.20f * fade),
                                    1f to SearchScrim.copy(alpha = 0f),
                                    startY = 0f,
                                    endY = size.height * coverage,
                                )
                                drawRect(brush = brush)
                            }
                        },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (searchOwnsForeground) 2f else -0.5f)
                    .then(emptySearchPageGestureModifier)
                    .graphicsLayer {
                        val p = verticalPosition.value.coerceIn(0f, 1f)
                        if (verticalTransition.returningFromSearch) {
                            // During the return gesture the root Search plane stays in
                            // screen space. SearchContent owns the small 28/32dp motion,
                            // local shrink and staged fade so the full page never flies
                            // off the top edge.
                            alpha = 1f
                            translationY = 0f
                            scaleX = 1f
                            scaleY = 1f
                            transformOrigin = TransformOrigin.Center
                        } else {
                            val eased = easeOutCubic(p)
                            alpha = eased
                            translationY = transitionPullOffset.value
                            val scale = SEARCH_SCALE_MIN + (1f - SEARCH_SCALE_MIN) * eased
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0.5f, 0.15f)
                        }
                    },
            ) {
                SearchContent(
                    apps = searchApps,
                    hiddenComponentKeys = appCustomizations.hiddenComponentKeys,
                    preferences = searchPreferences,
                    imePresentationReady = searchImePresentationReady,
                    imeControlEnabled = !showSearchSettings,
                    coordinator = searchCoordinator,
                    reboundDurationMs = launcherPreferences.searchReboundDurationMs,
                    onReturnHomeThreshold = {
                        // Search decides *when* the configured threshold is crossed.
                        // Search enters Returning only after GestureCoordinator atomically
                        // accepts ownership of the current pointer session.
                        val accepted = gestureCoordinator.commitSearchBottomReturn(
                            searchSessionId = searchCoordinator.state.sessionId,
                        )
                        if (accepted) {
                            searchCoordinator.dispatch(SearchIntent.PageReturnStarted)
                            searchCoordinator.dispatch(SearchIntent.SuppressImeForPageReturn)
                        }
                        accepted
                    },
                    transitionProgress = { verticalPosition.value.coerceIn(0f, 1f) },
                    returnProgress = {
                        if (verticalTransition.returningFromSearch) {
                            verticalTransition.searchReturnProgress(verticalPosition.value)
                        } else {
                            0f
                        }
                    },
                    onAppSelected = { app ->
                        scope.launch { verticalTransition.reset() }
                        onLaunchApp(app)
                    },
                    onShareApp = ::shareApp,
                    onAppInfo = ::openAppInfo,
                    onRenameApp = ::renameApp,
                    onSetAppHidden = ::setAppHidden,
                    onOpenAppStore = ::openAppStore,
                    onUninstallApp = ::uninstallApp,
                    onOpenSettings = { showSearchSettings = true },
                    targets = searchTargets,
                    groups = searchGroups,
                    expandedGroupIds = expandedSearchGroups,
                    onToggleGroup = ::toggleSearchGroup,
                    onSearchTarget = ::searchWithTarget,
                    onOpenSystemSettings = { launchIntent(Intent(Settings.ACTION_SETTINGS)) },
                )
            }
        }

        if (drawerLayerActive) {
            // V67 validation: legacy drawer scrim is intentionally disabled so the
            // cached Home pixels are directly visible behind the foreground.
            // All Apps backdrop: this is intentionally separate from Search's scrim.
            // It fades in with the upward gesture while Home progressively blurs.
            // The lower part is slightly darker so the floating drawer feels anchored
            // to the direction it is arriving from rather than covering Home at once.
            if (false) Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1.25f)
                    .drawWithCache {
                        val brush = Brush.verticalGradient(
                            0f to SearchScrim.copy(alpha = DRAWER_SCRIM_TOP_ALPHA),
                            0.58f to SearchScrim.copy(alpha = DRAWER_SCRIM_MID_ALPHA),
                            1f to SearchScrim.copy(alpha = DRAWER_SCRIM_BOTTOM_ALPHA),
                        )
                        onDrawBehind {
                            val p = (-verticalPosition.value).coerceIn(0f, 1f)
                            val delayed = ((p - DRAWER_SCRIM_DELAY) / (1f - DRAWER_SCRIM_DELAY))
                                .coerceIn(0f, 1f)
                            val fade = smoothStep(delayed)
                            if (fade > 0f) drawRect(brush = brush, alpha = fade)
                        }
                    },
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1.5f)
                    .graphicsLayer {
                        val p = (-verticalPosition.value).coerceIn(0f, 1f)
                        // The drawer first becomes visible with its top about 200 px
                        // below the physical screen top, then smoothly approaches its
                        // final position as the upward gesture progresses.
                        val approach = smoothStep(p)
                        val reveal = smoothStep(
                            (p / DRAWER_REVEAL_COMPLETE_PROGRESS).coerceIn(0f, 1f),
                        )
                        alpha = reveal
                        translationY = DRAWER_START_TOP_PX * (1f - approach)
                        val scale = DRAWER_FLOAT_SCALE_MIN +
                            (1f - DRAWER_FLOAT_SCALE_MIN) * approach
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    },
            ) {
                AllAppsContent(
                    catalog = if (drawerHiddenOnly) hiddenDrawerCatalog else visibleDrawerCatalog,
                    showHiddenOnly = drawerHiddenOnly,
                    openSheetRequest = drawerSheetRequest,
                    sortMode = drawerSortMode,
                    preferences = launcherPreferences,
                    gridState = drawerGridState,
                    // V46: LauncherRoot owns all vertical Drawer/Home motion, including
                    // grid scrolling. Keeping LazyVerticalGrid's native vertical gesture
                    // disabled removes the controller hand-off between Drawer and Home;
                    // taps, long-presses, the header controls and alphabet index remain
                    // interactive because only LazyVerticalGrid's native scroll is gated.
                    nativeGridScrollEnabled = false,
                    onSortModeChange = { mode ->
                        drawerSortMode = mode
                        updateLauncherPreferences(launcherPreferences.copy(drawerSortMode = mode.name))
                    },
                    onLaunchApp = { app ->
                        resetDrawerImmediately()
                        onLaunchApp(app)
                    },
                    onOpenSearch = {
                        drawerHiddenOnly = false
                        scope.launch {
                            verticalTransition.reset()
                            openSearch()
                        }
                    },
                    onOpenDrawerSettings = {
                        resetDrawerImmediately()
                        launcherSettingsInitialSection = LauncherSettingsSection.Apps
                        launcherSettingsFromDrawerSheet = true
                        showLauncherSettings = true
                    },
                    onOpenFullAppsSettings = {
                        resetDrawerImmediately()
                        launcherSettingsInitialSection = LauncherSettingsSection.Apps
                        launcherSettingsFromDrawerSheet = true
                        showLauncherSettings = true
                    },
                    onShowHiddenApps = { drawerHiddenOnly = true },
                    onShowAllApps = { drawerHiddenOnly = false },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }


        if (showSearchSettings) {
            ScaledUiContent(uiScale) {
                SearchSettingsScreen(
                    preferences = searchPreferences,
                    targets = searchTargets,
                    groups = searchGroups,
                    apps = customizedApps,
                    onBack = { showSearchSettings = false },
                    onChange = ::updateSearchPreferences,
                    onTargetsChange = ::updateSearchTargets,
                    onGroupsChange = ::updateSearchGroups,
                    modifier = Modifier.fillMaxSize().zIndex(SETTINGS_OVERLAY_Z),
                )
            }
        }

        if (showLauncherSettings) {
            ScaledUiContent(uiScale) {
                LauncherSettingsScreen(
                    homePreferences = homePreferences,
                    searchPreferences = searchPreferences,
                    launcherPreferences = launcherPreferences,
                    apps = customizedApps,
                    hiddenComponentKeys = appCustomizations.hiddenComponentKeys,
                    searchTargets = searchTargets,
                    onSearchTargetsChange = ::updateSearchTargets,
                    searchGroups = searchGroups,
                    onSearchGroupsChange = ::updateSearchGroups,
                    initialSection = launcherSettingsInitialSection,
                    onClose = {
                        showLauncherSettings = false
                        launcherSettingsInitialSection = null
                        launcherSettingsFromDrawerSheet = false
                    },
                    onReturnToCaller = if (launcherSettingsFromDrawerSheet) {
                        { returnToDrawerSheet() }
                    } else {
                        null
                    },
                    onHomeChange = ::updateHomePreferences,
                    onSearchChange = ::updateSearchPreferences,
                    onLauncherChange = ::updateLauncherPreferences,
                    onSetAppHidden = ::setAppHidden,
                    onOpenWallpaper = {
                        showLauncherSettings = false
                        launcherSettingsInitialSection = null
                        launcherSettingsFromDrawerSheet = false
                        onOpenWallpaper()
                    },
                    onAddFolder = {
                        showLauncherSettings = false
                        launcherSettingsInitialSection = null
                        launcherSettingsFromDrawerSheet = false
                        activeDialog = DesktopDialog.AddFolder
                    },
                    onAddWidget = {
                        showLauncherSettings = false
                        launcherSettingsInitialSection = null
                        launcherSettingsFromDrawerSheet = false
                        onAddWidget()
                    },
                    modifier = Modifier.fillMaxSize().zIndex(SETTINGS_OVERLAY_Z),
                )
            }
        }
    }

    if (showDesktopSheet) {
        ScaledUiContent(uiScale) {
            DesktopSettingsSheet(
                preferences = homePreferences,
                onDismiss = { showDesktopSheet = false },
                onToggleLock = { updateHomePreferences(homePreferences.copy(layoutLocked = !homePreferences.layoutLocked)) },
                onWallpaper = {
                    showDesktopSheet = false
                    onOpenWallpaper()
                },
                onAppearance = {
                    showDesktopSheet = false
                    launcherSettingsInitialSection = LauncherSettingsSection.Appearance
                    launcherSettingsFromDrawerSheet = false
                    showLauncherSettings = true
                },
                onHomeSettings = {
                    showDesktopSheet = false
                    launcherSettingsInitialSection = LauncherSettingsSection.Home
                    launcherSettingsFromDrawerSheet = false
                    showLauncherSettings = true
                },
                onSwipeUpToSettings = {
                    showDesktopSheet = false
                    launcherSettingsInitialSection = null
                    launcherSettingsFromDrawerSheet = false
                    showLauncherSettings = true
                },
            )
        }
    }

    when (activeDialog) {
        DesktopDialog.AddApp -> AddAppDialog(
            apps = apps,
            onDismiss = { activeDialog = null },
            onSelected = { app ->
                addAppToHome(app)
                activeDialog = null
            },
        )

        DesktopDialog.AddFolder -> AddFolderDialog(
            apps = apps,
            onDismiss = { activeDialog = null },
            onCreate = { name, appKeys ->
                updateHomePreferences(
                    homePreferences.copy(
                        folders = homePreferences.folders + HomeFolder(
                            id = System.currentTimeMillis(),
                            name = name,
                            appKeys = appKeys,
                        ),
                    ).let { updated ->
                        val newFolder = updated.folders.last()
                        updated.copy(
                            folderPageById = updated.folderPageById + (newFolder.id to currentHomePage),
                            pageCount = maxOf(updated.pageCount, currentHomePage + 1),
                            lastHomePage = currentHomePage,
                        )
                    },
                )
                activeDialog = null
            },
        )

        DesktopDialog.Appearance -> AppearanceDialog(
            preferences = homePreferences,
            onDismiss = { activeDialog = null },
            onUpdate = ::updateHomePreferences,
        )

        DesktopDialog.HomeSettings -> HomeSettingsDialog(
            preferences = homePreferences,
            onDismiss = { activeDialog = null },
            onUpdate = ::updateHomePreferences,
        )

        null -> Unit
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private enum class DesktopDialog {
    AddApp,
    AddFolder,
    Appearance,
    HomeSettings,
}

private fun easeOutCubic(value: Float): Float {
    val inverse = 1f - value.coerceIn(0f, 1f)
    return 1f - inverse * inverse * inverse
}

private fun smoothStep(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun rubberBandDistance(rawDistance: Float, limit: Float): Float {
    if (rawDistance <= 0f || limit <= 0f) return 0f
    // Initial slope is 1:1. Resistance continuously increases and the
    // visible displacement asymptotically approaches [limit].
    return (limit * rawDistance) / (limit + rawDistance)
}

private fun verticalTargetForRelease(
    position: Float,
    velocityY: Float,
    velocityThreshold: Float,
    settleThreshold: Float,
): VerticalPage {
    val p = position.coerceIn(-1f, 1f)

    // A fast gesture settles in the direction of travel, but never skips over
    // Home from the opposite side. This prevents a short reversal from jumping
    // directly from Search to All Apps (or vice versa).
    if (velocityY >= velocityThreshold) {
        return if (p >= 0f) VerticalPage.Search else VerticalPage.Home
    }
    if (velocityY <= -velocityThreshold) {
        return if (p <= 0f) VerticalPage.AllApps else VerticalPage.Home
    }

    return when {
        p >= settleThreshold -> VerticalPage.Search
        p <= -settleThreshold -> VerticalPage.AllApps
        else -> VerticalPage.Home
    }
}

private fun signedRubberBandDistance(rawDistance: Float, limit: Float): Float {
    if (rawDistance == 0f) return 0f
    val sign = if (rawDistance > 0f) 1f else -1f
    return rubberBandDistance(kotlin.math.abs(rawDistance), limit) * sign
}


private fun composeBlurLevel(progress: Float, strength: Float): Int {
    if (strength <= 0f) return 0
    val linear = (progress.coerceIn(0f, 1f) / HOME_BLUR_SATURATION_PROGRESS).coerceIn(0f, 1f)
    val eased = easeOutCubic(linear)
    return (eased * BLUR_LEVELS).toInt().coerceIn(0, BLUR_LEVELS)
}


private const val DRAWER_HANDOFF_FLING_FRAME_MS = 16L
private const val DRAWER_HANDOFF_FLING_FRICTION = 5.5f
private const val DRAWER_HANDOFF_FLING_STOP_VELOCITY_PX = 45f
private const val KEYBOARD_TRIGGER_PROGRESS = 0.055f
private const val HOME_PULL_LIMIT_RATIO = 0.34f
private const val HOME_BLUR_SATURATION_PROGRESS = 0.58f
private const val SEARCH_SCALE_MIN = 0.985f
// Home -> All Apps floating-drawer visual mapping.
private const val DRAWER_START_TOP_PX = 200f
private const val DRAWER_REVEAL_COMPLETE_PROGRESS = 0.62f
private const val DRAWER_FLOAT_SCALE_MIN = 0.985f
private const val DRAWER_SCRIM_DELAY = 0.035f
private const val DRAWER_SCRIM_TOP_ALPHA = 0.12f
private const val DRAWER_SCRIM_MID_ALPHA = 0.22f
private const val DRAWER_SCRIM_BOTTOM_ALPHA = 0.34f
// Above Search (2f), the search scrim (1f) and the drawer layers (1.25/1.5), so the
// settings overlays always cover the page they were opened from.
private const val SETTINGS_OVERLAY_Z = 3f
private const val SEARCH_STABLE_PROGRESS = 0.995f
private const val SEARCH_RETURN_SETTLE_MS = 110
private const val BLUR_LEVELS = HOME_BACKDROP_BLUR_LEVELS
