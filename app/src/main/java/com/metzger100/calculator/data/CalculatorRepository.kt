package com.metzger100.calculator.data

import com.metzger100.calculator.data.local.CalculationDao
import com.metzger100.calculator.data.local.CalculationEntity

class CalculatorRepository(private val calcdao: CalculationDao) {

    suspend fun insert(input: String, result: String) {
        calcdao.insert(CalculationEntity(input = input, result = result))
    }

    suspend fun getHistory(): List<CalculationEntity> {
        return calcdao.getAll()
    }

    suspend fun clearHistory() {
        calcdao.clearAll()
    }
}