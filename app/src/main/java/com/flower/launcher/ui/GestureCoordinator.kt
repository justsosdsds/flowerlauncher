package com.flower.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.util.VelocityTracker

/**
 * Owns the lifecycle of cross-surface pointer sessions.
 *
 * Search keeps responsibility for deciding when its bottom rubber-band reaches the
 * configured Home-return threshold. From that semantic boundary onward this
 * coordinator becomes the single owner of the same pointer stream until release:
 * Search -> Home drag, reversal, velocity and the final settle target all live here.
 *
 * LauncherRoot only forwards pointer frames and applies the returned page-transition
 * commands. SearchCoordinator no longer carries pointer-held/armed handoff latches.
 */
@Stable
class GestureCoordinator internal constructor() {
    private sealed interface VerticalGestureSession {
        data object Idle : VerticalGestureSession

        data class SearchBottomCandidate(
            val pointerId: Long,
            val searchSessionId: Long,
        ) : VerticalGestureSession

        data class SearchToHome(
            val pointerId: Long,
            val searchSessionId: Long,
            val originEstablished: Boolean = false,
            val originFingerY: Float = 0f,
            val originPosition: Float = 1f,
            val lastFingerY: Float = 0f,
            val latestPosition: Float = 1f,
            val downwardAfterCommitPx: Float = 0f,
        ) : VerticalGestureSession
    }

    private var session: VerticalGestureSession = VerticalGestureSession.Idle
    private var searchReturnVelocityTracker: VelocityTracker? = null
    private var pointerGeneration = 0L

    /** Register the Search pointer before any bottom-threshold commit can occur. */
    fun beginSearchBottomPointer(
        pointerId: Long,
        searchSessionId: Long,
        eligible: Boolean,
    ): Long? {
        if (!eligible) return null
        pointerGeneration++
        session = VerticalGestureSession.SearchBottomCandidate(
            pointerId = pointerId,
            searchSessionId = searchSessionId,
        )
        searchReturnVelocityTracker = null
        return pointerGeneration
    }

    /**
     * Called exactly when Search's local bottom rubber-band crosses its configured
     * Home-return threshold. This is a semantic ownership transfer only; the crossing
     * delta itself still belongs to the Search rubber-band. The next page pixel starts
     * from the pointer position observed by LauncherRoot on the same event.
     */
    fun commitSearchBottomReturn(searchSessionId: Long): Boolean {
        val candidate = session as? VerticalGestureSession.SearchBottomCandidate
            ?: return false
        if (candidate.searchSessionId != searchSessionId) return false

        session = VerticalGestureSession.SearchToHome(
            pointerId = candidate.pointerId,
            searchSessionId = searchSessionId,
        )
        searchReturnVelocityTracker = null
        return true
    }

    /**
     * Feed one Final-pass frame from the original pointer stream.
     *
     * If the threshold and pointer-up happen in the same event, this method establishes
     * the handoff origin and returns a Home settle in that same call. There is therefore
     * no armed-but-unreleased intermediate state that can strand Search visually.
     */
    fun onSearchPointerFrame(
        pointerId: Long,
        searchSessionId: Long,
        position: Offset,
        uptimeMillis: Long,
        pressed: Boolean,
        currentPagePosition: Float,
        dragDistancePx: Float,
        touchSlopPx: Float,
        settleThreshold: Float,
        openVelocityPx: Float,
    ): SearchReturnGestureFrame {
        val current = session

        if (current is VerticalGestureSession.SearchBottomCandidate) {
            if (current.pointerId == pointerId && current.searchSessionId == searchSessionId && !pressed) {
                clearSession()
            }
            return SearchReturnGestureFrame.None
        }

        var returnSession = current as? VerticalGestureSession.SearchToHome
            ?: return SearchReturnGestureFrame.None
        if (returnSession.searchSessionId != searchSessionId) {
            return SearchReturnGestureFrame.None
        }
        if (returnSession.pointerId != pointerId) {
            return SearchReturnGestureFrame.None
        }

        if (!returnSession.originEstablished) {
            val originPosition = currentPagePosition.coerceIn(0f, 1f)
            returnSession = returnSession.copy(
                originEstablished = true,
                originFingerY = position.y,
                originPosition = originPosition,
                lastFingerY = position.y,
                latestPosition = originPosition,
            )
            session = returnSession
            searchReturnVelocityTracker = VelocityTracker().also {
                it.addPosition(uptimeMillis, position)
            }

            if (!pressed) {
                clearSession()
                return SearchReturnGestureFrame(
                    beginInteractiveAt = originPosition,
                    finishTarget = SearchReturnSettleTarget.Home,
                    finishPosition = originPosition,
                )
            }

            return SearchReturnGestureFrame(beginInteractiveAt = originPosition)
        }

        val dy = position.y - returnSession.lastFingerY
        val latestPosition = (
            returnSession.originPosition +
                (position.y - returnSession.originFingerY) / dragDistancePx.coerceAtLeast(1f)
            ).coerceIn(0f, 1f)
        val downwardAfterCommit = returnSession.downwardAfterCommitPx + dy.coerceAtLeast(0f)

        searchReturnVelocityTracker?.addPosition(uptimeMillis, position)
        returnSession = returnSession.copy(
            lastFingerY = position.y,
            latestPosition = latestPosition,
            downwardAfterCommitPx = downwardAfterCommit,
        )
        session = returnSession

        if (pressed) {
            return SearchReturnGestureFrame(
                consumePointer = dy != 0f,
                dragPosition = latestPosition,
            )
        }

        val velocityY = runCatching {
            searchReturnVelocityTracker?.calculateVelocity()?.y ?: 0f
        }.getOrDefault(0f)
        val target = settleTargetForSearchReturn(
            latestPosition = latestPosition,
            downwardAfterCommitPx = downwardAfterCommit,
            velocityY = velocityY,
            touchSlopPx = touchSlopPx,
            settleThreshold = settleThreshold,
            openVelocityPx = openVelocityPx,
        )
        clearSession()
        return SearchReturnGestureFrame(
            dragPosition = latestPosition,
            finishTarget = target,
            finishPosition = latestPosition,
        )
    }

