package com.playerid.app.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {
    
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private val _showLoginDialog = MutableStateFlow(false)
    val showLoginDialog: StateFlow<Boolean> = _showLoginDialog.asStateFlow()
    
    // In production, use secure storage
    private val adminPassword = "PlayerID2025!"
    
    fun authenticate(password: String): Boolean {
        val isValid = password == adminPassword
        _isAuthenticated.value = isValid
        if (isValid) {
            _showLoginDialog.value = false
        }
        return isValid
    }
    
    fun logout() {
        _isAuthenticated.value = false
    }
    
    fun showLoginDialog() {
        _showLoginDialog.value = true
    }
    
    fun hideLoginDialog() {
        _showLoginDialog.value = false
    }
}