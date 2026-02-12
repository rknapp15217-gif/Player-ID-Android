package com.playerid.app.data.teamsnap

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object TeamSnapAuthCallback {
    private val _redirectUri = MutableStateFlow<Uri?>(null)
    val redirectUri = _redirectUri.asStateFlow()

    fun setRedirect(uri: Uri) {
        _redirectUri.value = uri
    }

    fun clear() {
        _redirectUri.value = null
    }
}
