package com.playerid.app.ui.team

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.playerid.app.domain.team.InviteProximityAction
import com.playerid.app.domain.team.InviteTeamEvent
import com.playerid.app.domain.team.InviteTeamPage
import com.playerid.app.domain.team.InviteTeamState

@Composable
fun InviteTeamDialog(
    teamName: String,
    nfcAvailable: Boolean,
    nfcEnabled: Boolean,
    onDismiss: () -> Unit,
    onSendText: () -> Unit,
    onOpenNfcSettings: () -> Unit,
    onShareNearby: () -> Unit,
    headerIcon: @Composable () -> Unit,
    closeIcon: @Composable () -> Unit,
    textIcon: @Composable () -> Unit,
    qrIcon: @Composable () -> Unit,
    proximityIcon: @Composable () -> Unit,
    nfcHeroIcon: @Composable () -> Unit,
    qrCodeContent: @Composable () -> Unit,
    nfcSession: @Composable () -> Unit
) {
    var state by remember(nfcAvailable, nfcEnabled) {
        mutableStateOf(
            InviteTeamState(
                nfcAvailable = nfcAvailable,
                nfcEnabled = nfcEnabled
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        headerIcon()
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when (state.page) {
                                InviteTeamPage.Options -> "Invite to $teamName"
                                InviteTeamPage.QrCode -> "Scan QR Code"
                                InviteTeamPage.Nfc -> "Tap to Share via NFC"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        closeIcon()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                when (state.page) {
                    InviteTeamPage.Options -> InviteOptions(
                        nfcAvailable = nfcAvailable,
                        nfcEnabled = nfcEnabled,
                        onSendText = onSendText,
                        onShowQr = {
                            state = state.reduce(InviteTeamEvent.QrCodeSelected)
                        },
                        onProximity = {
                            when (state.proximityAction) {
                                InviteProximityAction.ShowNfc -> {
                                    state = state.reduce(InviteTeamEvent.ProximitySelected)
                                }
                                InviteProximityAction.OpenNfcSettings -> onOpenNfcSettings()
                                InviteProximityAction.ShareNearby -> onShareNearby()
                            }
                        },
                        textIcon = textIcon,
                        qrIcon = qrIcon,
                        proximityIcon = proximityIcon
                    )
                    InviteTeamPage.QrCode -> InviteQrPage(
                        teamName = teamName,
                        qrCodeContent = qrCodeContent,
                        onBack = {
                            state = state.reduce(InviteTeamEvent.OptionsSelected)
                        }
                    )
                    InviteTeamPage.Nfc -> InviteNfcPage(
                        teamName = teamName,
                        nfcHeroIcon = nfcHeroIcon,
                        nfcSession = nfcSession,
                        onCancel = {
                            state = state.reduce(InviteTeamEvent.OptionsSelected)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InviteOptions(
    nfcAvailable: Boolean,
    nfcEnabled: Boolean,
    onSendText: () -> Unit,
    onShowQr: () -> Unit,
    onProximity: () -> Unit,
    textIcon: @Composable () -> Unit,
    qrIcon: @Composable () -> Unit,
    proximityIcon: @Composable () -> Unit
) {
    InviteOptionButton(onClick = onSendText, icon = textIcon) {
        Text("Send via Text", fontWeight = FontWeight.Medium)
    }
    Spacer(modifier = Modifier.height(10.dp))
    InviteOptionButton(onClick = onShowQr, icon = qrIcon) {
        Text("Show QR Code", fontWeight = FontWeight.Medium)
    }
    Spacer(modifier = Modifier.height(10.dp))
    InviteOptionButton(onClick = onProximity, icon = proximityIcon) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                if (nfcAvailable) "Tap Phones (NFC)" else "Nearby Share",
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium
            )
            if (nfcAvailable && !nfcEnabled) {
                Text(
                    "NFC is off - tap to enable",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InviteOptionButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.width(10.dp))
        content()
    }
}

@Composable
private fun InviteQrPage(
    teamName: String,
    qrCodeContent: @Composable () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        qrCodeContent()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Have the other parent scan this to join \"$teamName\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun InviteNfcPage(
    teamName: String,
    nfcHeroIcon: @Composable () -> Unit,
    nfcSession: @Composable () -> Unit,
    onCancel: () -> Unit
) {
    nfcSession()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        nfcHeroIcon()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Hold phones back-to-back",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "The other parent will receive a link to join \"$teamName\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}