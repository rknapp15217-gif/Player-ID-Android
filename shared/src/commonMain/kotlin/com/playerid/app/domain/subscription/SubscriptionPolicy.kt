package com.playerid.app.domain.subscription

import com.playerid.app.domain.referral.ReferralData

enum class SubscriptionStatus {
    FREE_TRIAL,
    SUBSCRIBED,
    REFERRAL_FREE_YEAR,
    EXPIRED
}

fun canRecordVideo(
    status: SubscriptionStatus,
    trialDaysRemaining: Int,
    hasReferralFreeYear: Boolean
): Boolean = hasReferralFreeYear || when (status) {
    SubscriptionStatus.FREE_TRIAL -> trialDaysRemaining > 0
    SubscriptionStatus.SUBSCRIBED,
    SubscriptionStatus.REFERRAL_FREE_YEAR -> true
    SubscriptionStatus.EXPIRED -> false
}

fun canAccessAdvancedFeatures(
    status: SubscriptionStatus,
    hasReferralFreeYear: Boolean
): Boolean = hasReferralFreeYear || status == SubscriptionStatus.SUBSCRIBED ||
    status == SubscriptionStatus.REFERRAL_FREE_YEAR

fun trialDaysRemaining(trialStartMs: Long, currentMs: Long, trialLengthDays: Int = 14): Int {
    val elapsedDays = ((currentMs - trialStartMs).coerceAtLeast(0L) / DAY_MS).toInt()
    return (trialLengthDays - elapsedDays).coerceAtLeast(0)
}

fun statusAfterTrialCheck(status: SubscriptionStatus, daysRemaining: Int): SubscriptionStatus =
    if (daysRemaining <= 0 && status == SubscriptionStatus.FREE_TRIAL) {
        SubscriptionStatus.EXPIRED
    } else {
        status
    }

fun trialMessage(daysRemaining: Int): String = when {
    daysRemaining > 7 -> "Free trial: $daysRemaining days remaining"
    daysRemaining > 3 -> "Trial ending soon: $daysRemaining days left"
    daysRemaining > 0 -> "Last chance: $daysRemaining day${if (daysRemaining == 1) "" else "s"} remaining"
    else -> "Trial expired - Subscribe to continue"
}

fun referralStatus(data: ReferralData): String = when {
    data.freeYearEarned && !data.freeYearUsed -> "🎉 Free year available to claim!"
    data.freeYearUsed -> "✅ Using referral free year"
    else -> "Refer ${(5 - data.totalReferrals).coerceAtLeast(0)} more friends for a free year!"
}

private const val DAY_MS = 24L * 60L * 60L * 1000L