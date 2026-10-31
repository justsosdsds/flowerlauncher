package com.flower.launcher.search

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShepherdHabitShortcutTest {
    @Test fun matchesCompleteChineseCommand() {
        assertTrue(ShepherdHabitShortcut.matches("习惯"))
        assertTrue(ShepherdHabitShortcut.matches(" 习惯 "))
    }

    @Test fun doesNotCaptureNormalAppSearches() {
        listOf("", "习", "习惯养成", "我的习惯", "/习惯", "habit", "记录")
            .forEach { assertFalse("query=$it", ShepherdHabitShortcut.matches(it)) }
    }
}
