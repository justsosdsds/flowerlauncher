package com.flower.launcher.apps

import android.icu.text.Transliterator

/**
 * Produces a Latin/pinyin sort key and index initial for app labels.
 *
 * Uses the platform ICU transliterator ("Han-Latin") so Chinese names are indexed
 * by their pinyin initial, e.g. 原子笔记 -> Y. Results are cached per label because
 * they are only recomputed when the app list changes.
 */
object AppNameSortKey {

    private val transliterator: Transliterator? by lazy { createTransliterator() }

    private val cache = HashMap<String, AppNameKeys>()
    private val lock = Any()

    fun sortKey(label: String): String = keys(label).sortKey

    fun initial(label: String): String = keys(label).initial

    // Cache the complete derived values, not just ICU output. The same lock also
    // serializes ICU access between catalog loading and alias edits on the UI.
    internal fun keys(label: String): AppNameKeys = synchronized(lock) {
        cache.getOrPut(label) {
            val transliterated = runCatching {
                transliterator?.transliterate(label) ?: label
            }.getOrDefault(label)
            appNameKeysFromTransliteration(transliterated)
        }
    }

    private fun createTransliterator(): Transliterator? {
        val ids = listOf("Han-Latin; Latin-ASCII", "Han-Latin")
        for (id in ids) {
            val created = runCatching { Transliterator.getInstance(id) }.getOrNull()
            if (created != null) return created
        }
        return null
    }
}
