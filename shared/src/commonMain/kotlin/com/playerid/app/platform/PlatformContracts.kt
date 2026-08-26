package com.playerid.app.platform

/**
 * Platform-neutral reference to media owned by Android MediaStore, Apple Photos,
 * app storage, or a remote service. Shared code must not persist native URI types.
 */
data class MediaReference(
    val identifier: String,
    val kind: MediaKind,
    val mimeType: String? = null
)

enum class MediaKind {
    IMAGE,
    VIDEO
}

/** Capabilities let shared UI select a supported workflow without platform checks. */
data class PlatformCapabilities(
    val externalScreenCapture: Boolean,
    val backgroundVideoAnalysis: Boolean,
    val contactPicker: Boolean
)

interface MediaPicker {
    suspend fun pickImage(): MediaReference?
    suspend fun pickVideo(): MediaReference?
}

interface TextRecognitionService {
    suspend fun recognizeText(image: MediaReference): List<RecognizedText>
}

data class RecognizedText(
    val text: String,
    val confidence: Float? = null
)
