package com.playerid.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.uikit.OnFocusBehavior
import com.playerid.app.platform.IosTeamSnapshotStorage
import com.playerid.app.platform.IosAccount
import com.playerid.app.platform.IosCameraService
import com.playerid.app.platform.IosRosterImportAdapter
import com.playerid.app.domain.camera.CameraCaptureController
import com.playerid.app.domain.camera.CaptureMode
import com.playerid.app.ui.IosCameraPage
import com.playerid.app.ui.IosMemoryLibraryPage
import com.playerid.app.ui.team.TeamWorkspaceApp
import com.playerid.app.ui.theme.PlayerIDBrandTheme
import platform.UIKit.UIViewController
import platform.Foundation.NSUUID

fun MainViewController(): UIViewController = ComposeUIViewController(
    configure = { onFocusBehavior = OnFocusBehavior.DoNothing }
) {
    PlayerIDBrandTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val storage = remember { IosTeamSnapshotStorage() }
            val account = remember { IosAccount() }
            val camera = remember { IosCameraService() }
            val capture = remember(camera) { CameraCaptureController(camera, initialMode = CaptureMode.ROLLING) }
            val cameraScope = rememberCoroutineScope()
            val rosterImport = remember { IosRosterImportAdapter() }
            val contactPicker = remember { com.playerid.app.platform.IosGuardianSharing() }
            TeamWorkspaceApp(
                storage = storage,
                account = account.auth,
                newId = { NSUUID().UUIDString },
                cameraContent = { workspace, onTeams ->
                    IosCameraPage(camera, capture, cameraScope, workspace.repository, onTeams)
                },
                memoriesContent = { workspace, goMode, goOpenGeneration, onOpenMore, onNavigate ->
                    IosMemoryLibraryPage(camera, workspace.repository, onNavigate, onOpenMore, goMode, goOpenGeneration)
                },
                rosterImport = rosterImport,
                onRotationAllowed = com.playerid.app.platform.IosOrientationLock::setRotationAllowed,
                pickContact = contactPicker::pickContact
            )
        }
    }
}