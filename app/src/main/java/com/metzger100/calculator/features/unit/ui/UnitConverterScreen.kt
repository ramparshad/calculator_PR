package com.metzger100.calculator.features.unit.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.metzger100.calculator.features.unit.viewmodel.UnitConverterViewModel
import com.metzger100.calculator.features.unit.ui.UnitConverterConstants.UnitDef
import java.math.BigDecimal

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun UnitConverterScreen(
    viewModel: UnitConverterViewModel
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val keyboardHeight = maxHeight * 0.5f

        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = keyboardHeight)
        ) {
            UnitRow(
                labelRes = viewModel.fromUnit.nameRes,
                value    = viewModel.fromValue,
                isSel    = viewModel.selectedField==1,
                units    = viewModel.availableUnits,
                onUnitSelected = viewModel::onFromUnitChanged,
                onClick        = { viewModel.onSelectField(1) }
            )
            Spacer(Modifier.height(8.dp))
            UnitRow(
                labelRes = viewModel.toUnit.nameRes,
                value    = viewModel.toValue,
                isSel    = viewModel.selectedField==2,
                units    = viewModel.availableUnits,
                onUnitSelected = viewModel::onToUnitChanged,
                onClick        = { viewModel.onSelectField(2) }
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(keyboardHeight)
                .align(Alignment.BottomCenter)
        ) {
            UnitConverterKeyboard(
                onInput = { label ->
                    val current = if (viewModel.selectedField == 1) viewModel.fromValue else viewModel.toValue
                    viewModel.onValueChange(current + label)
                },
                onClear = { viewModel.onValueChange("") },
                onBack = {
                    val current = if (viewModel.selectedField == 1) viewModel.fromValue else viewModel.toValue
                    if (current.isNotEmpty()) viewModel.onValueChange(current.dropLast(1))
                }
            )
        }
    }
}

@Composable
fun UnitRow(
    @StringRes labelRes: Int,
    value: String,
    isSel: Boolean,
    units: List<UnitDef>,
    onUnitSelected: (UnitDef)->Unit,
    onClick: ()->Unit
) {
    var show by remember { mutableStateOf(false) }
    val borderM = if (isSel) Modifier.border(
        2.dp,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.shapes.medium
    ) else Modifier

    Card(
        Modifier
            .fillMaxWidth()
            .then(borderM)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(labelRes),
                fontSize = 18.sp,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .clickable { show = true })
            Spacer(Modifier.width(16.dp))

            val displayText = if (isSel) {
                value.ifEmpty { "0" }
            } else {
                formatForDisplay(value)
            }

            Text(
                text = displayText,
                fontSize = if (isSel) 24.sp else 20.sp,
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                softWrap = true,
                maxLines = Int.MAX_VALUE
            )
        }
    }

    if (show) {
        UnitSelectorDialogRV(
            units = units,
            onUnitSelected = {
                onUnitSelected(it)
                show = false
            },
            onDismissRequest = { show = false }
        )
    }
}

@Composable
fun UnitSelectorDialogRV(
    units: List<UnitDef>,
    onUnitSelected: (UnitDef) -> Unit,
    onDismissRequest: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Dialog(onDismissRequest = onDismissRequest) {
        BoxWithConstraints {
            val maxDialogHeight = maxHeight * 0.75f

            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = maxDialogHeight)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        RecyclerView(context).apply {
                            layoutManager = LinearLayoutManager(context)
                            adapter = object : RecyclerView.Adapter<UnitViewHolder>() {
                                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnitViewHolder {
                                    val textView = TextView(context).apply {
                                        setPadding(32, 24, 32, 24)
                                        textSize = 18f
                                        setTextColor(textColor)
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT
                                        )
                                    }
                                    return UnitViewHolder(textView)
                                }

                                override fun getItemCount() = units.size

                                override fun onBindViewHolder(holder: UnitViewHolder, position: Int) {
                                    val unit = units[position]
                                    holder.textView.text = holder.textView.context.getString(unit.nameRes)
                                    holder.itemView.setOnClickListener {
                                        onUnitSelected(unit)
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

private class UnitViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

private fun formatForDisplay(input: String): String {
    val bigdDec = try {
        BigDecimal(input)
    } catch (e: NumberFormatException) {
        return input.ifEmpty { "0" }
    }

    // Sonderfall Null: signum()==0
    if (bigdDec.signum() == 0) return "0"
    val smallRegex = Regex("""^(-?)0*\.((?:0){5,})(\d+)$""")
    smallRegex.matchEntire(input)?.let { m ->
        val sign      = m.groupValues[1]
        val zeroCount = m.groupValues[2].length
        val digits    = m.groupValues[3]
        val exponent  = -(zeroCount + 1)
        val mantissa  = if (digits.length > 1)
            "${digits[0]}.${digits.substring(1)}"
        else
            digits
        return "$sign$mantissa×10${toSuperscript(exponent)}"
    }

    val bd = try {
        BigDecimal(input)
    } catch (e: Exception) {
        return input
    }

    if (bd.signum() == 0) return "0"

    val absBd      = bd.abs()
    val normalized = bd.stripTrailingZeros()
    val scale      = normalized.scale()
    val exponent   = -scale
    val unscaled   = normalized.unscaledValue().abs().toString()

    val lower = BigDecimal("0.001")
    val upper = BigDecimal("1000000000")

    val useSci = (absBd < lower && exponent >= 6)
            || (absBd > upper && scale < 0)

    val core = if (useSci) {
        val mantissa = if (unscaled.length > 1)
            "${unscaled[0]}.${unscaled.substring(1)}"
        else
            unscaled
        val sign = if (bd.signum() < 0) "-" else ""
        "$sign$mantissa×10${toSuperscript(exponent)}"
    } else {
        groupIntegerPart(input)
    }

    return core
}

/** gruppiert nur den Integer-Teil mit ' ' und hängt alle originalen Dezimalstellen an */
private fun groupIntegerPart(orig: String): String {
    val negative = orig.startsWith("-")
    val parts   = orig.trimStart('-').split(".", limit = 2)
    val intPart = parts[0].ifEmpty { "0" }
    val frac    = parts.getOrNull(1)

    val groupedInt = intPart
        .reversed()
        .chunked(3)
        .joinToString(" ")
        .reversed()

    return buildString {
        if (negative) append('-')
        append(groupedInt)
        if (frac != null) {
            append('.').append(frac)
        }
    }
}

/** Unicode-Hochstellung für den Exponenten, z.B. -6 → ⁻⁶ */
private fun toSuperscript(exp: Int): String {
    val sup = mapOf(
        '0' to '⁰','1' to '¹','2' to '²','3' to '³','4' to '⁴',
        '5' to '⁵','6' to '⁶','7' to '⁷','8' to '⁸','9' to '⁹','-' to '⁻'
    )
    return exp.toString().map { sup[it] ?: it }.joinToString("")
}