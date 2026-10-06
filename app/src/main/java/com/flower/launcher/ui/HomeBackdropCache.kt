package com.flower.launcher.ui

import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlin.math.max
import kotlin.math.roundToInt

internal const val HOME_BACKDROP_BLUR_LEVELS = 5 // indices 0..5 = SIX textures
internal const val HOME_BACKDROP_DIM_DEFAULT = 0.20f
private const val HOME_BACKDROP_DOWNSAMPLE = 2
private val HOME_BACKDROP_MAX_BLUR = 8.dp

internal data class HomeBackdropCacheState(
    val levels: List<ImageBitmap> = emptyList(),
    val buildKey: Any? = null,
) {
    val ready: Boolean get() = levels.size == HOME_BACKDROP_BLUR_LEVELS + 1
}

/** Shared by the static Home plane and each isolated dynamic region.
 * Gesture progress is deliberately NOT an input. A gesture can cancel/defer work,
 * but returning Home with an unchanged key does not build again.
 */
@Composable
private fun rememberHomeBackdropCache(
    sourceLayer: GraphicsLayer,
    sourceSize: IntSize,
    contentRevision: Any,
    blurStrength: Float,
    updatesEnabled: Boolean,
    cacheName: String,
): HomeBackdropCacheState {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val captureLayer = rememberGraphicsLayer()
    val blurLayers = rememberGraphicsLayers(HOME_BACKDROP_BLUR_LEVELS + 1)
    val buildKey = listOf(contentRevision, sourceSize, blurStrength, density.density,
        density.fontScale, layoutDirection)
    var state by remember { mutableStateOf(HomeBackdropCacheState()) }

    LaunchedEffect(buildKey, updatesEnabled) {
        if (!updatesEnabled || sourceSize.width <= 0 || sourceSize.height <= 0 ||
            state.buildKey == buildKey) return@LaunchedEffect
        // Frame callbacks precede drawing. A second frame lets the new display list land.
        repeat(2) { withFrameNanos { } }
        val started = System.nanoTime()
        try {
            val halfSize = IntSize(max(1, sourceSize.width / HOME_BACKDROP_DOWNSAMPLE),
                max(1, sourceSize.height / HOME_BACKDROP_DOWNSAMPLE))
            // Rasterize at half resolution directly: no full-screen readback/software copy.
            captureLayer.record(density, layoutDirection, halfSize) {
                scale(halfSize.width.toFloat() / sourceSize.width,
                    halfSize.height.toFloat() / sourceSize.height, pivot = Offset.Zero) {
                    drawLayer(sourceLayer)
                }
            }
            val base = captureLayer.toImageBitmap()
            val levels = rasterizeBlurLevels(base, halfSize, blurLayers,
                with(density) { HOME_BACKDROP_MAX_BLUR.toPx() } *
                    blurStrength.coerceIn(0f, 1f) / HOME_BACKDROP_DOWNSAMPLE,
                density, layoutDirection)
            // Publication is atomic; incomplete generations are never visible.
            state = HomeBackdropCacheState(levels, buildKey)
            HomeCacheDiagnostics.built(cacheName, halfSize, levels, started)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Log.w("HomeCache", "$cacheName capture failed; keeping live content", failure)
        } finally {
            captureLayer.record(density, layoutDirection, IntSize(1, 1)) { }
        }
    }
    // On wallpaper/geometry changes, never display an obsolete generation.
    val previousKey = state.buildKey as? List<*>
    val canKeepDynamicFrame = cacheName.startsWith("DynamicRegionCache.") && !updatesEnabled &&
        previousKey?.drop(1) == buildKey.drop(1)
    return if (state.buildKey == buildKey || canKeepDynamicFrame) state else HomeBackdropCacheState()
}

/** Keeps content composed while drawing only a cached texture during transitions. */
@Composable
internal fun CachedHomeSurface(
    cacheName: String,
    contentRevision: Any,
    blurStrength: Float,
    updatesEnabled: Boolean,
    blurLevel: Int,
    backgroundDimAmount: Float = HOME_BACKDROP_DIM_DEFAULT,
    applyBackgroundDim: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val source = rememberGraphicsLayer()
    var sourceSize by remember { mutableStateOf(IntSize.Zero) }
    val cache = rememberHomeBackdropCache(source, sourceSize, contentRevision,
        blurStrength, updatesEnabled, cacheName)
    Box(modifier.onSizeChanged { sourceSize = it }.drawWithContent {
        val bitmap = cache.levels.getOrNull(blurLevel.coerceIn(0, HOME_BACKDROP_BLUR_LEVELS))
        if (!updatesEnabled && bitmap != null) {
            drawImage(bitmap, srcOffset = IntOffset.Zero,
                srcSize = IntSize(bitmap.width, bitmap.height), dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
        } else {
            source.record { this@drawWithContent.drawContent() }
            drawLayer(source)
        }
        // Dim is composited outside the cache. Keeping it here makes the cached
        // half-resolution surface and live Home surface have different brightness
        // at the handoff point.
    }) { content() }
}

@Composable
private fun rememberGraphicsLayers(count: Int): List<GraphicsLayer> {
    val layers = ArrayList<GraphicsLayer>(count)
    repeat(count) { layers += rememberGraphicsLayer() }
    return layers
}

/** Counters are observational only; never read by Compose or used as invalidation keys. */
internal object HomeCacheDiagnostics {
    private val builds = mutableMapOf<String, Int>()
    fun built(name: String, size: IntSize, levels: List<ImageBitmap>, started: Long) {
        val count = (builds[name] ?: 0) + 1
        builds[name] = count
        val bytes = levels.distinct().sumOf { it.width.toLong() * it.height * 4 }
        Log.d("HomeCache", "$name build=$count size=${size.width}x${size.height} " +
            "levels=${levels.size} textureBytes=$bytes ms=${(System.nanoTime() - started) / 1_000_000}")
    }
}

private suspend fun rasterizeBlurLevels(
    source: ImageBitmap,
    outputSize: IntSize,
    layers: List<GraphicsLayer>,
    maxBlurRadiusPx: Float,
    density: androidx.compose.ui.unit.Density,
    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
): List<ImageBitmap> {
    return List(HOME_BACKDROP_BLUR_LEVELS + 1) { index ->
        if (index == 0 || maxBlurRadiusPx <= 0f || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            source
        } else {
            val radius = maxBlurRadiusPx * index / HOME_BACKDROP_BLUR_LEVELS
            val layer = layers[index]
            layer.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
            layer.record(density, layoutDirection, outputSize) {
                drawImage(
                    image = source,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(source.width, source.height),
                    dstOffset = IntOffset.Zero,
                    dstSize = outputSize,
                )
            }
            try {
                layer.toImageBitmap()
            } finally {
                // Do not retain the source texture through this temporary display list.
                layer.renderEffect = null
                layer.record(density, layoutDirection, IntSize(1, 1)) { }
            }
        }
    }
}

