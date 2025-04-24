package com.metzger100.calculator.features.calculator.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metzger100.calculator.features.calculator.model.CalculatorMode
import com.metzger100.calculator.features.calculator.viewmodel.CalculatorViewModel

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel) {
    var keyboardVisible by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val keyboardHeight = maxHeight * 0.5f

        val listState = rememberLazyListState()

        LaunchedEffect(viewModel.history) {
            listState.scrollToItem(0)
        }

        LaunchedEffect(keyboardVisible, viewModel.input) {
            if (keyboardVisible || viewModel.input.isNotEmpty()) {
                listState.scrollToItem(0)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // History (nimmt den verfügbaren Platz ein)
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true,
                    contentPadding = PaddingValues(bottom = 4.dp),
                ) {
                    items(viewModel.history.reversed()) { (input, result) ->
                        Column {
                            Text(
                                input.split(" ").joinToString(" ") { mapToDisplaySymbols(it) },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 20.sp
                                )
                            )
                            Text(
                                "= $result",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 20.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Eingabezeile (dynamische Höhe)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { keyboardVisible = true },
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    if (viewModel.previewResult.isNotEmpty()) {
                        Text(
                            text = mapToDisplaySymbols(viewModel.input),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "= ${viewModel.previewResult}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = mapToDisplaySymbols(viewModel.input),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Spacer to make sure input field doesn't overlap with history or keyboard
            Spacer(modifier = Modifier.height(8.dp))

            // Tastatur (wird nur sichtbar, wenn benötigt)
            if (keyboardVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(keyboardHeight)
                ) {
                    if (viewModel.mode == CalculatorMode.STANDARD) {
                        StandardKeyboard(
                            onInput = viewModel::onInput,
                            onClear = viewModel::clear,
                            onBack = viewModel::backspace,
                            onEquals = {
                                viewModel.calculate()
                            },
                            onToggleMode = viewModel::toggleMode
                        )
                    } else {
                        ScientificKeyboard(
                            inverse = viewModel.inverseMode,
                            onInput = viewModel::onInput,
                            onClear = viewModel::clear,
                            onBack = viewModel::backspace,
                            onEquals = {
                                viewModel.calculate()
                            },
                            onToggleMode = viewModel::toggleMode,
                            onToggleInverse = viewModel::toggleInverse,
                            onToggleDegreeMode = viewModel::toggleDegreeMode,
                            isDegreeMode = viewModel.isDegreeMode
                        )
                    }
                }
            }
        }
    }
}

fun mapToDisplaySymbols(expression: String): String {
    return expression
        .replace("ASIN\\(".toRegex(), "sin⁻¹(")
        .replace("ACOS\\(".toRegex(), "cos⁻¹(")
        .replace("ATAN\\(".toRegex(), "tan⁻¹(")
        .replace("SIN\\(".toRegex(), "sin(")
        .replace("SINR\\(".toRegex(), "sin(")
        .replace("COS\\(".toRegex(), "cos(")
        .replace("COSR\\(".toRegex(), "cos(")
        .replace("TAN\\(".toRegex(), "tan(")
        .replace("TANR\\(".toRegex(), "tan(")
        .replace("SQRT\\(".toRegex(), "√(")
        .replace("LOG10\\(".toRegex(), "lg(")
        .replace("LOG\\(".toRegex(), "ln(")
        .replace("PI", "π")
}