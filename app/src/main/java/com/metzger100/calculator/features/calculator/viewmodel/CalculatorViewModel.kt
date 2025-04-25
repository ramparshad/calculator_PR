package com.metzger100.calculator.features.calculator.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metzger100.calculator.features.calculator.model.CalculatorMode
import com.ezylang.evalex.Expression
import com.metzger100.calculator.R
import com.metzger100.calculator.data.CalculatorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val repository: CalculatorRepository,
    private val application: Application
) : ViewModel() {

    var history by mutableStateOf(listOf<Pair<String, String>>())
        private set

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            history = repository.getHistory()
        }
    }

    var forceRefresh by mutableStateOf(false)
        private set

    var mode by mutableStateOf(CalculatorMode.STANDARD)
        private set

    var input by mutableStateOf("")
        private set

    var previewResult by mutableStateOf("")
        private set

    var inverseMode by mutableStateOf(false)
        private set

    var isDegreeMode by mutableStateOf(true) // Startet im Grad-Modus
        private set

    fun toggleDegreeMode() {
        isDegreeMode = !isDegreeMode
        input = convertTrigFunctions(input)
        updatePreviewResult()
    }

    fun toggleMode() {
        mode = if (mode == CalculatorMode.STANDARD) CalculatorMode.SCIENTIFIC else CalculatorMode.STANDARD
    }

    fun toggleInverse() {
        inverseMode = !inverseMode
    }

    fun onInput(value: String) {
        input += value
        updatePreviewResult()
    }

    fun backspace() {
        val tokens = listOf(
            "ASINR(", "ACOSR(", "ATANR(",
            "ASIN(", "ACOS(", "ATAN(",
            "SINR(", "COSR(", "TANR(",
            "SIN(", "COS(", "TAN(",
            "SQRT(", "FACT(", "LOG10(", "LOG(",
            "1/(", "PI"
        )
        val match = tokens.firstOrNull { input.uppercase().endsWith(it) }
        input = if (match != null) input.dropLast(match.length) else input.dropLast(1)
        updatePreviewResult()
    }

    fun clear() {
        input = ""
        previewResult = ""
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            history = emptyList()
            forceRefresh = !forceRefresh
        }
    }

    fun calculate() {
        try {
            val finalInput = if (isDegreeMode) {
                convertToRadians(input)
            } else {
                input
            }

            val expression = Expression(finalInput)
            val result = expression.evaluate().numberValue

            if (result != null) {
                val resultString = BigDecimal(result.toString()).stripTrailingZeros().toPlainString()

                viewModelScope.launch {
                    repository.insert(input, resultString)
                    history = repository.getHistory()
                }

                input = resultString
                previewResult = ""
            } else {
                input = "0"
                previewResult = ""
            }

        } catch (e: Exception) {
            previewResult = application.getString(R.string.Calculator_Error)
        }
    }

    private fun updatePreviewResult() {
        if (input.isBlank()) {
            previewResult = ""
            return
        }

        try {
            val finalInput = if (isDegreeMode) {
                convertToRadians(input)
            } else {
                input
            }

            val expression = Expression(finalInput)
            previewResult = expression.evaluate().numberValue?.toString() ?: ""
            previewResult = formatResult(previewResult)
        } catch (e: Exception) {
            previewResult = application.getString(R.string.Calculator_Error)
        }
    }

    private fun convertTrigFunctions(expression: String): String {
        return if (isDegreeMode) {
            expression.replace("SINR\\(".toRegex(), "SIN(")
                .replace("COSR\\(".toRegex(), "COS(")
                .replace("TANR\\(".toRegex(), "TAN(")
                .replace("ASINR\\(".toRegex(), "ASIN(")
                .replace("ACOSR\\(".toRegex(), "ACOS(")
                .replace("ATANR\\(".toRegex(), "ATAN(")
        } else {
            expression.replace("SIN\\(".toRegex(), "SINR(")
                .replace("COS\\(".toRegex(), "COSR(")
                .replace("TAN\\(".toRegex(), "TANR(")
                .replace("ASIN\\(".toRegex(), "ASINR(")
                .replace("ACOS\\(".toRegex(), "ACOSR(")
                .replace("ATAN\\(".toRegex(), "ATANR(")
        }
    }

    private fun convertToRadians(expression: String): String {
        if (isDegreeMode) {
            return expression.replace(Regex("(SIN|COS|TAN|ASIN|ACOS|ATAN)\\(([^)]+)\\)")) {
                val trigValue = it.groupValues[2]
                val radianValue = "SINR(${trigValue}*PI/180)"
                it.value.replace(it.groupValues[0], radianValue)
            }
        }
        return expression
    }

    private fun formatResult(result: String): String {
        return try {
            val bigDecimalResult = BigDecimal(result).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            bigDecimalResult.toString()
        } catch (e: NumberFormatException) {
            result
        }
    }
}