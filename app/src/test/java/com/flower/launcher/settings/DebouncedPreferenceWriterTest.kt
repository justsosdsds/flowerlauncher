package com.flower.launcher.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebouncedPreferenceWriterTest {
    @Test fun rapidEditsPersistOnlyTheLatestValueAfterQuietPeriod() = runTest {
        val writes = mutableListOf<Int>()
        val writer = DebouncedPreferenceWriter(this, 0) { writes.add(it) }
        writer.submit(1)
        advanceTimeBy(100)
        writer.submit(2)
        advanceTimeBy(100)
        writer.submit(3)
        advanceTimeBy(299)
        runCurrent()
        assertEquals(emptyList<Int>(), writes)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(3), writes)
    }

    @Test fun leavingEditorFlushesOnceAndCancelsDelayedDuplicate() = runTest {
        val writes = mutableListOf<String>()
        val writer = DebouncedPreferenceWriter(this, "old") { writes.add(it) }
        writer.submit("new")
        writer.flush()
        writer.flush()
        advanceUntilIdle()
        assertEquals(listOf("new"), writes)
    }

    @Test fun returningToSavedValueNeedsNoWrite() = runTest {
        val writes = mutableListOf<Int>()
        val writer = DebouncedPreferenceWriter(this, 5) { writes.add(it) }
        writer.submit(6)
        writer.submit(5)
        advanceUntilIdle()
        writer.flush()
        assertEquals(emptyList<Int>(), writes)
    }

    @Test fun disposalCanFlushAfterCoroutineScopeIsCancelled() = runTest {
        val writes = mutableListOf<Int>()
        val editScope = CoroutineScope(coroutineContext + Job())
        val writer = DebouncedPreferenceWriter(editScope, 0) { writes.add(it) }
        writer.submit(7)
        editScope.cancel()
        writer.flush()
        advanceUntilIdle()
        assertEquals(listOf(7), writes)
    }

    @Test fun identicalSubmissionsDoNotPostponeSavingForever() = runTest {
        val writes = mutableListOf<Int>()
        val writer = DebouncedPreferenceWriter(this, 0) { writes.add(it) }
        writer.submit(1)
        advanceTimeBy(200)
        writer.submit(1)
        advanceTimeBy(100)
        runCurrent()
        assertEquals(listOf(1), writes)
    }

    @Test fun separatePreferenceGroupsDoNotCancelEachOther() = runTest {
        val a = mutableListOf<Int>()
        val b = mutableListOf<Int>()
        val first = DebouncedPreferenceWriter(this, 0) { a.add(it) }
        val second = DebouncedPreferenceWriter(this, 0) { b.add(it) }
        first.submit(1)
        advanceTimeBy(200)
        second.submit(2)
        advanceTimeBy(100)
        runCurrent()
        assertEquals(listOf(1), a)
        assertEquals(emptyList<Int>(), b)
        advanceUntilIdle()
        assertEquals(listOf(2), b)
    }
}
