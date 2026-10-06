from pathlib import Path
root=Path(__file__).resolve().parents[1]
sc=(root/'app/src/main/java/com/flower/launcher/ui/SearchContent.kt').read_text()
lr=(root/'app/src/main/java/com/flower/launcher/ui/LauncherRoot.kt').read_text()
prefs=(root/'app/src/main/java/com/flower/launcher/search/SearchPreferencesRepository.kt').read_text()
settings=(root/'app/src/main/java/com/flower/launcher/ui/SearchSettingsScreen.kt').read_text()
state=(root/'app/src/main/java/com/flower/launcher/ui/SearchInteractionState.kt').read_text()
checks={
 'root locks nonempty search from page swipe': 'searchOwnsForeground &&\n                !searchHasQuery' in lr,
 'v48 collapsed mode removed': 'searchImeCollapsedByGesture' not in lr,
 'search nested scroll present': 'NestedScrollConnection' in sc and '.nestedScroll(searchNestedScrollConnection)' in sc,
 'floating bar bottom aligned': '.align(Alignment.BottomCenter)' in sc,
 'ime inset follows bar': '.imePadding()' in sc and '.navigationBarsPadding()' in sc,
 'short/long share overscroll state': 'SearchInteractionState' in state and 'addBoundaryDrag' in state,
 'collapse overflow preference': 'imeCollapseOverflowThresholdDp' in prefs and '隐藏输入法内容溢出' in settings,
 'reveal pull preference': 'imeRevealPullThresholdDp' in prefs and '重新显示输入法下拉距离' in settings,
 'existing compact return constants preserved': all(x in sc for x in ['SEARCH_RETURN_BAR_MAX_UP = 28.dp','SEARCH_RETURN_CONTENT_EXTRA_UP = 4.dp','SEARCH_RETURN_FAST_EXIT_START = 0.55f','SEARCH_RETURN_BAR_GONE_PROGRESS = 0.80f','SEARCH_RETURN_CONTENT_GONE_PROGRESS = 0.85f','SEARCH_RETURN_FOREGROUND_SCALE = 0.965f']),
}
failed=[k for k,v in checks.items() if not v]
for k,v in checks.items(): print(('PASS' if v else 'FAIL'), k)
if failed: raise SystemExit('failed: '+', '.join(failed))
