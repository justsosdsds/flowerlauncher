package com.flower.launcher.search

/** The habit page is a destination, not a separate installed application. */
object ShepherdHabitShortcut {
    const val PACKAGE_NAME = "com.shepherd.timetracker"
    const val DEEP_LINK_URI = "shepherd://habit"

    /** Only a complete command should override normal app-search/IME behavior. */
    fun matches(query: String): Boolean = query.trim() == "习惯"
}
