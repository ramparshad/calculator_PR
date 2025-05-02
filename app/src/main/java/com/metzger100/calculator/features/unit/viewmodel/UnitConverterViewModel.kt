package com.metzger100.calculator.features.unit.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.metzger100.calculator.features.unit.ui.UnitConverterConstants
import com.metzger100.calculator.features.unit.ui.UnitConverterConstants.UnitDef
import com.metzger100.calculator.R
import com.metzger100.calculator.util.format.NumberFormatService
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

@HiltViewModel
class UnitConverterViewModel @Inject constructor(
    private val numberFormatService: NumberFormatService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val category: String = savedStateHandle.get<String>("category")
        ?: UnitConverterConstants.units.keys.first()

    /** alle UnitDef dieser Kategorie */
    val availableUnits: List<UnitDef> =
        UnitConverterConstants.units[category] ?: emptyList()

    // State
    var selectedField by mutableIntStateOf(1)
    var fromUnit by mutableStateOf(availableUnits[0])
    var toUnit   by mutableStateOf(availableUnits[1])
    var fromValue by mutableStateOf("")
    var toValue   by mutableStateOf("")

    fun formatNumber(input: String, shortMode: Boolean): String =
        numberFormatService.formatNumber(input, shortMode, inputLine = false)

    fun onValueChange(input: String) {
        if (selectedField == 1) {
            fromValue = input
            toValue = convert(input, fromUnit, toUnit)
        } else {
            toValue = input
            fromValue = convert(input, toUnit, fromUnit)
        }
    }

    fun onFromUnitChanged(u: UnitDef) {
        fromUnit = u
        if (selectedField == 1) {
            toValue = convert(fromValue, fromUnit, toUnit)
        } else {
            fromValue = convert(toValue, toUnit, fromUnit)
        }
    }

    fun onToUnitChanged(u: UnitDef) {
        toUnit = u
        if (selectedField == 1) {
            toValue = convert(fromValue, fromUnit, toUnit)
        } else {
            fromValue = convert(toValue, toUnit, fromUnit)
        }
    }

    fun onSelectField(f: Int) { selectedField = f }

    private fun convert(input: String, a: UnitDef, b: UnitDef): String {
        // Input in BigDecimal parsen
        val v = input.toBigDecimalOrNull() ?: return ""

        // Rechenkontext: 16 signifikante Stellen, Half-Up-Rundung
        val mc = MathContext(16, RoundingMode.HALF_UP)

        val result: BigDecimal = when (category) {
            "Temperature" -> {
                // erst alles in Celsius
                val c = when (a.nameRes) {
                    R.string.UnitConvCatTemperature_Fahrenheit ->
                        // (v - 32) * 5/9
                        v.subtract(BigDecimal(32), mc)
                            .multiply(BigDecimal(5), mc)
                            .divide(BigDecimal(9), mc)

                    R.string.UnitConvCatTemperature_Kelvin ->
                        // v - 273.15
                        v.subtract(BigDecimal("273.15"), mc)

                    R.string.UnitConvCatTemperature_Rankine ->
                        // (v - 491.67) * 5/9
                        v.subtract(BigDecimal("491.67"), mc)
                            .multiply(BigDecimal(5), mc)
                            .divide(BigDecimal(9), mc)

                    else ->
                        v // Celsius
                }

                // dann von Celsius in Ziel
                when (b.nameRes) {
                    R.string.UnitConvCatTemperature_Fahrenheit ->
                        c.multiply(BigDecimal(9), mc)
                            .divide(BigDecimal(5), mc)
                            .add(BigDecimal(32), mc)

                    R.string.UnitConvCatTemperature_Kelvin ->
                        c.add(BigDecimal("273.15"), mc)

                    R.string.UnitConvCatTemperature_Rankine ->
                        c.add(BigDecimal("273.15"), mc)
                            .multiply(BigDecimal(9), mc)
                            .divide(BigDecimal(5), mc)

                    else ->
                        c
                }
            }

            "FuelEconomy" -> {
                // Constants als exakte BigDecimal
                val mpgUS  = BigDecimal("235.214583")
                val mpgUK  = BigDecimal("282.481053")

                when (a.nameRes) {
                    R.string.UnitConvCatFuelEconomy_LitersPer100km -> when (b.nameRes) {
                        R.string.UnitConvCatFuelEconomy_MPG_US -> mpgUS.divide(v, mc)
                        R.string.UnitConvCatFuelEconomy_MPG_UK -> mpgUK.divide(v, mc)
                        else -> v
                    }

                    R.string.UnitConvCatFuelEconomy_MPG_US -> when (b.nameRes) {
                        R.string.UnitConvCatFuelEconomy_LitersPer100km ->
                            mpgUS.divide(v, mc)
                        R.string.UnitConvCatFuelEconomy_MPG_UK ->
                            mpgUK.divide(mpgUS.divide(v, mc), mc)
                        else -> v
                    }

                    R.string.UnitConvCatFuelEconomy_MPG_UK -> when (b.nameRes) {
                        R.string.UnitConvCatFuelEconomy_LitersPer100km ->
                            mpgUK.divide(v, mc)
                        R.string.UnitConvCatFuelEconomy_MPG_US ->
                            mpgUS.divide(mpgUK.divide(v, mc), mc)
                        else -> v
                    }

                    else -> v
                }
            }

            else -> {
                // universelle Umrechnung: v * a.factorToBase / b.factorToBase
                val factorA = BigDecimal.valueOf(a.factorToBase)
                val factorB = BigDecimal.valueOf(b.factorToBase)
                v.multiply(factorA, mc)
                    .divide(factorB, mc)
            }
        }

        // Ergebnis als String, ohne unnötige Nullen, in Plain-Notation
        return result.stripTrailingZeros().toPlainString()
    }

    // Extension, um String → BigDecimal? zu parsen
    private fun String.toBigDecimalOrNull(): BigDecimal? =
        try { BigDecimal(this) } catch (_: Exception) { null }
}