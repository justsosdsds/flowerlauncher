package com.flower.launcher.apps

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Retains the icon catalog across Activity recreation, without retaining the Activity. */
class AppListViewModel : ViewModel() {
    var apps by mutableStateOf<List<InstalledApp>>(emptyList())
        private set

    private val loadingScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loadedCatalogKey: String? = null
    private var requestedCatalogKey: String? = null
    private var loadJob: Job? = null
    private var refreshGeneration = 0L
    private val invalidatedPackages = LinkedHashSet<String>()
    private var observedContext: Context? = null
    private var launcherApps: LauncherApps? = null

    private val packageCallback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) =
            invalidatePackages(listOf(packageName), user)

        override fun onPackageRemoved(packageName: String, user: UserHandle) =
            invalidatePackages(listOf(packageName), user)

        override fun onPackageChanged(packageName: String, user: UserHandle) =
            invalidatePackages(listOf(packageName), user)

        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            invalidatePackages(packageNames.asList(), user)

        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            invalidatePackages(packageNames.asList(), user)
    }

    fun load(context: Context) {
        val appContext = context.applicationContext
        observePackageChanges(appContext)
        val key = configurationKey(appContext)
        if (requestedCatalogKey == key && loadJob?.isActive == true) return
        if (loadedCatalogKey == key && invalidatedPackages.isEmpty() && launcherApps != null) return
        scheduleLoad(appContext, key, coalesceEvents = false)
    }

    private fun configurationKey(context: Context): String {
        val configuration = context.resources.configuration
        return "${configuration.locales.toLanguageTags()}:${configuration.densityDpi}:${configuration.uiMode}"
    }

    private fun observePackageChanges(context: Context) {
        if (launcherApps != null) return
        observedContext = context
        try {
            val service = context.getSystemService(LauncherApps::class.java) ?: return
            // Keep all state mutations on Main. The application context and callback
            // survive Activity recreation without holding a destroyed Activity.
            service.registerCallback(packageCallback, Handler(Looper.getMainLooper()))
            launcherApps = service
            // A successful re-registration must reconcile changes that may have
            // happened while registration was unavailable.
            loadedCatalogKey = null
        } catch (exception: RuntimeException) {
            // load() retries on the next foreground entry and falls back to a full
            // scan while no observer is available, instead of freezing the catalog.
            Log.w("AppListViewModel", "Unable to observe installed apps", exception)
        }
    }

    private fun invalidatePackages(packageNames: Collection<String>, user: UserHandle) {
        // AppRepository currently queries this profile only; do not change that scope.
        if (user != Process.myUserHandle()) return
        val context = observedContext ?: return
        invalidatedPackages.addAll(packageNames)
        scheduleLoad(context, configurationKey(context), coalesceEvents = true)
    }

    private fun scheduleLoad(context: Context, key: String, coalesceEvents: Boolean) {
        loadJob?.cancel()
        requestedCatalogKey = key
        val generation = ++refreshGeneration
        loadJob = loadingScope.launch {
            try {
                if (coalesceEvents) delay(PACKAGE_CHANGE_DEBOUNCE_MS)
                val dirtyPackages = invalidatedPackages.toSet()
                val previous = if (loadedCatalogKey == key && launcherApps != null) apps else emptyList()
                val loaded = withContext(Dispatchers.IO) {
                    AppRepository(context).loadLaunchableApps(previous, dirtyPackages)
                }
                if (generation != refreshGeneration) return@launch
                apps = loaded
                loadedCatalogKey = key
                // Only a successful latest load acknowledges invalidations. A newer
                // event cancels this job without losing an earlier dirty package.
                invalidatedPackages.removeAll(dirtyPackages)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: RuntimeException) {
                if (generation != refreshGeneration) return@launch
                loadedCatalogKey = null
                Log.e("AppListViewModel", "Unable to load installed apps", exception)
            }
        }
    }

    override fun onCleared() {
        // Queued callbacks cannot schedule work once the application context is cleared.
        observedContext = null
        try {
            launcherApps?.unregisterCallback(packageCallback)
        } catch (exception: RuntimeException) {
            Log.w("AppListViewModel", "Unable to stop observing installed apps", exception)
        }
        launcherApps = null
        loadingScope.cancel()
        super.onCleared()
    }

    private companion object {
        const val PACKAGE_CHANGE_DEBOUNCE_MS = 150L
    }
}
