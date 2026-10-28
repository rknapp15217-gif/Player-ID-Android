package com.playerid.app.domain.memory

import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.platform.MediaReference

data class MemoryImportProgress(val completed: Int, val total: Int)
data class MemoryImportFailure(val selectionNumber: Int, val message: String)
data class MemoryImportResult(
    val imported: List<MediaReference>,
    val failures: List<MemoryImportFailure>
)

suspend fun <T> importMemoriesSequentially(
    selections: List<T>,
    onProgress: (MemoryImportProgress) -> Unit,
    importItem: suspend (T) -> MediaReference
): MemoryImportResult {
    val imported = mutableListOf<MediaReference>()
    val failures = mutableListOf<MemoryImportFailure>()
    onProgress(MemoryImportProgress(0, selections.size))
    selections.forEachIndexed { index, selection ->
        try {
            imported += importItem(selection)
        } catch (failure: CameraCaptureException) {
            failures += MemoryImportFailure(index + 1, failure.message ?: "This item could not be imported.")
        }
        onProgress(MemoryImportProgress(index + 1, selections.size))
    }
    return MemoryImportResult(imported.toList(), failures.toList())
}
