package com.playerid.app.referral

import android.content.Context
import android.content.SharedPreferences
import com.playerid.app.domain.referral.ReferralService
import com.playerid.app.domain.referral.ReferralStorage

typealias ReferralData = com.playerid.app.domain.referral.ReferralData
typealias ReferralProgress = com.playerid.app.domain.referral.ReferralProgress
typealias MilestoneReward = com.playerid.app.domain.referral.MilestoneReward
typealias ReferralEvent = com.playerid.app.domain.referral.ReferralEvent

class ReferralManager(context: Context) {
    private val preferences = context.getSharedPreferences("referral_prefs", Context.MODE_PRIVATE)
    private val service = ReferralService(SharedPreferencesReferralStorage(preferences))

    val referralData = service.referralData
    val referralProgress = service.referralProgress

    fun processReferralSignup(referralCode: String?): Boolean =
        service.processReferralSignup(referralCode)

    fun addReferralCount(count: Int = 1) = service.addReferralCount(count)

    fun claimFreeYear(): Boolean = service.claimFreeYear()

    fun hasEarnedFreeYear(): Boolean = service.hasEarnedFreeYear()

    fun getReferralShareMessage(): String = service.getReferralShareMessage()

    fun getReferralLink(): String = service.getReferralLink()

    fun getNextMilestoneReward(): MilestoneReward? = service.getNextMilestoneReward()
}

private class SharedPreferencesReferralStorage(
    private val preferences: SharedPreferences
) : ReferralStorage {
    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        preferences.getInt(key, defaultValue)

    override fun putInt(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        preferences.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }
}
