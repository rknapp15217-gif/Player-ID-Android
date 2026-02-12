package com.playerid.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppNavigationCallback {
    private val _route = MutableStateFlow<String?>(null)
    val route: StateFlow<String?> = _route.asStateFlow()

    fun setRoute(route: String) {
        _route.value = route
    }

    fun clear() {
        _route.value = null
    }

    const val EXTRA_NAV_ROUTE = "extra_nav_route"
}
