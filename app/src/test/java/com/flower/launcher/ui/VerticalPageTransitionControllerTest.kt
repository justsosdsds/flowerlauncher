package com.flower.launcher.ui

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VerticalPageTransitionControllerTest {
    @Test fun aNewGestureInvalidatesCleanupFromThePreviousGesture() = runTest {
        val controller = VerticalPageTransitionController(this)
        val old = controller.beginInteractive()
        val current = controller.beginInteractive()
        assertFalse(controller.ownsInteraction(old))
        assertTrue(controller.ownsInteraction(current))
        controller.dispose()
    }

    @Test fun homeResetInvalidatesPendingGestureRecovery() = runTest {
        val controller = VerticalPageTransitionController(this)
        val owner = controller.beginInteractive()
        controller.reset()
        assertFalse(controller.ownsInteraction(owner))
    }

    @Test fun leavingCompositionInvalidatesPageOwnership() = runTest {
        val controller = VerticalPageTransitionController(this)
        val owner = controller.beginInteractive()
        assertTrue(controller.ownsInteraction(owner))
        controller.dispose()
        assertFalse(controller.ownsInteraction(owner))
        assertTrue(controller.isDisposed)
    }

    @Test fun anExplicitNavigationInvalidatesOldGestureCleanupImmediately() = runTest {
        val controller = VerticalPageTransitionController(this)
        val owner = controller.beginInteractive()
        controller.settle(VerticalPage.Home, reboundDurationMs = 200)
        assertFalse(controller.ownsInteraction(owner))
        // Cancel before the test scheduler starts the animation (no UI frame clock).
        controller.dispose()
    }
}
