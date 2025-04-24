package com.metzger100.calculator.features.currency.ui

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metzger100.calculator.features.currency.viewmodel.CurrencyViewModel
import com.metzger100.calculator.features.currency.ui.Constants.MajorCurrencyCodes
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun CurrencyConverterScreen(viewModel: CurrencyViewModel) {
    // Währungsdaten mit Code und Titel
    val currenciesWithTitles by viewModel.currenciesWithTitles
    val rates by viewModel.rates
    val lastUpdated by viewModel.lastUpdated

    // nur wichtige Währungen filtern
    val filtered = currenciesWithTitles
        .filter { (code, _) -> MajorCurrencyCodes.contains(code) }
        .sortedBy { MajorCurrencyCodes.indexOf(it.first) }

    // fallback, falls die API-Liste noch leer oder gefiltert leer ist
    val codeList = when {
        filtered.isNotEmpty() -> filtered
        currenciesWithTitles.isNotEmpty() -> currenciesWithTitles
        else -> listOf("USD" to "US Dollar", "EUR" to "Euro")
    }

    // Initiale Auswahl
    var currency1 by remember { mutableStateOf("USD") }
    var currency2 by remember { mutableStateOf("EUR") }
    var selectedField by remember { mutableStateOf(1) }

    // Eingabewerte
    var value1 by remember { mutableStateOf("") }
    var value2 by remember { mutableStateOf("") }

    // Umrechnungsfunktion
    fun recalc() {
        if (selectedField == 1) {
            value2 = viewModel.convert(value1, currency1, currency2)
        } else {
            value1 = viewModel.convert(value2, currency2, currency1)
        }
    }

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
            // Fehlermeldung, falls offline ohne Daten
            if (rates.isEmpty()) {
                Text(
                    text = "No exchange rate data available.\nPlease go online or restart the app.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                        .padding(12.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            // Feld 1
            CurrencyRow(
                currency = codeList.find { it.first == currency1 }?.second ?: currency1,
                value = value1,
                isSelected = selectedField == 1,
                currencies = codeList,
                onCurrencySelected = {
                    currency1 = it
                    recalc()
                },
                onClick = { selectedField = 1 }
            )
            Spacer(Modifier.height(8.dp))

            // Feld 2
            CurrencyRow(
                currency = codeList.find { it.first == currency2 }?.second ?: currency2,
                value = value2,
                isSelected = selectedField == 2,
                currencies = codeList,
                onCurrencySelected = {
                    currency2 = it
                    recalc()
                },
                onClick = { selectedField = 2 }
            )
        }

        lastUpdated?.let { timestamp ->
            val formattedTime = remember(timestamp) {
                java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    .format(java.util.Date(timestamp))
            }
            Text(
                text = "Exchange rates as of $formattedTime",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = keyboardHeight + 8.dp)
            )
        }

        // Tastatur
        Box(
            Modifier
                .fillMaxWidth()
                .height(keyboardHeight)
                .align(Alignment.BottomCenter)
        ) {
            CurrencyConverterKeyboard(
                onInput = { label ->
                    if (selectedField == 1) value1 += label else value2 += label
                    recalc()
                },
                onClear = {
                    if (selectedField == 1) {
                        value1 = ""; value2 = ""
                    } else {
                        value2 = ""; value1 = ""
                    }
                },
                onBack = {
                    if (selectedField == 1 && value1.isNotEmpty()) {
                        value1 = value1.dropLast(1); recalc()
                    } else if (selectedField == 2 && value2.isNotEmpty()) {
                        value2 = value2.dropLast(1); recalc()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyRow(
    currency: String,  // Titel
    value: String,
    isSelected: Boolean,
    currencies: List<Pair<String, String>>,  // List mit Paaren (Code, Titel)
    onCurrencySelected: (String) -> Unit,
    onClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val background = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(background)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Text(
                    text = currency,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clickable { menuExpanded = true }
                        .padding(end = 8.dp)
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    // Fixed size container with LazyColumn for better performance
                    Box(modifier = Modifier.size(width = 150.dp, height = 300.dp)) {
                        val listState = rememberLazyListState()
                        LazyColumn(state = listState) {
                            items(currencies.size) { index ->
                                val (code, title) = currencies[index]
                                DropdownMenuItem(
                                    text = { Text(text = title) },
                                    onClick = {
                                        onCurrencySelected(code)
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Text(
                text = value.ifEmpty { "0" },
                fontSize = if (isSelected) 24.sp else 20.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
        }
    }
}