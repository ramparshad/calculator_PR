package com.metzger100.calculator.features.calculator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StandardKeyboard(
    modifier: Modifier = Modifier,
    onInput: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onEquals: () -> Unit,
    onToggleMode: () -> Unit
) {
    val buttonSpacing = 6.dp
    val buttons = listOf(
        listOf("C", "←", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"),
        listOf("1", "2", "3", "+"),
        listOf("⇄", "0", ",", "=")
    )

    // Extrahierter Modifier für das gesamte Layout
    val columnModifier = Modifier
        .fillMaxSize()
        .padding(top = 4.dp)

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = columnModifier,
            verticalArrangement = Arrangement.spacedBy(buttonSpacing)
        ) {
            buttons.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // Jede Zeile gleich hoch
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                ) {
                    row.forEach { label ->
                        // Reuse des gemeinsamen Modifiers
                        val buttonModifier = Modifier
                            .weight(1f)

                        CalculatorButton(
                            label = label,
                            modifier = buttonModifier,
                            onClick = {
                                when (label) {
                                    "C" -> onClear()
                                    "←" -> onBack()
                                    "=" -> onEquals()
                                    "⇄" -> onToggleMode()
                                    else -> onInput(mapSymbol(label))
                                }
                            },
                            isHighlighted = isHighlightedButton(label)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isHighlighted: Boolean
) {
    val buttonColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isHighlighted) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    // Extrahierter Modifier für die Button-Oberfläche
    val surfaceModifier = modifier
        .clickable { onClick() }

    Surface(
        modifier = surfaceModifier,
        color = buttonColor,
        tonalElevation = 4.dp,
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            when (label) {
                "←" -> Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace")
                "⇄" -> Icon(Icons.Default.SwapHoriz, contentDescription = "Mode Switch")
                else -> Text(
                    text = label,
                    fontSize = 24.sp,
                    color = textColor
                )
            }
        }
    }
}

fun isHighlightedButton(label: String): Boolean {
    return when (label) {
        "C", "←", "%", "÷", "×", "−", "+", "=", "⇄" -> true
        else -> false
    }
}

fun isFadedButton(label: String): Boolean {
    return when (label) {
        "INV", "deg", "sin⁻¹", "cos⁻¹", "tan⁻¹", "lg", "ln", "(", ")", "√", "x!", "1/x", "π", "e", "sin", "cos", "tan", "^" -> true
        else -> false
    }
}

fun mapSymbol(symbol: String): String {
    return when (symbol) {
        "÷" -> "/"
        "×" -> "*"
        "−" -> "-"
        "," -> "."
        else -> symbol
    }
}