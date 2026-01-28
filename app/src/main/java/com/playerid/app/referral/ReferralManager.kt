package com.playerid.app.referral

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import kotlin.random.Random

class ReferralManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("referral_prefs", Context.MODE_PRIVATE)
    
    private val _referralData = MutableStateFlow(ReferralData())
    val referralData: StateFlow<ReferralData> = _referralData.asStateFlow()
    
    private val _referralProgress = MutableStateFlow(ReferralProgress())
    val referralProgress: StateFlow<ReferralProgress> = _referralProgress.asStateFlow()
    
    companion object {
        private const val KEY_USER_REFERRAL_CODE = "user_referral_code"
        private const val KEY_REFERRAL_COUNT = "referral_count"
        private const val KEY_FREE_YEAR_EARNED = "free_year_earned"
        private const val KEY_FREE_YEAR_USED = "free_year_used"
        private const val KEY_REFERRED_BY_CODE = "referred_by_code"
        private const val KEY_REFERRAL_BONUS_CLAIMED = "referral_bonus_claimed"
        private const val REFERRALS_NEEDED_FOR_FREE_YEAR = 5
    }
    
    init {
        loadReferralData()
    }
    
    private fun loadReferralData() {
        val userCode = getUserReferralCode()
        val referralCount = prefs.getInt(KEY_REFERRAL_COUNT, 0)
        val freeYearEarned = prefs.getBoolean(KEY_FREE_YEAR_EARNED, false)
        val freeYearUsed = prefs.getBoolean(KEY_FREE_YEAR_USED, false)
        val referredByCode = prefs.getString(KEY_REFERRED_BY_CODE, null)
        val bonusClaimed = prefs.getBoolean(KEY_REFERRAL_BONUS_CLAIMED, false)
        
        _referralData.value = ReferralData(
            userReferralCode = userCode,
            totalReferrals = referralCount,
            freeYearEarned = freeYearEarned,
            freeYearUsed = freeYearUsed,
            referredByCode = referredByCode,
            referralBonusClaimed = bonusClaimed
        )
        
        _referralProgress.value = ReferralProgress(
            currentReferrals = referralCount,
            targetReferrals = REFERRALS_NEEDED_FOR_FREE_YEAR,
            progressPercentage = (referralCount.toFloat() / REFERRALS_NEEDED_FOR_FREE_YEAR).coerceAtMost(1f),
            canClaimReward = referralCount >= REFERRALS_NEEDED_FOR_FREE_YEAR && !freeYearEarned
        )
    }
    
    private fun getUserReferralCode(): String {
        var code = prefs.getString(KEY_USER_REFERRAL_CODE, null)
        if (code == null) {
            code = generateReferralCode()
            prefs.edit().putString(KEY_USER_REFERRAL_CODE, code).apply()
        }
        return code
    }
    
    private fun generateReferralCode(): String {
        // Generate a unique 6-character alphanumeric code
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }
    
    fun processReferralSignup(referralCode: String?): Boolean {
        if (referralCode.isNullOrEmpty() || referralCode == getUserReferralCode()) {
            return false // Can't refer yourself
        }
        
        // Store that this user was referred
        prefs.edit().putString(KEY_REFERRED_BY_CODE, referralCode).apply()
        
        // In a real app, this would make an API call to increment the referrer's count
        // For now, we'll simulate local tracking (in production, use Firebase/backend)
        
        return true
    }
    
    fun addReferralCount(count: Int = 1) {
        val currentCount = prefs.getInt(KEY_REFERRAL_COUNT, 0)
        val newCount = currentCount + count
        
        prefs.edit().putInt(KEY_REFERRAL_COUNT, newCount).apply()
        
        // Check if user earned free year
        if (newCount >= REFERRALS_NEEDED_FOR_FREE_YEAR && !_referralData.value.freeYearEarned) {
            prefs.edit().putBoolean(KEY_FREE_YEAR_EARNED, true).apply()
        }
        
        loadReferralData()
    }
    
    fun claimFreeYear(): Boolean {
        if (_referralProgress.value.canClaimReward) {
            prefs.edit().putBoolean(KEY_FREE_YEAR_USED, true).apply()
            loadReferralData()
            return true
        }
        return false
    }
    
    fun hasEarnedFreeYear(): Boolean {
        return _referralData.value.freeYearEarned && !_referralData.value.freeYearUsed
    }
    
    fun getReferralShareMessage(): String {
        val code = getUserReferralCode()
        return """
            🏆 Join me on Spotr - the ultimate sports highlight app for parents!
            
            ⚽ Record games with AR player names
            📹 Create amazing highlight videos
            📱 Share QR team invites
            
            Use my referral code: $code
            
            We both get special bonuses when you sign up!
            
            Download: [App Store Link]
        """.trimIndent()
    }
    
    fun getReferralLink(): String {
        val code = getUserReferralCode()
        return "https://spotr.app/invite/$code"
    }
    
    // Milestone rewards beyond free year
    fun getNextMilestoneReward(): MilestoneReward? {
        val currentReferrals = _referralData.value.totalReferrals
        
        return when {
            currentReferrals < 5 -> MilestoneReward("Free Year Subscription", 5)
            currentReferrals < 10 -> MilestoneReward("Exclusive Spotr Merchandise", 10)
            currentReferrals < 20 -> MilestoneReward("Premium Feature Early Access", 20)
            currentReferrals < 50 -> MilestoneReward("Spotr Ambassador Status", 50)
            else -> null
        }
    }
}

data class ReferralData(
    val userReferralCode: String = "",
    val totalReferrals: Int = 0,
    val freeYearEarned: Boolean = false,
    val freeYearUsed: Boolean = false,
    val referredByCode: String? = null,
    val referralBonusClaimed: Boolean = false
)

data class ReferralProgress(
    val currentReferrals: Int = 0,
    val targetReferrals: Int = 5,
    val progressPercentage: Float = 0f,
    val canClaimReward: Boolean = false
)

data class MilestoneReward(
    val title: String,
    val requiredReferrals: Int
)

// Referral tracking events for analytics
sealed class ReferralEvent {
    object ShareLinkGenerated : ReferralEvent()
    object QRCodeShared : ReferralEvent()
    data class CodeEntered(val code: String) : ReferralEvent()
    object FreeYearClaimed : ReferralEvent()
    data class MilestoneReached(val referralCount: Int) : ReferralEvent()
}