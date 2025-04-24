package com.metzger100.calculator.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CurrencyRateDao {
    @Query("SELECT * FROM currency_rates WHERE base = :base")
    suspend fun get(base: String): CurrencyRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CurrencyRateEntity)
}