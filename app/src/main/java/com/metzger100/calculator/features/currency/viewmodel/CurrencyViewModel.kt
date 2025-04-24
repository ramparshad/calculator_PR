package com.metzger100.calculator.features.currency.viewmodel

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metzger100.calculator.data.CurrencyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val repo: CurrencyRepository
) : ViewModel() {

    private val _rates = mutableStateOf<Map<String, Double>>(emptyMap())
    val rates: State<Map<String, Double>> = _rates

    private val _currenciesWithTitles = mutableStateOf<List<Pair<String, String>>>(emptyList())
    val currenciesWithTitles: State<List<Pair<String, String>>> = _currenciesWithTitles

    private val _base = mutableStateOf("USD")
    val base: State<String> = _base

    private val _lastUpdated = mutableStateOf<Long?>(null)
    val lastUpdated: State<Long?> = _lastUpdated

    init {
        viewModelScope.launch {
            // Lade Währungen und deren Titel
            _currenciesWithTitles.value = repo.getAvailableCurrenciesWithTitles()
            // Lade Kurse für die Standard-Base
            _rates.value = repo.getRates(_base.value)
            _lastUpdated.value = repo.getLastTimestampForBase(_base.value)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadBase(newBase: String) {
        _base.value = newBase
        viewModelScope.launch {
            _currenciesWithTitles.value = repo.getAvailableCurrenciesWithTitles()
            _rates.value = repo.getRates(newBase)
            _lastUpdated.value = repo.getLastTimestampForBase(newBase)
        }
    }

    /** Konvertiert [amount] von [from] nach [to], benutzt den geladenen Kurs. */
    @SuppressLint("DefaultLocale")
    fun convert(amount: String, from: String, to: String): String {
        val a = amount.toDoubleOrNull() ?: return "0.00"
        // Wir haben eine Map:  rates["bat"] = 8.068…  bedeutet 1 EUR = 8.068 BAT
        val map = rates.value
        val fromRate = map[from.lowercase()] ?: return "0.00"   // wie viel 1 EUR in „from“ wert ist
        val toRate   = map[to.lowercase()]   ?: return "0.00"   // wie viel 1 EUR in „to“ wert ist
        val result = (a / fromRate) * toRate

        return String.format(Locale.US, "%.2f", result)
    }
}