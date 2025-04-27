package com.metzger100.calculator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currency_list")
data class CurrencyListEntity(
    @PrimaryKey val id: Int = 0,        // wir speichern genau eine Zeile
    val json: String,                   // das rohe JSON
    val timestamp: Long                 // epochMillis
)