    /**
     * Cancellation fallback for a pointer stream that disappears without a normal up.
     * A committed return resolves to Home; a mere candidate is simply discarded.
     */
    fun cancelSearchPointer(
        pointerId: Long,
        searchSessionId: Long,
        currentPagePosition: Float,
        generation: Long,
    ): SearchReturnGestureFrame {
        if (generation != pointerGeneration) return SearchReturnGestureFrame.None
        return when (val current = session) {
            is VerticalGestureSession.SearchBottomCandidate -> {
                if (current.pointerId == pointerId && current.searchSessionId == searchSessionId) clearSession()
                SearchReturnGestureFrame.None
            }

            is VerticalGestureSession.SearchToHome -> {
                if (current.pointerId != pointerId || current.searchSessionId != searchSessionId) {
                    SearchReturnGestureFrame.None
                } else {
                    val position = if (current.originEstablished) {
                        current.latestPosition
                    } else {
                        currentPagePosition.coerceIn(0f, 1f)
                    }
                    val needsBegin = !current.originEstablished
                    clearSession()
                    SearchReturnGestureFrame(
                        beginInteractiveAt = position.takeIf { needsBegin },
                        finishTarget = SearchReturnSettleTarget.Home,
                        finishPosition = position,
                    )
                }
            }

            VerticalGestureSession.Idle -> SearchReturnGestureFrame.None
        }
    }

    fun reset() {
        pointerGeneration++
        clearSession()
    }

    private fun settleTargetForSearchReturn(
        latestPosition: Float,
        downwardAfterCommitPx: Float,
        velocityY: Float,
        touchSlopPx: Float,
        settleThreshold: Float,
        openVelocityPx: Float,
    ): SearchReturnSettleTarget {
        val reversedTowardSearch =
            downwardAfterCommitPx >= touchSlopPx || velocityY >= openVelocityPx

        if (!reversedTowardSearch) return SearchReturnSettleTarget.Home

        return when {
            velocityY >= openVelocityPx -> SearchReturnSettleTarget.Search
            velocityY <= -openVelocityPx -> SearchReturnSettleTarget.Home
            latestPosition >= settleThreshold -> SearchReturnSettleTarget.Search
            else -> SearchReturnSettleTarget.Home
        }
    }

    private fun clearSession() {
        session = VerticalGestureSession.Idle
        searchReturnVelocityTracker = null
    }
}

enum class SearchReturnSettleTarget {
    Home,
    Search,
}

data class SearchReturnGestureFrame(
    val beginInteractiveAt: Float? = null,
    val consumePointer: Boolean = false,
    val dragPosition: Float? = null,
    val finishTarget: SearchReturnSettleTarget? = null,
    val finishPosition: Float? = null,
) {
    companion object {
        val None = SearchReturnGestureFrame()
    }
}

@Composable
fun rememberGestureCoordinator(): GestureCoordinator = remember { GestureCoordinator() }
