package com.flower.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope

/**
 * Search domain state owned independently from LauncherRoot.
 *
 * The coordinator owns Search session lifecycle, the desired presentation mode,
 * semantic IME phase, boundary overscroll and single-fire threshold commits.
 * UI reports intents and executes returned effects; LauncherRoot only reacts to
 * page-level effects such as ReturnHome.
 */
enum class SearchPhase {
    Hidden,
    Active,
    Returning,
}

enum class SearchPresentationMode {
    /** Search is in text-input mode: the field wants focus and IME when the page is ready. */
    Input,

    /** Search is expanded: IME is hidden and the results viewport uses the full height. */
    Expanded,
}

enum class SearchImePhase {
    Hidden,
    Showing,
    Visible,
    Hiding,
}

enum class SearchImeCommand {
    None,
    Show,
    Hide,
}

data class SearchState(
    val query: String = "",
    val phase: SearchPhase = SearchPhase.Hidden,
    val presentationMode: SearchPresentationMode = SearchPresentationMode.Input,
    val imePhase: SearchImePhase = SearchImePhase.Hidden,
    val sessionId: Long = 0L,
    /**
     * True while SearchContent owns the window IME state machine. It is false while a
     * full-screen overlay (Search Settings) owns the IME, so Search must not observe,
     * reconcile, show or hide the shared window keyboard during that time.
     */
    val imeControlEnabled: Boolean = true,
    val pageReturnImeSuppressed: Boolean = false,
) {
    val hasQuery: Boolean
        get() = query.trim().isNotEmpty()
}

sealed interface SearchIntent {
    data class QueryChanged(val query: String) : SearchIntent
    data class SessionActiveChanged(val active: Boolean) : SearchIntent

    /**
     * Reports the actual IME visibility. userInitiatedHide is true only when the field
     * still owns focus and SearchContent did not initiate the hide itself.
     */
    data class ImeVisibilityChanged(
        val visible: Boolean,
        val userInitiatedHide: Boolean = false,
    ) : SearchIntent

    /** Explicitly target input mode, e.g. same-pointer Search return reversal. */
    data object EnterInputMode : SearchIntent

    /** Explicitly target the full-height expanded mode. */
    data object EnterExpandedMode : SearchIntent

    data object SuppressImeForPageReturn : SearchIntent
    data object AllowImeAfterPageReturn : SearchIntent
    data object PageReturnStarted : SearchIntent
    data object ResumeActive : SearchIntent
    data object LauncherReset : SearchIntent

    /**
     * Transfers IME ownership between Search and a full-screen overlay. When disabled,
     * Search pauses its IME state machine; when re-enabled, Search resumes from a
     * recoverable Hidden baseline and re-derives its target from presentationMode.
     */
    data class ImeControlChanged(val enabled: Boolean) : SearchIntent
}

enum class SearchPreScrollEffect {
    None,
    HideIme,
    ConsumeAll,
}

enum class SearchBoundaryEffect {
    None,
    RevealIme,
    ReturnHome,
    ConsumeAll,
}

