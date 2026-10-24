package com.flower.launcher.apps

import android.content.Context

/** User-owned app customizations that should survive launcher restarts. */
data class AppCustomizations(
    val hiddenComponentKeys: Set<String> = emptySet(),
    /** Blacklisted apps are fully excluded from Search results and the app drawer. */
    val blockedComponentKeys: Set<String> = emptySet(),
    val aliases: Map<String, String> = emptyMap(),
)

class AppCustomizationRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): AppCustomizations {
        val hidden = prefs.getStringSet(KEY_HIDDEN, emptySet()).orEmpty().toSet()
        val blocked = prefs.getStringSet(KEY_BLOCKED, emptySet()).orEmpty().toSet()
        val aliases = prefs.all
            .asSequence()
            .filter { (key, value) -> key.startsWith(ALIAS_PREFIX) && value is String }
            .associate { (key, value) -> key.removePrefix(ALIAS_PREFIX) to (value as String) }
        return AppCustomizations(
            hiddenComponentKeys = hidden,
            blockedComponentKeys = blocked,
            aliases = aliases,
        )
    }

    fun setHidden(current: AppCustomizations, componentKey: String, hidden: Boolean): AppCustomizations {
        if ((componentKey in current.hiddenComponentKeys) == hidden) return current
        val updated = if (hidden) {
            current.hiddenComponentKeys + componentKey
        } else {
            current.hiddenComponentKeys - componentKey
        }
        prefs.edit().putStringSet(KEY_HIDDEN, updated).apply()
        return current.copy(hiddenComponentKeys = updated)
    }

    fun setBlocked(current: AppCustomizations, componentKey: String, blocked: Boolean): AppCustomizations {
        if ((componentKey in current.blockedComponentKeys) == blocked) return current
        val updated = if (blocked) {
            current.blockedComponentKeys + componentKey
        } else {
            current.blockedComponentKeys - componentKey
        }
        prefs.edit().putStringSet(KEY_BLOCKED, updated).apply()
        return current.copy(blockedComponentKeys = updated)
    }

    fun setAlias(current: AppCustomizations, componentKey: String, alias: String?): AppCustomizations {
        val normalized = alias?.trim().orEmpty()
        if (current.aliases[componentKey].orEmpty() == normalized) return current
        val updated = current.aliases.toMutableMap().apply {
            if (normalized.isBlank()) remove(componentKey) else put(componentKey, normalized)
        }.toMap()

        prefs.edit().apply {
            if (normalized.isBlank()) remove(ALIAS_PREFIX + componentKey)
            else putString(ALIAS_PREFIX + componentKey, normalized)
        }.apply()

        return current.copy(aliases = updated)
    }

    private companion object {
        const val PREFS = "flower_app_customizations"
        const val KEY_HIDDEN = "hidden_components"
        const val KEY_BLOCKED = "blocked_components"
        const val ALIAS_PREFIX = "alias::"
    }
}
