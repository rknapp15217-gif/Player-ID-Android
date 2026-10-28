package com.playerid.app.domain.memory

import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class MemoryImportTest {
    private fun media(index: Int) = MediaReference("file:///media/$index.jpg", MediaKind.IMAGE)

    @Test
    fun canceledPickerImportsNothing() = runTest {
        val progress = mutableListOf<MemoryImportProgress>()
        val result = importMemoriesSequentially(emptyList<Int>(), { progress += it }) {
            fail("No import should be requested")
        }
        assertTrue(result.imported.isEmpty())
        assertTrue(result.failures.isEmpty())
        assertEquals(listOf(MemoryImportProgress(0, 0)), progress)
    }

    @Test
    fun mixedPhotoVideoBatchRunsSequentiallyAndReportsEveryCompletion() = runTest {
        val progress = mutableListOf<MemoryImportProgress>()
        val calls = mutableListOf<Int>()
        val photo = media(1)
        val video = MediaReference("file:///media/2.mov", MediaKind.VIDEO)
        val result = importMemoriesSequentially(listOf(photo, video), { progress += it }) {
            calls += calls.size + 1
            assertEquals(calls.size - 1, progress.last().completed)
            it
        }
        assertEquals(listOf(photo, video), result.imported)
        assertTrue(result.failures.isEmpty())
        assertEquals(listOf(0, 1, 2).map { MemoryImportProgress(it, 2) }, progress)
        assertEquals(listOf(1, 2), calls)
    }

    @Test
    fun perItemFailuresKeepSuccessesAndContinueThroughTheWholeBatch() = runTest {
        val progress = mutableListOf<MemoryImportProgress>()
        val calls = mutableListOf<Int>()
        val result = importMemoriesSequentially((1..5).toList(), { progress += it }) {
            calls += it
            if (it == 2 || it == 4) throw CameraCaptureException("Unavailable $it")
            media(it)
        }
        assertEquals(listOf(media(1), media(3), media(5)), result.imported)
        assertEquals(listOf(MemoryImportFailure(2, "Unavailable 2"), MemoryImportFailure(4, "Unavailable 4")), result.failures)
        assertEquals((1..5).toList(), calls)
        assertEquals((0..5).map { MemoryImportProgress(it, 5) }, progress)
    }

    @Test
    fun allFailedItemsAreReportedNotPresentedAsCancelOrSuccess() = runTest {
        val result = importMemoriesSequentially(listOf(1, 2), {}) {
            throw CameraCaptureException("Storage full")
        }
        assertTrue(result.imported.isEmpty())
        assertEquals(listOf(1, 2), result.failures.map { it.selectionNumber })
        assertTrue(result.failures.all { it.message == "Storage full" })
    }

    @Test
    fun batchesAreNotSilentlyCappedAtTwentyItems() = runTest {
        val result = importMemoriesSequentially((1..25).toList(), {}, ::media)
        assertEquals((1..25).map(::media), result.imported)
        assertTrue(result.failures.isEmpty())
    }

    @Test
    fun cancellationAndUnexpectedErrorsAreNotSwallowedAsPerItemFailures() = runTest {
        assertFailsWith<CancellationException> {
            importMemoriesSequentially(listOf(1), {}) { throw CancellationException("Canceled") }
        }
        assertFailsWith<IllegalStateException> {
            importMemoriesSequentially(listOf(1), {}) { error("Unexpected state") }
        }
    }
}
