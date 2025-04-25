// com/metzger100/calculator/data/local/CurrencyPrefsEntity.kt
package com.metzger100.calculator.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currency_prefs")
data class CurrencyPrefsEntity(
    @PrimaryKey val id: Int = 1,       // always a single row
    val activeField: Int,              // 1 or 2
    val currency1: String,
    val currency2: String,
    val amount1: String,
    val amount2: String
)
