package com.flower.launcher.ui

import androidx.compose.foundation.ScrollState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchCoordinatorTest {
    private fun activeCoordinator(
        query: String = "",
        expanded: Boolean = false,
    ): SearchCoordinator {
        val coordinator = SearchCoordinator(SearchInteractionState(ScrollState(0)))
        coordinator.dispatch(SearchIntent.SessionActiveChanged(true))
        coordinator.dispatch(SearchIntent.QueryChanged(query))
        coordinator.dispatch(SearchIntent.ImeVisibilityChanged(false))
        if (expanded) coordinator.dispatch(SearchIntent.EnterExpandedMode)
        return coordinator
    }

    @Test fun freshSearchSessionTargetsInputPresentation() {
        val coordinator = SearchCoordinator(SearchInteractionState(ScrollState(0)))

        coordinator.dispatch(SearchIntent.SessionActiveChanged(true))

        assertEquals(SearchPhase.Active, coordinator.state.phase)
        assertEquals(SearchPresentationMode.Input, coordinator.state.presentationMode)
        assertEquals(SearchImePhase.Hidden, coordinator.state.imePhase)
    }

    @Test fun hiddenImeBeforeFirstShowDoesNotCollapseInputPresentation() {
        val coordinator = SearchCoordinator(SearchInteractionState(ScrollState(0)))
        coordinator.dispatch(SearchIntent.SessionActiveChanged(true))

        coordinator.dispatch(
            SearchIntent.ImeVisibilityChanged(
                visible = false,
                userInitiatedHide = true,
            ),
        )

        assertEquals(SearchPresentationMode.Input, coordinator.state.presentationMode)
        assertEquals(SearchImePhase.Hidden, coordinator.state.imePhase)
    }

    @Test fun userDismissedVisibleImeTransitionsToExpandedPresentation() {
        val coordinator = activeCoordinator()
        coordinator.dispatch(SearchIntent.ImeVisibilityChanged(true))

        coordinator.dispatch(
            SearchIntent.ImeVisibilityChanged(
                visible = false,
                userInitiatedHide = true,
            ),
        )

        assertEquals(SearchPresentationMode.Expanded, coordinator.state.presentationMode)
        assertEquals(SearchImePhase.Hidden, coordinator.state.imePhase)
    }

    @Test fun programmaticImeHideDoesNotChangeInputPresentation() {
        val coordinator = activeCoordinator()
        coordinator.dispatch(SearchIntent.ImeVisibilityChanged(true))

        coordinator.dispatch(
            SearchIntent.ImeVisibilityChanged(
                visible = false,
                userInitiatedHide = false,
            ),
        )

        assertEquals(SearchPresentationMode.Input, coordinator.state.presentationMode)
        assertEquals(SearchImePhase.Hidden, coordinator.state.imePhase)
    }

    @Test fun pageReturnSuppressionDoesNotDestroyInputTarget() {
        val coordinator = activeCoordinator()
        coordinator.dispatch(SearchIntent.ImeVisibilityChanged(true))
        coordinator.dispatch(SearchIntent.SuppressImeForPageReturn)

        coordinator.dispatch(
            SearchIntent.ImeVisibilityChanged(
                visible = false,
                userInitiatedHide = true,
            ),
        )

        assertEquals(SearchPresentationMode.Input, coordinator.state.presentationMode)
        assertTrue(coordinator.state.pageReturnImeSuppressed)
    }

    @Test fun emptyQueryTopPullUsesTheSameImeRevealThreshold() {
        val coordinator = activeCoordinator(expanded = true)

        val effect = coordinator.onTopBoundaryDrag(
            deltaY = 100f,
            imeVisible = false,
            overscrollLimitPx = 1000f,
            revealPullThresholdPx = 50f,
        )

        assertEquals(SearchBoundaryEffect.RevealIme, effect)
        assertEquals(SearchPresentationMode.Input, coordinator.state.presentationMode)
        assertEquals(SearchImePhase.Hidden, coordinator.state.imePhase)
        assertEquals(
            SearchImeCommand.Show,
            coordinator.reconcileIme(presentationReady = true),
        )
        assertEquals(SearchImePhase.Showing, coordinator.state.imePhase)
        assertTrue(coordinator.interaction.rawOverscrollPx > 0f)
    }

    @Test fun nonEmptyQueryTopPullUsesTheSameImeRevealThreshold() {
        val coordinator = activeCoordinator(query = "flower", expanded = true)

        val effect = coordinator.onTopBoundaryDrag(
            deltaY = 100f,
            imeVisible = false,
            overscrollLimitPx = 1000f,
            revealPullThresholdPx = 50f,
        )

        assertEquals(SearchBoundaryEffect.RevealIme, effect)
        assertEquals(SearchPresentationMode.Input, coordinator.state.presentationMode)
        assertEquals(SearchImePhase.Hidden, coordinator.state.imePhase)
        assertEquals(
            SearchImeCommand.Show,
            coordinator.reconcileIme(presentationReady = true),
        )
        assertEquals(SearchImePhase.Showing, coordinator.state.imePhase)
    }

    @Test fun inputPresentationDoesNotFirePullRevealBeforeItBecomesExpanded() {
        val coordinator = activeCoordinator()

        val effect = coordinator.onTopBoundaryDrag(
            deltaY = 100f,
            imeVisible = false,
            overscrollLimitPx = 1000f,
            revealPullThresholdPx = 50f,
        )

        assertEquals(SearchBoundaryEffect.None, effect)
        assertEquals(SearchPresentationMode.Input, coordinator.state.presentationMode)
    }

    @Test fun nonEmptyUpScrollCollapseTargetsExpandedPresentation() {
        val coordinator = activeCoordinator(query = "flower")
        coordinator.dispatch(SearchIntent.ImeVisibilityChanged(true))

        val (_, effect) = coordinator.onPreScroll(
            deltaY = -20f,
            imeVisible = true,
            contentOverflowAboveImePx = 200f,
            collapseOverflowThresholdPx = 50f,
        )

        assertEquals(SearchPreScrollEffect.HideIme, effect)
        assertEquals(SearchPresentationMode.Expanded, coordinator.state.presentationMode)
        assertEquals(SearchImePhase.Visible, coordinator.state.imePhase)
        assertEquals(
            SearchImeCommand.Hide,
            coordinator.reconcileIme(presentationReady = true),
        )
        assertEquals(SearchImePhase.Hiding, coordinator.state.imePhase)
    }

    @Test fun emptyQueryDoesNotAcquireTheNonEmptyBottomReturnPath() {
        val coordinator = activeCoordinator()

        val effect = coordinator.onBottomBoundaryDrag(
            deltaY = -120f,
            overscrollLimitPx = 1000f,
            homeReturnPullThresholdPx = 50f,
        )

        assertEquals(SearchBoundaryEffect.None, effect)
        assertEquals(0f, coordinator.interaction.rawOverscrollPx, 0.0001f)
    }

    @Test fun emptyQueryCanReverseAnExistingTopRubberBand() {
        val coordinator = activeCoordinator(expanded = true)
        coordinator.onTopBoundaryDrag(
            deltaY = 100f,
            imeVisible = true,
            overscrollLimitPx = 1000f,
            revealPullThresholdPx = 50f,
        )

        val (consumed, effect) = coordinator.onPreScroll(
            deltaY = -30f,
            imeVisible = false,
            contentOverflowAboveImePx = 0f,
            collapseOverflowThresholdPx = 0f,
        )

        assertEquals(-30f, consumed, 0.0001f)
        assertEquals(SearchPreScrollEffect.None, effect)
        assertEquals(70f, coordinator.interaction.rawOverscrollPx, 0.0001f)
    }

    @Test fun clearingQueryDoesNotResetTheSharedTopPullState() {
        val coordinator = activeCoordinator(query = "flower", expanded = true)
        coordinator.onTopBoundaryDrag(
            deltaY = 80f,
            imeVisible = true,
            overscrollLimitPx = 1000f,
            revealPullThresholdPx = 300f,
        )
        val before = coordinator.interaction.rawOverscrollPx

        coordinator.dispatch(SearchIntent.QueryChanged(""))

        assertEquals(before, coordinator.interaction.rawOverscrollPx, 0.0001f)
    }

    @Test fun aNewSessionAlwaysResetsThePresentationTargetToInput() {
        val coordinator = activeCoordinator(expanded = true)
        coordinator.dispatch(SearchIntent.SessionActiveChanged(false))

        coordinator.dispatch(SearchIntent.SessionActiveChanged(true))

        assertEquals(SearchPresentationMode.Input, coordinator.state.presentationMode)
        assertEquals(SearchPhase.Active, coordinator.state.phase)
    }

    @Test fun pageReturnReversalSerializesShowThenHideThenShow() {
        val coordinator = activeCoordinator()

        assertEquals(
            SearchImeCommand.Show,
            coordinator.reconcileIme(presentationReady = true),
        )
        assertEquals(SearchImePhase.Showing, coordinator.state.imePhase)

        coordinator.dispatch(SearchIntent.SuppressImeForPageReturn)

        // A newer hide target must not send Hide while Show is still in flight.
        assertEquals(
            SearchImeCommand.None,
            coordinator.reconcileIme(presentationReady = false),
        )
        assertEquals(SearchImePhase.Showing, coordinator.state.imePhase)

        // The late Visible acknowledgement does not overwrite the desired Input target
        // or suppression policy; it merely completes the in-flight Show.
        coordinator.dispatch(SearchIntent.ImeVisibilityChanged(true))
        assertEquals(SearchPresentationMode.Input, coordinator.state.presentationMode)
        assertEquals(SearchImePhase.Visible, coordinator.state.imePhase)

        assertEquals(
            SearchImeCommand.Hide,
            coordinator.reconcileIme(presentationReady = false),
        )
        assertEquals(SearchImePhase.Hiding, coordinator.state.imePhase)

        // Same-pointer reversal back to Search clears suppression while Hide is still
        // in flight. Show must wait until Hidden is acknowledged.
        coordinator.dispatch(SearchIntent.AllowImeAfterPageReturn)
        coordinator.dispatch(SearchIntent.ResumeActive)
        assertEquals(
            SearchImeCommand.None,
            coordinator.reconcileIme(presentationReady = true),
        )
        assertEquals(SearchImePhase.Hiding, coordinator.state.imePhase)

        coordinator.dispatch(SearchIntent.ImeVisibilityChanged(false))
        assertEquals(SearchImePhase.Hidden, coordinator.state.imePhase)
        assertEquals(
            SearchImeCommand.Show,
            coordinator.reconcileIme(presentationReady = true),
        )
        assertEquals(SearchImePhase.Showing, coordinator.state.imePhase)
    }

    @Test fun presentationReadinessFallingBelowThresholdDoesNotHideVisibleIme() {
        val coordinator = activeCoordinator()
        coordinator.dispatch(SearchIntent.ImeVisibilityChanged(true))

        assertEquals(
            SearchImeCommand.None,
            coordinator.reconcileIme(presentationReady = false),
        )
        assertEquals(SearchImePhase.Visible, coordinator.state.imePhase)
        assertEquals(SearchPresentationMode.Input, coordinator.state.presentationMode)
    }

    @Test fun lateVisibleAcknowledgementCannotUndoNewerExpandedTarget() {
        val coordinator = activeCoordinator()
        assertEquals(
            SearchImeCommand.Show,
            coordinator.reconcileIme(presentationReady = true),
        )
        assertEquals(SearchImePhase.Showing, coordinator.state.imePhase)

        coordinator.dispatch(SearchIntent.EnterExpandedMode)
        coordinator.dispatch(SearchIntent.ImeVisibilityChanged(true))

        assertEquals(SearchPresentationMode.Expanded, coordinator.state.presentationMode)
        assertEquals(SearchImePhase.Visible, coordinator.state.imePhase)
        assertEquals(
            SearchImeCommand.Hide,
            coordinator.reconcileIme(presentationReady = true),
        )
        assertEquals(SearchImePhase.Hiding, coordinator.state.imePhase)
    }

    @Test fun userOrSystemRevealFromStableExpandedStateReturnsToInput() {
        val coordinator = activeCoordinator(expanded = true)
        assertEquals(SearchImePhase.Hidden, coordinator.state.imePhase)

        coordinator.dispatch(SearchIntent.ImeVisibilityChanged(true))

        assertEquals(SearchPresentationMode.Input, coordinator.state.presentationMode)
        assertEquals(SearchImePhase.Visible, coordinator.state.imePhase)
    }


    @Test fun abruptSessionEndStillSerializesFinalImeHide() {
        val coordinator = activeCoordinator()
        assertEquals(
            SearchImeCommand.Show,
            coordinator.reconcileIme(presentationReady = true),
        )
        coordinator.dispatch(SearchIntent.SessionActiveChanged(false))

        // Session teardown must not erase the in-flight Show phase.
        assertEquals(SearchPhase.Hidden, coordinator.state.phase)
        assertEquals(SearchImePhase.Showing, coordinator.state.imePhase)
        assertEquals(
            SearchImeCommand.None,
            coordinator.reconcileIme(presentationReady = false),
        )

        // If the old Show becomes visible after Home already won, acknowledge it and hide once.
        coordinator.dispatch(SearchIntent.ImeVisibilityChanged(true))
        assertEquals(SearchImePhase.Visible, coordinator.state.imePhase)
        assertEquals(
            SearchImeCommand.Hide,
            coordinator.reconcileIme(presentationReady = false),
        )
        assertEquals(SearchImePhase.Hiding, coordinator.state.imePhase)
    }

    @Test fun imeControlDisabledPausesReconcile() {
        val coordinator = activeCoordinator()
        coordinator.dispatch(SearchIntent.ImeControlChanged(false))

        assertEquals(SearchImeCommand.None, coordinator.reconcileIme(presentationReady = true))
        assertEquals(SearchImePhase.Hidden, coordinator.state.imePhase)
    }

    @Test fun overlayImeVisibilityDoesNotChangeSearchStateWhileControlDisabled() {
        val coordinator = activeCoordinator(expanded = true)
        coordinator.dispatch(SearchIntent.ImeControlChanged(false))

        // The Settings field opens and closes its own keyboard; Search ignores it entirely.
        coordinator.dispatch(SearchIntent.ImeVisibilityChanged(true))
        coordinator.dispatch(SearchIntent.ImeVisibilityChanged(false, userInitiatedHide = true))

        assertEquals(SearchPresentationMode.Expanded, coordinator.state.presentationMode)
        assertEquals(SearchImePhase.Hidden, coordinator.state.imePhase)
        assertEquals(SearchImeCommand.None, coordinator.reconcileIme(presentationReady = true))
    }

    @Test fun regainingControlInInputModeShowsImeAgain() {
        val coordinator = activeCoordinator()
        coordinator.dispatch(SearchIntent.ImeControlChanged(false))
        coordinator.dispatch(SearchIntent.ImeVisibilityChanged(true))
        coordinator.dispatch(SearchIntent.ImeControlChanged(true))

        assertEquals(SearchPresentationMode.Input, coordinator.state.presentationMode)
        assertEquals(SearchImePhase.Hidden, coordinator.state.imePhase)
        assertEquals(SearchImeCommand.Show, coordinator.reconcileIme(presentationReady = true))
        assertEquals(SearchImePhase.Showing, coordinator.state.imePhase)
    }

    @Test fun regainingControlInExpandedModeStaysHidden() {
        val coordinator = activeCoordinator(expanded = true)
        coordinator.dispatch(SearchIntent.ImeControlChanged(false))
        coordinator.dispatch(SearchIntent.ImeVisibilityChanged(true))
        coordinator.dispatch(SearchIntent.ImeControlChanged(true))

        assertEquals(SearchPresentationMode.Expanded, coordinator.state.presentationMode)
        assertEquals(SearchImePhase.Hidden, coordinator.state.imePhase)
        assertEquals(SearchImeCommand.None, coordinator.reconcileIme(presentationReady = true))
    }

    @Test fun ownershipHandoffDuringInFlightShowDoesNotEmitConcurrentHide() {
        val coordinator = activeCoordinator()
        assertEquals(SearchImeCommand.Show, coordinator.reconcileIme(presentationReady = true))
        assertEquals(SearchImePhase.Showing, coordinator.state.imePhase)

        // Settings opens while Show is in flight: no Hide, no phase overwrite.
        coordinator.dispatch(SearchIntent.ImeControlChanged(false))
        assertEquals(SearchImeCommand.None, coordinator.reconcileIme(presentationReady = false))
        assertEquals(SearchImePhase.Showing, coordinator.state.imePhase)

        // The overlay keyboard's late visibility is ignored.
        coordinator.dispatch(SearchIntent.ImeVisibilityChanged(true))
        assertEquals(SearchImePhase.Showing, coordinator.state.imePhase)

        // Returning ownership converges to a recoverable Hidden baseline, then re-derives
        // Show from the untouched Input target.
        coordinator.dispatch(SearchIntent.ImeControlChanged(true))
        assertEquals(SearchImePhase.Hidden, coordinator.state.imePhase)
        assertEquals(SearchImeCommand.Show, coordinator.reconcileIme(presentationReady = true))
        assertEquals(SearchImePhase.Showing, coordinator.state.imePhase)
    }

    @Test fun ownershipHandoffDuringInFlightHideDoesNotEmitConcurrentShow() {
        val coordinator = activeCoordinator(query = "flower")
        coordinator.dispatch(SearchIntent.ImeVisibilityChanged(true))
        coordinator.dispatch(SearchIntent.EnterExpandedMode)
        assertEquals(SearchImeCommand.Hide, coordinator.reconcileIme(presentationReady = true))
        assertEquals(SearchImePhase.Hiding, coordinator.state.imePhase)

        coordinator.dispatch(SearchIntent.ImeControlChanged(false))
        assertEquals(SearchImeCommand.None, coordinator.reconcileIme(presentationReady = true))
        assertEquals(SearchImePhase.Hiding, coordinator.state.imePhase)

        coordinator.dispatch(SearchIntent.ImeControlChanged(true))
        assertEquals(SearchImePhase.Hidden, coordinator.state.imePhase)
        // Expanded target is preserved; ownership return alone must not pop the keyboard.
        assertEquals(SearchImeCommand.None, coordinator.reconcileIme(presentationReady = true))
    }

}
