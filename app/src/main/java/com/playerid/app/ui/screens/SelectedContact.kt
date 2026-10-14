package com.playerid.app.ui.screens

internal data class SelectedContact(
    val displayName: String,
    val phoneNumber: String?,
    val email: String?,
    val linkedPlayerIds: Set<String> = emptySet()
) {
    val cacheKey: String = phoneNumber
        ?.filter(Char::isDigit)
        ?.takeIf { it.isNotEmpty() }
        ?.let { "phone:$it" }
        ?: email
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() }
            ?.let { "email:$it" }
        ?: "name:${displayName.trim().lowercase()}"
}