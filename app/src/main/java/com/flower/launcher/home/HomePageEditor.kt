package com.flower.launcher.home

const val MAX_HOME_PAGES = 9

fun homePageItemCount(preferences: HomePreferences, index: Int): Int {
    val apps = preferences.appPageByKey.values.count { it == index }
    val folders = preferences.folderPageById.values.count { it == index }
    return apps + folders
}

fun addHomePage(preferences: HomePreferences): HomePreferences {
    val count = preferences.pageCount.coerceIn(1, MAX_HOME_PAGES)
    if (count >= MAX_HOME_PAGES) return preferences
    return preferences.copy(pageCount = count + 1)
}

fun removeHomePage(preferences: HomePreferences, index: Int): HomePreferences {
    val count = preferences.pageCount.coerceIn(1, MAX_HOME_PAGES)
    if (count <= 1) return preferences
    val removed = index.coerceIn(0, count - 1)
    val appPages = preferences.appPageByKey
        .filterValues { it != removed }
        .mapValues { (_, page) -> if (page > removed) page - 1 else page }
    val folderPages = preferences.folderPageById
        .filterValues { it != removed }
        .mapValues { (_, page) -> if (page > removed) page - 1 else page }
    val newCount = count - 1
    val newLast = pageAfterRemoval(preferences.lastHomePage, removed, newCount)
    return preferences.copy(
        pageCount = newCount,
        appPageByKey = appPages,
        folderPageById = folderPages,
        lastHomePage = newLast,
        defaultHomePage = pageAfterRemoval(preferences.defaultHomePage, removed, newCount),
    )
}

fun setDefaultHomePage(preferences: HomePreferences, index: Int): HomePreferences {
    val count = preferences.pageCount.coerceIn(1, MAX_HOME_PAGES)
    return preferences.copy(defaultHomePage = index.coerceIn(0, count - 1))
}

/** Keep the same logical page when an earlier page is deleted. */
private fun pageAfterRemoval(page: Int, removed: Int, newCount: Int): Int = when {
    page == removed -> (removed - 1).coerceAtLeast(0)
    page > removed -> page - 1
    else -> page
}.coerceIn(0, newCount - 1)

/** V61 had one ambiguous page value; preserve it once as the user's default. */
internal fun restoredDefaultHomePage(savedDefault: Int?, legacyLastPage: Int, pageCount: Int): Int =
    (savedDefault ?: legacyLastPage).coerceIn(0, pageCount.coerceIn(1, MAX_HOME_PAGES) - 1)

/** Visiting a page must never change the default. */
fun visitHomePage(preferences: HomePreferences, index: Int): HomePreferences =
    preferences.copy(lastHomePage = index.coerceIn(0, preferences.pageCount.coerceIn(1, MAX_HOME_PAGES) - 1))

/** An explicit navigation command, independent of persisted visit history. */
data class HomePageRequest(val page: Int, val sequence: Long = 0L) {
    fun navigateTo(page: Int): HomePageRequest = HomePageRequest(page, sequence + 1)
}
