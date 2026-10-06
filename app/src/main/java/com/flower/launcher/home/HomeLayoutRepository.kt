package com.flower.launcher.home

import android.content.Context
import android.util.Base64

class HomeLayoutRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): HomePreferences {
        ensureEmptyHomeV24Migration()
        val pinned = prefs.getString(KEY_PINNED_APPS, "")
            .orEmpty()
            .lineSequence()
            .filter { it.isNotBlank() }
            .toList()
        val folders = decodeFolders(prefs.getString(KEY_FOLDERS, "").orEmpty())
        val appPages = decodeStringIntMap(prefs.getString(KEY_APP_PAGES, "").orEmpty())
        val folderPages = decodeLongIntMap(prefs.getString(KEY_FOLDER_PAGES, "").orEmpty())
        val maxAssignedPage = maxOf(
            appPages.values.maxOrNull() ?: 0,
            folderPages.values.maxOrNull() ?: 0,
        )
        val pageCount = prefs.getInt(KEY_PAGE_COUNT, 1)
            .coerceAtLeast(maxAssignedPage + 1)
            .coerceIn(1, MAX_HOME_PAGES)
        val defaultPage = restoredDefaultHomePage(
            savedDefault = if (prefs.contains(KEY_DEFAULT_HOME_PAGE)) prefs.getInt(KEY_DEFAULT_HOME_PAGE, 0) else null,
            legacyLastPage = prefs.getInt(KEY_LAST_HOME_PAGE, 0),
            pageCount = pageCount,
        )
        if (!prefs.contains(KEY_DEFAULT_HOME_PAGE)) {
            // Persist the migration immediately; later visits cannot redefine it.
            prefs.edit().putInt(KEY_DEFAULT_HOME_PAGE, defaultPage).apply()
        }
        return HomePreferences(
            pinnedAppKeys = pinned,
            removedAppKeys = prefs.getStringSet(KEY_REMOVED_APPS, emptySet()).orEmpty(),
            folders = folders,
            showClock = prefs.getBoolean(KEY_SHOW_CLOCK, false),
            showSearchBar = prefs.getBoolean(KEY_SHOW_SEARCH, false),
            gridColumns = prefs.getInt(KEY_GRID_COLUMNS, 4).coerceIn(3, 5),
            layoutLocked = prefs.getBoolean(KEY_LAYOUT_LOCKED, false),
            pageCount = pageCount,
            appPageByKey = pinned.associateWith { appPages[it]?.coerceIn(0, pageCount - 1) ?: 0 },
            folderPageById = folders.associate { folder ->
                folder.id to (folderPages[folder.id]?.coerceIn(0, pageCount - 1) ?: 0)
            },
            lastHomePage = prefs.getInt(KEY_LAST_HOME_PAGE, 0).coerceIn(0, pageCount - 1),
            defaultHomePage = defaultPage,
            clockFont = prefs.getInt(KEY_CLOCK_FONT, 0).coerceIn(0, CLOCK_FONTS.size - 1),
            clockFontWeight = prefs.getInt(KEY_CLOCK_FONT_WEIGHT, 300).coerceIn(100, 900),
            clockFontSizeSp = prefs.getInt(KEY_CLOCK_FONT_SIZE, 78).coerceIn(40, 160),
            clockScaleXPercent = prefs.getInt(KEY_CLOCK_SCALE_X, 100).coerceIn(50, 200),
            clockScaleYPercent = prefs.getInt(KEY_CLOCK_SCALE_Y, 100).coerceIn(50, 200),
            clockTimeColor = prefs.getInt(KEY_CLOCK_TIME_COLOR, 0xFFFFFFFF.toInt()),
            clockDateColor = prefs.getInt(KEY_CLOCK_DATE_COLOR, 0xE6FFFFFF.toInt()),
            clockOffsetX = prefs.getInt(KEY_CLOCK_OFFSET_X, 0),
            clockOffsetY = prefs.getInt(KEY_CLOCK_OFFSET_Y, 0),
        )
    }

    private fun ensureEmptyHomeV24Migration() {
        if (prefs.getBoolean(KEY_V24_EMPTY_HOME_MIGRATED, false)) return
        prefs.edit()
            .putString(KEY_PINNED_APPS, "")
            .putStringSet(KEY_REMOVED_APPS, emptySet())
            .putString(KEY_FOLDERS, "")
            .putBoolean(KEY_SHOW_CLOCK, false)
            .putBoolean(KEY_SHOW_SEARCH, false)
            .putInt(KEY_PAGE_COUNT, 1)
            .putString(KEY_APP_PAGES, "")
            .putString(KEY_FOLDER_PAGES, "")
            .putInt(KEY_LAST_HOME_PAGE, 0)
            .putBoolean(KEY_V24_EMPTY_HOME_MIGRATED, true)
            .apply()
    }

    fun save(value: HomePreferences) {
        val pageCount = value.pageCount.coerceIn(1, MAX_HOME_PAGES)
        prefs.edit()
            .putString(KEY_PINNED_APPS, value.pinnedAppKeys.distinct().joinToString("\n"))
            .putStringSet(KEY_REMOVED_APPS, value.removedAppKeys)
            .putString(KEY_FOLDERS, encodeFolders(value.folders))
            .putBoolean(KEY_SHOW_CLOCK, value.showClock)
            .putBoolean(KEY_SHOW_SEARCH, value.showSearchBar)
            .putInt(KEY_GRID_COLUMNS, value.gridColumns.coerceIn(3, 5))
            .putBoolean(KEY_LAYOUT_LOCKED, value.layoutLocked)
            .putInt(KEY_PAGE_COUNT, pageCount)
            .putString(KEY_APP_PAGES, encodeStringIntMap(value.appPageByKey))
            .putString(KEY_FOLDER_PAGES, encodeLongIntMap(value.folderPageById))
            .putInt(KEY_LAST_HOME_PAGE, value.lastHomePage.coerceIn(0, pageCount - 1))
            .putInt(KEY_DEFAULT_HOME_PAGE, value.defaultHomePage.coerceIn(0, pageCount - 1))
            .putInt(KEY_CLOCK_FONT, value.clockFont.coerceIn(0, CLOCK_FONTS.size - 1))
            .putInt(KEY_CLOCK_FONT_WEIGHT, value.clockFontWeight.coerceIn(100, 900))
            .putInt(KEY_CLOCK_FONT_SIZE, value.clockFontSizeSp.coerceIn(40, 160))
            .putInt(KEY_CLOCK_SCALE_X, value.clockScaleXPercent.coerceIn(50, 200))
            .putInt(KEY_CLOCK_SCALE_Y, value.clockScaleYPercent.coerceIn(50, 200))
            .putInt(KEY_CLOCK_TIME_COLOR, value.clockTimeColor)
            .putInt(KEY_CLOCK_DATE_COLOR, value.clockDateColor)
            .putInt(KEY_CLOCK_OFFSET_X, value.clockOffsetX)
            .putInt(KEY_CLOCK_OFFSET_Y, value.clockOffsetY)
            .apply()
    }

    private fun encodeFolders(folders: List<HomeFolder>): String = folders.joinToString("\n") { folder ->
        val encodedName = Base64.encodeToString(
            folder.name.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP or Base64.URL_SAFE,
        )
        "${folder.id}|$encodedName|${folder.appKeys.distinct().joinToString(",")}" 
    }

    private fun decodeFolders(raw: String): List<HomeFolder> = raw
        .lineSequence()
        .mapNotNull { line ->
            val parts = line.split('|', limit = 3)
            if (parts.size != 3) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val name = runCatching {
                String(Base64.decode(parts[1], Base64.NO_WRAP or Base64.URL_SAFE), Charsets.UTF_8)
            }.getOrNull() ?: return@mapNotNull null
            val appKeys = parts[2].split(',').filter { it.isNotBlank() }
            HomeFolder(id = id, name = name, appKeys = appKeys)
        }
        .toList()

    private fun encodeStringIntMap(values: Map<String, Int>): String = values.entries.joinToString("\n") { (key, page) ->
        val encoded = Base64.encodeToString(key.toByteArray(Charsets.UTF_8), Base64.NO_WRAP or Base64.URL_SAFE)
        "$encoded|${page.coerceIn(0, MAX_HOME_PAGES - 1)}"
    }

    private fun decodeStringIntMap(raw: String): Map<String, Int> = raw.lineSequence().mapNotNull { line ->
        val parts = line.split('|', limit = 2)
        if (parts.size != 2) return@mapNotNull null
        val key = runCatching {
            String(Base64.decode(parts[0], Base64.NO_WRAP or Base64.URL_SAFE), Charsets.UTF_8)
        }.getOrNull() ?: return@mapNotNull null
        val page = parts[1].toIntOrNull() ?: return@mapNotNull null
        key to page
    }.toMap()

    private fun encodeLongIntMap(values: Map<Long, Int>): String = values.entries.joinToString("\n") { (id, page) ->
        "$id|${page.coerceIn(0, MAX_HOME_PAGES - 1)}"
    }

    private fun decodeLongIntMap(raw: String): Map<Long, Int> = raw.lineSequence().mapNotNull { line ->
        val parts = line.split('|', limit = 2)
        if (parts.size != 2) return@mapNotNull null
        val id = parts[0].toLongOrNull() ?: return@mapNotNull null
        val page = parts[1].toIntOrNull() ?: return@mapNotNull null
        id to page
    }.toMap()

    private companion object {
        const val MAX_HOME_PAGES = 9
        const val PREFS_NAME = "flower_home_layout"
        const val KEY_PINNED_APPS = "pinned_apps"
        const val KEY_REMOVED_APPS = "removed_apps"
        const val KEY_FOLDERS = "folders"
        const val KEY_SHOW_CLOCK = "show_clock"
        const val KEY_SHOW_SEARCH = "show_search"
        const val KEY_GRID_COLUMNS = "grid_columns"
        const val KEY_LAYOUT_LOCKED = "layout_locked"
        const val KEY_PAGE_COUNT = "page_count"
        const val KEY_APP_PAGES = "app_pages"
        const val KEY_FOLDER_PAGES = "folder_pages"
        const val KEY_LAST_HOME_PAGE = "last_home_page"
        const val KEY_DEFAULT_HOME_PAGE = "default_home_page"
        const val KEY_CLOCK_FONT = "clock_font"
        const val KEY_CLOCK_FONT_WEIGHT = "clock_font_weight"
        const val KEY_CLOCK_FONT_SIZE = "clock_font_size_sp"
        const val KEY_CLOCK_SCALE_X = "clock_scale_x_percent"
        const val KEY_CLOCK_SCALE_Y = "clock_scale_y_percent"
        const val KEY_CLOCK_TIME_COLOR = "clock_time_color"
        const val KEY_CLOCK_DATE_COLOR = "clock_date_color"
        const val KEY_CLOCK_OFFSET_X = "clock_offset_x"
        const val KEY_CLOCK_OFFSET_Y = "clock_offset_y"
        const val KEY_V24_EMPTY_HOME_MIGRATED = "v24_empty_home_migrated"
    }
}
