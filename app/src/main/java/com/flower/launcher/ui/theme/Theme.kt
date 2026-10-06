package com.flower.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LauncherAccent = Color(0xFF0066FF)
val LauncherAccentLight = Color(0xFF2288FF)
val OnHomePrimary = Color(0xFFFFFFFF)
val OnHomeSecondary = Color(0xE6FFFFFF)
val SearchFieldBackground = Color(0x1FFFFFFF)
val SearchScrim = Color(0xCC101318)

private val LauncherColors = darkColorScheme(
    primary = LauncherAccentLight,
    onPrimary = Color.White,
    background = Color(0xFF101318),
    onBackground = Color.White,
    surface = Color(0xFF191D23),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF232830),
    onSurfaceVariant = Color(0xFFB6BEC9),
    outline = Color(0xFF3A414B),
)

@Composable
fun FlowerLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LauncherColors,
        content = content,
    )
}
