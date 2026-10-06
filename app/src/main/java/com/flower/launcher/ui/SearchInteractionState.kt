package com.flower.launcher.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Search-local scroll boundary state.
 *
 * Normal result scrolling stays in [ScrollState]. Only unconsumed drag after a
 * boundary reaches this state, so short and long result pages share the same
 * rubber-band physics without involving LauncherRoot's page transition.
 *
 * Rebound is deliberately interruptible: animation owns the overscroll only
 * while no finger is down. A new pointer-down synchronously cancels the running
 * rebound, preserving its current position so user input can take over without
 * a snap or a one-frame fight over [rawOverscrollPx].
 */
@Stable
class SearchInteractionState internal constructor(
    val scrollState: ScrollState,
) {
    var rawOverscrollPx by mutableFloatStateOf(0f)
        private set

    private var reboundJob: Job? = null
    private var reboundGeneration = 0L

    val isRebounding: Boolean
        get() = reboundJob?.isActive == true

    /** Best-effort synchronous cancellation for drag callbacks. */
    fun cancelRebound() {
        reboundGeneration += 1L
        reboundJob?.cancel()
    }

    /**
     * Called as soon as a new pointer goes down. Cancellation is deliberately
     * synchronous here because Compose's restricted pointer scope cannot call an
     * arbitrary suspend function. The generation guard prevents a cancellation-edge
     * animation frame from writing over the new gesture.
     */
    fun beginInteractive() {
        val job = reboundJob ?: return
        reboundGeneration += 1L
        reboundJob = null
        job.cancel()
    }

    fun consumeExistingOverscroll(deltaY: Float): Float {
        cancelRebound()
        val current = rawOverscrollPx
        if (current == 0f) return 0f
        val reverses = (current > 0f && deltaY < 0f) || (current < 0f && deltaY > 0f)
        if (!reverses) return 0f

        val consumed = if (abs(deltaY) >= abs(current)) -current else deltaY
        rawOverscrollPx = current + consumed
        if (abs(rawOverscrollPx) < 0.5f) rawOverscrollPx = 0f
        return consumed
    }

    fun addBoundaryDrag(deltaY: Float) {
        cancelRebound()
        rawOverscrollPx += deltaY
    }

    fun visualOverscrollPx(limitPx: Float): Float {
        if (rawOverscrollPx == 0f || limitPx <= 0f) return 0f
        val sign = if (rawOverscrollPx < 0f) -1f else 1f
        val raw = abs(rawOverscrollPx)
        return sign * (limitPx * raw / (limitPx + raw))
    }

    /**
     * Start a release-time rebound without blocking nested-scroll callbacks.
     * A later pointer-down can cancel this job and continue from its live value.
     */
    fun startRebound(scope: CoroutineScope, durationMs: Int) {
        if (rawOverscrollPx == 0f || reboundJob?.isActive == true) return

        reboundGeneration += 1L
        val generation = reboundGeneration
        val job = scope.launch {
            val start = rawOverscrollPx
            if (start == 0f) return@launch
            animate(
                initialValue = start,
                targetValue = 0f,
                animationSpec = tween(durationMs.coerceAtLeast(1)),
            ) { value, _ ->
                // A new touch invalidates this animator before cancelling its job,
                // so even a cancellation-edge frame cannot overwrite user input.
                if (reboundGeneration == generation) rawOverscrollPx = value
            }
            if (reboundGeneration == generation) rawOverscrollPx = 0f
        }
        reboundJob = job
        job.invokeOnCompletion {
            if (reboundJob === job) reboundJob = null
        }
    }

    fun resetOverscroll() {
        cancelRebound()
        reboundJob = null
        rawOverscrollPx = 0f
    }

    /**
     * End one Search interaction session without suspension. The result list may
     * be at its bottom when a Home-return gesture commits, so a suspending
     * scrollTo(0) is unsafe here: a new Search session can cancel that coroutine
     * before the caller clears its one-shot gesture latches. dispatchRawDelta is
     * synchronous and the Search surface is already leaving the foreground, so
     * it gives the next session a clean top position without a teardown race.
     */
    fun resetSessionImmediately() {
        resetOverscroll()
        val currentScroll = scrollState.value
        if (currentScroll != 0) {
            scrollState.dispatchRawDelta(-currentScroll.toFloat())
        }
    }
}

@Composable
fun rememberSearchInteractionState(): SearchInteractionState {
    val scrollState = rememberScrollState()
    return remember(scrollState) { SearchInteractionState(scrollState) }
}
