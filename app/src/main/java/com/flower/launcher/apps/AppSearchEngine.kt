package com.flower.launcher.apps

object AppSearchEngine {
    fun prepare(apps: List<InstalledApp>): AppSearchIndex = AppSearchIndex(apps)

    fun search(apps: List<InstalledApp>, query: String): List<InstalledApp> =
        prepare(apps).search(query)
}

/** One index per app/alias list. Preserves the original score and stable tie order. */
class AppSearchIndex internal constructor(private val apps: List<InstalledApp>) {
    private val alphabetical = apps.sortedBy { it.searchLabel }

    fun search(query: String): List<InstalledApp> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return apps

        // Already sorted within each score: no Pair allocations or query-time sorting.
        val exact = ArrayList<InstalledApp>()
        val prefix = ArrayList<InstalledApp>()
        val contains = ArrayList<InstalledApp>()
        val packages = ArrayList<InstalledApp>()
        for (app in alphabetical) {
            when {
                app.searchLabel == needle -> exact.add(app)
                app.searchLabel.startsWith(needle) -> prefix.add(app)
                app.searchLabel.contains(needle) -> contains.add(app)
                app.searchPackageName.contains(needle) -> packages.add(app)
            }
        }
        return ArrayList<InstalledApp>(exact.size + prefix.size + contains.size + packages.size).apply {
            addAll(exact)
            addAll(prefix)
            addAll(contains)
            addAll(packages)
        }
    }
}
