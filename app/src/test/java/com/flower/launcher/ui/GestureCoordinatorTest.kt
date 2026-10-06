package com.flower.launcher.ui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureCoordinatorTest {
    private fun GestureCoordinator.begin(id: Long = 10) =
        beginSearchBottomPointer(pointerId = 7, searchSessionId = id, eligible = true)

    private fun GestureCoordinator.frame(y: Float, time: Long, pressed: Boolean = true) =
        onSearchPointerFrame(
            pointerId = 7, searchSessionId = 10, position = Offset(0f, y),
            uptimeMillis = time, pressed = pressed, currentPagePosition = 1f,
            dragDistancePx = 100f, touchSlopPx = 8f, settleThreshold = 0.4f,
            openVelocityPx = Float.MAX_VALUE,
        )

    @Test fun candidateCancellationDoesNotMoveThePage() {
        val coordinator = GestureCoordinator()
        coordinator.begin()
        assertEquals(SearchReturnGestureFrame.None, coordinator.cancelSearchPointer(7, 10, 1f, 1))
        assertFalse(coordinator.commitSearchBottomReturn(10))
    }

    @Test fun commitAndReleaseInSameFrameSettlesHomeExactlyOnce() {
        val coordinator = GestureCoordinator()
        coordinator.begin()
        assertTrue(coordinator.commitSearchBottomReturn(10))
        assertFalse(coordinator.commitSearchBottomReturn(10))
        val result = coordinator.frame(100f, 0, pressed = false)
        assertEquals(SearchReturnSettleTarget.Home, result.finishTarget)
        assertEquals(1f, result.finishPosition!!, 0.0001f)
        assertEquals(SearchReturnGestureFrame.None, coordinator.cancelSearchPointer(7, 10, 1f, 1))
    }

    @Test fun cancellationBeforeFirstPageFrameProvidesAHomeRecovery() {
        val coordinator = GestureCoordinator()
        coordinator.begin()
        coordinator.commitSearchBottomReturn(10)
        val result = coordinator.cancelSearchPointer(7, 10, 0.8f, 1)
        assertEquals(0.8f, result.beginInteractiveAt!!, 0.0001f)
        assertEquals(SearchReturnSettleTarget.Home, result.finishTarget)
        assertEquals(SearchReturnGestureFrame.None, coordinator.cancelSearchPointer(7, 10, 0.8f, 1))
    }

    @Test fun cancellationUsesLiveDragPositionWithoutRestartingTheGesture() {
        val coordinator = GestureCoordinator()
        coordinator.begin()
        coordinator.commitSearchBottomReturn(10)
        coordinator.frame(100f, 0)
        coordinator.frame(60f, 200)
        val result = coordinator.cancelSearchPointer(7, 10, 1f, 1)
        assertNull(result.beginInteractiveAt)
        assertEquals(0.6f, result.finishPosition!!, 0.0001f)
        assertEquals(SearchReturnSettleTarget.Home, result.finishTarget)
    }

    @Test fun staleCancellationCannotClearANewerSearchSession() {
        val coordinator = GestureCoordinator()
        coordinator.begin(11)
        assertEquals(SearchReturnGestureFrame.None, coordinator.cancelSearchPointer(7, 10, 1f, 1))
        assertTrue(coordinator.commitSearchBottomReturn(11))
        assertEquals(SearchReturnGestureFrame.None, coordinator.cancelSearchPointer(7, 10, 1f, 1))
        assertEquals(SearchReturnSettleTarget.Home, coordinator.cancelSearchPointer(7, 11, 1f, 1).finishTarget)
    }

    @Test fun resetInvalidatesTheOldRelease() {
        val coordinator = GestureCoordinator()
        coordinator.begin()
        coordinator.commitSearchBottomReturn(10)
        coordinator.reset()
        assertEquals(SearchReturnGestureFrame.None, coordinator.frame(50f, 200, pressed = false))
        coordinator.begin(11)
        assertTrue(coordinator.commitSearchBottomReturn(11))
    }

    @Test fun reversalCanReturnToSearchWhileContinuingUpReturnsHome() {
        val coordinator = GestureCoordinator()
        coordinator.begin()
        coordinator.commitSearchBottomReturn(10)
        coordinator.frame(100f, 0)
        coordinator.frame(60f, 200)
        val reversed = coordinator.frame(90f, 400, pressed = false)
        assertEquals(SearchReturnSettleTarget.Search, reversed.finishTarget)
        coordinator.begin()
        coordinator.commitSearchBottomReturn(10)
        coordinator.frame(100f, 1000)
        assertEquals(SearchReturnSettleTarget.Home, coordinator.frame(60f, 1200, pressed = false).finishTarget)
    }
    @Test fun lateCancellationCannotClearANewerPointerInTheSameSearchSession() {
        val coordinator = GestureCoordinator()
        val old = coordinator.begin()!!
        val current = coordinator.begin()!!
        assertEquals(SearchReturnGestureFrame.None, coordinator.cancelSearchPointer(7, 10, 1f, old))
        assertTrue(coordinator.commitSearchBottomReturn(10))
        assertEquals(SearchReturnSettleTarget.Home, coordinator.cancelSearchPointer(7, 10, 1f, current).finishTarget)
    }

}
