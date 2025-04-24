package com.metzger100.calculator.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CalculationDao {

    @Insert
    suspend fun insert(calculation: CalculationEntity)

    @Query("SELECT * FROM calculations ORDER BY id ASC")
    suspend fun getAll(): List<CalculationEntity>

    @Query("DELETE FROM calculations")
    suspend fun clearAll()
}