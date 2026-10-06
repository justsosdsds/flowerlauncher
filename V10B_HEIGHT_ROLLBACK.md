# v10b rollback

Retained the v10 Search layer fix:
- fixed Search backdrop
- only Search foreground moves during Search-only rubber-band overscroll

Rolled back only the full-display-height rubber-band change.
`searchPullLimitPx` once again uses `configuration.screenHeightDp`, matching the earlier behavior.
