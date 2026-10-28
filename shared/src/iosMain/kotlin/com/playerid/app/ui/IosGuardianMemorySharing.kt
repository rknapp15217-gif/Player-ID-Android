package com.playerid.app.ui

import androidx.compose.runtime.*
import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.domain.memory.MemoryShareResult
import com.playerid.app.domain.team.LocalTeamRepository
import com.playerid.app.platform.IosGuardianSharing
import com.playerid.app.platform.MediaReference
import com.playerid.app.ui.memory.GuardianMemoryShareDialog
import kotlinx.coroutines.CompletableDeferred

private class GuardianShareRequest(
    val memories: List<MediaReference>,
    val recipientName: String?,
    val result: CompletableDeferred<MemoryShareResult> = CompletableDeferred()
)

@Composable
fun rememberGuardianMemorySharing(repository: LocalTeamRepository): suspend (List<MediaReference>, String?) -> MemoryShareResult {
    val native = remember { IosGuardianSharing() }
    var pending by remember { mutableStateOf<GuardianShareRequest?>(null) }
    val share = remember(repository) {
        val action: suspend (List<MediaReference>, String?) -> MemoryShareResult = { memories, recipient ->
            if (pending != null) throw CameraCaptureException("A sharing request is already open.")
            if (memories.isEmpty()) throw CameraCaptureException("Select at least one memory to share.")
            val request = GuardianShareRequest(memories.distinctBy { it.identifier }.toList(), recipient)
            pending = request
            try {
                request.result.await()
            } finally {
                pending = null
            }
        }
        action
    }
    DisposableEffect(repository) {
        onDispose {
            pending?.result?.complete(MemoryShareResult.CANCELLED)
        }
    }
    pending?.let { request ->
        GuardianMemoryShareDialog(
            memories = request.memories,
            requestedRecipient = request.recipientName,
            repository = repository,
            pickContact = native::pickContact,
            findPhoneContacts = native::findContacts,
            sendToContacts = native::send,
            preview = { media, modifier -> ShareMemoryPreview(media, modifier) },
            onComplete = { request.result.complete(it) }
        )
    }
    return share
}
