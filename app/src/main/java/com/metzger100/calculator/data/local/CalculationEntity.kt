package com.metzger100.calculator.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculations")
data class CalculationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val input: String,
    val result: String
)