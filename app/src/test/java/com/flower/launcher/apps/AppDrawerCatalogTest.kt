package com.flower.launcher.apps

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AppDrawerCatalogTest {
    @Test
    fun pinyinInitialsAndNonLettersKeepTheOriginalOrder() {
        val apps = listOf("原子笔记", "123", "地图", "Alpha", "😀", "alpha")
            .mapIndexed { index, label -> app(label, index) }
        val latin = mapOf("原子笔记" to "yuan zi bi ji", "地图" to "di tu")
        val catalog = AppDrawerCatalog(apps) { label ->
            appNameKeysFromTransliteration(latin[label] ?: label)
        }
        assertEquals(listOf(apps[3], apps[5], apps[2], apps[0], apps[4], apps[1]), catalog.alphabetical.apps)
        assertEquals(listOf("A", "D", "Y", "#"), catalog.alphabetical.letters)
        assertEquals(mapOf("A" to 0, "D" to 2, "Y" to 3, "#" to 4), catalog.alphabetical.letterStarts)
        assertSame(apps, catalog.original.apps)
        assertEquals(emptyList<String>(), catalog.original.letters)
    }

    @Test
    fun equalNamesKeepActivityOrderAndAliasesBuildANewIndex() {
        val original = listOf(app("MAP", 1), app("map", 2), app("Maps", 3))
        val old = AppDrawerCatalog(original, ::appNameKeysFromTransliteration)
        assertEquals(original, old.alphabetical.apps)
        val renamed = original.map { if (it == original[0]) it.copy(label = "Zoo") else it }
        val updated = AppDrawerCatalog(renamed, ::appNameKeysFromTransliteration)
        assertEquals(listOf(renamed[1], renamed[2], renamed[0]), updated.alphabetical.apps)
        assertEquals(listOf("M", "Z"), updated.alphabetical.letters)
        assertEquals(original, old.alphabetical.apps)
    }

    @Test
    fun emptyCatalogDoesNotProduceAPhantomIndexLetter() {
        val catalog = AppDrawerCatalog(emptyList(), ::appNameKeysFromTransliteration)
        assertEquals(emptyList<InstalledApp>(), catalog.alphabetical.apps)
        assertEquals(emptyList<String>(), catalog.alphabetical.letters)
        assertEquals(emptyMap<String, Int>(), catalog.alphabetical.letterStarts)
    }

    @Test
    fun preparedKeysPreserveOriginalComparatorAcrossMixedNames() {
        val random = Random(20260929)
        val tokens = listOf("A", "a", "Z", "123", " ", "-", "😀", "İ", "ß", "é", "地图")
        repeat(100) {
            val apps = List(random.nextInt(0, 400)) { index ->
                app(List(random.nextInt(0, 6)) { tokens.random(random) }.joinToString(""), index)
            }.shuffled(random)
            val catalog = AppDrawerCatalog(apps, ::appNameKeysFromTransliteration)
            val expected = apps.sortedWith(compareBy({ originalSortKey(it.label) }, { it.searchLabel }))
            assertEquals(expected, catalog.alphabetical.apps)
            expected.forEachIndexed { index, app ->
                val key = originalSortKey(app.label)
                val initial = if (key.first() == '\uFFFF') "#" else key.first().uppercaseChar().toString()
                val firstIndex = catalog.alphabetical.letterStarts.getValue(initial)
                assertEquals(initial, appNameKeysFromTransliteration(expected[firstIndex].label).initial)
                check(firstIndex <= index)
            }
        }
    }

    private fun app(label: String, id: Int) = InstalledApp(label, "com.test.app$id", "Activity$id")

    // Previous algorithm after ICU transliteration; ICU itself remains unchanged.
    private fun originalSortKey(transliterated: String): String {
        val normalized = buildString(transliterated.length) {
            for (ch in transliterated.lowercase()) {
                if (ch in 'a'..'z' || ch in '0'..'9') append(ch)
            }
        }
        return if (normalized.isEmpty() || normalized[0] !in 'a'..'z') '\uFFFF' + normalized else normalized
    }
}
