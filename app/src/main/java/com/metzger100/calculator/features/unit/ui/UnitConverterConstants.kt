package com.metzger100.calculator.features.unit.ui

import androidx.annotation.StringRes
import com.metzger100.calculator.R

/**
 * Definiert pro Kategorie eine Liste von Einheiten mit Umrechnungsfaktor
 * relativ zur Basiseinheit.
 */
object UnitConverterConstants {

    /** Einheit mit Namen und Faktor zur Basiseinheit */
    data class UnitDef(
        @StringRes val nameRes: Int,
        val factorToBase: Double
    )

    /** Map: Kategorie → Liste aller UnitDef in dieser Kategorie */
    val units: Map<String, List<UnitDef>> = mapOf(
        "Length" to listOf(
            UnitDef(R.string.UnitConvCatLength_Meter,1.0),
            UnitDef(R.string.UnitConvCatLength_Kilometer,1_000.0),
            UnitDef(R.string.UnitConvCatLength_Centimeter,0.01),
            UnitDef(R.string.UnitConvCatLength_Millimeter,0.001),
            UnitDef(R.string.UnitConvCatLength_Mile,1_609.344),
            UnitDef(R.string.UnitConvCatLength_Yard,0.9144),
            UnitDef(R.string.UnitConvCatLength_Foot,0.3048),
            UnitDef(R.string.UnitConvCatLength_Inch,0.0254)
        ),
        "Weight" to listOf(
            UnitDef(R.string.UnitConvCatWeight_Kilogram,1.0),
            UnitDef(R.string.UnitConvCatWeight_Gram,0.001),
            UnitDef(R.string.UnitConvCatWeight_Milligram,0.000001),
            UnitDef(R.string.UnitConvCatWeight_Pound,0.45359237),
            UnitDef(R.string.UnitConvCatWeight_Ounce,0.0283495231)
        ),
        "Volume" to listOf(
            UnitDef(R.string.UnitConvCatVolume_Liter,1.0),
            UnitDef(R.string.UnitConvCatVolume_Milliliter,0.001),
            UnitDef(R.string.UnitConvCatVolume_CubicMeter,1000.0),
            UnitDef(R.string.UnitConvCatVolume_GallonUS,3.78541),
            UnitDef(R.string.UnitConvCatVolume_PintUS,0.473176)
        ),
        "Area" to listOf(
            UnitDef(R.string.UnitConvCatArea_SquareMeter,1.0),
            UnitDef(R.string.UnitConvCatArea_SquareKilometer,1_000_000.0),
            UnitDef(R.string.UnitConvCatArea_SquareCentimeter,0.0001),
            UnitDef(R.string.UnitConvCatArea_Hectare,10_000.0),
            UnitDef(R.string.UnitConvCatArea_Acre,4_046.8564224)
        ),
        "Temperature" to listOf(
            UnitDef(R.string.UnitConvCatTemperature_Celsius,1.0),
            UnitDef(R.string.UnitConvCatTemperature_Fahrenheit,1.0),
            UnitDef(R.string.UnitConvCatTemperature_Kelvin,1.0)
        ),
        "Time" to listOf(
            UnitDef(R.string.UnitConvCatTime_Second,1.0),
            UnitDef(R.string.UnitConvCatTime_Minute,60.0),
            UnitDef(R.string.UnitConvCatTime_Hour,3600.0),
            UnitDef(R.string.UnitConvCatTime_Day,86400.0)
        ),
        "Speed" to listOf(
            UnitDef(R.string.UnitConvCatSpeed_MeterPerSecond,1.0),
            UnitDef(R.string.UnitConvCatSpeed_KilometerPerHour,0.277778),
            UnitDef(R.string.UnitConvCatSpeed_MilePerHour,0.44704)
        ),
        "Energy" to listOf(
            UnitDef(R.string.UnitConvCatEnergy_Joule,1.0),
            UnitDef(R.string.UnitConvCatEnergy_Kilojoule,1000.0),
            UnitDef(R.string.UnitConvCatEnergy_Calorie,4.1868),
            UnitDef(R.string.UnitConvCatEnergy_Kilocalorie,4184.0)
        ),
        "Power" to listOf(
            UnitDef(R.string.UnitConvCatPower_Watt,1.0),
            UnitDef(R.string.UnitConvCatPower_Kilowatt,1000.0),
            UnitDef(R.string.UnitConvCatPower_Horsepower,745.699872)
        ),
        "Pressure" to listOf(
            UnitDef(R.string.UnitConvCatPressure_Pascal,1.0),
            UnitDef(R.string.UnitConvCatPressure_Bar,100000.0),
            UnitDef(R.string.UnitConvCatPressure_PSI,6894.75729)
        ),
        "Frequency" to listOf(
            UnitDef(R.string.UnitConvCatFrequency_Hertz,1.0),
            UnitDef(R.string.UnitConvCatFrequency_Kilohertz,1000.0),
            UnitDef(R.string.UnitConvCatFrequency_Megahertz,1_000_000.0)
        ),
        "Data" to listOf(
            UnitDef(R.string.UnitConvCatData_Bit,1.0),
            UnitDef(R.string.UnitConvCatData_Byte,8.0),
            UnitDef(R.string.UnitConvCatData_Kilobyte,8192.0),
            UnitDef(R.string.UnitConvCatData_Megabyte,8_388_608.0)
        ),
        "FuelEconomy" to listOf(
            UnitDef(R.string.UnitConvCatFuelEconomy_LitersPer100km,0.01),
            UnitDef(R.string.UnitConvCatFuelEconomy_MPG_US,235.214583)
        ),
        "PlaneAngle" to listOf(
            UnitDef(R.string.UnitConvCatPlaneAngle_Degree,1.0),
            UnitDef(R.string.UnitConvCatPlaneAngle_Radian,0.0174533)
        )
    )
}
