package com.metzger100.calculator.util.format

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NumberFormatService @Inject constructor() {

    private val smallRegex = Regex("""^(-?)0*\.((?:0){5,})(\d+)$""")
    private val lower = BigDecimal("0.001")
    private val upper = BigDecimal("1000000000")

    /**
     * vereinheitlichte Format‑Methode für alle drei Screens.
     *
     * @param input roher Eingabe‑String (Zahl oder Ausdruck)
     * @param shortMode wenn true: Currency‑Kurzmodus (immer 2 Dez), sonst Voll‑Modus
     */
    fun formatNumber(input: String, shortMode: Boolean, inputLine: Boolean): String {
        // Calculator Input Line wird nicht formatiert, nur die ResultPreview
        if (inputLine) {
            val base = input.ifEmpty { "0" }
            return replaceOperators(base)
        }

        // 1) Parsen oder Early‑Return
        val bigDec = try {
            BigDecimal(input)
        } catch (e: NumberFormatException) {
            // leeres input → "0", sonst original + Operator‑Replacement
            val base = input.ifEmpty { "0" }
            return replaceOperators(base)
        }

        // 2) Sonderfall Null
        if (bigDec.signum() == 0) return "0"

        // 3) Spezialfall viele führende Nullen nach dem Komma → Exponential‑Form
        smallRegex.matchEntire(input)?.let { m ->
            val sign      = m.groupValues[1]
            val zeroCount = m.groupValues[2].length
            val digits    = m.groupValues[3]
            val exponent  = -(zeroCount + 1)
            val mantissa  = if (digits.length > 1)
                "${digits[0]}.${digits.substring(1)}"
            else
                digits
            val sci = "$sign$mantissa×10${toSuperscript(exponent)}"
            return replaceOperators(sci)
        }

        // 4) Normale Zahl
        val formatted = if (shortMode) {
            // Currency‑Kurzmodus: immer 2 Dezimalstellen
            val rounded = bigDec.setScale(2, RoundingMode.HALF_UP)
            groupOrSci(rounded, usePlainFallback = false)
        } else {
            // Vollmodus (Calculator & Unit & Currency‑Voll): keine Rundung,
            // Sci‑Notation nur bei extremen Werten
            groupOrSci(bigDec.stripTrailingZeros(), usePlainFallback = true)
        }

        // 5) Operator‑Replacement (macht nur bei Calculator etwas)
        return replaceOperators(formatted)
    }

    /** gruppiert oder liefert wissenschaftliche Notation zurück */
    private fun groupOrSci(
        bd: BigDecimal,
        usePlainFallback: Boolean
    ): String {
        val absBd = bd.abs()
        // precision/scale/exponent auf dem normierten Wert
        val norm  = bd.stripTrailingZeros()
        val prec  = norm.precision()
        val scale = norm.scale()
        val exp   = prec - scale - 1
        val unscaled = norm.unscaledValue().abs().toString()

        val useSci = (!usePlainFallback && ((absBd < lower && exp >= 6) || (absBd > upper && scale < 0)))
                || (usePlainFallback && ((absBd < lower && exp >= 6) || (absBd > upper && scale < 0)))

        return if (useSci) buildSci(bd.signum(), unscaled, exp)
        else groupIntegerPart(bd.toPlainString())
    }

    /** baut Mantisse×10^Exp mit Superscript‑Exponent */
    private fun buildSci(signum: Int, unscaled: String, exp: Int): String {
        val mantissa = if (unscaled.length > 1)
            "${unscaled[0]}.${unscaled.substring(1)}"
        else
            unscaled
        val signChar = if (signum < 0) "-" else ""
        return "$signChar$mantissa×10${toSuperscript(exp)}"
    }

    /** gruppiert den Integer‑Teil in Dreierblöcke, erhält Dezimalteil unverändert */
    private fun groupIntegerPart(orig: String): String {
        val negative = orig.startsWith("-")
        val parts    = orig.trimStart('-').split('.', limit = 2)
        val intPart  = parts[0].ifEmpty { "0" }
        val frac     = parts.getOrNull(1)
        val grouped  = intPart.reversed().chunked(3).joinToString(" ").reversed()

        return buildString {
            if (negative) append('-')
            append(grouped)
            if (frac != null) append('.').append(frac)
        }
    }

    /** Unicode‑Hochstellung für Exponenten, z. B. -6 → ⁻⁶ */
    private fun toSuperscript(exp: Int): String {
        val sup = mapOf(
            '0' to '⁰','1' to '¹','2' to '²','3' to '³','4' to '⁴',
            '5' to '⁵','6' to '⁶','7' to '⁷','8' to '⁸','9' to '⁹','-' to '⁻'
        )
        return exp.toString().map { sup[it] ?: it }.joinToString("")
    }

    /** ersetzt Rechen‑Operatoren gemäß Calculator‑Display */
    private fun replaceOperators(expr: String): String =
        expr
            .replace("/", "÷")
            .replace("*", "×")
            .replace("-", "−")
            .replace("RECIPROCAL(", "1/(")
            .replace("EXP(", "e^(")
            .replace("PI()", "π")
            .replace("E()", "e")
            .replace("SQRT(", "√(")
            .replace("FACT(", "x!(")
            .replace("LOG10(", "lg(")
            .replace("LOG(", "ln(")
            .replace("ASINR(", "sin⁻¹(")
            .replace("ACOSR(", "cos⁻¹(")
            .replace("ATANR(", "tan⁻¹(")
            .replace("ASIN(", "sin⁻¹(")
            .replace("ACOS(", "cos⁻¹(")
            .replace("ATAN(", "tan⁻¹(")
            .replace("SINR(", "sin(")
            .replace("COSR(", "cos(")
            .replace("TANR(", "tan(")
            .replace("SIN(", "sin(")
            .replace("COS(", "cos(")
            .replace("TAN(", "tan(")
}