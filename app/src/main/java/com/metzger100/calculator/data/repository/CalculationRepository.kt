package com.metzger100.calculator.data.repository

import com.metzger100.calculator.data.local.dao.CalculationDao
import com.metzger100.calculator.data.local.entity.CalculationEntity

class CalculationRepository(private val calcdao: CalculationDao) {

    suspend fun insert(input: String, result: String) {
        // Füge den neuen Eintrag hinzu
        calcdao.insert(CalculationEntity(input = input, result = result))

        // Überprüfe, ob mehr als 25 Einträge in der Datenbank sind und lösche ggf. die ältesten
        val count = calcdao.getCount()
        if (count > 25) {
            val excessEntries = count - 25
            calcdao.deleteOldestEntries(excessEntries)
        }
    }

    suspend fun getHistory(): List<CalculationEntity> {
        return calcdao.getAll()
    }

    suspend fun clearHistory() {
        calcdao.clearAll()
    }
}