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
        val result = if (category=="Temperature") {
            val c = when(a.nameRes) {
                R.string.UnitConvCatTemperature_Fahrenheit -> (v-32)*5/9
                R.string.UnitConvCatTemperature_Kelvin     -> v-273.15
                else -> v
            }
            when(b.nameRes) {
                R.string.UnitConvCatTemperature_Fahrenheit -> c*9/5+32
                R.string.UnitConvCatTemperature_Kelvin     -> c+273.15
                else -> c
            }
        } else {
            v * a.factorToBase / b.factorToBase
        }
        return result.toString()
    }
}