package com.metzger100.calculator.features.calculator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Utility für Highlight- und Fade-Logik
 */
object KeyboardButtonStyle {
    fun isHighlighted(label: String): Boolean = when (label) {
        "C", "←", "%", "÷", "×", "−", "+", "=", "⇄" -> true
        else -> false
    }

    fun isFaded(label: String): Boolean = when (label) {
        "INV", "deg", "sin⁻¹", "cos⁻¹", "tan⁻¹", "lg",
        "10ˣ", "ln", "eˣ", "(", ")", "√", "x²", "x!(", "1/x",
        "π", "e", "sin", "cos", "tan", "^" -> true
        else -> false
    }
}

/**
 * Ein universeller Button für Standard- und Scientific-Keyboards.
 *
 * @param label       Das angezeigte Zeichen
 * @param fontSize    Schriftgröße (z.B. 24.sp in Standard, 20.sp in Scientific)
 * @param onClick     Callback für den Klick
 */
@Composable
fun KeyboardButton(
    label: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    onClick: () -> Unit
) {
    val highlighted = KeyboardButtonStyle.isHighlighted(label)
    val faded       = KeyboardButtonStyle.isFaded(label)

    val buttonColor = when {
        highlighted -> MaterialTheme.colorScheme.primary
        faded       -> Color.Gray
        else        -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (highlighted)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier.clickable { onClick() },
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
                    fontSize = fontSize,
                    color = textColor
                )
            }
        }
    }
}