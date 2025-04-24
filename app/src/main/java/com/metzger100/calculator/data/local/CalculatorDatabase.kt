package com.metzger100.calculator.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CalculationEntity::class,
        CurrencyRateEntity::class,
        CurrencyListEntity::class
    ],
    version = 1
)
abstract class CalculatorDatabase : RoomDatabase() {
    abstract fun calculationDao(): CalculationDao
    abstract fun currencyRateDao(): CurrencyRateDao
    abstract fun currencyListDao(): CurrencyListDao

    companion object {
        // falls Version geändert, bei Dev am einfachsten:
        fun getDatabase(context: Context): CalculatorDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                CalculatorDatabase::class.java,
                "calculator_db"
            )
                .fallbackToDestructiveMigration(false)
                .build()
        }
    }
}