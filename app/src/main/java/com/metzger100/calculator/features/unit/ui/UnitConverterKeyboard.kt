package com.metzger100.calculator.features.unit.ui

import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import splitties.systemservices.audioManager
import splitties.systemservices.vibrator

@Composable
fun UnitConverterKeyboard(
    modifier: Modifier = Modifier,
    onInput: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    val buttonSpacing = 6.dp
    val buttons = listOf(
        listOf("7", "8", "9", "C"),
        listOf("4", "5", "6", ""),
        listOf("1", "2", "3", ""),
        listOf("00", "0", ".", "←")
    )

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(buttonSpacing)
        ) {
            buttons.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                ) {
                    row.forEach { label ->
                        UnitConverterButton(
                            label = label,
                            modifier = Modifier.weight(1f),
                            onClick = {

                                // Vibrate & sound effect -----------------------------------------------------------
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(50)
                                }

                                audioManager.playSoundEffect(
                                    AudioManager.FX_KEY_CLICK,
                                    1.0f // Full volume
                                )
                                //---------------------------------------------------------------


                                when (label) {
                                    "C" -> onClear()
                                    "←" -> onBack()
                                    else -> onInput(label)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UnitConverterButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val buttonColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface

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
                else -> Text(
                    text = label,
                    fontSize = 24.sp,
                    color = textColor
                )
            }
        }
    }
}
