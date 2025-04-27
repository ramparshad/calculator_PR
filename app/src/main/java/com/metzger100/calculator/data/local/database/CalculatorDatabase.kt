package com.metzger100.calculator.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.metzger100.calculator.data.local.dao.CalculationDao
import com.metzger100.calculator.data.local.dao.CurrencyListDao
import com.metzger100.calculator.data.local.dao.CurrencyPrefsDao
import com.metzger100.calculator.data.local.dao.CurrencyRateDao
import com.metzger100.calculator.data.local.entity.CalculationEntity
import com.metzger100.calculator.data.local.entity.CurrencyListEntity
import com.metzger100.calculator.data.local.entity.CurrencyPrefsEntity
import com.metzger100.calculator.data.local.entity.CurrencyRateEntity

@Database(
    entities = [
        CalculationEntity::class,
        CurrencyRateEntity::class,
        CurrencyListEntity::class,
        CurrencyPrefsEntity::class
    ],
    version = 2
)
abstract class CalculatorDatabase : RoomDatabase() {
    abstract fun calculationDao(): CalculationDao
    abstract fun currencyRateDao(): CurrencyRateDao
    abstract fun currencyListDao(): CurrencyListDao
    abstract fun currencyPrefsDao(): CurrencyPrefsDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1) neue Tabelle anlegen
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `currency_prefs` (
                `id` INTEGER NOT NULL PRIMARY KEY,
                `activeField` INTEGER NOT NULL,
                `currency1` TEXT NOT NULL,
                `currency2` TEXT NOT NULL,
                `amount1` TEXT NOT NULL,
                `amount2` TEXT NOT NULL
            )
            """.trimIndent()
        )

        // 2) optional: einen Default-Datensatz einfügen,
        //    damit get() nicht null zurückgibt
        db.execSQL(
            """
            INSERT OR IGNORE INTO `currency_prefs`
                (id, activeField, currency1, currency2, amount1, amount2)
            VALUES
                (1, 1, 'USD', 'EUR', '1', '0')
            """
        )
    }
}