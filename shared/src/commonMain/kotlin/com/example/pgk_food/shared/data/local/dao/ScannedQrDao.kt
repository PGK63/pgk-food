package com.example.pgk_food.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.pgk_food.shared.data.local.entity.ScannedQrEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedQrDao {
    @Insert
    suspend fun insert(scannedQr: ScannedQrEntity)

    @Query("SELECT * FROM scanned_qrs ORDER BY timestamp DESC")
    fun getAllScannedQrs(): Flow<List<ScannedQrEntity>>

    @Query("SELECT * FROM scanned_qrs WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getScannedQrsSince(sinceTimestamp: Long): Flow<List<ScannedQrEntity>>

    @Query("DELETE FROM scanned_qrs")
    suspend fun clearHistory()
}
