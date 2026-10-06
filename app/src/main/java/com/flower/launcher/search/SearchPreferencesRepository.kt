package com.flower.launcher.search

import android.content.Context

data class SearchPreferences(
    val provider: String = "Baidu",
    val showHiddenApps: Boolean = false,
    val launchContactsCount: Int = 2,
    val launchAppsCount: Int = 4,
    val contactsEnabled: Boolean = true,
    val shortcutsEnabled: Boolean = true,
    val mediaEnabled: Boolean = true,
    val calendarEnabled: Boolean = true,
    val settingsEnabled: Boolean = true,
    val imeCollapseOverflowThresholdDp: Int = 80,
    val imeRevealPullThresholdDp: Int = 200,
    val homeReturnPullThresholdDp: Int = 200,
    val imeRevealHapticEnabled: Boolean = true,
    val imeRevealHapticStrengthPercent: Int = 60,
    val homeReturnHapticEnabled: Boolean = true,
    val homeReturnHapticStrengthPercent: Int = 60,
    val searchFieldHeightDp: Int = 44,
    val searchFieldCornerRadiusDp: Int = 22,
    val searchBarColorPresetId: String = "neutral",
    val searchBarColorDepth: Int = 30,
    val searchBarOpacityPercent: Int = 24,
)

class SearchPreferencesRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): SearchPreferences = SearchPreferences(
        provider = prefs.getString(KEY_PROVIDER, "Baidu") ?: "Baidu",
        showHiddenApps = prefs.getBoolean(KEY_SHOW_HIDDEN, false),
        launchContactsCount = prefs.getInt(KEY_LAUNCH_CONTACTS, 2).coerceIn(0, 8),
        launchAppsCount = prefs.getInt(KEY_LAUNCH_APPS, 4).coerceIn(0, 12),
        contactsEnabled = prefs.getBoolean(KEY_CONTACTS, true),
        shortcutsEnabled = prefs.getBoolean(KEY_SHORTCUTS, true),
        mediaEnabled = prefs.getBoolean(KEY_MEDIA, true),
        calendarEnabled = prefs.getBoolean(KEY_CALENDAR, true),
        settingsEnabled = prefs.getBoolean(KEY_SETTINGS, true),
        imeCollapseOverflowThresholdDp = prefs.getInt(KEY_IME_COLLAPSE_OVERFLOW, 80).coerceIn(0, 300),
        imeRevealPullThresholdDp = prefs.getInt(KEY_IME_REVEAL_PULL, 200).coerceIn(80, 360),
        homeReturnPullThresholdDp = prefs.getInt(KEY_HOME_RETURN_PULL, 200).coerceIn(80, 360),
        imeRevealHapticEnabled = prefs.getBoolean(KEY_IME_REVEAL_HAPTIC_ENABLED, true),
        imeRevealHapticStrengthPercent = prefs.getInt(KEY_IME_REVEAL_HAPTIC_STRENGTH, 60).coerceIn(10, 100),
        homeReturnHapticEnabled = prefs.getBoolean(KEY_HOME_RETURN_HAPTIC_ENABLED, true),
        homeReturnHapticStrengthPercent = prefs.getInt(KEY_HOME_RETURN_HAPTIC_STRENGTH, 60).coerceIn(10, 100),
        searchFieldHeightDp = prefs.getInt(KEY_SEARCH_FIELD_HEIGHT, 44).coerceIn(36, 64),
        searchFieldCornerRadiusDp = prefs.getInt(KEY_SEARCH_FIELD_CORNER_RADIUS, 22).coerceIn(8, 32),
        searchBarColorPresetId = prefs.getString(KEY_SEARCH_BAR_COLOR_PRESET_ID, "neutral")?.ifBlank { "neutral" } ?: "neutral",
        searchBarColorDepth = prefs.getInt(KEY_SEARCH_BAR_COLOR_DEPTH, 30).coerceIn(0, 100),
        searchBarOpacityPercent = if (prefs.contains(KEY_SEARCH_BAR_OPACITY)) {
            prefs.getInt(KEY_SEARCH_BAR_OPACITY, 24).coerceIn(0, 100)
        } else {
            // Migrate the old V54 depth-driven alpha (10%..55%) into the new
            // independent opacity control so an upgrade keeps a similar visual weight.
            val legacyDepth = prefs.getInt(KEY_SEARCH_BAR_COLOR_DEPTH, 30).coerceIn(0, 100)
            (10 + (legacyDepth * 45 + 50) / 100).coerceIn(0, 100)
        },
    )

    fun save(value: SearchPreferences) {
        prefs.edit()
            .putString(KEY_PROVIDER, value.provider)
            .putBoolean(KEY_SHOW_HIDDEN, value.showHiddenApps)
            .putInt(KEY_LAUNCH_CONTACTS, value.launchContactsCount)
            .putInt(KEY_LAUNCH_APPS, value.launchAppsCount)
            .putBoolean(KEY_CONTACTS, value.contactsEnabled)
            .putBoolean(KEY_SHORTCUTS, value.shortcutsEnabled)
            .putBoolean(KEY_MEDIA, value.mediaEnabled)
            .putBoolean(KEY_CALENDAR, value.calendarEnabled)
            .putBoolean(KEY_SETTINGS, value.settingsEnabled)
            .putInt(KEY_IME_COLLAPSE_OVERFLOW, value.imeCollapseOverflowThresholdDp.coerceIn(0, 300))
            .putInt(KEY_IME_REVEAL_PULL, value.imeRevealPullThresholdDp.coerceIn(80, 360))
            .putInt(KEY_HOME_RETURN_PULL, value.homeReturnPullThresholdDp.coerceIn(80, 360))
            .putBoolean(KEY_IME_REVEAL_HAPTIC_ENABLED, value.imeRevealHapticEnabled)
            .putInt(KEY_IME_REVEAL_HAPTIC_STRENGTH, value.imeRevealHapticStrengthPercent.coerceIn(10, 100))
            .putBoolean(KEY_HOME_RETURN_HAPTIC_ENABLED, value.homeReturnHapticEnabled)
            .putInt(KEY_HOME_RETURN_HAPTIC_STRENGTH, value.homeReturnHapticStrengthPercent.coerceIn(10, 100))
            .putInt(KEY_SEARCH_FIELD_HEIGHT, value.searchFieldHeightDp.coerceIn(36, 64))
            .putInt(KEY_SEARCH_FIELD_CORNER_RADIUS, value.searchFieldCornerRadiusDp.coerceIn(8, 32))
            .putString(KEY_SEARCH_BAR_COLOR_PRESET_ID, value.searchBarColorPresetId.ifBlank { "neutral" })
            .putInt(KEY_SEARCH_BAR_COLOR_DEPTH, value.searchBarColorDepth.coerceIn(0, 100))
            .putInt(KEY_SEARCH_BAR_OPACITY, value.searchBarOpacityPercent.coerceIn(0, 100))
            .apply()
    }

    private companion object {
        const val PREFS = "flower_search_settings"
        const val KEY_PROVIDER = "provider"
        const val KEY_SHOW_HIDDEN = "show_hidden_apps"
        const val KEY_LAUNCH_CONTACTS = "launch_contacts_count"
        const val KEY_LAUNCH_APPS = "launch_apps_count"
        const val KEY_CONTACTS = "contacts_enabled"
        const val KEY_SHORTCUTS = "shortcuts_enabled"
        const val KEY_MEDIA = "media_enabled"
        const val KEY_CALENDAR = "calendar_enabled"
        const val KEY_SETTINGS = "settings_enabled"
        const val KEY_IME_COLLAPSE_OVERFLOW = "ime_collapse_overflow_threshold_dp"
        const val KEY_IME_REVEAL_PULL = "ime_reveal_pull_threshold_dp"
        const val KEY_HOME_RETURN_PULL = "home_return_pull_threshold_dp"
        const val KEY_IME_REVEAL_HAPTIC_ENABLED = "ime_reveal_haptic_enabled"
        const val KEY_IME_REVEAL_HAPTIC_STRENGTH = "ime_reveal_haptic_strength_percent"
        const val KEY_HOME_RETURN_HAPTIC_ENABLED = "home_return_haptic_enabled"
        const val KEY_HOME_RETURN_HAPTIC_STRENGTH = "home_return_haptic_strength_percent"
        const val KEY_SEARCH_FIELD_HEIGHT = "search_field_height_dp"
        const val KEY_SEARCH_FIELD_CORNER_RADIUS = "search_field_corner_radius_dp"
        const val KEY_SEARCH_BAR_COLOR_PRESET_ID = "search_bar_color_preset_id"
        const val KEY_SEARCH_BAR_COLOR_DEPTH = "search_bar_color_depth"
        const val KEY_SEARCH_BAR_OPACITY = "search_bar_opacity_percent"
    }
}
