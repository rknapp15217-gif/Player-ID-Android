package com.playerid.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Entity(tableName = "referrals")
data class Referral(
    @PrimaryKey val id: String,
    val referrerUserId: String,
    val referredUserId: String, 
    val referralCode: String,
    val dateReferred: Date,
    val isActive: Boolean = true, // False if referred user cancels subscription
    val rewardClaimed: Boolean = false
)

@Entity(tableName = "referral_rewards")
data class ReferralReward(
    @PrimaryKey val id: String,
    val userId: String,
    val rewardType: String, // "free_year", "merchandise", "early_access", etc.
    val referralsRequired: Int,
    val dateEarned: Date,
    val dateClaimed: Date? = null,
    val isActive: Boolean = true
)

@Entity(tableName = "referral_codes")
data class ReferralCode(
    @PrimaryKey val code: String,
    val userId: String,
    val dateCreated: Date,
    val isActive: Boolean = true,
    val totalUses: Int = 0
)

@Dao
interface ReferralDao {
    
    @Query("SELECT * FROM referrals WHERE referrerUserId = :userId AND isActive = 1")
    fun getUserReferrals(userId: String): Flow<List<Referral>>
    
    @Query("SELECT COUNT(*) FROM referrals WHERE referrerUserId = :userId AND isActive = 1")
    suspend fun getReferralCount(userId: String): Int
    
    @Query("SELECT * FROM referral_codes WHERE userId = :userId AND isActive = 1 LIMIT 1")
    suspend fun getUserReferralCode(userId: String): ReferralCode?
    
    @Query("SELECT * FROM referral_rewards WHERE userId = :userId ORDER BY dateEarned DESC")
    fun getUserRewards(userId: String): Flow<List<ReferralReward>>
    
    @Query("SELECT * FROM referral_rewards WHERE userId = :userId AND rewardType = 'free_year' AND dateClaimed IS NULL LIMIT 1")
    suspend fun getUnclaimedFreeYear(userId: String): ReferralReward?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReferral(referral: Referral)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReferralCode(referralCode: ReferralCode)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReward(reward: ReferralReward)
    
    @Update
    suspend fun updateReferral(referral: Referral)
    
    @Update
    suspend fun updateReward(reward: ReferralReward)
    
    @Query("UPDATE referral_codes SET totalUses = totalUses + 1 WHERE code = :code")
    suspend fun incrementCodeUsage(code: String)
    
    @Transaction
    @Query("""
        SELECT r.*, rc.userId as referrerUserId 
        FROM referrals r 
        JOIN referral_codes rc ON r.referralCode = rc.code 
        WHERE r.referredUserId = :userId 
        LIMIT 1
    """)
    suspend fun getReferralInfo(userId: String): ReferralWithReferrer?
}

data class ReferralWithReferrer(
    @Embedded val referral: Referral,
    val referrerUserId: String
)

// Database type converters for Date
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}