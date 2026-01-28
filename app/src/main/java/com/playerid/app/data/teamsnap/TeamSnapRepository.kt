package com.playerid.app.data.teamsnap

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.playerid.app.data.Player
import com.playerid.app.data.PlayerDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Repository for TeamSnap API integration
 * Handles authentication, API calls, and data synchronization
 */
class TeamSnapRepository(
    private val context: Context,
    private val playerDao: PlayerDao
) {
    companion object {
        private const val TAG = "TeamSnapRepository"
        private const val PREFS_NAME = "teamsnap_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRES = "token_expires"
        private const val KEY_USER_EMAIL = "user_email"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // Create HTTP client with logging
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // Create Retrofit instance
    private val retrofit = Retrofit.Builder()
        .baseUrl(TeamSnapApiService.BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    private val apiService = retrofit.create(TeamSnapApiService::class.java)
    
    // Authentication state
    private val _authState = MutableStateFlow<TeamSnapAuthState>(TeamSnapAuthState.NotAuthenticated)
    val authState: Flow<TeamSnapAuthState> = _authState.asStateFlow()
    
    // Available teams from TeamSnap
    private val _availableTeams = MutableStateFlow<List<TeamSnapTeam>>(emptyList())
    val availableTeams: Flow<List<TeamSnapTeam>> = _availableTeams.asStateFlow()
    
    init {
        // Check if we have a valid stored token on init
        checkStoredAuthentication()
    }
    
    /**
     * Check if we have valid stored authentication
     */
    private fun checkStoredAuthentication() {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val expiresAt = prefs.getLong(KEY_TOKEN_EXPIRES, 0)
        val userEmail = prefs.getString(KEY_USER_EMAIL, null)
        
        if (token != null && expiresAt > System.currentTimeMillis() && userEmail != null) {
            _authState.value = TeamSnapAuthState.Authenticated(userEmail, token)
        }
    }
    
    /**
     * Authenticate with TeamSnap using email/password
     */
    suspend fun authenticate(email: String, password: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                _authState.value = TeamSnapAuthState.Authenticating
                
                val response = apiService.authenticate(
                    TeamSnapAuthRequest(email, password)
                )
                
                if (response.isSuccessful) {
                    val authResponse = response.body()!!
                    val expiresAt = System.currentTimeMillis() + (authResponse.expiresIn * 1000)
                    
                    // Store tokens securely
                    prefs.edit()
                        .putString(KEY_ACCESS_TOKEN, authResponse.accessToken)
                        .putString(KEY_REFRESH_TOKEN, authResponse.refreshToken)
                        .putLong(KEY_TOKEN_EXPIRES, expiresAt)
                        .putString(KEY_USER_EMAIL, email)
                        .apply()
                    
                    _authState.value = TeamSnapAuthState.Authenticated(email, authResponse.accessToken)
                    
                    // Load user's teams after successful authentication
                    loadUserTeams()
                    
                    Result.success("Successfully authenticated with TeamSnap")
                } else {
                    _authState.value = TeamSnapAuthState.NotAuthenticated
                    Result.failure(Exception("Authentication failed: ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Authentication error", e)
                _authState.value = TeamSnapAuthState.NotAuthenticated
                Result.failure(e)
            }
        }
    }
    
    /**
     * Sign out and clear stored credentials
     */
    fun signOut() {
        prefs.edit().clear().apply()
        _authState.value = TeamSnapAuthState.NotAuthenticated
        _availableTeams.value = emptyList()
    }
    
    /**
     * Load user's teams from TeamSnap
     */
    private suspend fun loadUserTeams() {
        val currentState = _authState.value
        if (currentState !is TeamSnapAuthState.Authenticated) return
        
        try {
            val response = apiService.getUserTeams("Bearer ${currentState.token}")
            if (response.isSuccessful) {
                val teams = response.body()?.collection?.items ?: emptyList()
                _availableTeams.value = teams
                Log.d(TAG, "Loaded ${teams.size} teams from TeamSnap")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load teams", e)
        }
    }
    
    /**
     * Import team roster from TeamSnap
     */
    suspend fun importTeamRoster(
        teamSnapTeam: TeamSnapTeam,
        localTeamName: String
    ): Result<TeamSnapImportResult> {
        return withContext(Dispatchers.IO) {
            try {
                val currentState = _authState.value
                if (currentState !is TeamSnapAuthState.Authenticated) {
                    return@withContext Result.failure(Exception("Not authenticated with TeamSnap"))
                }
                
                Log.d(TAG, "Importing roster for team: ${teamSnapTeam.name}")
                
                // Get team members from TeamSnap
                val response = apiService.getTeamMembers("Bearer ${currentState.token}", teamSnapTeam.id)
                
                if (response.isSuccessful) {
                    val members = response.body()?.collection?.items ?: emptyList()
                    Log.d(TAG, "Found ${members.size} members in TeamSnap team")
                    
                    val importedPlayers = mutableListOf<Player>()
                    val errors = mutableListOf<String>()
                    var skippedCount = 0
                    
                    for (member in members) {
                        // Skip non-players (coaches, managers, etc.)
                        if (member.isNonPlayer) {
                            skippedCount++
                            continue
                        }
                        
                        // Skip members without names
                        if (member.firstName.isNullOrBlank() && member.lastName.isNullOrBlank()) {
                            skippedCount++
                            errors.add("Skipped member with ID ${member.id}: No name provided")
                            continue
                        }
                        
                        // Create local player from TeamSnap member
                        val player = Player(
                            id = UUID.randomUUID().toString(),
                            name = member.fullName,
                                number = (member.jerseyNumber?.toString() ?: "0"), // Default to "0" if no jersey number
                            position = member.position ?: "",
                            team = localTeamName,
                            academicYear = "Unknown", // Default since TeamSnap doesn't have this field
                            addedBy = "teamsnap_import",
                            teamSnapId = member.id.toString(),
                            teamSnapTeamId = teamSnapTeam.id.toString(),
                            lastSyncDate = System.currentTimeMillis(),
                            syncStatus = TeamSnapSyncStatus.SYNCED
                        )
                        
                        try {
                            playerDao.insertPlayer(player)
                            importedPlayers.add(player)
                            Log.d(TAG, "Imported player: ${player.name} (${player.number})")
                        } catch (e: Exception) {
                            errors.add("Failed to import ${member.fullName}: ${e.message}")
                            Log.e(TAG, "Failed to import player ${member.fullName}", e)
                        }
                    }
                    
                    val result = TeamSnapImportResult(
                        team = teamSnapTeam,
                        members = members,
                        importedCount = importedPlayers.size,
                        skippedCount = skippedCount,
                        errors = errors
                    )
                    
                    Log.d(TAG, "Import completed: ${result.importedCount} imported, ${result.skippedCount} skipped")
                    Result.success(result)
                } else {
                    Result.failure(Exception("Failed to fetch team members: ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Import error", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Refresh authentication token if needed
     */
    private suspend fun refreshTokenIfNeeded(): Boolean {
        val expiresAt = prefs.getLong(KEY_TOKEN_EXPIRES, 0)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        
        // Check if token expires within next 5 minutes
        if (expiresAt > System.currentTimeMillis() + (5 * 60 * 1000)) {
            return true // Token is still valid
        }
        
        if (refreshToken == null) {
            signOut()
            return false
        }
        
        return try {
            val response = apiService.refreshToken(refreshToken)
            if (response.isSuccessful) {
                val authResponse = response.body()!!
                val newExpiresAt = System.currentTimeMillis() + (authResponse.expiresIn * 1000)
                
                prefs.edit()
                    .putString(KEY_ACCESS_TOKEN, authResponse.accessToken)
                    .putLong(KEY_TOKEN_EXPIRES, newExpiresAt)
                    .apply()
                
                true
            } else {
                signOut()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh failed", e)
            signOut()
            false
        }
    }
}

/**
 * Authentication state sealed class
 */
sealed class TeamSnapAuthState {
    object NotAuthenticated : TeamSnapAuthState()
    object Authenticating : TeamSnapAuthState()
    data class Authenticated(val email: String, val token: String) : TeamSnapAuthState()
}