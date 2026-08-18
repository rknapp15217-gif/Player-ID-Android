package com.playerid.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppNavigationCallback {
    private val _route = MutableStateFlow<String?>(null)
    val route: StateFlow<String?> = _route.asStateFlow()

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()

    fun setRoute(route: String) {
        _route.value = route
    }

    fun clear() {
        _route.value = null
    }

    fun permissionsGranted() {
        _permissionsGranted.value = true
    }

    fun clearPermissionNotification() {
        _permissionsGranted.value = false
    }

    const val EXTRA_NAV_ROUTE = "extra_nav_route"
}
