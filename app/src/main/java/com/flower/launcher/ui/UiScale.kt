package com.flower.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Scales the content by overriding [LocalDensity]. Every dp/sp inside shrinks
 * (or grows) by the same factor, so text, spacing, icons and touch targets stay
 * proportional. Insets remain pixel-correct because they are converted to dp and
 * back with the same density.
 */
@Composable
fun ScaledUiContent(
    scale: Float,
    content: @Composable () -> Unit,
) {
    if (scale <= 0f || scale == 1f) {
        content()
        return
    }
    val base = LocalDensity.current
    val scaled = remember(base, scale) { Density(base.density * scale, base.fontScale) }
    CompositionLocalProvider(LocalDensity provides scaled) { content() }
}
