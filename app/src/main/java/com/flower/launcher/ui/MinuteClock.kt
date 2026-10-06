package com.flower.launcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

internal data class MinuteClockValue(val time: LocalDateTime, val locale: Locale)

private fun currentMinute() = MinuteClockValue(
    LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES),
    Locale.getDefault(),
)

/** No private polling loop: listen only while the host is visible. */
@Composable
internal fun rememberMinuteClock(): State<MinuteClockValue> {
    val context = LocalContext.current
    val view = LocalView.current
    val clock = remember { mutableStateOf(currentMinute()) }
    DisposableEffect(context, view) {
        val lifecycle = view.findViewTreeLifecycleOwner()?.lifecycle
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                clock.value = currentMinute()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
        }
        var registered = false
        fun stop() {
            if (registered) {
                context.unregisterReceiver(receiver)
                registered = false
            }
        }
        fun start() {
            if (!registered) {
                ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
                registered = true
            }
            // Covers time spent off-screen/asleep and changes while unregistered.
            clock.value = currentMinute()
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> start()
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> stop()
                else -> Unit
            }
        }
        lifecycle?.addObserver(observer)
        if (lifecycle == null || lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) start()
        onDispose {
            lifecycle?.removeObserver(observer)
            stop()
        }
    }
    return clock
}
