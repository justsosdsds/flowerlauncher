package com.flower.launcher.apps

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AppSearchEngineTest {
    @Test
    fun blankQueryKeepsInputOrderAndIdentity() {
        val apps = listOf(app("Zulu", 0), app("Alpha", 1))
        val index = AppSearchEngine.prepare(apps)
        assertSame(apps, index.search(""))
        assertSame(apps, index.search(" \t\n"))
        assertEquals(emptyList<InstalledApp>(), AppSearchEngine.search(emptyList(), "missing"))
    }

    @Test
    fun scoreThenNameThenStableTiesMatchesOriginal() {
        val apps = listOf(
            app("Zed", 0).copy(packageName = "com.test.map"),
            app("Road Map", 1),
            app("Maps", 2),
            app("MAP", 3),
            app("map", 4),
            app("Maple", 5),
            app("Unrelated", 6),
        )
        val index = AppSearchEngine.prepare(apps)
        assertEquals(listOf(apps[3], apps[4], apps[5], apps[2], apps[1], apps[0]), index.search(" MAP "))
        assertEquals(originalSearch(apps, "map"), index.search("map"))
    }

    @Test
    fun aliasCopiesRebuildCachedFields() {
        val original = app("Old Name", 1)
        val renamed = original.copy(label = "新名称")
        val index = AppSearchEngine.prepare(listOf(renamed))
        assertEquals(emptyList<InstalledApp>(), index.search("Old Name"))
        assertEquals(listOf(renamed), index.search("新名"))
        assertEquals(original.componentKey, renamed.componentKey)
        assertEquals("com.changed/Changed", renamed.copy(packageName = "com.changed", activityName = "Changed").componentKey)
    }

    @Test
    fun multilingualAndPackageMatchesAreUnchanged() {
        val apps = listOf("地图", "高德地图", "相机", "İstanbul", "Straße", "ΣΟΣ", "😀 Tools", "café")
            .mapIndexed { i, label -> app(label, i) }
        val index = AppSearchEngine.prepare(apps)
        for (query in listOf("图", " 地图 ", "İ", "i", "ß", "σ", "😀", "é", "COM.TEST", "absent")) {
            assertEquals("Query: $query", originalSearch(apps, query), index.search(query))
        }
    }

    @Test
    fun randomizedCatalogsMatchOriginalAlgorithm() {
        val random = Random(20260929)
        val tokens = listOf("Map", "MAP", "map", "地图", "a", "Z", "İ", "ß", "😀", " ")
        repeat(100) {
            val apps = List(random.nextInt(0, 400)) { i ->
                app(List(random.nextInt(1, 5)) { tokens[random.nextInt(tokens.size)] }.joinToString(""), i)
                    .copy(packageName = "com.test.${tokens[random.nextInt(tokens.size)]}.$i")
            }.shuffled(random)
            val index = AppSearchEngine.prepare(apps)
            repeat(30) {
                val query = if (random.nextBoolean()) tokens[random.nextInt(tokens.size)] else "com.test"
                assertEquals(originalSearch(apps, query), index.search(query))
            }
        }
    }

    private fun app(label: String, id: Int) = InstalledApp(label, "com.test.app$id", "Activity$id")

    // The source archive's original algorithm, kept as the compatibility oracle.
    private fun originalSearch(apps: List<InstalledApp>, query: String): List<InstalledApp> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return apps
        val needle = trimmed.lowercase()
        return apps.mapNotNull { app ->
            val label = app.label.lowercase()
            val score = when {
                label == needle -> 4
                label.startsWith(needle) -> 3
                label.contains(needle) -> 2
                app.packageName.lowercase().contains(needle) -> 1
                else -> -1
            }
            if (score < 0) null else app to score
        }.sortedWith(compareByDescending<Pair<InstalledApp, Int>> { it.second }
            .thenBy { it.first.label.lowercase() }).map { it.first }
    }
}
