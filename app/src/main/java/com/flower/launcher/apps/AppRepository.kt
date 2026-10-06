package com.flower.launcher.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class AppRepository(context: Context) {

    private val packageManager = context.packageManager
    private val ownPackageName = context.packageName

    suspend fun loadLaunchableApps(
        previousApps: List<InstalledApp> = emptyList(),
        invalidatedPackages: Set<String> = emptySet(),
    ): List<InstalledApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolvedActivities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0L),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        val loadingContext = currentCoroutineContext()
        val seenComponents = HashSet<String>()
        val reusableApps = previousApps.associateBy(InstalledApp::componentKey)
        return resolvedActivities
            .asSequence()
            .mapNotNull { resolveInfo ->
                loadingContext.ensureActive()
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val packageName = activityInfo.packageName ?: return@mapNotNull null
                val activityName = activityInfo.name ?: return@mapNotNull null

                if (packageName == ownPackageName) return@mapNotNull null
                // Filter duplicate resolver entries before decoding their bitmaps.
                val componentKey = "$packageName/$activityName"
                if (!seenComponents.add(componentKey)) return@mapNotNull null
                // Walk the fresh resolver list to preserve its stable tie order and
                // omit removed activities, while retaining unaffected icon bitmaps.
                val previous = reusableApps[componentKey]
                if (previous != null && packageName !in invalidatedPackages) {
                    return@mapNotNull previous
                }

                val label = try {
                    resolveInfo.loadLabel(packageManager).toString().trim().ifEmpty { packageName }
                } catch (exception: RuntimeException) {
                    // Packages can disappear between query and resource loading.
                    Log.w("AppRepository", "Unable to load label for $packageName", exception)
                    packageName
                }

                val icon = runCatching {
                    resolveInfo
                        .loadIcon(packageManager)
                        .toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX)
                        .asImageBitmap()
                }.getOrNull()

                // ICU and full sort-key normalization run with catalog IO, before
                // publishing the apps to Compose or starting an opening gesture.
                loadingContext.ensureActive()
                AppNameSortKey.keys(label)

                InstalledApp(
                    label = label,
                    packageName = packageName,
                    activityName = activityName,
                    icon = icon,
                )
            }
            .sortedBy { it.searchLabel }
            .toList()
    }

    private companion object {
        const val ICON_SIZE_PX = 144
    }
}
