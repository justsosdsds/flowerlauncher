# V12 Search upward-swipe fix

- Search-open foreground now owns both vertical directions in one gesture state machine.
- Downward drag keeps the existing rubber-band pull.
- Upward drag directly drives `progress` from Search toward Home, so child search content no longer steals/cancels the close gesture.
- The Search gesture modifier stays installed for the whole `targetOpen` state. It is no longer removed as soon as `progress` falls below 0.995 during an upward close drag.
- Opening transition behavior and the fixed Search backdrop from v10/v11 are retained.
