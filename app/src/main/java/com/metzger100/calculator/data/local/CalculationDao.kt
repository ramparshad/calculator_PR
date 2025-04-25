package com.metzger100.calculator.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CalculationDao {

    @Insert
    suspend fun insert(calculation: CalculationEntity)

    @Query("""
        DELETE FROM calculations
        WHERE id IN (
            SELECT id FROM calculations
            ORDER BY id ASC
            LIMIT (
                SELECT COUNT(*) - :limit FROM calculations
                WHERE (SELECT COUNT(*) FROM calculations) > :limit
            )
        )
    """)
    suspend fun deleteOldEntriesIfOverLimit(limit: Int)

    @Query("SELECT * FROM calculations ORDER BY id ASC")
    suspend fun getAll(): List<CalculationEntity>

    @Query("DELETE FROM calculations")
    suspend fun clearAll()
}