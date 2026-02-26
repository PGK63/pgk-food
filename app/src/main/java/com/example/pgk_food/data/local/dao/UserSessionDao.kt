package com.example.pgk_food.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pgk_food.data.local.entity.UserSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSessionDao {
    @Query("SELECT * FROM user_session LIMIT 1")
    fun getUserSession(): Flow<UserSessionEntity?>

    @Query("SELECT * FROM user_session LIMIT 1")
    fun getUserSessionSync(): UserSessionEntity?

    @Query("SELECT token FROM user_session LIMIT 1")
    fun getTokenSync(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: UserSessionEntity)

    @Query("DELETE FROM user_session")
    suspend fun clearSession()
}
