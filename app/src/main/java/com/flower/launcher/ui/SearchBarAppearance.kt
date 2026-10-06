package com.flower.launcher.ui

import androidx.compose.ui.graphics.Color

internal data class SearchBarColorPreset(
    val id: String,
    val label: String,
    val color: Color,
)

internal val SearchBarColorPresets = listOf(
    SearchBarColorPreset("neutral", "中性", Color(0xFF36383D)),
    SearchBarColorPreset("red", "红色", Color(0xFFE05F68)),
    SearchBarColorPreset("orange", "橙色", Color(0xFFE28A4B)),
    SearchBarColorPreset("green", "绿色", Color(0xFF58A56B)),
    SearchBarColorPreset("cyan", "青色", Color(0xFF4FA4A8)),
    SearchBarColorPreset("blue", "蓝色", Color(0xFF5C7FC8)),
    SearchBarColorPreset("purple", "紫色", Color(0xFF896BC5)),
    SearchBarColorPreset("pink", "粉色", Color(0xFFC36997)),
)

internal fun searchBarColorFor(
    presetId: String,
    depthPercent: Int,
    opacityPercent: Int,
): Color {
    val base = SearchBarColorPresets.firstOrNull { it.id == presetId }?.color
        ?: SearchBarColorPresets.first().color
    val depth = depthPercent.coerceIn(0, 100) / 100f
    // "Depth" only darkens the selected hue. Opacity is intentionally applied later
    // so the two controls stay visually and semantically independent.
    val darken = depth * 0.85f
    val r = base.red * (1f - darken)
    val g = base.green * (1f - darken)
    val b = base.blue * (1f - darken)
    return Color(
        red = r,
        green = g,
        blue = b,
        alpha = opacityPercent.coerceIn(0, 100) / 100f,
    )
}
