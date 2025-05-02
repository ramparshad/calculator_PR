package com.metzger100.calculator.features.currency.viewmodel

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metzger100.calculator.data.ConnectivityObserver
import com.metzger100.calculator.data.local.entity.CurrencyPrefsEntity
import com.metzger100.calculator.data.repository.CurrencyRepository
import com.metzger100.calculator.util.format.NumberFormatService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import javax.inject.Inject

// 1) Datenklasse für den gesamten UI‑State
data class CurrencyUiState(
    val currency1: String = "USD",
    val currency2: String = "EUR",
    val selectedField: Int = 1,
    val value1: String = "",
    val value2: String = ""
)

@HiltViewModel
@SuppressLint("DefaultLocale")
class CurrencyViewModel @Inject constructor(
    private val repo: CurrencyRepository,
    private val numberFormatService: NumberFormatService,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    companion object {
        private const val TAG = "CurrencyViewModel"
    }

    // monolithischer UI‑State
    var uiState by mutableStateOf(CurrencyUiState())
        private set

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
            val online = connectivityObserver.isOnline()
            // Lade gespeicherte Prefs
            repo.getPrefs()?.let { prefs ->
                uiState = CurrencyUiState(
                    currency1 = prefs.currency1,
                    currency2 = prefs.currency2,
                    selectedField = prefs.activeField,
                    value1 = prefs.amount1,
                    value2 = prefs.amount2
                )
                Log.d(TAG, "init: loaded prefs → $uiState")
            }
            // Currencies Flow
            repo.getCurrenciesFlow(isOnline = online)
                .onEach { list -> _currenciesWithTitles.value = list }
                .catch { e -> Log.e(TAG, "currenciesFlow error", e) }
                .launchIn(this)
            // Rates Flow
            repo.getRatesFlow(base.value, isOnline = online)
                .onEach { map ->
                    _rates.value = map
                    _lastApiDate.value = repo.getLastApiDateForBase(base.value)
                    recalc()  // atomar über uiState.copy()
                }
                .catch { e -> Log.e(TAG, "ratesFlow error", e) }
                .launchIn(this)
            Log.d(TAG, "init: END")
        }
    }

    fun formatNumber(input: String, shortMode: Boolean): String =
        numberFormatService.formatNumber(input, shortMode, inputLine = false)

    fun refreshData() {
        viewModelScope.launch {
            val online = connectivityObserver.isOnline()
            repo.getCurrenciesFlow(isOnline = online)
                .onEach { _currenciesWithTitles.value = it }
                .catch { }
                .launchIn(this)
            repo.getRatesFlow(base.value, isOnline = online)
                .onEach {
                    _rates.value = it
                    _lastApiDate.value = repo.getLastApiDateForBase(base.value)
                    recalc()
                }
                .catch { }
                .launchIn(this)
        }
    }

    private fun persistPrefs() {
        viewModelScope.launch {
            val prefs = CurrencyPrefsEntity(
                id = 1,
                activeField = uiState.selectedField,
                currency1 = uiState.currency1,
                currency2 = uiState.currency2,
                amount1 = uiState.value1,
                amount2 = uiState.value2
            )
            repo.savePrefs(prefs)
        }
    }

    fun onSelectField(field: Int) {
        uiState = uiState.copy(selectedField = field)
        persistPrefs()
    }

    fun onCurrencyChanged1(code: String) {
        uiState = uiState.copy(currency1 = code)
        recalc()
        persistPrefs()
    }

    fun onCurrencyChanged2(code: String) {
        uiState = uiState.copy(currency2 = code)
        recalc()
        persistPrefs()
    }

    fun onValueChange(newValue: String) {
        uiState = if (uiState.selectedField == 1) {
            uiState.copy(value1 = newValue)
        } else {
            uiState.copy(value2 = newValue)
        }
        recalc()
        persistPrefs()
    }

    private fun recalc() {
        val fromAmt: String
        val toAmt: String
        val fromCode: String
        val toCode: String

        if (uiState.selectedField == 1) {
            fromAmt = uiState.value1
            fromCode = uiState.currency1
            toCode = uiState.currency2
            toAmt = if (fromAmt.isBlank()) "" else convert(fromAmt, fromCode, toCode)
            uiState = uiState.copy(value2 = toAmt)
        } else {
            fromAmt = uiState.value2
            fromCode = uiState.currency2
            toCode = uiState.currency1
            toAmt = if (fromAmt.isBlank()) "" else convert(fromAmt, fromCode, toCode)
            uiState = uiState.copy(value1 = toAmt)
        }
    }

    @SuppressLint("DefaultLocale")
    private fun convert(amount: String, from: String, to: String): String {
        val normalized = amount.replace(',', '.')
        val aBD = try { BigDecimal(normalized) } catch (e: Exception) { return "0" }
        val map = rates.value
        val fromRate = map[from.lowercase()]?.let { BigDecimal.valueOf(it) } ?: return "0"
        val toRate   = map[to.lowercase()]?.let { BigDecimal.valueOf(it) }   ?: return "0"
        val mc = MathContext(16, RoundingMode.HALF_UP)
        val intermediate = try { aBD.divide(fromRate, mc) } catch (e: ArithmeticException) { return "0" }
        return intermediate.multiply(toRate, mc).stripTrailingZeros().toPlainString()
    }
}