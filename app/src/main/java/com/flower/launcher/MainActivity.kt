package com.flower.launcher

import android.app.AlertDialog
import android.app.WallpaperManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.flower.launcher.ui.HomeWallpaperStore
import com.flower.launcher.apps.AppListViewModel
import com.flower.launcher.apps.InstalledApp
import com.flower.launcher.ui.LauncherRoot
import com.flower.launcher.ui.theme.FlowerLauncherTheme

class MainActivity : ComponentActivity() {

    private var wallpaperSelectionRevision by mutableIntStateOf(0)
    private val wallpaperImagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { HomeWallpaperStore.importImage(this@MainActivity, uri) }
                wallpaperSelectionRevision++
                Toast.makeText(this@MainActivity, "桌面壁纸已更新", Toast.LENGTH_SHORT).show()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Log.w("HomeCache", "Unable to import wallpaper", failure)
                Toast.makeText(this@MainActivity, "无法读取这张图片，请重新选择", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var resetSignal by mutableIntStateOf(0)
    private var widgetIds by mutableStateOf<List<Int>>(emptyList())
    private var widgetHeights by mutableStateOf<Map<Int, Int>>(emptyMap())
    private lateinit var appWidgetHost: AppWidgetHost
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appListViewModel: AppListViewModel
    private var pendingWidgetId: Int? = null
    private var configuringExistingWidget: Boolean = false

    private val widgetConfigureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val id = pendingWidgetId ?: return@registerForActivityResult
        if (result.resultCode == RESULT_OK && widgetInfo(id) != null) {
            commitWidget(id)
        } else if (!configuringExistingWidget) {
            releaseWidgetId(id)
        }
        clearPendingWidget()
    }

