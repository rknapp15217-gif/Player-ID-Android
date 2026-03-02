package com.playerid.app.ui.screens

import androidx.compose.runtime.mutableStateOf

// This is a hack to allow PlayerIDApp to force CameraScreen's isCameraReady to false on navigation
var cameraScreenForceNotReady = mutableStateOf(false)

fun CameraScreen_forceNotReady() {
    cameraScreenForceNotReady.value = true
}