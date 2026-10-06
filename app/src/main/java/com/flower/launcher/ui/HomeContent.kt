package com.flower.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flower.launcher.apps.InstalledApp
import com.flower.launcher.home.CLOCK_FONTS
import com.flower.launcher.home.HomePreferences
import com.flower.launcher.home.HomePageRequest
import com.flower.launcher.ui.theme.OnHomePrimary
import com.flower.launcher.ui.theme.OnHomeSecondary
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private data class ClockPlacement(val offset: IntOffset, val size: IntSize)

/** Static and dynamic surfaces are siblings: no clock-shaped cut-out in the wallpaper. */
@Composable
internal fun HomeScene(
    wallpaper: HomeWallpaperSnapshot,
    staticRevision: Any,
    preferences: HomePreferences,
    pageRequest: HomePageRequest,
    currentPage: Int,
    clockValue: MinuteClockValue,
    blurStrength: Float,
    blurLevel: Int,
    backgroundDimAmount: Float = HOME_BACKDROP_DIM_DEFAULT,
    updatesEnabled: Boolean,
    onLongPressEmptyArea: () -> Unit,
    onCurrentPageChanged: (Int) -> Unit,
) {
    val density = LocalDensity.current
    var sceneCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var clockCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var placement by remember { mutableStateOf<ClockPlacement?>(null) }
    fun updatePlacement() {
        val scene = sceneCoordinates ?: return
        val clock = clockCoordinates ?: return
        if (!scene.isAttached || !clock.isAttached) return
        val origin = scene.localPositionOf(clock, Offset.Zero)
        placement = ClockPlacement(IntOffset(origin.x.roundToInt(), origin.y.roundToInt()), clock.size)
    }
    Box(Modifier.fillMaxSize().onGloballyPositioned {
        sceneCoordinates = it
        updatePlacement()
    }) {
        CachedHomeSurface(
            cacheName = "StaticHomeCache",
            contentRevision = staticRevision,
            blurStrength = blurStrength,
            updatesEnabled = updatesEnabled,
            blurLevel = blurLevel,
            backgroundDimAmount = backgroundDimAmount,
            modifier = Modifier.fillMaxSize(),
        ) {
            HomeWallpaperLayer(wallpaper, Modifier.fillMaxSize())
            HomeContent(preferences, pageRequest, onLongPressEmptyArea, onCurrentPageChanged,
                clockValue, onClockPlaced = { clockCoordinates = it; updatePlacement() })
        }
        // Apply dim after the static surface, but before transparent dynamic regions.
        // This keeps cache/live handoff brightness identical and avoids darkening clock.
        if (backgroundDimAmount > 0f) {
            Box(Modifier.fillMaxSize().drawWithContent {
                drawContent()
                drawRect(Color.Black.copy(alpha = backgroundDimAmount.coerceIn(0f, 0.5f)))
            })
        }
        val region = placement
        if (preferences.showClock && currentPage == 0 && region != null) {
            // Transparent halo prevents glyph shadows / Gaussian edges being cut off.
            val halo = with(density) { 32.dp.roundToPx() }
            val clockStyle = listOf(preferences.clockFont, preferences.clockFontWeight,
                preferences.clockFontSizeSp, preferences.clockTimeColor, preferences.clockDateColor)
            CachedHomeSurface(
                cacheName = "DynamicRegionCache.Clock",
                contentRevision = listOf(clockValue, clockStyle),
                blurStrength = blurStrength,
                updatesEnabled = updatesEnabled,
                blurLevel = blurLevel,
                applyBackgroundDim = false,
                modifier = Modifier
                    .offset { region.offset + IntOffset(with(density) { preferences.clockOffsetX.dp.roundToPx() } - halo,
                        with(density) { preferences.clockOffsetY.dp.roundToPx() } - halo) }
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .requiredSize(with(density) { (region.size.width + 2 * halo).toDp() },
                        with(density) { (region.size.height + 2 * halo).toDp() })
                    .graphicsLayer {
                        scaleX = preferences.clockScaleXPercent / 100f
                        scaleY = preferences.clockScaleYPercent / 100f
                        transformOrigin = TransformOrigin.Center
                    },
            ) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    HomeClockFace(clockValue, preferences.copy(clockOffsetX = 0, clockOffsetY = 0,
                        clockScaleXPercent = 100, clockScaleYPercent = 100))
                }
            }
        }
    }
}

@Composable
private fun HomeClockFace(clock: MinuteClockValue, preferences: HomePreferences) {
    ClockBlock(clock, preferences.clockFont, preferences.clockFontWeight,
        preferences.clockFontSizeSp, preferences.clockScaleXPercent, preferences.clockScaleYPercent,
        Color(preferences.clockTimeColor), Color(preferences.clockDateColor),
        preferences.clockOffsetX, preferences.clockOffsetY)
}

