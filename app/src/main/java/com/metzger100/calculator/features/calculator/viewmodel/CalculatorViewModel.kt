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

    var history by mutableStateOf(listOf<CalculationEntity>())
        private set

    var uiState by mutableStateOf(CalculatorUiState())
        private set

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

            if (!validateExpression(uiState.input)) {
                uiState = uiState.copy(preview = application.getString(R.string.Calculator_Error))
                return
            }

            val config = buildConfig()
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

    private fun updatePreviewResults(input: String): String {
        if (input.isBlank()) return ""
        if (!validateExpression(input)) {
            return application.getString(R.string.Calculator_Error)
        }
        return try {
            val config = buildConfig()
            val expr = BigMathExpression(input, config)
            expr.evaluate().numberValue
                ?.toString()
                ?.let { formatResult(it) }
                ?: ""
        } catch (e: Exception) {
            application.getString(R.string.Calculator_Error)
        }
    }

    /** Baut die EvalEx‑Konfiguration mit gewünschter Genauigkeit und Standard‑Funktionen. */
    private fun buildConfig(): ExpressionConfiguration {
        return ExpressionConfiguration.builder()
            .decimalPlacesResult(16)
            .mathContext(MathContext(18, RoundingMode.HALF_UP))
            .build()
    }

    /** Prüft, ob der gesamte Ausdruck in allen reellen Domains gültig ist. */
    private fun validateExpression(input: String): Boolean {
        val tag = "CalculatorViewModel"
        Log.d(tag, "validateExpression: checking input=\"$input\"")

        val config = buildConfig()

        if (input.trim() == "0^0") {
            Log.w(tag, "Invalid: 0^0 is undefined")
            return false
        }

        if (!validateFactorials(input, config)) {
            Log.w(tag, "Invalid: factorial validation failed")
            return false
        }

        if (!validateTanDomain(input, config, uiState.isDegree)) {
            Log.w(tag, "Invalid: tangent domain validation failed")
            return false
        }

        if (!validateUnaryDomain(input, "SQRT", BigDecimal.ZERO, allowEqual = true, config)) {
            Log.w(tag, "Invalid: square root domain validation failed")
            return false
        }
        if (!validateUnaryDomain(input, "LOG", BigDecimal.ZERO, allowEqual = false, config)) {
            Log.w(tag, "Invalid: LOG argument must be greater than 0")
            return false
        }
        if (!validateUnaryDomain(input, "LOG10", BigDecimal.ZERO, allowEqual = false, config)) {
            Log.w(tag, "Invalid: LOG10 argument must be greater than 0")
            return false
        }

        Log.d(tag, "validateExpression: input is valid")
        return true
    }

    /** Extrahiert alle inneren Ausdrücke von FUNKTION(...) inklusive verschachtelter Klammern. */
    private fun extractArgs(expr: String, functionName: String): List<String> {
        val args = mutableListOf<String>()
        var idx = 0
        while (true) {
            val start = expr.indexOf("$functionName(", idx).takeIf { it >= 0 } ?: break
            var depth = 1
            var pos = start + functionName.length + 1
            while (pos < expr.length && depth > 0) {
                when (expr[pos]) {
                    '(' -> depth++
                    ')' -> depth--
                }
                pos++
            }
            if (depth == 0) {
                args += expr.substring(start + functionName.length + 1, pos - 1)
                idx = pos
            } else {
                break
            }
        }
        return args
    }

    /** Prüft unäre Funktionen (SQRT, LOG, LOG10) auf ihren Definitionsbereich. */
    private fun validateUnaryDomain(
        input: String,
        functionName: String,
        bound: BigDecimal,
        allowEqual: Boolean,
        config: ExpressionConfiguration
    ): Boolean {
        for (inner in extractArgs(input, functionName)) {
            val v = try {
                BigMathExpression(inner, config).evaluate().numberValue ?: return false
            } catch (_: Exception) {
                return false
            }
            if (allowEqual) {
                if (v < bound) return false
            } else {
                if (v <= bound) return false
            }
        }
        return true
    }

    /** Prüft TAN‑Polstellen über cos(inner) != 0 mit adaptivem Epsilon (ULP). */
    private fun validateTanDomain(
        input: String,
        config: ExpressionConfiguration,
        isDegree: Boolean
    ): Boolean {
        val fn = if (isDegree) "TAN" else "TANR"
        val cosFn = if (isDegree) "COS" else "COSR"
        for (inner in extractArgs(input, fn)) {
            val cosVal = try {
                BigMathExpression("$cosFn($inner)", config).evaluate().numberValue ?: return false
            } catch (_: Exception) {
                return false
            }
            val eps = cosVal.ulp().multiply(BigDecimal(2))
            if (cosVal.abs() <= eps) return false
        }
        return true
    }

    private fun validateFactorials(expression: String, config: ExpressionConfiguration): Boolean {
        var expr = expression
        while (true) {
            val idx = expr.indexOf("FACT(")
            if (idx == -1) break
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
            if (!validateFactorials(inner, config)) return false
            try {
                val valInner = BigMathExpression(inner, config).evaluate().numberValue
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