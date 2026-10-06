package com.flower.launcher.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main-thread confined. UI state changes immediately; only persistence is delayed.
 * flush() is synchronous so lifecycle/disposal does not depend on a cancelled scope.
 * The supplied save function should enqueue persistence (SharedPreferences.apply).
 */
internal class DebouncedPreferenceWriter<T>(
    private val scope: CoroutineScope,
    initialValue: T,
    private val delayMillis: Long = 300L,
    private val save: (T) -> Unit,
) {
    private var savedValue = initialValue
    private var latestValue = initialValue
    private var job: Job? = null

    fun submit(value: T) {
        if (value == latestValue) return
        latestValue = value
        job?.cancel()
        job = null
        if (latestValue == savedValue) return
        job = scope.launch {
            delay(delayMillis)
            persistLatest()
        }
    }

    fun flush() {
        job?.cancel()
        job = null
        persistLatest()
    }

    private fun persistLatest() {
        if (latestValue == savedValue) return
        val value = latestValue
        save(value)
        savedValue = value
    }
}
