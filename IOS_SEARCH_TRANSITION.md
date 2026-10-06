# v8 iOS-style continuous Search transition

This revision joins the Home -> Search opening gesture and the already-open Search rubber-band into one continuous interaction.

## Home -> Search
- Home now physically moves downward while the finger pulls down.
- The movement uses the same rational resistance family as the Search overscroll:
  `visible = limit * raw / (limit + raw)`.
- This gives an approximately 1:1 response at the beginning and continuously increasing resistance later.
- Home scale reduction is intentionally small; translation + blur now carry most of the motion.

## Blur
- Home content blur increases with drag progress.
- Blur reaches maximum around 58% of the opening gesture and then stays capped.
- On Android 12+, the launcher also requests background-window blur so the wallpaper itself can soften when the device supports cross-window blur.
- Compose RenderEffect remains as a fallback for launcher content.

## Release / settle
- Home -> Search and Search -> Home no longer use a generic spring.
- They use a fixed 310 ms settle with the same LinearThenBrake easing as the open-Search rubber-band rebound.
- The curve is close to constant speed early and performs most deceleration near the end.

## Fully open Search
- Continuing to pull down still enters the v5 rubber-band region.
- Initial response is nearly 1:1, resistance increases continuously, and visible travel asymptotically approaches ~46% of screen height.
- Release always returns in 310 ms, so a larger pull naturally has a higher average return speed.
