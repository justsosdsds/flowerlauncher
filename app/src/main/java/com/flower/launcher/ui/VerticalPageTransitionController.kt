package com.flower.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val FastStartSoftFinishEasing = Easing { fraction ->
    // Selected rebound curve: ease-out quartic.
    // It covers more distance immediately after release, then eases into the
    // resting position while preserving the configured total duration.
    val t = fraction.coerceIn(0f, 1f)
    val inverse = 1f - t
    1f - inverse * inverse * inverse * inverse
}


internal enum class VerticalPage(val anchor: Float) {
    AllApps(-1f),
    Home(0f),
    Search(1f),
}

internal class VerticalPageTransitionController(
    private val scope: kotlinx.coroutines.CoroutineScope,
) {
    val position = Animatable(VerticalPage.Home.anchor)
    val transitionPullOffset = Animatable(0f)

    var targetPage by mutableStateOf(VerticalPage.Home)
        private set
    var stablePage by mutableStateOf(VerticalPage.Home)
        private set
    var searchLayerActive by mutableStateOf(false)
        private set
    var drawerLayerActive by mutableStateOf(false)
        private set

    // True only for the Search -> Home exit path (including a cancelled return
    // animating back to Search). Keeping this separate from stablePage avoids a
    // visual pop when Home -> Search finishes and stablePage becomes Search.
    var returningFromSearch by mutableStateOf(false)
        private set

    // V41: compact return can begin from any positive Search-side progress, not
    // only from a fully settled Search at +1. Normalizing exit progress against
    // this snapshot keeps the reversal continuous with no instant fade jump.
    var searchReturnStartPosition by mutableStateOf(VerticalPage.Search.anchor)
        private set

    private var settleJob: Job? = null
    private var liveSnapJob: Job? = null
    private var pendingLivePosition = VerticalPage.Home.anchor
    private var pendingLivePullOffset = 0f
    private var liveSnapVersion = 0L
    private var interactionGeneration = 0L
    private var interactive = false
    var isDisposed = false
        private set

    fun ownsInteraction(generation: Long): Boolean =
        !isDisposed && interactive && generation == interactionGeneration

    fun beginSearchReturn(positionValue: Float = position.value) {
        val positivePosition = positionValue.coerceIn(VerticalPage.Home.anchor, VerticalPage.Search.anchor)
        if (positivePosition <= VerticalPage.Home.anchor || returningFromSearch) return
        searchReturnStartPosition = positivePosition
        returningFromSearch = true
    }

    fun searchReturnProgress(positionValue: Float = position.value): Float {
        if (!returningFromSearch) return 0f
        val start = searchReturnStartPosition.coerceAtLeast(0.001f)
        val current = positionValue.coerceIn(VerticalPage.Home.anchor, VerticalPage.Search.anchor)
        return ((start - current) / start).coerceIn(0f, 1f)
    }

    fun beginInteractive(): Long {
        check(!isDisposed)
        interactive = true
        interactionGeneration++
        settleJob?.cancel()
        liveSnapJob?.cancel()
        // Both sibling pages stay renderable during an interactive gesture so the
        // signed position can cross Home without waiting for composition/state swaps.
        searchLayerActive = true
        drawerLayerActive = true
        return interactionGeneration
    }

    fun snapTo(positionValue: Float, pullOffset: Float) {
        searchLayerActive = true
        drawerLayerActive = true

        // Pointer events can arrive faster than two Animatable.snapTo calls can finish.
        // Cancelling/restarting a coroutine for every move creates a race where rapid
        // Home -> All Apps drags can temporarily stop applying live positions. Keep one
        // worker and coalesce intermediate events to the newest requested frame instead.
        pendingLivePosition = positionValue.coerceIn(-1f, 1f)
        pendingLivePullOffset = pullOffset
        liveSnapVersion++
        if (liveSnapJob?.isActive == true) return

        liveSnapJob = scope.launch {
            var appliedVersion = Long.MIN_VALUE
            while (appliedVersion != liveSnapVersion) {
                val version = liveSnapVersion
                val nextPosition = pendingLivePosition
                val nextPullOffset = pendingLivePullOffset
                position.snapTo(nextPosition)
                transitionPullOffset.snapTo(nextPullOffset)
                appliedVersion = version
            }
        }
    }

    fun settle(
        page: VerticalPage,
        startValue: Float? = null,
        reboundDurationMs: Int,
        durationOverrideMs: Int? = null,
    ) {
        if (isDisposed) return
        interactive = false
        interactionGeneration++
        val effectiveStart = startValue ?: position.value
        if (
            page == VerticalPage.Home &&
            effectiveStart > VerticalPage.Home.anchor &&
            (stablePage == VerticalPage.Search || returningFromSearch)
        ) {
            beginSearchReturn(effectiveStart)
        }

        targetPage = page
        settleJob?.cancel()
        liveSnapJob?.cancel()

        when (page) {
            VerticalPage.Search -> searchLayerActive = true
            VerticalPage.AllApps -> drawerLayerActive = true
            VerticalPage.Home -> {
                // Keep whichever page(s) are currently visible alive until the settle
                // reaches Home, otherwise a partially visible layer would disappear.
            }
        }

        val duration = durationOverrideMs?.coerceIn(80, 600)
            ?: reboundDurationMs.coerceIn(160, 600)
        settleJob = scope.launch {
            startValue?.let {
                position.snapTo(it.coerceIn(-1f, 1f))
            }

            kotlinx.coroutines.coroutineScope {
                launch {
                    position.animateTo(
                        targetValue = page.anchor,
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = FastStartSoftFinishEasing,
                        ),
                    )
                }
                launch {
                    transitionPullOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = FastStartSoftFinishEasing,
                        ),
                    )
                }
            }

            stablePage = page
            targetPage = page
            searchLayerActive = page == VerticalPage.Search
            drawerLayerActive = page == VerticalPage.AllApps
            returningFromSearch = false
            searchReturnStartPosition = VerticalPage.Search.anchor
        }
    }

    suspend fun reset() {
        interactive = false
        interactionGeneration++
        settleJob?.cancel()
        liveSnapJob?.cancel()
        targetPage = VerticalPage.Home
        stablePage = VerticalPage.Home
        position.snapTo(VerticalPage.Home.anchor)
        transitionPullOffset.snapTo(0f)
        searchLayerActive = false
        drawerLayerActive = false
        returningFromSearch = false
        searchReturnStartPosition = VerticalPage.Search.anchor
    }

    /** Do not start recovery animations once the composition has been destroyed. */
    fun dispose() {
        isDisposed = true
        interactive = false
        interactionGeneration++
        settleJob?.cancel()
        liveSnapJob?.cancel()
    }
}
