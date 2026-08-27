package com.playerid.app.domain.referral

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReferralServiceTest {
    @Test
    fun generatedCodeIsPersistedAndReused() {
        val storage = MemoryStorage()
        val service = ReferralService(storage) { "ABC123" }

        assertEquals("ABC123", service.referralData.value.userReferralCode)
        assertEquals("https://spotr.app/invite/ABC123", service.getReferralLink())
        assertEquals("ABC123", storage.strings["user_referral_code"])
    }

    @Test
    fun referralsEarnAvailableFreeYearAtThreshold() {
        val service = ReferralService(MemoryStorage()) { "ABC123" }

        service.addReferralCount(5)

        assertEquals(5, service.referralData.value.totalReferrals)
        assertTrue(service.hasEarnedFreeYear())
        assertTrue(service.referralProgress.value.canClaimReward)
        assertEquals(1f, service.referralProgress.value.progressPercentage)

        assertTrue(service.claimFreeYear())
        assertTrue(service.referralData.value.freeYearUsed)
        assertFalse(service.referralProgress.value.canClaimReward)
        assertTrue(service.hasEarnedFreeYear())
    }

    @Test
    fun freeYearCannotBeClaimedBeforeItIsEarned() {
        val service = ReferralService(MemoryStorage()) { "ABC123" }

        assertFalse(service.claimFreeYear())
        assertFalse(service.referralData.value.freeYearUsed)
    }

    @Test
    fun signupRejectsEmptyAndOwnCodes() {
        val service = ReferralService(MemoryStorage()) { "ABC123" }

        assertFalse(service.processReferralSignup(null))
        assertFalse(service.processReferralSignup("ABC123"))
        assertTrue(service.processReferralSignup("OTHER1"))
        assertEquals("OTHER1", service.referralData.value.referredByCode)
    }

    private class MemoryStorage : ReferralStorage {
        val strings = mutableMapOf<String, String>()
        private val integers = mutableMapOf<String, Int>()
        private val booleans = mutableMapOf<String, Boolean>()

        override fun getString(key: String): String? = strings[key]
        override fun putString(key: String, value: String) { strings[key] = value }
        override fun getInt(key: String, defaultValue: Int): Int = integers[key] ?: defaultValue
        override fun putInt(key: String, value: Int) { integers[key] = value }
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = booleans[key] ?: defaultValue
        override fun putBoolean(key: String, value: Boolean) { booleans[key] = value }
    }
}