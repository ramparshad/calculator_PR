// com/metzger100/calculator/data/local/CurrencyPrefsDao.kt
package com.metzger100.calculator.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CurrencyPrefsDao {
    @Query("SELECT * FROM currency_prefs WHERE id = 1")
    suspend fun get(): CurrencyPrefsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CurrencyPrefsEntity)
}