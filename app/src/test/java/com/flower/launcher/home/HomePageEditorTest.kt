package com.flower.launcher.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HomePageEditorTest {
    @Test fun visitingPagesDoesNotChangeTheDefault() {
        val original = HomePreferences(pageCount = 4, defaultHomePage = 1)
        val visited = visitHomePage(visitHomePage(original, 2), 3)
        assertEquals(1, visited.defaultHomePage)
        assertEquals(3, visited.lastHomePage)
    }

    @Test fun changingDefaultDoesNotOverwriteVisitHistory() {
        val updated = setDefaultHomePage(HomePreferences(pageCount = 4, lastHomePage = 3), 2)
        assertEquals(2, updated.defaultHomePage)
        assertEquals(3, updated.lastHomePage)
    }

    @Test fun oldInstallKeepsItsExistingPageOnce() {
        val migrated = restoredDefaultHomePage(null, 2, 4)
        assertEquals(2, migrated)
        assertEquals(2, restoredDefaultHomePage(migrated, 3, 4))
    }

    @Test fun corruptOrOutOfRangePagesAreClamped() {
        assertEquals(0, restoredDefaultHomePage(null, -10, 4))
        assertEquals(2, restoredDefaultHomePage(99, 0, 3))
        assertEquals(0, restoredDefaultHomePage(null, 8, 0))
        assertEquals(2, setDefaultHomePage(HomePreferences(pageCount = 3), 99).defaultHomePage)
    }

    @Test fun deletionRemapsDefaultAndRecentPageIndependently() {
        val updated = removeHomePage(
            HomePreferences(pageCount = 5, defaultHomePage = 3, lastHomePage = 1), 2,
        )
        assertEquals(4, updated.pageCount)
        assertEquals(2, updated.defaultHomePage)
        assertEquals(1, updated.lastHomePage)
    }

    @Test fun deletingTheDefaultUsesThePreviousSurvivingPage() {
        val updated = removeHomePage(
            HomePreferences(pageCount = 4, defaultHomePage = 2, lastHomePage = 3), 2,
        )
        assertEquals(1, updated.defaultHomePage)
        assertEquals(2, updated.lastHomePage)
        assertEquals(0, removeHomePage(HomePreferences(pageCount = 2), 0).defaultHomePage)
    }

    @Test fun homeCommandCanRepeatEvenWhenItsTargetIsUnchanged() {
        val first = HomePageRequest(1)
        val next = first.navigateTo(1)
        assertNotEquals(first, next)
        assertEquals(first.page, next.page)
    }
}
