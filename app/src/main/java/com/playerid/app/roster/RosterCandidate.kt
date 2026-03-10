package com.playerid.app.roster

data class RosterCandidate(
    val name: String,
    val number: String,
    val position: String,
    val graduationYear: String?,
    val academicYear: String?
)
