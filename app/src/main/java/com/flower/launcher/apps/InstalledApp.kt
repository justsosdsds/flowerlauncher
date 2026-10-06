package com.flower.launcher.apps

import androidx.compose.ui.graphics.ImageBitmap

data class InstalledApp(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: ImageBitmap? = null,
) {
    // Recomputed by copy() when an alias changes, never per frame/query.
    val componentKey: String = "$packageName/$activityName"
    internal val searchLabel: String = label.lowercase()
    internal val searchPackageName: String = packageName.lowercase()
}
