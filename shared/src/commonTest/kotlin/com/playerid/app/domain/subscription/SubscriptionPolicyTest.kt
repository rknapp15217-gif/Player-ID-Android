package com.playerid.app.domain.subscription

import com.playerid.app.domain.referral.ReferralData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscriptionPolicyTest {
    @Test
    fun trialAndReferralAccessFollowPolicy() {
        assertTrue(canRecordVideo(SubscriptionStatus.FREE_TRIAL, 1, false))
        assertFalse(canRecordVideo(SubscriptionStatus.FREE_TRIAL, 0, false))
        assertTrue(canRecordVideo(SubscriptionStatus.EXPIRED, 0, true))
        assertFalse(canAccessAdvancedFeatures(SubscriptionStatus.FREE_TRIAL, false))
        assertTrue(canAccessAdvancedFeatures(SubscriptionStatus.SUBSCRIBED, false))
    }

    @Test
    fun trialCountdownExpiresAtFourteenDays() {
        val start = 1_000L
        assertEquals(14, trialDaysRemaining(start, start))
        assertEquals(1, trialDaysRemaining(start, start + 13L * 24L * 60L * 60L * 1000L))
        assertEquals(0, trialDaysRemaining(start, start + 14L * 24L * 60L * 60L * 1000L))
        assertEquals(
            SubscriptionStatus.EXPIRED,
            statusAfterTrialCheck(SubscriptionStatus.FREE_TRIAL, 0)
        )
    }

    @Test
    fun messagesCoverTrialAndReferralStates() {
        assertEquals("Last chance: 1 day remaining", trialMessage(1))
        assertEquals(
            "Refer 3 more friends for a free year!",
            referralStatus(ReferralData(totalReferrals = 2))
        )
    }
}