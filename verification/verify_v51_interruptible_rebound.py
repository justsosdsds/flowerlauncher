from pathlib import Path
root = Path(__file__).resolve().parents[1]
sc = (root/'app/src/main/java/com/flower/launcher/ui/SearchContent.kt').read_text()
state = (root/'app/src/main/java/com/flower/launcher/ui/SearchInteractionState.kt').read_text()
root_ui = root/'app/src/main/java/com/flower/launcher/ui/LauncherRoot.kt'
v50_root = Path('/mnt/data/v50work/app/src/main/java/com/flower/launcher/ui/LauncherRoot.kt')

trigger_start = sc.index('visual >= revealPullThresholdPx')
trigger_end = sc.index('return Offset(0f, available.y)', trigger_start)
trigger_block = sc[trigger_start:trigger_end]

checks = {
    'threshold still immediate': 'focusRequester.requestFocus()' in trigger_block and 'keyboardController?.show()' in trigger_block,
    'threshold still haptic': 'performImeRevealThresholdHaptic(' in trigger_block,
    'threshold does not rebound': 'rebound' not in trigger_block.lower() and 'startRebound' not in trigger_block,
    'post-threshold drag not forcibly swallowed': 'imeRevealRequested && available.y > 0f' not in sc,
    'release starts rebound': 'interactionState.startRebound(interactionScope, reboundDurationMs)' in sc,
    'pointer-down interrupts rebound': 'awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)' in sc and 'interactionState.beginInteractive()' in sc,
    'rebound has owned job': 'private var reboundJob: Job? = null' in state,
    'beginInteractive joins cancellation': 'job.cancelAndJoin()' in state,
    'animation write generation guard': 'reboundGeneration == generation' in state,
    'drag cancels old rebound': 'cancelRebound()' in state[state.index('fun consumeExistingOverscroll'):state.index('fun visualOverscrollPx')],
    'LauncherRoot preserved': v50_root.exists() and root_ui.read_bytes() == v50_root.read_bytes(),
    'compact return constants preserved': all(x in sc for x in [
        'SEARCH_RETURN_BAR_MAX_UP = 28.dp',
        'SEARCH_RETURN_CONTENT_EXTRA_UP = 4.dp',
        'SEARCH_RETURN_FAST_EXIT_START = 0.55f',
        'SEARCH_RETURN_BAR_GONE_PROGRESS = 0.80f',
        'SEARCH_RETURN_CONTENT_GONE_PROGRESS = 0.85f',
        'SEARCH_RETURN_FOREGROUND_SCALE = 0.965f',
    ]),
}
failed = [k for k, v in checks.items() if not v]
for k, v in checks.items():
    print(('PASS' if v else 'FAIL'), k)
if failed:
    raise SystemExit('failed: ' + ', '.join(failed))