    private val widgetPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val id = pendingWidgetId ?: return@registerForActivityResult
        val returnedId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id) ?: id
        if (result.resultCode != RESULT_OK || returnedId != id) {
            // Only release the ID allocated by this host for this request.
            releaseWidgetId(id)
            clearPendingWidget()
            return@registerForActivityResult
        }
        val info = widgetInfo(id)
        if (info == null) {
            releaseWidgetId(id)
            clearPendingWidget()
            showWidgetFailure()
        } else if (info.configure != null) {
            launchWidgetConfiguration(id, info.configure, existing = false)
        } else {
            commitWidget(id)
            clearPendingWidget()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // A translucent launcher window must never dim the system wallpaper while Home is idle.
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.attributes = window.attributes.apply { dimAmount = 0f }

        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = AppWidgetHost(this, APP_WIDGET_HOST_ID)
        pendingWidgetId = savedInstanceState?.getInt(KEY_PENDING_WIDGET_ID, -1)?.takeIf { it > 0 }
        configuringExistingWidget = savedInstanceState?.getBoolean(KEY_CONFIGURING_EXISTING, false) ?: false
        clearHomeWidgetsForV24Once()
        widgetIds = loadWidgetIds()
        widgetHeights = loadWidgetHeights()
        appListViewModel = ViewModelProvider(this)[AppListViewModel::class.java]
        appListViewModel.load(applicationContext)

        setContent {
            FlowerLauncherTheme {
                LauncherRoot(
                    apps = appListViewModel.apps,
                    resetSignal = resetSignal,
                    wallpaperSelectionRevision = wallpaperSelectionRevision,
                    widgetIds = widgetIds,
                    widgetHeights = widgetHeights,
                    appWidgetHost = appWidgetHost,
                    appWidgetManager = appWidgetManager,
                    onLaunchApp = ::launchApp,
                    onAddWidget = ::pickWidget,
                    onConfigureWidget = ::configureWidget,
                    onRemoveWidget = ::removeWidget,
                    onSetWidgetHeight = ::setWidgetHeight,
                    onOpenWallpaper = ::openWallpaperPicker,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Usually a no-op; retry a failed catalog load on the next foreground entry.
        appListViewModel.load(applicationContext)
        runCatching { appWidgetHost.startListening() }
    }

    override fun onStop() {
        runCatching { appWidgetHost.stopListening() }
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        resetSignal++
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingWidgetId?.let { outState.putInt(KEY_PENDING_WIDGET_ID, it) }
        outState.putBoolean(KEY_CONFIGURING_EXISTING, configuringExistingWidget)
        super.onSaveInstanceState(outState)
    }

    private fun pickWidget() {
        if (pendingWidgetId != null) return
        val id = try {
            appWidgetHost.allocateAppWidgetId()
        } catch (exception: RuntimeException) {
            Log.w("MainActivity", "Unable to allocate widget", exception)
            showWidgetFailure()
            return
        }
        pendingWidgetId = id
        configuringExistingWidget = false
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        }
        try {
            widgetPickerLauncher.launch(intent)
        } catch (exception: RuntimeException) {
            Log.w("MainActivity", "Unable to open widget picker", exception)
            releaseWidgetId(id)
            clearPendingWidget()
            Toast.makeText(this, "系统未提供小部件选择器", Toast.LENGTH_SHORT).show()
        }
    }

    private fun configureWidget(id: Int) {
        if (pendingWidgetId != null) return
        val configure = widgetInfo(id)?.configure ?: return
        launchWidgetConfiguration(id, configure, existing = true)
    }

    private fun launchWidgetConfiguration(id: Int, configure: ComponentName, existing: Boolean) {
        pendingWidgetId = id
        configuringExistingWidget = existing
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            component = configure
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        }
        try {
            widgetConfigureLauncher.launch(intent)
        } catch (exception: RuntimeException) {
            Log.w("MainActivity", "Unable to configure widget", exception)
            if (!existing) releaseWidgetId(id)
            clearPendingWidget()
            showWidgetFailure()
        }
    }

    private fun widgetInfo(id: Int): AppWidgetProviderInfo? = try {
        appWidgetManager.getAppWidgetInfo(id)
    } catch (exception: RuntimeException) {
        Log.w("MainActivity", "Unable to read widget $id", exception)
        null
    }

    private fun releaseWidgetId(id: Int) {
        try {
            appWidgetHost.deleteAppWidgetId(id)
        } catch (exception: RuntimeException) {
            Log.w("MainActivity", "Unable to release widget $id", exception)
        }
    }

    private fun clearPendingWidget() {
        pendingWidgetId = null
        configuringExistingWidget = false
    }

    private fun showWidgetFailure() {
        Toast.makeText(this, "无法加载或配置该小部件", Toast.LENGTH_SHORT).show()
    }

    private fun removeWidget(id: Int) {
        widgetIds = widgetIds.filterNot { it == id }
        widgetHeights = widgetHeights - id
        saveWidgetIds(widgetIds)
        saveWidgetHeights(widgetHeights)
        releaseWidgetId(id)
    }

    private fun setWidgetHeight(id: Int, heightDp: Int) {
        val height = heightDp.coerceIn(96, 320)
        if (widgetHeights[id] == height) return
        widgetHeights = widgetHeights + (id to height)
        saveWidgetHeights(widgetHeights)
    }

    private fun commitWidget(id: Int) {
        if (id !in widgetIds) {
            widgetIds = widgetIds + id
            saveWidgetIds(widgetIds)
        }
    }


    private fun clearHomeWidgetsForV24Once() {
        val prefs = getSharedPreferences(WIDGET_PREFS, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_V24_WIDGETS_CLEARED, false)) return

        val oldIds = prefs.getStringSet(KEY_WIDGET_IDS, emptySet())
            .orEmpty()
            .mapNotNull(String::toIntOrNull)
        oldIds.forEach { id -> releaseWidgetId(id) }

        prefs.edit()
            .remove(KEY_WIDGET_IDS)
            .putBoolean(KEY_V24_WIDGETS_CLEARED, true)
            .apply()
    }

    private fun loadWidgetIds(): List<Int> = getSharedPreferences(WIDGET_PREFS, MODE_PRIVATE)
        .getStringSet(KEY_WIDGET_IDS, emptySet())
        .orEmpty()
        .mapNotNull(String::toIntOrNull)
        .filter { widgetInfo(it) != null }
        .sorted()

    private fun saveWidgetIds(ids: List<Int>) {
        getSharedPreferences(WIDGET_PREFS, MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_WIDGET_IDS, ids.map(Int::toString).toSet())
            .apply()
    }

    private fun loadWidgetHeights(): Map<Int, Int> {
        val prefs = getSharedPreferences(WIDGET_PREFS, MODE_PRIVATE)
        return widgetIds.associateWith { id -> prefs.getInt("widget_height_$id", 138).coerceIn(96, 320) }
    }

    private fun saveWidgetHeights(values: Map<Int, Int>) {
        val editor = getSharedPreferences(WIDGET_PREFS, MODE_PRIVATE).edit()
        widgetIds.forEach { id -> editor.putInt("widget_height_$id", values[id] ?: 138) }
        editor.apply()
    }

    private fun openWallpaperPicker() {
        AlertDialog.Builder(this)
            .setTitle("桌面壁纸")
            .setItems(arrayOf(
                if (android.os.Build.VERSION.SDK_INT >= 31) "选择图片（支持缓存模糊）" else "选择桌面图片",
                "使用系统壁纸", "打开系统壁纸设置")) { _, choice ->
                when (choice) {
                    0 -> wallpaperImagePicker.launch("image/*")
                    1 -> {
                        HomeWallpaperStore.useSystem(this)
                        wallpaperSelectionRevision++
                    }
                    2 -> {
                        HomeWallpaperStore.useSystem(this)
                        wallpaperSelectionRevision++
                        openSystemWallpaperPicker()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun openSystemWallpaperPicker() {
        val intent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
        val fallback = Intent(Intent.ACTION_SET_WALLPAPER)
        runCatching { startActivity(intent) }
            .recoverCatching { startActivity(fallback) }
            .onFailure {
                Toast.makeText(this, "无法打开系统壁纸选择器", Toast.LENGTH_SHORT).show()
            }
    }

    private fun launchApp(app: InstalledApp) {
        hideSearchInput()

        val component = ComponentName(app.packageName, app.activityName)
        val primaryIntent = Intent.makeMainActivity(component).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }

        try {
            startActivity(primaryIntent)
        } catch (exception: ActivityNotFoundException) {
            openFallbackLaunchIntent(app)
        } catch (exception: SecurityException) {
            openFallbackLaunchIntent(app)
        } catch (exception: RuntimeException) {
            openFallbackLaunchIntent(app)
        }
    }

    private fun openFallbackLaunchIntent(app: InstalledApp) {
        val fallbackIntent = try {
            packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
        } catch (exception: RuntimeException) {
            Log.w("MainActivity", "Unable to resolve fallback launch intent", exception)
            null
        }

        if (fallbackIntent == null) {
            showLaunchFailure(app.label)
            return
        }

        runCatching { startActivity(fallbackIntent) }
            .onFailure { showLaunchFailure(app.label) }
    }

    private fun showLaunchFailure(appLabel: String) {
        Toast.makeText(
            this,
            getString(R.string.launch_failed, appLabel),
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun hideSearchInput() {
        window.decorView.clearFocus()
        getSystemService(InputMethodManager::class.java)
            ?.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

    private companion object {
        const val KEY_PENDING_WIDGET_ID = "pending_widget_id"
        const val KEY_CONFIGURING_EXISTING = "configuring_existing_widget"
        const val APP_WIDGET_HOST_ID = 0x464C4F57
        const val WIDGET_PREFS = "flower_widgets"
        const val KEY_WIDGET_IDS = "widget_ids"
        const val KEY_V24_WIDGETS_CLEARED = "v24_widgets_cleared"
    }
}
