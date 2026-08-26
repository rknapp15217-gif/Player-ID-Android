package com.playerid.app.domain.team

import kotlin.test.Test
import kotlin.test.assertEquals

class InviteTeamStateTest {
    @Test
    fun enabledNfcSelectsNfcPage() {
        val state = InviteTeamState(nfcAvailable = true, nfcEnabled = true)

        assertEquals(InviteProximityAction.ShowNfc, state.proximityAction)
        assertEquals(
            InviteTeamPage.Nfc,
            state.reduce(InviteTeamEvent.ProximitySelected).page
        )
    }

    @Test
    fun disabledNfcRequestsSettingsWithoutChangingPage() {
        val state = InviteTeamState(nfcAvailable = true, nfcEnabled = false)

        assertEquals(InviteProximityAction.OpenNfcSettings, state.proximityAction)
        assertEquals(state, state.reduce(InviteTeamEvent.ProximitySelected))
    }

    @Test
    fun missingNfcUsesNearbyShareWithoutChangingPage() {
        val state = InviteTeamState(nfcAvailable = false, nfcEnabled = false)

        assertEquals(InviteProximityAction.ShareNearby, state.proximityAction)
        assertEquals(state, state.reduce(InviteTeamEvent.ProximitySelected))
    }

    @Test
    fun qrAndBackTransitionsRemainPlatformNeutral() {
        val state = InviteTeamState(nfcAvailable = false, nfcEnabled = false)
            .reduce(InviteTeamEvent.QrCodeSelected)

        assertEquals(InviteTeamPage.QrCode, state.page)
        assertEquals(
            InviteTeamPage.Options,
            state.reduce(InviteTeamEvent.OptionsSelected).page
        )
    }
}