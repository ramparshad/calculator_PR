package com.metzger100.calculator.features.calculator.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metzger100.calculator.features.calculator.model.CalculatorMode
import com.ezylang.evalex.bigmath.BigMathExpression
import com.metzger100.calculator.R
import com.metzger100.calculator.data.repository.CalculationRepository
import com.metzger100.calculator.data.local.entity.CalculationEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val repository: CalculationRepository,
    private val application: Application
) : ViewModel() {

    var history by mutableStateOf(listOf<CalculationEntity>())
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
            "RECIPROCAL(","EXP(","PI()","E()"
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
            Log.d("CalculatorViewModel", "Starting calculation for input: $input")

            if (!validateFactorials(input)) {
                previewResult = application.getString(R.string.Calculator_Error)
                Log.d("CalculatorViewModel", "Factorial validation failed for input: $input")
                return
            }

            Log.d("CalculatorViewModel", "Valid input, evaluating expression...")

            val expression = BigMathExpression(input)
            val result = expression.evaluate().numberValue

            if (result != null) {
                val resultString = BigDecimal(result.toString())
                    .stripTrailingZeros()
                    .toPlainString()

                viewModelScope.launch {
                    repository.insert(input, resultString)
                    history = repository.getHistory()
                }

                input = resultString
                previewResult = ""
                Log.d("CalculatorViewModel", "Calculation successful, result: $resultString")
            } else {
                input = "0"
                previewResult = ""
                Log.d("CalculatorViewModel", "Result is null, input set to 0")
            }
        } catch (e: Exception) {
            previewResult = application.getString(R.string.Calculator_Error)
            Log.e("CalculatorViewModel", "Error during calculation", e)
        }
    }

    private fun updatePreviewResult() {
        if (input.isBlank()) {
            previewResult = ""
            Log.d("CalculatorViewModel", "Input is blank, preview result cleared.")
            return
        }

        Log.d("CalculatorViewModel", "Updating preview result for input: $input")

        if (!validateFactorials(input)) {
            previewResult = application.getString(R.string.Calculator_Error)
            Log.d("CalculatorViewModel", "Factorial validation failed for input: $input")
            return
        }

        try {
            val expression = BigMathExpression(input)
            previewResult = expression.evaluate().numberValue
                ?.toString()
                ?: ""
            previewResult = formatResult(previewResult)
            Log.d("CalculatorViewModel", "Preview result updated: $previewResult")
        } catch (e: Exception) {
            previewResult = application.getString(R.string.Calculator_Error)
            Log.e("CalculatorViewModel", "Error updating preview result", e)
        }
    }

    private fun validateFactorials(expression: String): Boolean {
        Log.d("CalculatorViewModel", "Validating factorials in expression: $expression")
        var expr = expression
        while (true) {
            val factIndex = expr.indexOf("FACT(")
            if (factIndex == -1) break

            var openBrackets = 1
            var closeIndex = factIndex + 5
            while (closeIndex < expr.length && openBrackets > 0) {
                when (expr[closeIndex]) {
                    '(' -> openBrackets++
                    ')' -> openBrackets--
                }
                closeIndex++
            }

            if (openBrackets != 0) {
                Log.d("CalculatorViewModel", "Mismatched brackets in expression: $expr")
                return false
            }

            val innerExpression = expr.substring(factIndex + 5, closeIndex - 1)
            Log.d("CalculatorViewModel", "Found inner expression for FACT: $innerExpression")

            if (!validateFactorials(innerExpression)) {
                Log.d("CalculatorViewModel", "Nested factorial validation failed for inner expression: $innerExpression")
                return false
            }

            try {
                val evalResult = BigMathExpression(innerExpression).evaluate().numberValue
                if (evalResult == null || evalResult < BigDecimal.ZERO || evalResult.stripTrailingZeros().scale() > 0) {
                    Log.d("CalculatorViewModel", "Invalid factorial argument: $innerExpression, result: $evalResult")
                    return false
                }
            } catch (e: Exception) {
                Log.e("CalculatorViewModel", "Error evaluating factorial argument: $innerExpression", e)
                return false
            }

            expr = expr.substring(0, factIndex) + expr.substring(closeIndex)
            Log.d("CalculatorViewModel", "Continuing validation with remaining expression: $expr")
        }
        Log.d("CalculatorViewModel", "Factorial validation passed for expression: $expression")
        return true
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

    private fun formatResult(result: String): String {
        return try {
            val bigDecimalResult = BigDecimal(result).stripTrailingZeros().toPlainString()
            bigDecimalResult.toString()
        } catch (e: NumberFormatException) {
            result
        }
    }
}