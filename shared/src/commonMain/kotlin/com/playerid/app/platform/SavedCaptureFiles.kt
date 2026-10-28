package com.playerid.app.platform

fun savedCaptureKind(filename: String): MediaKind? = when (filename.substringAfterLast('.', "").lowercase()) {
    "jpg" -> MediaKind.IMAGE
    "mov", "mp4", "m4v" -> MediaKind.VIDEO
    else -> null
}
