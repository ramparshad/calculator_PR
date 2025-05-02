package com.metzger100.calculator.features.calculator.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ezylang.evalex.bigmath.BigMathExpression
import com.ezylang.evalex.config.ExpressionConfiguration
import com.metzger100.calculator.R
import com.metzger100.calculator.data.local.entity.CalculationEntity
import com.metzger100.calculator.data.repository.CalculationRepository
import com.metzger100.calculator.features.calculator.model.CalculatorMode
import com.metzger100.calculator.util.format.NumberFormatService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import javax.inject.Inject

data class CalculatorUiState(
    val input: String = "",
    val preview: String = "",
    val mode: CalculatorMode = CalculatorMode.STANDARD,
    val isDegree: Boolean = true,
    val inverse: Boolean = false
)

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val repository: CalculationRepository,
    private val numberFormatService: NumberFormatService,
    private val application: Application
) : ViewModel() {

    // History bleibt separat
    var history by mutableStateOf(listOf<CalculationEntity>())
        private set

    // Einzige UI-State–Variable
    var uiState by mutableStateOf(CalculatorUiState())
        private set

    // Nur für forcierte List-Refresh nach clearHistory
    var forceRefresh by mutableStateOf(false)
        private set

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            history = repository.getHistory()
        }
    }

    fun formatNumber(
        input: String,
        shortMode: Boolean,
        inputLine: Boolean
    ): String = numberFormatService.formatNumber(input, shortMode, inputLine)

    fun toggleDegreeMode() {
        val newIsDeg = !uiState.isDegree
        // Trig-Funktionen im Input umschreiben
        val converted = convertTrigFunctions(uiState.input, newIsDeg)
        uiState = uiState.copy(
            isDegree = newIsDeg,
            input = converted,
            preview = updatePreviewResults(converted)
        )
    }

    fun toggleMode() {
        val newMode = if (uiState.mode == CalculatorMode.STANDARD)
            CalculatorMode.SCIENTIFIC
        else
            CalculatorMode.STANDARD

        uiState = uiState.copy(mode = newMode)
    }

    fun toggleInverse() {
        uiState = uiState.copy(inverse = !uiState.inverse)
    }

    // Eingabe-Handler
    fun onInput(token: String) {
        val newInput = uiState.input + token
        uiState = uiState.copy(
            input = newInput,
            preview = updatePreviewResults(newInput)
        )
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
        val match = tokens.firstOrNull { uiState.input.uppercase().endsWith(it) }
        val newInput = if (match != null)
            uiState.input.dropLast(match.length)
        else
            uiState.input.dropLast(1)
        uiState = uiState.copy(
            input = newInput,
            preview = updatePreviewResults(newInput)
        )
    }

    fun clear() {
        uiState = uiState.copy(input = "", preview = "")
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
            Log.d("CalculatorViewModel", "Calculating: ${uiState.input}")

            if (!validateFactorials(uiState.input)) {
                uiState = uiState.copy(preview = application.getString(R.string.Calculator_Error))
                return
            }

            val config = ExpressionConfiguration.builder()
                .mathContext(MathContext(18, RoundingMode.HALF_UP))
                .decimalPlacesResult(16)
                .build()
            val expr = BigMathExpression(uiState.input, config)
            val result = expr.evaluate().numberValue

            if (result != null) {
                val resultStr = BigDecimal(result.toString())
                    .stripTrailingZeros()
                    .toPlainString()

                viewModelScope.launch {
                    repository.insert(uiState.input, resultStr)
                    history = repository.getHistory()
                }

                uiState = uiState.copy(input = resultStr, preview = "")
                Log.d("CalculatorViewModel", "Result: $resultStr")
            } else {
                uiState = uiState.copy(input = "0", preview = "")
            }
        } catch (e: Exception) {
            uiState = uiState.copy(preview = application.getString(R.string.Calculator_Error))
            Log.e("CalculatorViewModel", "Error during calculate", e)
        }
    }

    // --- Hilfsfunktionen ---

    private fun updatePreviewResults(input: String): String {
        if (input.isBlank()) return ""
        if (!validateFactorials(input)) {
            return application.getString(R.string.Calculator_Error)
        }
        return try {
            val config = ExpressionConfiguration.builder()
                .mathContext(MathContext(18, RoundingMode.HALF_UP))
                .decimalPlacesResult(16)
                .build()
            val expr = BigMathExpression(input, config)
            expr.evaluate().numberValue
                ?.toString()
                ?.let { formatResult(it) }
                ?: ""
        } catch (e: Exception) {
            application.getString(R.string.Calculator_Error)
        }
    }

    private fun validateFactorials(expression: String): Boolean {
        var expr = expression
        while (true) {
            val idx = expr.indexOf("FACT(")
            if (idx == -1) break
            // finde schließende Klammer…
            var open = 1; var pos = idx + 5
            while (pos < expr.length && open > 0) {
                when (expr[pos]) {
                    '(' -> open++
                    ')' -> open--
                }
                pos++
            }
            if (open != 0) return false
            val inner = expr.substring(idx + 5, pos - 1)
            if (!validateFactorials(inner)) return false
            try {
                val cfg = ExpressionConfiguration.builder()
                    .mathContext(MathContext(18, RoundingMode.HALF_UP))
                    .decimalPlacesResult(16)
                    .build()
                val valInner = BigMathExpression(inner, cfg).evaluate().numberValue
                if (valInner == null ||
                    valInner < BigDecimal.ZERO ||
                    valInner.stripTrailingZeros().scale() > 0
                ) return false
            } catch (_: Exception) {
                return false
            }
            expr = expr.removeRange(idx, pos)
        }
        return true
    }

    private fun convertTrigFunctions(expr: String, toDegreeMode: Boolean): String {
        return if (toDegreeMode) {
            expr.replace("SINR\\(".toRegex(), "SIN(")
                .replace("COSR\\(".toRegex(), "COS(")
                .replace("TANR\\(".toRegex(), "TAN(")
                .replace("ASINR\\(".toRegex(), "ASIN(")
                .replace("ACOSR\\(".toRegex(), "ACOS(")
                .replace("ATANR\\(".toRegex(), "ATAN(")
        } else {
            expr.replace("SIN\\(".toRegex(), "SINR(")
                .replace("COS\\(".toRegex(), "COSR(")
                .replace("TAN\\(".toRegex(), "TANR(")
                .replace("ASIN\\(".toRegex(), "ASINR(")
                .replace("ACOS\\(".toRegex(), "ACOSR(")
                .replace("ATAN\\(".toRegex(), "ATANR(")
        }
    }

    private fun formatResult(raw: String): String =
        try {
            BigDecimal(raw).stripTrailingZeros().toPlainString()
        } catch (_: NumberFormatException) {
            raw
        }

}