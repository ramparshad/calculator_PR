package com.metzger100.calculator.features.currency.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.metzger100.calculator.R
import com.metzger100.calculator.features.currency.viewmodel.CurrencyViewModel
import com.metzger100.calculator.features.currency.ui.Constants.MajorCurrencyCodes
import java.util.Locale

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun CurrencyConverterScreen(viewModel: CurrencyViewModel) {
    // Währungsdaten mit Code und Titel
    val currenciesWithTitles by viewModel.currenciesWithTitles
    val rates by viewModel.rates
    val lastUpdated by viewModel.lastUpdated

    // nur wichtige Währungen filtern
    val filtered by remember(currenciesWithTitles) {
        derivedStateOf {
            currenciesWithTitles
                .filter { (code, _) -> MajorCurrencyCodes.contains(code) }
                .sortedBy { MajorCurrencyCodes.indexOf(it.first) }
        }
    }

    // fallback, falls die API-Liste noch leer oder gefiltert leer ist
    val codeList = when {
        filtered.isNotEmpty() -> filtered
        currenciesWithTitles.isNotEmpty() -> currenciesWithTitles
        else -> listOf("USD" to "US Dollar", "EUR" to "Euro")
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
                    text = stringResource(id = R.string.no_exchange_data),
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
                currency = codeList.find { it.first == viewModel.currency1 }?.second ?: viewModel.currency1,
                value = viewModel.value1,
                isSelected = viewModel.selectedField == 1,
                currencies = codeList,
                onCurrencySelected = { viewModel.onCurrencyChanged1(it) },
                onClick = { viewModel.onSelectField(1) }
            )
            Spacer(Modifier.height(8.dp))

            // Feld 2
            CurrencyRow(
                currency = codeList.find { it.first == viewModel.currency2 }?.second ?: viewModel.currency2,
                value = viewModel.value2,
                isSelected = viewModel.selectedField == 2,
                currencies = codeList,
                onCurrencySelected = { viewModel.onCurrencyChanged2(it) },
                onClick = { viewModel.onSelectField(2) }
            )
        }

        lastUpdated?.let { timestamp ->
            val formattedTime = remember(timestamp) {
                java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    .format(java.util.Date(timestamp))
            }

            val hoursUntilNextUpdate = remember(timestamp) {
                val nextUpdateMillis = timestamp + 12 * 60 * 60 * 1000  // 12 hours
                val now = System.currentTimeMillis()
                val diffMillis = nextUpdateMillis - now
                (diffMillis / (1000 * 60 * 60)).coerceAtLeast(0)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = keyboardHeight + 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.exchange_rates_as_of) + " " + formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = stringResource(id = R.string.next_rates_update_in) + " " + hoursUntilNextUpdate + " " + stringResource(id = R.string.next_rates_update_in_end),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
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
                    val currentValue = if (viewModel.selectedField == 1) viewModel.value1 else viewModel.value2
                    viewModel.onValueChange(currentValue + label)
                },
                onClear = { viewModel.onValueChange("") },
                onBack = {
                    val currentValue = if (viewModel.selectedField == 1) viewModel.value1 else viewModel.value2
                    if (currentValue.isNotEmpty()) viewModel.onValueChange(currentValue.dropLast(1))
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyRow(
    currency: String,
    value: String,
    isSelected: Boolean,
    currencies: List<Pair<String, String>>,
    onCurrencySelected: (String) -> Unit,
    onClick: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val borderModifier = if (isSelected) {
        Modifier.border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.medium
        )
    } else Modifier

    val cardModifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .then(borderModifier)
        .clickable { onClick() }

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currency,
                fontSize = 18.sp,
                modifier = Modifier.clickable { showDialog = true }
            )

            Spacer(Modifier.width(16.dp))

            Text(
                text = value.ifEmpty { "0" },
                fontSize = if (isSelected) 24.sp else 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
        }
    }

    if (showDialog) {
        CurrencySelectorDialog(
            currencies = currencies,
            onCurrencySelected = { code ->
                onCurrencySelected(code)
                showDialog = false
            },
            onDismissRequest = { showDialog = false }
        )
    }
}

@Composable
private fun CurrencySelectorDialog(
    currencies: List<Pair<String, String>>,
    onCurrencySelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp,
            modifier = Modifier
                .width(screenWidth * 0.75f)
                .height(screenHeight * 0.75f)
                .padding(16.dp)
        ) {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = currencies,
                    key   = { (code, _) -> code }
                ) { (code, title) ->
                    ListItem(
                        headlineContent = { Text(title) },
                        modifier = Modifier
                            .clickable { onCurrencySelected(code) }
                            .height(48.dp)
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}