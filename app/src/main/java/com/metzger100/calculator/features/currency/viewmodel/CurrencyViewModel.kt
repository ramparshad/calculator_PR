package com.metzger100.calculator.features.currency.viewmodel

import android.annotation.SuppressLint
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metzger100.calculator.data.CurrencyRepository
import com.metzger100.calculator.data.local.CurrencyPrefsEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val repo: CurrencyRepository
) : ViewModel() {

    // expose UI state
    var currency1 by mutableStateOf("USD"); private set
    var currency2 by mutableStateOf("EUR"); private set
    var selectedField by mutableStateOf(1); private set
    var value1 by mutableStateOf(""); private set
    var value2 by mutableStateOf(""); private set

    private val _rates = mutableStateOf<Map<String, Double>>(emptyMap())
    val rates: State<Map<String, Double>> = _rates

    private val _currenciesWithTitles = mutableStateOf<List<Pair<String, String>>>(emptyList())
    val currenciesWithTitles: State<List<Pair<String, String>>> = _currenciesWithTitles

    private val _base = mutableStateOf("USD")
    val base: State<String> = _base

    private val _lastApiDate = mutableStateOf<LocalDate?>(null)
    val lastApiDate: State<LocalDate?> = _lastApiDate

    init {
        viewModelScope.launch {
            // load persisted prefs if any
            repo.getPrefs()?.let { p ->
                selectedField = p.activeField
                currency1     = p.currency1
                currency2     = p.currency2
                value1        = p.amount1
                value2        = p.amount2
            }
            // Lade Währungen und deren Titel
            _currenciesWithTitles.value = repo.getCurrencies()
            // Lade Kurse für die Standard-Base
            _rates.value = repo.getRates(_base.value)
            _lastApiDate.value = repo.getLastApiDateForBase(_base.value)
        }
    }

    private fun persistPrefs() {
        viewModelScope.launch {
            repo.savePrefs(
                CurrencyPrefsEntity(
                    id = 1,
                    activeField = selectedField,
                    currency1 = currency1,
                    currency2 = currency2,
                    amount1 = value1,
                    amount2 = value2
                )
            )
        }
    }

    fun onSelectField(field: Int) {
        selectedField = field
        persistPrefs()
    }

    fun onCurrencyChanged1(code: String) {
        currency1 = code
        recalc()
        persistPrefs()
    }
    fun onCurrencyChanged2(code: String) {
        currency2 = code
        recalc()
        persistPrefs()
    }
    fun onValueChange(newValue: String) {
        if (selectedField == 1) {
            value1 = newValue
        } else {
            value2 = newValue
        }
        recalc()
        persistPrefs()
    }

    private fun recalc() {
        if (selectedField == 1) {
            value2 = convert(value1, currency1, currency2)
        } else {
            value1 = convert(value2, currency2, currency1)
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _currenciesWithTitles.value = repo.getCurrencies()
            _rates.value = repo.getRates(_base.value)
            _lastApiDate.value = repo.getLastApiDateForBase(_base.value)
        }
    }

    fun forceRefreshData(newBase: String) {
        _base.value = newBase
        viewModelScope.launch {
            _currenciesWithTitles.value = repo.getCurrencies(forceRefresh = true)
            _rates.value = repo.getRates(newBase, forceRefresh = true)
            _lastApiDate.value = repo.getLastApiDateForBase(newBase)
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