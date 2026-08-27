package com.playerid.app.domain.referral

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

interface ReferralStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
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

data class MilestoneReward(val title: String, val requiredReferrals: Int)

sealed class ReferralEvent {
    object ShareLinkGenerated : ReferralEvent()
    object QRCodeShared : ReferralEvent()
    data class CodeEntered(val code: String) : ReferralEvent()
    object FreeYearClaimed : ReferralEvent()
    data class MilestoneReached(val referralCount: Int) : ReferralEvent()
}

class ReferralService(
    private val storage: ReferralStorage,
    private val codeGenerator: () -> String = ::generateReferralCode
) {
    private val mutableData = MutableStateFlow(ReferralData())
    val referralData: StateFlow<ReferralData> = mutableData.asStateFlow()

    private val mutableProgress = MutableStateFlow(ReferralProgress())
    val referralProgress: StateFlow<ReferralProgress> = mutableProgress.asStateFlow()

    init {
        reload()
    }

    fun processReferralSignup(referralCode: String?): Boolean {
        if (referralCode.isNullOrEmpty() || referralCode == userReferralCode()) return false
        storage.putString(KEY_REFERRED_BY_CODE, referralCode)
        reload()
        return true
    }

    fun addReferralCount(count: Int = 1) {
        val newCount = storage.getInt(KEY_REFERRAL_COUNT, 0) + count
        storage.putInt(KEY_REFERRAL_COUNT, newCount)
        if (newCount >= REFERRALS_NEEDED_FOR_FREE_YEAR && !mutableData.value.freeYearEarned) {
            storage.putBoolean(KEY_FREE_YEAR_EARNED, true)
        }
        reload()
    }

    fun claimFreeYear(): Boolean {
        if (!mutableProgress.value.canClaimReward) return false
        storage.putBoolean(KEY_FREE_YEAR_USED, true)
        reload()
        return true
    }

    fun hasEarnedFreeYear(): Boolean =
        mutableData.value.freeYearEarned

    fun getReferralLink(): String = "https://spotr.app/invite/${userReferralCode()}"

    fun getReferralShareMessage(): String = """
        🏆 Join me on Spotr - the ultimate sports highlight app for parents!

        ⚽ Record games with AR player names
        📹 Create amazing highlight videos
        📱 Share QR team invites

        Use my referral code: ${userReferralCode()}

        We both get special bonuses when you sign up!

        Download: [App Store Link]
    """.trimIndent()

    fun getNextMilestoneReward(): MilestoneReward? = when {
        mutableData.value.totalReferrals < 5 -> MilestoneReward("Free Year Subscription", 5)
        mutableData.value.totalReferrals < 10 -> MilestoneReward("Exclusive Spotr Merchandise", 10)
        mutableData.value.totalReferrals < 20 -> MilestoneReward("Premium Feature Early Access", 20)
        mutableData.value.totalReferrals < 50 -> MilestoneReward("Spotr Ambassador Status", 50)
        else -> null
    }

    private fun reload() {
        val data = ReferralData(
            userReferralCode = userReferralCode(),
            totalReferrals = storage.getInt(KEY_REFERRAL_COUNT, 0),
            freeYearEarned = storage.getBoolean(KEY_FREE_YEAR_EARNED, false),
            freeYearUsed = storage.getBoolean(KEY_FREE_YEAR_USED, false),
            referredByCode = storage.getString(KEY_REFERRED_BY_CODE),
            referralBonusClaimed = storage.getBoolean(KEY_REFERRAL_BONUS_CLAIMED, false)
        )
        mutableData.value = data
        mutableProgress.value = ReferralProgress(
            currentReferrals = data.totalReferrals,
            targetReferrals = REFERRALS_NEEDED_FOR_FREE_YEAR,
            progressPercentage = (data.totalReferrals.toFloat() / REFERRALS_NEEDED_FOR_FREE_YEAR).coerceAtMost(1f),
            canClaimReward = data.freeYearEarned && !data.freeYearUsed
        )
    }

    private fun userReferralCode(): String {
        storage.getString(KEY_USER_REFERRAL_CODE)?.let { return it }
        return codeGenerator().also { storage.putString(KEY_USER_REFERRAL_CODE, it) }
    }

    private companion object {
        const val KEY_USER_REFERRAL_CODE = "user_referral_code"
        const val KEY_REFERRAL_COUNT = "referral_count"
        const val KEY_FREE_YEAR_EARNED = "free_year_earned"
        const val KEY_FREE_YEAR_USED = "free_year_used"
        const val KEY_REFERRED_BY_CODE = "referred_by_code"
        const val KEY_REFERRAL_BONUS_CLAIMED = "referral_bonus_claimed"
        const val REFERRALS_NEEDED_FOR_FREE_YEAR = 5

        fun generateReferralCode(): String {
            val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            return (1..6).map { characters[Random.nextInt(characters.length)] }.joinToString("")
        }
    }
}