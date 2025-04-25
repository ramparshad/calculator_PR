package com.metzger100.calculator.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CalculationDao {

    @Insert
    suspend fun insert(calculation: CalculationEntity)

    @Query("SELECT COUNT(*) FROM calculations")
    suspend fun getCount(): Int

    @Query("DELETE FROM calculations WHERE id IN (SELECT id FROM calculations ORDER BY id ASC LIMIT :count)")
    suspend fun deleteOldestEntries(count: Int)

    @Query("SELECT * FROM calculations ORDER BY id ASC")
    suspend fun getAll(): List<CalculationEntity>

    @Query("DELETE FROM calculations")
    suspend fun clearAll()
}