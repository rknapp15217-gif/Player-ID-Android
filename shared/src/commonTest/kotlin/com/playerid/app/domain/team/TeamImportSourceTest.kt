package com.playerid.app.domain.team

import kotlin.test.Test
import kotlin.test.assertEquals

class TeamImportSourceTest {
    @Test
    fun routeKeysPreserveNavigationContract() {
        assertEquals("screenshot", TeamImportSource.Screenshot.routeKey)
        assertEquals("app", TeamImportSource.App.routeKey)
        assertEquals("website", TeamImportSource.Website.routeKey)
    }
}