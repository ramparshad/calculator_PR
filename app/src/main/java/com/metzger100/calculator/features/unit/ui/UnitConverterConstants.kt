package com.metzger100.calculator.features.unit.ui

import androidx.annotation.StringRes
import com.metzger100.calculator.R

/**
 * Defines per category a list of units with conversion factor relative to the base unit.
 * Verified factors against authoritative sources and extended with additional units.
 */
object UnitConverterConstants {

    /** Unit definition with name resource and factor to base unit */
    data class UnitDef(
        @StringRes val nameRes: Int,
        val factorToBase: Double
    )

    /** Map: Category → List of all UnitDef in that category */
    val units: Map<String, List<UnitDef>> = mapOf(
        // LENGTH (grouped by metric, imperial, astronomical) (base = meter)
        "Length" to listOf(
            // Metric & non-SI small
            UnitDef(R.string.UnitConvCatLength_Angstrom, 1e-10),
            UnitDef(R.string.UnitConvCatLength_Nanometer, 1e-9),
            UnitDef(R.string.UnitConvCatLength_Micrometer, 1e-6),
            UnitDef(R.string.UnitConvCatLength_Millimeter, 0.001),
            UnitDef(R.string.UnitConvCatLength_Centimeter, 0.01),
            UnitDef(R.string.UnitConvCatLength_Meter, 1.0),
            UnitDef(R.string.UnitConvCatLength_Kilometer, 1_000.0),
            // Imperial
            UnitDef(R.string.UnitConvCatLength_Inch, 0.0254),
            UnitDef(R.string.UnitConvCatLength_Foot, 0.3048),
            UnitDef(R.string.UnitConvCatLength_Yard, 0.9144),
            UnitDef(R.string.UnitConvCatLength_Mile, 1_609.344),
            UnitDef(R.string.UnitConvCatLength_NauticalMile, 1_852.0),
            // Astronomical
            UnitDef(R.string.UnitConvCatLength_AstronomicalUnit, 1.495978707e11),
            UnitDef(R.string.UnitConvCatLength_LightYear, 9.4607304725808e15),
            UnitDef(R.string.UnitConvCatLength_Parsec, 3.085677581491367e16)
        ),

        // WEIGHT/MASS (metric then non-SI then imperial) (base = kilogram)
        "Weight" to listOf(
            // Metric
            UnitDef(R.string.UnitConvCatWeight_Milligram, 0.000001),
            UnitDef(R.string.UnitConvCatWeight_Gram, 0.001),
            UnitDef(R.string.UnitConvCatWeight_Carat, 0.0002),
            UnitDef(R.string.UnitConvCatWeight_Kilogram, 1.0),
            UnitDef(R.string.UnitConvCatWeight_Tonne, 1_000.0),
            // Imperial & non-SI
            UnitDef(R.string.UnitConvCatWeight_Ounce, 0.028349523125),
            UnitDef(R.string.UnitConvCatWeight_Pound, 0.45359237),
            UnitDef(R.string.UnitConvCatWeight_Stone, 6.35029318),
            UnitDef(R.string.UnitConvCatWeight_ShortHundredweight, 45.359237),
            UnitDef(R.string.UnitConvCatWeight_LongHundredweight, 50.80234544),
            UnitDef(R.string.UnitConvCatWeight_US_Ton, 907.18474),
            UnitDef(R.string.UnitConvCatWeight_LongTon, 1016.0469088)
        ),

        // VOLUME (metric then US then UK) (base = liter)
        "Volume" to listOf(
            UnitDef(R.string.UnitConvCatVolume_Milliliter, 0.001),
            UnitDef(R.string.UnitConvCatVolume_Liter, 1.0),
            UnitDef(R.string.UnitConvCatVolume_CubicMeter, 1_000.0),
            // US
            UnitDef(R.string.UnitConvCatVolume_TeaspoonUS, 0.00492892159375),
            UnitDef(R.string.UnitConvCatVolume_TablespoonUS, 0.01478676478125),
            UnitDef(R.string.UnitConvCatVolume_FluidOunceUS, 0.0295735295625),
            UnitDef(R.string.UnitConvCatVolume_CupUS, 0.2365882365),
            UnitDef(R.string.UnitConvCatVolume_PintUS, 0.4731764730),
            UnitDef(R.string.UnitConvCatVolume_QuartUS, 0.946352946),
            UnitDef(R.string.UnitConvCatVolume_GallonUS, 3.785411784),
            // UK
            UnitDef(R.string.UnitConvCatVolume_TeaspoonUK, 0.005919388020833333),
            UnitDef(R.string.UnitConvCatVolume_TablespoonUK, 0.0177581640625),
            UnitDef(R.string.UnitConvCatVolume_FluidOunceUK, 0.0284130625),
            UnitDef(R.string.UnitConvCatVolume_PintUK, 0.56826125),
            UnitDef(R.string.UnitConvCatVolume_QuartUK, 1.1365225000),
            UnitDef(R.string.UnitConvCatVolume_GallonUK, 4.54609)
        ),


        // AREA (metric then imperial) (base = square meter)
        "Area" to listOf(
            UnitDef(R.string.UnitConvCatArea_SquareMillimeter, 1e-6),
            UnitDef(R.string.UnitConvCatArea_SquareCentimeter, 0.0001),
            UnitDef(R.string.UnitConvCatArea_SquareMeter, 1.0),
            UnitDef(R.string.UnitConvCatArea_Hectare, 10_000.0),
            UnitDef(R.string.UnitConvCatArea_SquareKilometer, 1_000_000.0),
            // Imperial
            UnitDef(R.string.UnitConvCatArea_SquareInch, 0.00064516),
            UnitDef(R.string.UnitConvCatArea_SquareFoot, 0.09290304),
            UnitDef(R.string.UnitConvCatArea_SquareYard, 0.83612736),
            UnitDef(R.string.UnitConvCatArea_Acre, 4_046.8564224),
            UnitDef(R.string.UnitConvCatArea_SquareMile, 2.589988110336e6)
        ),

        // TEMPERATURE (Celsius/Kelvin then Fahrenheit/Rankine) (no base -> calculation in UnitConverterViewModel.convert())
        "Temperature" to listOf(
            UnitDef(R.string.UnitConvCatTemperature_Celsius, 1.0),
            UnitDef(R.string.UnitConvCatTemperature_Kelvin, 1.0),
            UnitDef(R.string.UnitConvCatTemperature_Fahrenheit, 1.0),
            UnitDef(R.string.UnitConvCatTemperature_Rankine, 1.0)
        ),

        // TIME (ascending) (base = second)
        "Time" to listOf(
            UnitDef(R.string.UnitConvCatTime_Nanosecond, 1e-9),
            UnitDef(R.string.UnitConvCatTime_Microsecond, 1e-6),
            UnitDef(R.string.UnitConvCatTime_Millisecond, 0.001),
            UnitDef(R.string.UnitConvCatTime_Second, 1.0),
            UnitDef(R.string.UnitConvCatTime_Minute, 60.0),
            UnitDef(R.string.UnitConvCatTime_Hour, 3_600.0),
            UnitDef(R.string.UnitConvCatTime_Day, 86_400.0),
            UnitDef(R.string.UnitConvCatTime_Week, 604_800.0),
            UnitDef(R.string.UnitConvCatTime_Month, 2_629_746.0),
            UnitDef(R.string.UnitConvCatTime_Year, 31_556_952.0)
        ),

        // SPEED (metric then imperial then other) (base = meter per second)
        "Speed" to listOf(
            UnitDef(R.string.UnitConvCatSpeed_MeterPerSecond, 1.0),
            UnitDef(R.string.UnitConvCatSpeed_KilometerPerSecond, 1000.0),
            UnitDef(R.string.UnitConvCatSpeed_KilometerPerHour, 0.2777777777777778),
            UnitDef(R.string.UnitConvCatSpeed_FootPerSecond, 0.3048),
            UnitDef(R.string.UnitConvCatSpeed_MilePerHour, 0.44704),
            UnitDef(R.string.UnitConvCatSpeed_Knot, 0.51444444444444),
            UnitDef(R.string.UnitConvCatSpeed_Mach, 340.29)
        ),

        // ENERGY (SI then others) (base = joule)
        "Energy" to listOf(
            UnitDef(R.string.UnitConvCatEnergy_Erg, 1e-7),
            UnitDef(R.string.UnitConvCatEnergy_Joule, 1.0),
            UnitDef(R.string.UnitConvCatEnergy_Kilojoule, 1_000.0),
            UnitDef(R.string.UnitConvCatEnergy_Electronvolt, 1.602176634e-19),
            UnitDef(R.string.UnitConvCatEnergy_Calorie, 4.184),
            UnitDef(R.string.UnitConvCatEnergy_Kilocalorie, 4_184.0),
            UnitDef(R.string.UnitConvCatEnergy_WattHour, 3_600.0),
            UnitDef(R.string.UnitConvCatEnergy_KilowattHour, 3_600_000.0),
            UnitDef(R.string.UnitConvCatEnergy_FootPound, 1.355817948331400),
            UnitDef(R.string.UnitConvCatEnergy_BTU, 1055.05585262)
        ),

        // POWER (base = watt)
        "Power" to listOf(
            UnitDef(R.string.UnitConvCatPower_Watt, 1.0),
            UnitDef(R.string.UnitConvCatPower_Kilowatt, 1_000.0),
            UnitDef(R.string.UnitConvCatPower_Megawatt, 1_000_000.0),
            UnitDef(R.string.UnitConvCatPower_Horsepower, 745.6998715822702),
            UnitDef(R.string.UnitConvCatPower_MetricHorsepower, 735.49875)
        ),

        // PRESSURE (SI then non-SI) (base = pascal)
        "Pressure" to listOf(
            UnitDef(R.string.UnitConvCatPressure_Pascal, 1.0),
            UnitDef(R.string.UnitConvCatPressure_Kilopascal, 1000.0),
            UnitDef(R.string.UnitConvCatPressure_Millibar, 100.0),
            UnitDef(R.string.UnitConvCatPressure_Bar, 100_000.0),
            UnitDef(R.string.UnitConvCatPressure_Atmosphere, 101_325.0),
            UnitDef(R.string.UnitConvCatPressure_Torr, 133.32236842105263),
            UnitDef(R.string.UnitConvCatPressure_MmHg, 133.322),
            UnitDef(R.string.UnitConvCatPressure_KgfPerCm2, 98_066.5),
            UnitDef(R.string.UnitConvCatPressure_PSI, 6894.7572931783)
        ),

        // FREQUENCY (base = hertz)
        "Frequency" to listOf(
            UnitDef(R.string.UnitConvCatFrequency_Hertz, 1.0),
            UnitDef(R.string.UnitConvCatFrequency_Kilohertz, 1_000.0),
            UnitDef(R.string.UnitConvCatFrequency_Megahertz, 1_000_000.0),
            UnitDef(R.string.UnitConvCatFrequency_Gigahertz, 1_000_000_000.0)
        ),

        // DATA (decimal bits, decimal bytes, binary) (base = bit)
        "Data" to listOf(
            // Bits (SI)
            UnitDef(R.string.UnitConvCatData_Bit, 1.0),
            UnitDef(R.string.UnitConvCatData_Kilobit, 1_000.0),
            UnitDef(R.string.UnitConvCatData_Megabit, 1_000_000.0),
            UnitDef(R.string.UnitConvCatData_Gigabit, 1_000_000_000.0),
            UnitDef(R.string.UnitConvCatData_Terabit, 1_000_000_000_000.0),
            UnitDef(R.string.UnitConvCatData_Petabit, 1_000_000_000_000_000.0),
            UnitDef(R.string.UnitConvCatData_Exabit, 1_000_000_000_000_000_000.0),

            // Bytes (SI)
            UnitDef(R.string.UnitConvCatData_Byte, 8.0),
            UnitDef(R.string.UnitConvCatData_Kilobyte, 8_000.0),
            UnitDef(R.string.UnitConvCatData_Megabyte, 8_000_000.0),
            UnitDef(R.string.UnitConvCatData_Gigabyte, 8_000_000_000.0),
            UnitDef(R.string.UnitConvCatData_Terabyte, 8_000_000_000_000.0),
            UnitDef(R.string.UnitConvCatData_Petabyte, 8_000_000_000_000_000.0),
            UnitDef(R.string.UnitConvCatData_Exabyte, 8_000_000_000_000_000_000.0),

            // Binary (IEC)
            UnitDef(R.string.UnitConvCatData_Kibibyte, 8_192.0),
            UnitDef(R.string.UnitConvCatData_Mebibyte, 8_388_608.0),
            UnitDef(R.string.UnitConvCatData_Gibibyte, 8_589_934_592.0),
            UnitDef(R.string.UnitConvCatData_Tebibyte, 8_796_093_022_208.0),
            UnitDef(R.string.UnitConvCatData_Pebibyte, 9_007_199_254_740_992.0),
            UnitDef(R.string.UnitConvCatData_Exbibyte, 9_223_372_036_854_775_808.0)
        ),

        // FUEL ECONOMY (special-case conversion L/100km ↔ mpg) (no base -> calculation in UnitConverterViewModel.convert())
        "FuelEconomy" to listOf(
            UnitDef(R.string.UnitConvCatFuelEconomy_LitersPer100km, 1.0),
            UnitDef(R.string.UnitConvCatFuelEconomy_MPG_US, 235.214583),
            UnitDef(R.string.UnitConvCatFuelEconomy_MPG_UK, 282.481053)
        ),

        // PLANE ANGLE (base = radian)
        "PlaneAngle" to listOf(
            UnitDef(R.string.UnitConvCatPlaneAngle_Radian, 1.0),
            UnitDef(R.string.UnitConvCatPlaneAngle_Degree, 0.017453292519943295),
            UnitDef(R.string.UnitConvCatPlaneAngle_Grad, 0.01570796326794896),
            UnitDef(R.string.UnitConvCatPlaneAngle_Arcminute, 0.0002908882086657216),
            UnitDef(R.string.UnitConvCatPlaneAngle_Arcsecond, 4.84813681109536e-6)
        ),

        // AMOUNT OF SUBSTANCE (metric prefixes) (base = mole)
        "Amount" to listOf(
            UnitDef(R.string.UnitConvCatAmount_Picomole, 1e-12),
            UnitDef(R.string.UnitConvCatAmount_Nanomole, 1e-9),
            UnitDef(R.string.UnitConvCatAmount_Micromole, 1e-6),
            UnitDef(R.string.UnitConvCatAmount_Millimole, 1e-3),
            UnitDef(R.string.UnitConvCatAmount_Mole, 1.0),
            UnitDef(R.string.UnitConvCatAmount_Kilomole, 1e3)
        )
    )
}