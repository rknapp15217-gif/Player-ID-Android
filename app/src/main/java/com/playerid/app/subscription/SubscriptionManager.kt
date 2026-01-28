package com.playerid.app.subscription

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
        // Check if user has referral free year first
        if (referralManager.hasEarnedFreeYear()) {
            return true
        }
        
        return when (_subscriptionStatus.value) {
            SubscriptionStatus.FREE_TRIAL -> _trialDaysRemaining.value > 0
            SubscriptionStatus.SUBSCRIBED -> true
            SubscriptionStatus.REFERRAL_FREE_YEAR -> true
            SubscriptionStatus.EXPIRED -> false
        }
    }
    
    fun canExportVideo(): Boolean {
        return canRecordVideo()
    }
    
    fun canShareHighlights(): Boolean {
        return canRecordVideo()
    }
    
    fun canAccessAdvancedFeatures(): Boolean {
        return _subscriptionStatus.value == SubscriptionStatus.SUBSCRIBED || 
               _subscriptionStatus.value == SubscriptionStatus.REFERRAL_FREE_YEAR ||
               referralManager.hasEarnedFreeYear()
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
        val referralData = referralManager.referralData.value
        return when {
            referralData.freeYearEarned && !referralData.freeYearUsed -> "🎉 Free year available to claim!"
            referralData.freeYearUsed -> "✅ Using referral free year"
            else -> "Refer ${5 - referralData.totalReferrals} more friends for a free year!"
        }
    }
    
    fun checkTrialStatus() {
        // In production, this would check actual trial start date
        // For now, simulate trial countdown
        val trialStartDate = getTrialStartDate()
        val currentDate = Date()
        val daysSinceStart = ((currentDate.time - trialStartDate.time) / (1000 * 60 * 60 * 24)).toInt()
        val daysRemaining = maxOf(0, 14 - daysSinceStart)
        
        _trialDaysRemaining.value = daysRemaining
        
        if (daysRemaining <= 0 && _subscriptionStatus.value == SubscriptionStatus.FREE_TRIAL) {
            _subscriptionStatus.value = SubscriptionStatus.EXPIRED
        }
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
        val days = _trialDaysRemaining.value
        return when {
            days > 7 -> "Free trial: $days days remaining"
            days > 3 -> "Trial ending soon: $days days left"
            days > 0 -> "Last chance: $days day${if (days == 1) "" else "s"} remaining"
            else -> "Trial expired - Subscribe to continue"
        }
    }
}

enum class SubscriptionStatus {
    FREE_TRIAL,
    SUBSCRIBED,
    REFERRAL_FREE_YEAR,
    EXPIRED
}

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