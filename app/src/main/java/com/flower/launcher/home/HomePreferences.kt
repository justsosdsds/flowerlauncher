package com.flower.launcher.home

data class HomeFolder(
    val id: Long,
    val name: String,
    val appKeys: List<String>,
)

data class HomePreferences(
    val pinnedAppKeys: List<String> = emptyList(),
    val removedAppKeys: Set<String> = emptySet(),
    val folders: List<HomeFolder> = emptyList(),
    val showClock: Boolean = false,
    val showSearchBar: Boolean = false,
    val gridColumns: Int = 4,
    val layoutLocked: Boolean = false,
    val pageCount: Int = 1,
    val appPageByKey: Map<String, Int> = emptyMap(),
    val folderPageById: Map<Long, Int> = emptyMap(),
    val lastHomePage: Int = 0,
    val defaultHomePage: Int = 0,
    val clockFont: Int = 0,
    val clockFontWeight: Int = 300,
    val clockFontSizeSp: Int = 78,
    val clockScaleXPercent: Int = 100,
    val clockScaleYPercent: Int = 100,
    val clockTimeColor: Int = 0xFFFFFFFF.toInt(),
    val clockDateColor: Int = 0xE6FFFFFF.toInt(),
    val clockOffsetX: Int = 0,
    val clockOffsetY: Int = 0,
)

