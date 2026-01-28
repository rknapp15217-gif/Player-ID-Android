package com.playerid.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.playerid.app.data.AcademicYear
import com.playerid.app.data.Player
import com.playerid.app.data.Secrets
import com.playerid.app.ui.dialogs.AddPlayerDialog
import com.playerid.app.ui.dialogs.EditPlayerDialog
import com.playerid.app.viewmodels.AuthViewModel
import com.playerid.app.viewmodels.PlayerViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    authViewModel: AuthViewModel,
    playerViewModel: PlayerViewModel
) {
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    val showLoginDialog by authViewModel.showLoginDialog.collectAsState()
    
    if (isAuthenticated) {
        AdminPanelView(
            playerViewModel = playerViewModel,
            onLogout = { authViewModel.logout() }
        )
    } else {
        AdminLoginView(
            onLogin = { authViewModel.showLoginDialog() }
        )
    }
    
    if (showLoginDialog) {
        LoginDialog(
            onDismiss = { authViewModel.hideLoginDialog() },
            onLogin = { password ->
                if (authViewModel.authenticate(password)) {
                    authViewModel.hideLoginDialog()
                }
            }
        )
    }
}

@Composable
fun AdminLoginView(
    onLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AdminPanelSettings,
            contentDescription = "Admin",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Administrator Access",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Login required to manage player database",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login as Administrator")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Admin features:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val features = listOf(
                    "Add new players" to Icons.Default.PersonAdd,
                    "Edit player information" to Icons.Default.Edit,
                    "Delete players" to Icons.Default.Delete,
                    "Import/Export database" to Icons.Default.ImportExport,
                    "Backup & restore" to Icons.Default.Backup
                )
                
                features.forEach { (feature, icon) ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = feature,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoginDialog(
    onDismiss: () -> Unit,
    onLogin: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loginFailed by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Admin Login",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        loginFailed = false
                    },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    isError = loginFailed,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (loginFailed) {
                    Text(
                        text = "Invalid password",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            if (password == Secrets.ADMIN_PASSWORD) {
                                onLogin(password)
                            } else {
                                loginFailed = true
                            }
                        }
                    ) {
                        Text("Login")
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPanelView(
    playerViewModel: PlayerViewModel,
    onLogout: () -> Unit
) {
    val players by playerViewModel.allPlayers.collectAsState(initial = emptyList())
    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var editingPlayer by remember { mutableStateOf<Player?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🛠️ Admin Panel",
                style = MaterialTheme.typography.headlineMedium
            )
            
            IconButton(onClick = onLogout) {
                Icon(Icons.Default.Logout, contentDescription = "Logout")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Stats cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = players.size.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Total Players",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = players.map { it.team }.distinct().size.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Teams",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Add player button
        Button(
            onClick = { showAddPlayerDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add New Player")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Players list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(players) { player ->
                AdminPlayerCard(
                    player = player,
                    onEdit = { editingPlayer = it },
                    onDelete = { playerViewModel.deletePlayer(player) }
                )
            }
        }
    }
    
    if (showAddPlayerDialog) {
        AddPlayerDialog(
            onDismiss = { showAddPlayerDialog = false },
            onAdd = { player ->
                playerViewModel.addPlayer(player)
                showAddPlayerDialog = false
            }
        )
    }

    editingPlayer?.let { player ->
        EditPlayerDialog(
            player = player,
            onDismiss = { editingPlayer = null },
            onSave = { updatedPlayer ->
                playerViewModel.updatePlayer(updatedPlayer)
                editingPlayer = null
            }
        )
    }
}

@Composable
fun AdminPlayerCard(
    player: Player,
    onEdit: (Player) -> Unit,
    onDelete: (Player) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Jersey number
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = player.number.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Player info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    text = "${player.position} • ${player.team}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = player.academicYear,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Action buttons
            Row {
                IconButton(onClick = { onEdit(player) }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(onClick = { onDelete(player) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
