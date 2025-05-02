package com.metzger100.calculator.features.currency.ui

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.metzger100.calculator.R
import com.metzger100.calculator.features.currency.viewmodel.CurrencyViewModel
import com.metzger100.calculator.features.currency.ui.CurrencyConverterConstants.MajorCurrencyCodes
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun CurrencyConverterScreen(viewModel: CurrencyViewModel = hiltViewModel()) {
    // Währungsdaten mit Code und Titel
    val currenciesWithTitles by viewModel.currenciesWithTitles
    val rates by viewModel.rates
    val lastApiDate by viewModel.lastApiDate

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

    val shortInput1 = runCatching {
        // wenn leer -> Default true (= kurzer Modus)
        val v = viewModel.value1.takeIf { it.isNotBlank() } ?: "0"
        BigDecimal(v).stripTrailingZeros().scale() < 3
    }.getOrDefault(true)

    val shortInput2 = runCatching {
        val v = viewModel.value2.takeIf { it.isNotBlank() } ?: "0"
        BigDecimal(v).stripTrailingZeros().scale() < 3
    }.getOrDefault(true)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                value = if (viewModel.selectedField == 1) viewModel.value1 else viewModel.formatNumber(viewModel.value1, shortInput2),
                isSelected = viewModel.selectedField == 1,
                currencies = codeList,
                onCurrencySelected = { viewModel.onCurrencyChanged1(it) },
                onClick = { viewModel.onSelectField(1) }
            )
            Spacer(Modifier.height(8.dp))

            // Feld 2
            CurrencyRow(
                currency = codeList.find { it.first == viewModel.currency2 }?.second ?: viewModel.currency2,
                value = if (viewModel.selectedField == 2) viewModel.value2 else viewModel.formatNumber(viewModel.value2, shortInput1),
                isSelected = viewModel.selectedField == 2,
                currencies = codeList,
                onCurrencySelected = { viewModel.onCurrencyChanged2(it) },
                onClick = { viewModel.onSelectField(2) }
            )
        }

        ExchangeRateInfo(
            lastApiDate = lastApiDate,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = keyboardHeight)
        )

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

@Composable
fun ExchangeRateInfo(
    lastApiDate: LocalDate?,
    modifier: Modifier = Modifier
) {
    // 1) Erzeuge einen Clock-State, der sich z.B. jede Minute aktualisiert
    val nowUtc by produceState(initialValue = Instant.now()) {
        while (true) {
            value = Instant.now()
            delay(60_000L) // 1 Minute Pause – alle 60 Sekunden neu recomposen
        }
    }

    // 2) Berechne Datum und Schwelle aus unserem live-Timestamp
    val nowUtcOffset = nowUtc.atOffset(ZoneOffset.UTC)
    val todayUtc     = nowUtcOffset.toLocalDate()
    val threshold    = todayUtc
        .atTime(2, 0)
        .atOffset(ZoneOffset.UTC)
        .toInstant()

    Column(
        modifier = modifier.padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Anzeige “as of”
        if (lastApiDate == null) {
            Text(
                text = stringResource(R.string.no_data_available),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Text(
                text = stringResource(R.string.exchange_rates_as_of) + " " +
                        lastApiDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(Modifier.height(4.dp))

        // Anzeige “update_due” oder “next update” – jetzt getriggert von unserem Clock-State
        if (lastApiDate == null || (nowUtc >= threshold && lastApiDate.isBefore(todayUtc))) {
            Text(
                text = stringResource(R.string.update_due),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        } else {
            val nextDateUtc = if (nowUtcOffset.hour < 2) todayUtc else todayUtc.plusDays(1)
            val nextUtcInst = nextDateUtc
                .atTime(2, 0)
                .atOffset(ZoneOffset.UTC)
                .toInstant()
            val nextLocal = nextUtcInst
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

            Text(
                text = stringResource(R.string.next_rates_update) + " " + nextLocal,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

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
        .then(borderModifier)
        .clickable { onClick() }

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currency,
                fontSize = 18.sp,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .clickable { showDialog = true }
            )

            Spacer(Modifier.width(16.dp))

            Text(
                text = value.ifEmpty { "0" },
                fontSize = if (isSelected) 24.sp else 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                softWrap = true,
                maxLines = Int.MAX_VALUE
            )
        }
    }

    if (showDialog) {
        CurrencySelectorDialogRV(
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
fun CurrencySelectorDialogRV(
    currencies: List<Pair<String, String>>,
    onCurrencySelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val dialogHeight = remember { mutableStateOf(0.dp) }

    Dialog(onDismissRequest = onDismissRequest) {
        BoxWithConstraints {
            val maxDialogHeight = maxHeight * 0.75f
            if (dialogHeight.value == 0.dp) {
                dialogHeight.value = maxDialogHeight
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .height(dialogHeight.value)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        RecyclerView(context).apply {
                            layoutManager = LinearLayoutManager(context)
                            adapter = object : RecyclerView.Adapter<CurrencyViewHolder>() {
                                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
                                    val linearLayout = LinearLayout(context).apply {
                                        orientation = LinearLayout.HORIZONTAL
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT
                                        )
                                    }

                                    val textView = TextView(context).apply {
                                        setPadding(32, 24, 32, 24)
                                        textSize = 16f
                                        setTextColor(textColor.toArgb())
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT
                                        )
                                    }

                                    linearLayout.addView(textView)

                                    return CurrencyViewHolder(linearLayout)
                                }

                                override fun getItemCount() = currencies.size

                                override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
                                    val (code, title) = currencies[position]
                                    val linearLayout = holder.itemView as LinearLayout
                                    val textView = linearLayout.getChildAt(0) as TextView
                                    textView.text = title

                                    holder.itemView.setOnClickListener {
                                        onCurrencySelected(code)
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

class CurrencyViewHolder(view: View) : RecyclerView.ViewHolder(view)