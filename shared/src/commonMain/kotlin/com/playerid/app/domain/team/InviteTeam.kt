package com.playerid.app.domain.team

enum class InviteTeamPage {
    Options,
    QrCode,
    Nfc
}

enum class InviteProximityAction {
    ShowNfc,
    OpenNfcSettings,
    ShareNearby
}

data class InviteTeamState(
    val page: InviteTeamPage = InviteTeamPage.Options,
    val nfcAvailable: Boolean,
    val nfcEnabled: Boolean
) {
    val proximityAction: InviteProximityAction
        get() = when {
            nfcAvailable && nfcEnabled -> InviteProximityAction.ShowNfc
            nfcAvailable -> InviteProximityAction.OpenNfcSettings
            else -> InviteProximityAction.ShareNearby
        }

    fun reduce(event: InviteTeamEvent): InviteTeamState = when (event) {
        InviteTeamEvent.OptionsSelected -> copy(page = InviteTeamPage.Options)
        InviteTeamEvent.QrCodeSelected -> copy(page = InviteTeamPage.QrCode)
        InviteTeamEvent.ProximitySelected -> when (proximityAction) {
            InviteProximityAction.ShowNfc -> copy(page = InviteTeamPage.Nfc)
            InviteProximityAction.OpenNfcSettings,
            InviteProximityAction.ShareNearby -> this
        }
    }
}

enum class InviteTeamEvent {
    OptionsSelected,
    QrCodeSelected,
    ProximitySelected
}