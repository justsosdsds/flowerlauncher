# v30 — Editable Home foundation

This version turns the previously empty Home container into a persistent paged desktop foundation without changing the Search or All Apps transition architecture.

## Implemented

- Persistent Home page model (up to 9 pages).
- Per-app and per-folder page assignment stored in `HomeLayoutRepository`.
- Existing/legacy Home items default safely to page 0.
- Horizontal Home paging with a page indicator.
- Last visible Home page is persisted and restored.
- Adding an app from the Home long-press sheet adds it to the current Home page.
- Long-pressing an app in All Apps now exposes `添加到主页`.
- If the requested page is at the basic capacity estimate, a newly added app is assigned to the next page automatically.
- Home app long-press menu now includes left/right position movement. Repeated movement at an edge moves the app to the neighboring page.
- Removing a Home app also clears its page assignment.
- New folders are assigned to the current Home page.
- Layout lock prevents Home app add/move operations.

## Preserved

- Home starts empty for users migrated by v24.
- Home swipe down -> Search.
- Home swipe up -> All Apps.
- Existing Search blur/scrim animation pipeline.
- Existing Home app context menu, widget context menu and Desktop settings sheet.

## Follow-up work

- Free-form/drag-and-drop cell placement is not yet a full coordinate-based workspace model; v30 establishes paging and ordered placement first.
- Widgets still live on the first Home page and are not yet stored as page/grid cells.
- Folder/app interleaving is still rendered as apps followed by folders on each page; a unified cell entity model is the next step for true arbitrary drag placement.
