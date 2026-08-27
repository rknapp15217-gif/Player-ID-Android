package com.playerid.app.subscription

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.playerid.app.domain.subscription.canAccessAdvancedFeatures
import com.playerid.app.domain.subscription.canRecordVideo
import com.playerid.app.domain.subscription.referralStatus
import com.playerid.app.domain.subscription.statusAfterTrialCheck
import com.playerid.app.domain.subscription.trialDaysRemaining
import com.playerid.app.domain.subscription.trialMessage
import com.playerid.app.referral.ReferralManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class SubscriptionManager(private val context: Context) {
    
    private val referralManager = ReferralManager(context)
    
    private val _subscriptionStatus = MutableStateFlow(SubscriptionStatus.FREE_TRIAL)
    val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()
    
    private val _trialDaysRemaining = MutableStateFlow(14)
    val trialDaysRemaining: StateFlow<Int> = _trialDaysRemaining.asStateFlow()
    
    private val _isPaywallVisible = MutableStateFlow(false)
    val isPaywallVisible: StateFlow<Boolean> = _isPaywallVisible.asStateFlow()
    
    private val _hasReferralFreeYear = MutableStateFlow(false)
    val hasReferralFreeYear: StateFlow<Boolean> = _hasReferralFreeYear.asStateFlow()
    
    // Features that require subscription
    fun canRecordVideo(): Boolean {
        return canRecordVideo(
            _subscriptionStatus.value,
            _trialDaysRemaining.value,
            referralManager.hasEarnedFreeYear()
        )
    }
    
    fun canExportVideo(): Boolean {
        return canRecordVideo()
    }
    
    fun canShareHighlights(): Boolean {
        return canRecordVideo()
    }
    
    fun canAccessAdvancedFeatures(): Boolean {
        return canAccessAdvancedFeatures(
            _subscriptionStatus.value,
            referralManager.hasEarnedFreeYear()
        )
    }
    
    fun processReferralReward(): Boolean {
        if (referralManager.hasEarnedFreeYear()) {
            _subscriptionStatus.value = SubscriptionStatus.REFERRAL_FREE_YEAR
            _hasReferralFreeYear.value = true
            return true
        }
        return false
    }
    
    fun getReferralStatus(): String {
        return referralStatus(referralManager.referralData.value)
    }
    
    fun checkTrialStatus() {
        // In production, this would check actual trial start date
        // For now, simulate trial countdown
        val trialStartDate = getTrialStartDate()
        val currentDate = Date()
        val daysRemaining = trialDaysRemaining(trialStartDate.time, currentDate.time)
        _trialDaysRemaining.value = daysRemaining
        _subscriptionStatus.value = statusAfterTrialCheck(_subscriptionStatus.value, daysRemaining)
    }
    
    fun showPaywall() {
        _isPaywallVisible.value = true
    }
    
    fun hidePaywall() {
        _isPaywallVisible.value = false
    }
    
    fun startSubscription() {
        // In production, this would integrate with Google Play Billing
        _subscriptionStatus.value = SubscriptionStatus.SUBSCRIBED
        _isPaywallVisible.value = false
    }
    
    fun restorePurchases() {
        // In production, this would check Google Play Billing for existing purchases
        // For now, simulate restore
    }
    
    private fun getTrialStartDate(): Date {
        // In production, this would be stored in secure preferences
        // For demo, assume trial started today
        return Date()
    }
    
    fun getSubscriptionPrice(): String {
        return "$9.99/year" // Slightly less than $10 for psychological pricing
    }
    
    fun getTrialMessage(): String {
        return trialMessage(_trialDaysRemaining.value)
    }
}

typealias SubscriptionStatus = com.playerid.app.domain.subscription.SubscriptionStatus

class SubscriptionViewModel(private val subscriptionManager: SubscriptionManager) : ViewModel() {
    
    val subscriptionStatus = subscriptionManager.subscriptionStatus
    val trialDaysRemaining = subscriptionManager.trialDaysRemaining
    val isPaywallVisible = subscriptionManager.isPaywallVisible
    val hasReferralFreeYear = subscriptionManager.hasReferralFreeYear
    
    fun canRecordVideo() = subscriptionManager.canRecordVideo()
    fun canExportVideo() = subscriptionManager.canExportVideo()
    fun showPaywall() = subscriptionManager.showPaywall()
    fun hidePaywall() = subscriptionManager.hidePaywall()
    fun startSubscription() = subscriptionManager.startSubscription()
    fun restorePurchases() = subscriptionManager.restorePurchases()
    fun getSubscriptionPrice() = subscriptionManager.getSubscriptionPrice()
    fun getTrialMessage() = subscriptionManager.getTrialMessage()
    fun processReferralReward() = subscriptionManager.processReferralReward()
    fun getReferralStatus() = subscriptionManager.getReferralStatus()
    
    init {
        subscriptionManager.checkTrialStatus()
        subscriptionManager.processReferralReward() // Check for referral rewards on startup
    }
}

class SubscriptionViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubscriptionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SubscriptionViewModel(SubscriptionManager(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}