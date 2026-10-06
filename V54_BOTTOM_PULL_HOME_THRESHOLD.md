# V54 - Bottom rubber-band threshold returns Home

## Behavior

- With a non-empty Search query, result scrolling remains local to Search.
- Before the result list reaches its bottom, upward drags only scroll results.
- After the bottom boundary is reached, continued upward drag enters the existing bottom rubber-band.
- If the visual rubber-band distance stays below the configured threshold, release uses the existing interruptible rebound and remains on Search.
- Once the visual bottom rubber-band distance reaches the threshold, Search commits immediately to the existing Search -> Home compact transition.
- The threshold works whether IME is visible or hidden.
- At commit, IME is hidden, no local rebound is started, and the local overscroll is held at its live position while LauncherRoot owns the page return.

## Setting

Search Settings -> Interaction Settings -> Page Return:

- Return Home upward pull distance (dp)
- Default: 200dp
- Range: 80-360dp
- Step: 10dp

The threshold is based on visual rubber-band displacement, not raw finger distance.

## Preserved behavior

- V52 fixed full-height Search Results / IME overlay model
- V51 interruptible rebound
- IME reveal threshold + haptic behavior
- Existing Search -> Home compact return animation
- Home / Drawer transitions and existing blur/gesture constants