@Stable
class SearchCoordinator internal constructor(
    val interaction: SearchInteractionState,
) {
    var state by mutableStateOf(SearchState())
        private set

    private fun endSession() {
        interaction.resetSessionImmediately()
        state = state.copy(
            phase = SearchPhase.Hidden,
            presentationMode = SearchPresentationMode.Input,
            // Preserve the acknowledged/in-flight IME phase. SearchContent remains mounted
            // long enough to serialize any required final Hide even for an abrupt Home reset.
            pageReturnImeSuppressed = false,
        )
    }

    fun dispatch(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> {
                state = state.copy(query = intent.query)
            }

            is SearchIntent.SessionActiveChanged -> {
                if (intent.active) {
                    if (state.phase == SearchPhase.Hidden) {
                        // Every fresh Home -> Search session has one deterministic target:
                        // Input. The page-progress gate decides when the UI may realize it.
                        state = state.copy(
                            phase = SearchPhase.Active,
                            presentationMode = SearchPresentationMode.Input,
                            // Do not erase a previous in-flight Hide/Show acknowledgement.
                            // The reconciler will converge it safely before the new Input show.
                            sessionId = state.sessionId + 1L,
                        )
                    }
                } else if (state.phase != SearchPhase.Hidden) {
                    endSession()
                }
            }

            is SearchIntent.ImeVisibilityChanged -> {
                // Another IME owner (Search Settings) is active: its keyboard must never
                // be interpreted as Search's own IME acknowledgement.
                if (!state.imeControlEnabled) return

                if (intent.visible) {
                    // Visibility is an acknowledgement, not the source of truth for the
                    // desired presentation. Only an external/user reveal from a fully
                    // Hidden-IME + Expanded active Search may promote the target to Input.
                    // Late visibility from an in-flight Show is still recorded even after
                    // the Search session became Hidden so the reconciler can issue one final Hide.
                    val externalReveal =
                        state.phase == SearchPhase.Active &&
                            !state.pageReturnImeSuppressed &&
                            state.presentationMode == SearchPresentationMode.Expanded &&
                            state.imePhase == SearchImePhase.Hidden

                    state = state.copy(
                        presentationMode = if (externalReveal) {
                            SearchPresentationMode.Input
                        } else {
                            state.presentationMode
                        },
                        imePhase = SearchImePhase.Visible,
                    )
                } else {
                    val userCollapsedIme =
                        intent.userInitiatedHide &&
                            state.phase == SearchPhase.Active &&
                            !state.pageReturnImeSuppressed &&
                            state.imePhase == SearchImePhase.Visible

                    state = state.copy(
                        presentationMode = if (userCollapsedIme) {
                            SearchPresentationMode.Expanded
                        } else {
                            state.presentationMode
                        },
                        imePhase = SearchImePhase.Hidden,
                    )
                }
            }

            SearchIntent.EnterInputMode -> {
                if (state.phase != SearchPhase.Hidden) {
                    state = state.copy(presentationMode = SearchPresentationMode.Input)
                }
            }

            SearchIntent.EnterExpandedMode -> {
                if (state.phase == SearchPhase.Active) {
                    // Change only the desired presentation. The serialized IME reconciler
                    // below owns the Visible -> Hiding transition immediately before it
                    // emits the actual Hide command.
                    state = state.copy(presentationMode = SearchPresentationMode.Expanded)
                }
            }

            SearchIntent.SuppressImeForPageReturn -> {
                // Suppression is transient page-navigation policy. Do not mutate the
                // presentation target; a cancelled return can therefore resume correctly.
                state = state.copy(pageReturnImeSuppressed = true)
            }

            SearchIntent.AllowImeAfterPageReturn -> {
                state = state.copy(pageReturnImeSuppressed = false)
            }

            SearchIntent.PageReturnStarted -> {
                if (state.phase != SearchPhase.Hidden) {
                    state = state.copy(phase = SearchPhase.Returning)
                }
            }

            SearchIntent.ResumeActive -> {
                if (state.phase != SearchPhase.Hidden) {
                    state = state.copy(phase = SearchPhase.Active)
                }
            }

            SearchIntent.LauncherReset -> {
                endSession()
            }

            is SearchIntent.ImeControlChanged -> {
                if (state.imeControlEnabled == intent.enabled) return
                state = if (intent.enabled) {
                    // Ownership returns after the overlay released the IME. Converge to a
                    // recoverable Hidden baseline; the serialized reconciler then derives
                    // the only valid target from presentationMode (Input -> show,
                    // Expanded -> stay hidden). This is not a desired-state overwrite:
                    // presentationMode is untouched and no command is emitted here.
                    state.copy(imeControlEnabled = true, imePhase = SearchImePhase.Hidden)
                } else {
                    // Ownership leaves. Keep the current imePhase so an in-flight Show/Hide
                    // is neither raced nor force-overwritten; the reconciler is paused.
                    state.copy(imeControlEnabled = false)
                }
            }
        }
    }

    /** Called for user-input pre-scroll while Search is active. */
    fun onPreScroll(
        deltaY: Float,
        imeVisible: Boolean,
        contentOverflowAboveImePx: Float,
        collapseOverflowThresholdPx: Float,
    ): Pair<Float, SearchPreScrollEffect> {
        if (state.phase == SearchPhase.Returning) return deltaY to SearchPreScrollEffect.ConsumeAll
        if (state.phase != SearchPhase.Active) return 0f to SearchPreScrollEffect.None

        var effect = SearchPreScrollEffect.None
        if (
            state.hasQuery &&
            deltaY < 0f &&
            imeVisible &&
            state.imePhase != SearchImePhase.Hiding &&
            contentOverflowAboveImePx > collapseOverflowThresholdPx
        ) {
            state = state.copy(presentationMode = SearchPresentationMode.Expanded)
            effect = SearchPreScrollEffect.HideIme
        }

        val consumed = interaction.consumeExistingOverscroll(deltaY)
        return consumed to effect
    }

    /**
     * Handle a downward pull after the result scroller reaches its top boundary.
     * This is intentionally query-agnostic: empty and non-empty Search share the
     * same rubber-band physics and IME reveal threshold.
     */
    fun onTopBoundaryDrag(
        deltaY: Float,
        imeVisible: Boolean,
        overscrollLimitPx: Float,
        revealPullThresholdPx: Float,
    ): SearchBoundaryEffect {
        if (state.phase == SearchPhase.Returning) return SearchBoundaryEffect.ConsumeAll
        if (state.phase != SearchPhase.Active || deltaY <= 0f) return SearchBoundaryEffect.None

        interaction.addBoundaryDrag(deltaY)
        val visual = interaction.visualOverscrollPx(overscrollLimitPx)

        if (
            visual > 0f &&
            !imeVisible &&
            state.presentationMode == SearchPresentationMode.Expanded &&
            visual >= revealPullThresholdPx &&
            state.imePhase != SearchImePhase.Showing
        ) {
            state = state.copy(presentationMode = SearchPresentationMode.Input)
            return SearchBoundaryEffect.RevealIme
        }

        return SearchBoundaryEffect.None
    }

    /**
     * Handle upward overscroll after non-empty results reach their bottom boundary.
     * Empty Search keeps its existing page-level Search -> Home gesture in LauncherRoot.
     */
    fun onBottomBoundaryDrag(
        deltaY: Float,
        overscrollLimitPx: Float,
        homeReturnPullThresholdPx: Float,
    ): SearchBoundaryEffect {
        if (!state.hasQuery) return SearchBoundaryEffect.None
        if (state.phase == SearchPhase.Returning) return SearchBoundaryEffect.ConsumeAll
        if (state.phase != SearchPhase.Active || deltaY >= 0f) return SearchBoundaryEffect.None

        interaction.addBoundaryDrag(deltaY)
        val visual = interaction.visualOverscrollPx(overscrollLimitPx)

        if (visual < 0f && -visual >= homeReturnPullThresholdPx) {
            return SearchBoundaryEffect.ReturnHome
        }

        return SearchBoundaryEffect.None
    }


    /**
     * Reconcile Search's desired presentation with the acknowledged IME phase.
     *
     * Show and Hide are strictly serialized:
     * - while Showing, a newer hide target waits for Visible before Hide is emitted;
     * - while Hiding, a newer input target waits for Hidden before Show is emitted.
     *
     * presentationReady is only a gate for starting Show. Falling below that gate does
     * not itself request Hide; Hidden session, page-return suppression, Expanded mode
     * and lost IME ownership own hiding.
     *
     * While [SearchState.imeControlEnabled] is false (Search Settings owns the IME) this
     * returns None and leaves [SearchState.imePhase] untouched.
     */
    fun reconcileIme(
        presentationReady: Boolean,
    ): SearchImeCommand {
        if (!state.imeControlEnabled) return SearchImeCommand.None

        val wantsHide =
            state.phase == SearchPhase.Hidden ||
                state.pageReturnImeSuppressed ||
                state.presentationMode == SearchPresentationMode.Expanded

        if (wantsHide) {
            return when (state.imePhase) {
                SearchImePhase.Visible -> {
                    state = state.copy(imePhase = SearchImePhase.Hiding)
                    SearchImeCommand.Hide
                }

                // Never issue Hide while a Show command is still in flight. Wait for the
                // Visible acknowledgement, then this reconciler will emit Hide exactly once.
                SearchImePhase.Showing,
                SearchImePhase.Hiding,
                SearchImePhase.Hidden,
                -> SearchImeCommand.None
            }
        }

        val wantsShow =
            state.phase == SearchPhase.Active &&
                state.presentationMode == SearchPresentationMode.Input &&
                presentationReady &&
                !state.pageReturnImeSuppressed

        if (wantsShow) {
            return when (state.imePhase) {
                SearchImePhase.Hidden -> {
                    state = state.copy(imePhase = SearchImePhase.Showing)
                    SearchImeCommand.Show
                }

                // Never issue Show while Hide is still in flight. Hidden acknowledgement
                // will cause the next reconciliation pass to emit Show exactly once.
                SearchImePhase.Hiding,
                SearchImePhase.Showing,
                SearchImePhase.Visible,
                -> SearchImeCommand.None
            }
        }

        return SearchImeCommand.None
    }

    fun beginInteractive() {
        interaction.beginInteractive()
    }

    fun clearBoundaryOverscroll() {
        interaction.resetOverscroll()
    }

    fun onRelease(scope: CoroutineScope, reboundDurationMs: Int) {
        if (state.phase == SearchPhase.Returning) return
        if (interaction.rawOverscrollPx != 0f) {
            interaction.startRebound(scope, reboundDurationMs)
        }
    }
}

@Composable
fun rememberSearchCoordinator(): SearchCoordinator {
    val interaction = rememberSearchInteractionState()
    return remember(interaction) { SearchCoordinator(interaction) }
}
