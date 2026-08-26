package com.playerid.app.domain.team

enum class TeamImportSource(val routeKey: String) {
    Screenshot("screenshot"),
    App("app"),
    Website("website")
}