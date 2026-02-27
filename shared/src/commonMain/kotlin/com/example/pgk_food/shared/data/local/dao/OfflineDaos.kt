package com.example.pgk_food.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pgk_food.shared.data.local.entity.MenuItemEntity
import com.example.pgk_food.shared.data.local.entity.OfflineTransactionEntity
import com.example.pgk_food.shared.data.local.entity.PermissionCacheEntity
import com.example.pgk_food.shared.data.local.entity.StudentKeyEntity

@Dao
interface MenuDao {
    @Query("SELECT * FROM menu_items")
    suspend fun getMenu(): List<MenuItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMenu(items: List<MenuItemEntity>)

    @Query("DELETE FROM menu_items")
    suspend fun clearMenu()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM offline_transactions WHERE synced = 0")
    suspend fun getUnsyncedTransactions(): List<OfflineTransactionEntity>

    @Query("SELECT * FROM offline_transactions ORDER BY scannedAt DESC")
    suspend fun getAllTransactions(): List<OfflineTransactionEntity>

    @Query("SELECT COUNT(*) FROM offline_transactions WHERE synced = 0")
    suspend fun getUnsyncedCount(): Int

    @Insert
    suspend fun saveTransaction(transaction: OfflineTransactionEntity)

    @Query("UPDATE offline_transactions SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Int>)

    @Query("DELETE FROM offline_transactions WHERE id IN (:ids)")
    suspend fun deleteTransactions(ids: List<Int>)

    @Query("SELECT * FROM offline_transactions WHERE studentId = :studentId AND mealType = :mealType AND scannedAt > :dayStart AND synced = 0")
    suspend fun findByStudentAndMealToday(studentId: String, mealType: String, dayStart: Long): List<OfflineTransactionEntity>
}

@Dao
interface StudentKeyDao {
    @Query("SELECT * FROM student_keys WHERE userId = :userId")
    suspend fun getKey(userId: String): StudentKeyEntity?

    @Query("SELECT * FROM student_keys")
    suspend fun getAllKeys(): List<StudentKeyEntity>

    @Query("SELECT COUNT(*) FROM student_keys")
    suspend fun getKeyCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveKeys(keys: List<StudentKeyEntity>)

    @Query("DELETE FROM student_keys")
    suspend fun clearAll()
}

@Dao
interface PermissionCacheDao {
    @Query("SELECT * FROM permission_cache WHERE studentId = :studentId AND date = :date")
    suspend fun getPermission(studentId: String, date: String): PermissionCacheEntity?

    @Query("SELECT COUNT(*) FROM permission_cache")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePermissions(perms: List<PermissionCacheEntity>)

    @Query("DELETE FROM permission_cache")
    suspend fun clearAll()
}
