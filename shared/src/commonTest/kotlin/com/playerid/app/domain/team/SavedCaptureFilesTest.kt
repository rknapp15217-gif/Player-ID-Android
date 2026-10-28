package com.playerid.app.domain.team

import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.savedCaptureKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SavedCaptureFilesTest {
    @Test
    fun recognizesCameraAndImportedFormats() {
        assertEquals(MediaKind.IMAGE, savedCaptureKind("photo.JPG"))
        for (name in listOf("capture.mov", "import.MP4", "import.m4v")) {
            assertEquals(MediaKind.VIDEO, savedCaptureKind(name))
            validateCaptureFileKey(name)
        }
    }

    @Test
    fun rejectsUnsupportedFormatsAndUnsafeKeys() {
        for (name in listOf("photo.heic", "temporary.tmp", "video.mp4.partial", "no-extension", "")) {
            assertNull(savedCaptureKind(name))
            assertFailsWith<IllegalArgumentException> { validateCaptureFileKey(name) }
        }
        for (name in listOf("../video.mp4", "folder/video.mov", "folder\\photo.jpg")) {
            assertFailsWith<IllegalArgumentException> { validateCaptureFileKey(name) }
        }
    }
}