@Composable
internal fun HomeContent(
    preferences: HomePreferences,
    pageRequest: HomePageRequest,
    onLongPressEmptyArea: () -> Unit,
    onCurrentPageChanged: (Int) -> Unit,
    clockValue: MinuteClockValue,
    onClockPlaced: (LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pageCount = preferences.pageCount.coerceIn(1, 9)
    val pagerState = rememberPagerState(
        initialPage = pageRequest.page.coerceIn(0, pageCount - 1),
        pageCount = { pageCount },
    )

    LaunchedEffect(pageRequest, pageCount) {
        val target = pageRequest.page.coerceIn(0, pageCount - 1)
        if (pagerState.currentPage != target) pagerState.scrollToPage(target)
    }



    val reportPage by rememberUpdatedState(onCurrentPageChanged)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage to pagerState.isScrollInProgress }
            .collect { (page, scrolling) -> if (!scrolling) reportPage(page) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPressEmptyArea,
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(0.30f))

        if (preferences.showClock && pagerState.currentPage == 0) {
            // Reserve layout, but NEVER put a clock pixel in the static capture.
            // Freeze this measuring value so a minute tick cannot resize the static plane.
            val measuringClock = remember(clockValue.locale) { clockValue }
            Box(Modifier.fillMaxWidth().onGloballyPositioned(onClockPlaced)
                .drawWithContent { }, contentAlignment = Alignment.Center) {
                HomeClockFace(measuringClock, preferences.copy(clockOffsetX = 0, clockOffsetY = 0,
                    clockScaleXPercent = 100, clockScaleYPercent = 100))
            }
            Spacer(modifier = Modifier.height(34.dp))
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            beyondViewportPageCount = 1,
            key = { it },
        ) { }

        if (pageCount > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                repeat(pageCount) { page ->
                    Box(
                        modifier = Modifier
                            .size(if (page == pagerState.currentPage) 7.dp else 5.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(
                                if (page == pagerState.currentPage) OnHomePrimary
                                else OnHomeSecondary.copy(alpha = 0.55f),
                            ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun ClockBlock(
    clock: MinuteClockValue,
    font: Int,
    fontWeight: Int,
    fontSizeSp: Int,
    scaleXPercent: Int,
    scaleYPercent: Int,
    timeColor: Color,
    dateColor: Color,
    offsetX: Int,
    offsetY: Int,
) {
    val resolvedClock = clock
    val timeFormatter = remember(resolvedClock.locale) { DateTimeFormatter.ofPattern("HH:mm", resolvedClock.locale) }
    val dateFormatter = remember(resolvedClock.locale) {
        val pattern = if (resolvedClock.locale.language.startsWith("zh")) "M月d日 EEE" else "EEE, d MMM"
        DateTimeFormatter.ofPattern(pattern, resolvedClock.locale)
    }
    val family = remember(font, fontWeight) { clockFontFamily(font, fontWeight) }
    val weight = FontWeight(fontWeight)
    val dateSizeSp = (fontSizeSp * 0.205f).roundToInt().coerceAtLeast(10)

    Column(
        modifier = Modifier
            .offset {
                IntOffset(offsetX.dp.roundToPx(), offsetY.dp.roundToPx())
            }
            .graphicsLayer {
                scaleX = scaleXPercent / 100f
                scaleY = scaleYPercent / 100f
                transformOrigin = TransformOrigin.Center
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = resolvedClock.time.format(timeFormatter),
            color = timeColor,
            fontFamily = family,
            fontSize = fontSizeSp.sp,
            fontWeight = weight,
            lineHeight = (fontSizeSp * 1.08f).sp,
            style = TextStyle(shadow = HOME_TEXT_SHADOW),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = resolvedClock.time.format(dateFormatter),
            color = dateColor,
            fontFamily = family,
            fontSize = dateSizeSp.sp,
            fontWeight = weight,
            style = TextStyle(shadow = HOME_TEXT_SHADOW),
        )
    }
}

private fun clockFontFamily(font: Int, fontWeight: Int): FontFamily {
    val name = CLOCK_FONTS.getOrNull(font)?.deviceFontFamily ?: return FontFamily.Default
    return FontFamily(Font(DeviceFontFamilyName(name), weight = FontWeight(fontWeight)))
}

@Composable
internal fun IconOrFallback(
    app: InstalledApp,
    size: Dp,
    fallbackFontSize: TextUnit,
) {
    val icon = app.icon
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = app.label,
            modifier = Modifier.size(size),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = app.label.firstOrNull()?.uppercase() ?: "?",
                color = OnHomePrimary,
                fontSize = fallbackFontSize,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private val HOME_TEXT_SHADOW = Shadow(
    color = Color.Black.copy(alpha = 0.30f),
    offset = Offset(0f, 2f),
    blurRadius = 14f,
)
