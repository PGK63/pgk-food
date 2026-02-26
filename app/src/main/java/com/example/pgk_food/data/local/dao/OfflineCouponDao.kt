package com.example.pgk_food.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pgk_food.data.local.entity.OfflineCouponEntity

@Dao
interface OfflineCouponDao {
    @Query("SELECT * FROM offline_coupons WHERE id = 'DAILY_COUPONS'")
    fun getDailyCoupons(): OfflineCouponEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveDailyCoupons(coupons: OfflineCouponEntity)

    @Query("DELETE FROM offline_coupons")
    fun clearCoupons()
}
