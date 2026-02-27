package com.example.pgk_food.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pgk_food.shared.data.local.entity.OfflineCouponEntity

@Dao
interface OfflineCouponDao {
    @Query("SELECT * FROM offline_coupons WHERE id = 'DAILY_COUPONS'")
    suspend fun getDailyCoupons(): OfflineCouponEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDailyCoupons(coupons: OfflineCouponEntity)

    @Query("DELETE FROM offline_coupons")
    suspend fun clearCoupons()
}
