package com.metzger100.calculator.features.unit.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.metzger100.calculator.features.unit.ui.UnitConverterConstants
import com.metzger100.calculator.features.unit.ui.UnitConverterConstants.UnitDef
import com.metzger100.calculator.R

class UnitConverterViewModel(
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
        val v = input.toDoubleOrNull() ?: return ""

        val result = when (category) {
            "Temperature" -> {
                // convert any input to Celsius first
                val c = when (a.nameRes) {
                    R.string.UnitConvCatTemperature_Fahrenheit -> (v - 32) * 5/9
                    R.string.UnitConvCatTemperature_Kelvin     -> v - 273.15
                    R.string.UnitConvCatTemperature_Rankine    -> (v - 491.67) * 5/9
                    else -> v  // Celsius
                }
                // then from Celsius to target
                when (b.nameRes) {
                    R.string.UnitConvCatTemperature_Fahrenheit -> c * 9/5 + 32
                    R.string.UnitConvCatTemperature_Kelvin     -> c + 273.15
                    R.string.UnitConvCatTemperature_Rankine    -> (c + 273.15) * 9/5
                    else -> c
                }
            }

            "FuelEconomy" -> {
                // L/100km <-> MPG (US & UK)
                if (a.nameRes == R.string.UnitConvCatFuelEconomy_LitersPer100km) {
                    when (b.nameRes) {
                        R.string.UnitConvCatFuelEconomy_MPG_US -> 235.214583 / v
                        R.string.UnitConvCatFuelEconomy_MPG_UK -> 282.481053 / v
                        else -> v
                    }
                } else if (a.nameRes == R.string.UnitConvCatFuelEconomy_MPG_US) {
                    when (b.nameRes) {
                        R.string.UnitConvCatFuelEconomy_LitersPer100km -> 235.214583 / v
                        R.string.UnitConvCatFuelEconomy_MPG_UK         -> 282.481053 / (235.214583 / v)
                        else -> v
                    }
                } else if (a.nameRes == R.string.UnitConvCatFuelEconomy_MPG_UK) {
                    when (b.nameRes) {
                        R.string.UnitConvCatFuelEconomy_LitersPer100km -> 282.481053 / v
                        R.string.UnitConvCatFuelEconomy_MPG_US         -> 235.214583 / (282.481053 / v)
                        else -> v
                    }
                } else {
                    v
                }
            }

            else -> {
                v * a.factorToBase / b.factorToBase
            }
        }

        return result.toString()
    }
}