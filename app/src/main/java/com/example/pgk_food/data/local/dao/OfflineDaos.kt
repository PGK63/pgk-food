package com.example.pgk_food.data.local.dao

import androidx.room.*
import com.example.pgk_food.data.local.entity.MenuItemEntity
import com.example.pgk_food.data.local.entity.OfflineTransactionEntity

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
    @Query("SELECT * FROM offline_transactions")
    suspend fun getPendingTransactions(): List<OfflineTransactionEntity>

    @Insert
    suspend fun saveTransaction(transaction: OfflineTransactionEntity)

    @Query("DELETE FROM offline_transactions WHERE id IN (:ids)")
    suspend fun deleteTransactions(ids: List<Int>)
}
