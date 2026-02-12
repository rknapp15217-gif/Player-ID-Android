package com.playerid.app.data.teamsnap

import com.playerid.app.BuildConfig

object TeamSnapOAuthConfig {
    const val REDIRECT_SCHEME = "playerid-teamsnap"
    const val REDIRECT_HOST = "oauth"
    const val REDIRECT_URI = "$REDIRECT_SCHEME://$REDIRECT_HOST"

    // TODO: Replace with your TeamSnap OAuth client ID.
    const val CLIENT_ID = BuildConfig.TEAMSNAP_CLIENT_ID
    const val CLIENT_SECRET = BuildConfig.TEAMSNAP_CLIENT_SECRET

    // TODO: Confirm scopes required for roster import.
    val SCOPES = listOf("read")

    // Authorization endpoint for TeamSnap OAuth.
    const val AUTHORIZE_URL = "https://auth.teamsnap.com/oauth/authorize"
}
