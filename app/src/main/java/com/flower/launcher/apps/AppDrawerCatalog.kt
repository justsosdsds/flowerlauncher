package com.flower.launcher.apps

internal data class AppNameKeys(val sortKey: String, val initial: String)

/** Same normalization and '#' ordering as the original drawer. Pure for testing. */
internal fun appNameKeysFromTransliteration(transliterated: String): AppNameKeys {
    val normalized = buildString(transliterated.length) {
        for (ch in transliterated.lowercase()) {
            if (ch in 'a'..'z' || ch in '0'..'9') append(ch)
        }
    }
    return if (normalized.isEmpty() || normalized[0] !in 'a'..'z') {
        AppNameKeys(sortKey = '\uFFFF' + normalized, initial = "#")
    } else {
        AppNameKeys(sortKey = normalized, initial = normalized[0].uppercaseChar().toString())
    }
}

internal data class DrawerAppList(
    val apps: List<InstalledApp>,
    val letters: List<String> = emptyList(),
    val letterStarts: Map<String, Int> = emptyMap(),
)

/** Owned above the conditional drawer UI, so closing it cannot discard the index. */
internal class AppDrawerCatalog(
    apps: List<InstalledApp>,
    keysForLabel: (String) -> AppNameKeys = AppNameSortKey::keys,
) {
    val original = DrawerAppList(apps)
    val alphabetical: DrawerAppList

    init {
        // Resolve each name once; comparator calls only compare prepared strings.
        val entries = apps.map { app -> app to keysForLabel(app.label) }
            .sortedWith(compareBy({ it.second.sortKey }, { it.first.searchLabel }))
        val starts = LinkedHashMap<String, Int>()
        entries.forEachIndexed { index, (_, keys) ->
            if (keys.initial !in starts) starts[keys.initial] = index
        }
        val letters = buildList {
            for (c in 'A'..'Z') {
                val key = c.toString()
                if (key in starts) add(key)
            }
            if ("#" in starts) add("#")
        }
        alphabetical = DrawerAppList(entries.map { it.first }, letters, starts)
    }
}
