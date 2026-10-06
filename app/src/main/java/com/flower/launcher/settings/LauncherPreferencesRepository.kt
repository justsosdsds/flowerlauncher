package com.flower.launcher.settings

import android.content.Context

data class LauncherPreferences(
    val enableSwipeGestures: Boolean = true,
    val vibrationOnGesture: Boolean = true,
    val blurEnabled: Boolean = true,
    val blurStrengthPercent: Int = 100,
    val homeBackgroundDimPercent: Int = 20,
    val searchReboundDurationMs: Int = 320,
    val gestureDragDistanceDp: Int = 320,
    val gestureSettleThresholdPercent: Int = 38,
    val gestureOpenVelocityDpPerSec: Int = 900,
    val searchReturnImeHideThresholdDp: Int = 8,
    val drawerSortMode: String = "Name",
    val drawerColumns: Int = 4,
    val drawerIconSizeDp: Int = 58,
    val drawerVerticalSpacingDp: Int = 20,
    val drawerShowLabels: Boolean = true,
    val drawerLabelSizeSp: Int = 12,
    val drawerAllowLabelWrap: Boolean = true,
    val drawerShowLetterHeaders: Boolean = true,
    val drawerUseCustomPadding: Boolean = false,
    val drawerUseCustomIconAppearance: Boolean = false,
    val drawerAdaptiveIcons: Boolean = true,
    val drawerIconShape: Int = 2,
    val drawerBackgroundColor: Int = 0xFF1B0D24.toInt(),
    val drawerBackgroundOpacityPercent: Int = 65,
    val uiScalePercent: Int = 90,
)

class LauncherPreferencesRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): LauncherPreferences = LauncherPreferences(
        enableSwipeGestures = prefs.getBoolean(KEY_ENABLE_GESTURES, true),
        vibrationOnGesture = prefs.getBoolean(KEY_GESTURE_HAPTICS, true),
        blurEnabled = prefs.getBoolean(KEY_BLUR_ENABLED, true),
        blurStrengthPercent = prefs.getInt(KEY_BLUR_STRENGTH, 100).coerceIn(0, 100),
        homeBackgroundDimPercent = prefs.getInt(KEY_HOME_BG_DIM, 20).coerceIn(0, 50),
        searchReboundDurationMs = prefs.getInt(KEY_SEARCH_REBOUND_DURATION, 320).coerceIn(160, 600),
        gestureDragDistanceDp = prefs.getInt(KEY_GESTURE_DRAG_DISTANCE, 320).coerceIn(160, 480),
        gestureSettleThresholdPercent = prefs.getInt(KEY_GESTURE_SETTLE_THRESHOLD, 38).coerceIn(15, 60),
        gestureOpenVelocityDpPerSec = prefs.getInt(KEY_GESTURE_OPEN_VELOCITY, 900).coerceIn(300, 1800),
        searchReturnImeHideThresholdDp = prefs.getInt(KEY_SEARCH_RETURN_IME_HIDE_THRESHOLD, 8).coerceIn(0, 40),
        drawerSortMode = prefs.getString(KEY_DRAWER_SORT_MODE, "Name") ?: "Name",
        drawerColumns = prefs.getInt(KEY_DRAWER_COLUMNS, 4).coerceIn(3, 7),
        drawerIconSizeDp = prefs.getInt(KEY_DRAWER_ICON_SIZE, 58).coerceIn(40, 96),
        drawerVerticalSpacingDp = prefs.getInt(KEY_DRAWER_VERTICAL_SPACING, 20).coerceIn(0, 48),
        drawerShowLabels = prefs.getBoolean(KEY_DRAWER_SHOW_LABELS, true),
        drawerLabelSizeSp = prefs.getInt(KEY_DRAWER_LABEL_SIZE, 12).coerceIn(8, 20),
        drawerAllowLabelWrap = prefs.getBoolean(KEY_DRAWER_ALLOW_LABEL_WRAP, true),
        drawerShowLetterHeaders = prefs.getBoolean(KEY_DRAWER_SHOW_LETTER_HEADERS, true),
        drawerUseCustomPadding = prefs.getBoolean(KEY_DRAWER_CUSTOM_PADDING, false),
        drawerUseCustomIconAppearance = prefs.getBoolean(KEY_DRAWER_CUSTOM_ICON_APPEARANCE, false),
        drawerAdaptiveIcons = prefs.getBoolean(KEY_DRAWER_ADAPTIVE_ICONS, true),
        drawerIconShape = prefs.getInt(KEY_DRAWER_ICON_SHAPE, 2).coerceIn(0, 4),
        drawerBackgroundColor = prefs.getInt(KEY_DRAWER_BG_COLOR, 0xFF1B0D24.toInt()),
        drawerBackgroundOpacityPercent = prefs.getInt(KEY_DRAWER_BG_OPACITY, 65).coerceIn(0, 100),
        uiScalePercent = prefs.getInt(KEY_UI_SCALE, 90).coerceIn(70, 110),
    )

    fun save(value: LauncherPreferences) {
        prefs.edit()
            .putBoolean(KEY_ENABLE_GESTURES, value.enableSwipeGestures)
            .putBoolean(KEY_GESTURE_HAPTICS, value.vibrationOnGesture)
            .putBoolean(KEY_BLUR_ENABLED, value.blurEnabled)
            .putInt(KEY_BLUR_STRENGTH, value.blurStrengthPercent.coerceIn(0, 100))
            .putInt(KEY_HOME_BG_DIM, value.homeBackgroundDimPercent.coerceIn(0, 50))
            .putInt(KEY_SEARCH_REBOUND_DURATION, value.searchReboundDurationMs.coerceIn(160, 600))
            .putInt(KEY_GESTURE_DRAG_DISTANCE, value.gestureDragDistanceDp.coerceIn(160, 480))
            .putInt(KEY_GESTURE_SETTLE_THRESHOLD, value.gestureSettleThresholdPercent.coerceIn(15, 60))
            .putInt(KEY_GESTURE_OPEN_VELOCITY, value.gestureOpenVelocityDpPerSec.coerceIn(300, 1800))
            .putInt(KEY_SEARCH_RETURN_IME_HIDE_THRESHOLD, value.searchReturnImeHideThresholdDp.coerceIn(0, 40))
            .putString(KEY_DRAWER_SORT_MODE, value.drawerSortMode)
            .putInt(KEY_DRAWER_COLUMNS, value.drawerColumns.coerceIn(3, 7))
            .putInt(KEY_DRAWER_ICON_SIZE, value.drawerIconSizeDp.coerceIn(40, 96))
            .putInt(KEY_DRAWER_VERTICAL_SPACING, value.drawerVerticalSpacingDp.coerceIn(0, 48))
            .putBoolean(KEY_DRAWER_SHOW_LABELS, value.drawerShowLabels)
            .putInt(KEY_DRAWER_LABEL_SIZE, value.drawerLabelSizeSp.coerceIn(8, 20))
            .putBoolean(KEY_DRAWER_ALLOW_LABEL_WRAP, value.drawerAllowLabelWrap)
            .putBoolean(KEY_DRAWER_SHOW_LETTER_HEADERS, value.drawerShowLetterHeaders)
            .putBoolean(KEY_DRAWER_CUSTOM_PADDING, value.drawerUseCustomPadding)
            .putBoolean(KEY_DRAWER_CUSTOM_ICON_APPEARANCE, value.drawerUseCustomIconAppearance)
            .putBoolean(KEY_DRAWER_ADAPTIVE_ICONS, value.drawerAdaptiveIcons)
            .putInt(KEY_DRAWER_ICON_SHAPE, value.drawerIconShape.coerceIn(0, 4))
            .putInt(KEY_DRAWER_BG_COLOR, value.drawerBackgroundColor)
            .putInt(KEY_DRAWER_BG_OPACITY, value.drawerBackgroundOpacityPercent.coerceIn(0, 100))
            .putInt(KEY_UI_SCALE, value.uiScalePercent.coerceIn(70, 110))
            .apply()
    }

    private companion object {
        const val PREFS = "flower_launcher_settings"
        const val KEY_ENABLE_GESTURES = "enable_swipe_gestures"
        const val KEY_GESTURE_HAPTICS = "vibration_on_gesture"
        const val KEY_BLUR_ENABLED = "blur_enabled"
        const val KEY_BLUR_STRENGTH = "blur_strength_percent"
        const val KEY_HOME_BG_DIM = "home_background_dim_percent"
        const val KEY_SEARCH_REBOUND_DURATION = "search_rebound_duration_ms"
        const val KEY_GESTURE_DRAG_DISTANCE = "gesture_drag_distance_dp"
        const val KEY_GESTURE_SETTLE_THRESHOLD = "gesture_settle_threshold_percent"
        const val KEY_GESTURE_OPEN_VELOCITY = "gesture_open_velocity_dp_per_sec"
        const val KEY_SEARCH_RETURN_IME_HIDE_THRESHOLD = "search_return_ime_hide_threshold_dp"
        const val KEY_DRAWER_SORT_MODE = "drawer_sort_mode"
        const val KEY_DRAWER_COLUMNS = "drawer_columns"
        const val KEY_DRAWER_ICON_SIZE = "drawer_icon_size_dp"
        const val KEY_DRAWER_VERTICAL_SPACING = "drawer_vertical_spacing_dp"
        const val KEY_DRAWER_SHOW_LABELS = "drawer_show_labels"
        const val KEY_DRAWER_LABEL_SIZE = "drawer_label_size_sp"
        const val KEY_DRAWER_ALLOW_LABEL_WRAP = "drawer_allow_label_wrap"
        const val KEY_DRAWER_SHOW_LETTER_HEADERS = "drawer_show_letter_headers"
        const val KEY_DRAWER_CUSTOM_PADDING = "drawer_use_custom_padding"
        const val KEY_DRAWER_CUSTOM_ICON_APPEARANCE = "drawer_use_custom_icon_appearance"
        const val KEY_DRAWER_ADAPTIVE_ICONS = "drawer_adaptive_icons"
        const val KEY_DRAWER_ICON_SHAPE = "drawer_icon_shape"
        const val KEY_DRAWER_BG_COLOR = "drawer_background_color"
        const val KEY_DRAWER_BG_OPACITY = "drawer_background_opacity_percent"
        const val KEY_UI_SCALE = "ui_scale_percent"
    }
}
