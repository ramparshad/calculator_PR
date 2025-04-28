package com.metzger100.calculator.features.currency.viewmodel

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metzger100.calculator.data.ConnectivityObserver
import com.metzger100.calculator.data.local.entity.CurrencyPrefsEntity
import com.metzger100.calculator.data.repository.CurrencyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
@SuppressLint("DefaultLocale")
class CurrencyViewModel @Inject constructor(
    private val repo: CurrencyRepository,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    companion object {
        private const val TAG = "CurrencyViewModel"
    }

    // UI state
    var currency1 by mutableStateOf("USD"); private set
    var currency2 by mutableStateOf("EUR"); private set
    var selectedField by mutableIntStateOf(1); private set
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
            Log.d(TAG, "init: START")
            // 1) Connectivity
            val online = connectivityObserver.isOnline()
            Log.d(TAG, "init: connectivity = $online")

            // 2) Load saved prefs
            repo.getPrefs()?.let { prefs ->
                Log.d(TAG, "init: loaded prefs: $prefs")
                selectedField = prefs.activeField
                currency1     = prefs.currency1
                currency2     = prefs.currency2
                value1        = prefs.amount1
                value2        = prefs.amount2
            }

            // 3) Currencies: Cache → Network
            Log.d(TAG, "init: subscribing to currenciesFlow")
            repo.getCurrenciesFlow(isOnline = online)
                .onEach { list ->
                    Log.d(TAG, "currenciesFlow: emit ${list.size} entries")
                    _currenciesWithTitles.value = list
                }
                .catch { e ->
                    Log.e(TAG, "currenciesFlow: error", e)
                }
                .launchIn(this)

            // 4) Rates: Cache → Network
            Log.d(TAG, "init: subscribing to ratesFlow(base=${_base.value})")
            repo.getRatesFlow(base.value, isOnline = online)
                .onEach { map ->
                    Log.d(TAG, "ratesFlow: emit ${map.size} entries")
                    _rates.value = map

                    // update lastApiDate
                    val date = repo.getLastApiDateForBase(base.value)
                    _lastApiDate.value = date
                    Log.d(TAG, "ratesFlow: lastApiDate = $date")

                    // recalc
                    recalc()
                    Log.d(TAG, "ratesFlow: recalc done, value1=$value1, value2=$value2")
                }
                .catch { e ->
                    Log.e(TAG, "ratesFlow: error", e)
                }
                .launchIn(this)

            Log.d(TAG, "init: END")
        }
    }

    /** Manuelles Nachladen (forceRefresh=false liefert Cache + frische Daten). */
    fun refreshData() {
        viewModelScope.launch {
            Log.d(TAG, "refreshData: START")
            val online = connectivityObserver.isOnline()
            Log.d(TAG, "refreshData: connectivity = $online")

            repo.getCurrenciesFlow(isOnline = online)
                .onEach { list ->
                    Log.d(TAG, "refreshData → currenciesFlow: emit ${list.size} entries")
                    _currenciesWithTitles.value = list
                }
                .catch { e -> Log.e(TAG, "refreshData → currenciesFlow error", e) }
                .launchIn(this)

            repo.getRatesFlow(base.value, isOnline = online)
                .onEach { map ->
                    Log.d(TAG, "refreshData → ratesFlow: emit ${map.size} entries")
                    _rates.value = map

                    val date = repo.getLastApiDateForBase(base.value)
                    _lastApiDate.value = date
                    Log.d(TAG, "refreshData → lastApiDate = $date")

                    recalc()
                    Log.d(TAG, "refreshData → recalc done, value1=$value1, value2=$value2")
                }
                .catch { e -> Log.e(TAG, "refreshData → ratesFlow error", e) }
                .launchIn(this)

            Log.d(TAG, "refreshData: END")
        }
    }

    private fun persistPrefs() {
        viewModelScope.launch {
            val prefs = CurrencyPrefsEntity(
                id = 1,
                activeField = selectedField,
                currency1 = currency1,
                currency2 = currency2,
                amount1 = value1,
                amount2 = value2
            )
            Log.d(TAG, "persistPrefs: $prefs")
            repo.savePrefs(prefs)
        }
    }

    fun onSelectField(field: Int) {
        Log.d(TAG, "onSelectField: $field")
        selectedField = field
        persistPrefs()
    }

    fun onCurrencyChanged1(code: String) {
        Log.d(TAG, "onCurrencyChanged1: $code")
        currency1 = code
        recalc()
        persistPrefs()
    }

    fun onCurrencyChanged2(code: String) {
        Log.d(TAG, "onCurrencyChanged2: $code")
        currency2 = code
        recalc()
        persistPrefs()
    }

    fun onValueChange(newValue: String) {
        Log.d(TAG, "onValueChange: field=$selectedField, newValue='$newValue'")
        if (selectedField == 1) value1 = newValue else value2 = newValue
        recalc()
        persistPrefs()
    }

    private fun recalc() {
        if (selectedField == 1) {
            if (value1.isBlank()) {
                Log.d(TAG, "recalc: value1 blank → clearing value2")
                value2 = ""
            } else {
                value2 = convert(value1, currency1, currency2)
                Log.d(TAG, "recalc: $value1 $currency1 → $value2 $currency2")
            }
        } else {
            if (value2.isBlank()) {
                Log.d(TAG, "recalc: value2 blank → clearing value1")
                value1 = ""
            } else {
                value1 = convert(value2, currency2, currency1)
                Log.d(TAG, "recalc: $value2 $currency2 → $value1 $currency1")
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun convert(amount: String, from: String, to: String): String {
        // 1) Eingabe normalisieren: Komma → Punkt
        val normalized = amount.replace(',', '.')
        val a = normalized.toDoubleOrNull()
        if (a == null) {
            Log.w(TAG, "convert: '$amount' not a number")
            return "0.00"
        }

        // 2) Kurse holen
        val map = rates.value
        val fromRate = map[from.lowercase()]
        val toRate   = map[to.lowercase()]
        if (fromRate == null || toRate == null) {
            Log.w(TAG, "convert: rate not available (fromRate=$fromRate, toRate=$toRate)")
            return "0.00"
        }

        // 3) Umrechnen
        val result = (a / fromRate) * toRate

        // 4) Ausgabe IMMER mit Punkt: US-Locale erzwingen
        val formatted = String.format(Locale.US, "%.2f", result)
        Log.d(TAG, "convert: $a $from → $formatted $to")
        return formatted
    }
}