# IME Search Launch

The keyboard Search action now launches the top-ranked app result directly.

Behavior:
- Type a query such as `王者`.
- `AppSearchEngine` ranks exact matches first, then prefix matches, substring matches, and package-name matches.
- Press the IME Search key.
- The first ranked result launches immediately.
- If the query is empty or there are no results, nothing launches.
- Normal tapping on a result remains unchanged.
