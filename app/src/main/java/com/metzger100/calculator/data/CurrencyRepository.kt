package com.metzger100.calculator.data

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.metzger100.calculator.data.local.CurrencyListDao
import com.metzger100.calculator.data.local.CurrencyListEntity
import com.metzger100.calculator.data.local.CurrencyPrefsDao
import com.metzger100.calculator.data.local.CurrencyPrefsEntity
import com.metzger100.calculator.data.local.CurrencyRateDao
import com.metzger100.calculator.data.local.CurrencyRateEntity
import com.metzger100.calculator.di.IoDispatcher
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyRepository @Inject constructor(
    private val rateDao: CurrencyRateDao,
    private val listDao: CurrencyListDao,
    private val prefsDao: CurrencyPrefsDao,
    private val httpClient: HttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher  // z.B. Dispatchers.IO
) {
    private val gson = Gson()

    private val TAG = "CurrencyRepository"  // Tag für das Loggen

    /** Gibt eine Map von Währungscode → Kurs zurück (Basis = [base]). */
    suspend fun getRates(base: String, forceRefresh: Boolean = false): Map<String, Double> = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val cutoff = now - 12 * 3_600_000L  // vor 12 Stunden

        Log.d(TAG, "getRates: Checking cache for base currency: $base")

        val cached = rateDao.get(base)
        val rawJson = try {
            if (forceRefresh || cached == null || cached.timestamp <= cutoff) {
                Log.d(TAG, "getRates: Cache expired, not available or force refresh triggered, fetching fresh rates for $base")
                val fresh = fetchRatesJson(base)
                rateDao.upsert(CurrencyRateEntity(base = base, json = fresh, timestamp = now))
                fresh
            } else {
                Log.d(TAG, "getRates: Using cached data for $base. Cached timestamp: ${cached.timestamp}, current time: $now")
                Log.d(TAG, "getRates: Cached data age: ${(now - cached.timestamp) / 1000} seconds")
                cached.json
            }
        } catch (_: Exception) {
            Log.e(TAG, "getRates: Error fetching rates for $base, using cache if available.")
            // Kein Netz → wenn Cache da, nimm ihn, sonst "{}"
            cached?.json ?: "{}"
        }

        // Parsen: "{}" oder fehlerhaftes JSON → leere Map
        val root = try {
            gson.fromJson(rawJson, JsonObject::class.java)
        } catch (_: Exception) {
            Log.e(TAG, "getRates: Error parsing JSON data for $base.")
            null
        }

        if (root == null || !root.has(base.lowercase())) {
            Log.d(TAG, "getRates: No valid data found for $base.")
            emptyMap()
        } else {
            val ratesObj = root.getAsJsonObject(base.lowercase())
            val ratesMap = ratesObj.entrySet().associate { it.key to it.value.asDouble }
            Log.d(TAG, "getRates: Successfully fetched rates for $base: $ratesMap")
            ratesMap
        }
    }

    suspend fun getLastTimestampForBase(base: String): Long? {
        return rateDao.get(base)?.timestamp
    }

    private suspend fun fetchRatesJson(base: String): String {
        val todayUtc = LocalDate.now(ZoneOffset.UTC)
        val tries = listOf(todayUtc.plusDays(1), todayUtc)
        val lower = base.lowercase()

        // Phase 1: jsDelivr für morgen und heute
        for (date in tries) {
            val versionTag = "${date.year}.${date.monthValue}.${date.dayOfMonth}"
            val jsDelivrUrl = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@$versionTag/v1/currencies/$lower.json"

            try {
                val body = httpClient.get(jsDelivrUrl).bodyAsText()
                if (!body.contains("Couldn't find the requested release version")) {
                    Log.d(TAG, "fetchRatesJson: got $versionTag from jsDelivr: $jsDelivrUrl")
                    return body
                }
                Log.d(TAG, "fetchRatesJson: jsDelivr $versionTag not ready yet")
            } catch (_: Exception) {
                Log.d(TAG, "fetchRatesJson: jsDelivr fetch error for $versionTag")
            }
        }

        // Phase 2: @latest
        runCatching {
            val latestUrl = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/$lower.json"
            Log.d(TAG, "fetchRatesJson: falling back to @latest")
            return httpClient.get(latestUrl).bodyAsText()
        }.onFailure {
            Log.d(TAG, "fetchRatesJson: @latest fetch failed, will try pages.dev")
        }

        // Phase 3: pages.dev für morgen und heute
        for (date in tries) {
            val pagesDevUrl = "https://${date}.currency-api.pages.dev/v1/currencies/$lower.json"
            try {
                val body = httpClient.get(pagesDevUrl).bodyAsText()
                if (!body.contains("<h1")) {
                    Log.d(TAG, "fetchRatesJson: got $date from pages.dev: $pagesDevUrl")
                    return body
                }
                Log.d(TAG, "fetchRatesJson: pages.dev $date not ready yet")
            } catch (_: Exception) {
                Log.d(TAG, "fetchRatesJson: pages.dev fetch error for $date")
            }
        }

        // letzter Ausweg: leeres JSON
        Log.d(TAG, "fetchRatesJson: all strategies failed, returning empty JSON")
        return "{}"
    }

    suspend fun getAvailableCurrenciesWithTitles(forceRefresh: Boolean = false): List<Pair<String, String>> =
        withContext(ioDispatcher) {
            val now = System.currentTimeMillis()
            val cutoff = now - 12 * 3_600_000L  // vor 12 Stunden

            Log.d(TAG, "getAvailableCurrenciesWithTitles: Checking cache for currencies list.")

            val cached = listDao.get()
            val rawJson = if (!forceRefresh && cached != null && cached.timestamp > cutoff) {
                Log.d(TAG, "getAvailableCurrenciesWithTitles: Using cached currency list. Cached timestamp: ${cached.timestamp}, current time: $now")
                Log.d(TAG, "getAvailableCurrenciesWithTitles: Cached list age: ${(now - cached.timestamp) / 1000} seconds")
                cached.json
            } else {
                // Versuche Primary & Fallback, aber im Fehlerfall einfach "{}" nehmen
                Log.d(TAG, "getAvailableCurrenciesWithTitles: Cache expired, not available or force refresh triggered, fetching fresh currency list.")
                val freshJson = try {
                    fetchCurrenciesJsonWithNetworkFallback()
                } catch (_: Exception) {
                    Log.e(TAG, "getAvailableCurrenciesWithTitles: Error fetching currency list.")
                    "{}"
                }
                // Falls wir wirklich was geholt haben, upserten:
                if (freshJson != "{}") {
                    listDao.upsert(CurrencyListEntity(json = freshJson, timestamp = now))
                }
                freshJson
            }

            // Parsen – "{}" ergibt einfach eine leere Map
            val root = gson.fromJson(rawJson, JsonObject::class.java)
            val currencies = root.entrySet()
                .map { it.key.uppercase() to it.value.asString }
                .sortedBy { it.first }

            Log.d(TAG, "getAvailableCurrenciesWithTitles: Successfully fetched currencies: $currencies")
            currencies
        }

    /** Holt currencies.min.json zuerst vom CDN, bei Fehlern vom Pages-dev-Fallback. */
    private suspend fun fetchCurrenciesJsonWithNetworkFallback(): String {
        val todayUtc = LocalDate.now(ZoneOffset.UTC)
        val tries = listOf(todayUtc.plusDays(1), todayUtc)

        // Phase 1: jsDelivr
        for (date in tries) {
            val versionTag = "${date.year}.${date.monthValue}.${date.dayOfMonth}"
            val jsDelivrUrl = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@$versionTag/v1/currencies.min.json"

            try {
                val body = httpClient.get(jsDelivrUrl).bodyAsText()
                if (!body.contains("Couldn't find the requested release version")) {
                    Log.d(TAG, "fetchCurrenciesJson: got $versionTag from jsDelivr: $jsDelivrUrl")
                    return body
                }
                Log.d(TAG, "fetchCurrenciesJson: jsDelivr $versionTag not ready yet")
            } catch (_: Exception) {
                Log.d(TAG, "fetchCurrenciesJson: jsDelivr fetch error for $versionTag")
            }
        }

        // Phase 2: jsDelivr @latest
        runCatching {
            val latestUrl = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies.min.json"
            Log.d(TAG, "fetchCurrenciesJson: falling back to @latest")
            return httpClient.get(latestUrl).bodyAsText()
        }.onFailure {
            Log.d(TAG, "fetchCurrenciesJson: @latest fetch failed, will try pages.dev")
        }

        // Phase 3: pages.dev
        for (date in tries) {
            val pagesDevUrl = "https://${date}.currency-api.pages.dev/v1/currencies.min.json"
            try {
                val body = httpClient.get(pagesDevUrl).bodyAsText()
                if (!body.contains("<h1")) {
                    Log.d(TAG, "fetchCurrenciesJson: got $date from pages.dev: $pagesDevUrl")
                    return body
                }
                Log.d(TAG, "fetchCurrenciesJson: pages.dev $date not ready yet")
            } catch (_: Exception) {
                Log.d(TAG, "fetchCurrenciesJson: pages.dev fetch error for $date")
            }
        }

        Log.d(TAG, "fetchCurrenciesJson: all strategies failed, returning empty JSON")
        return "{}"
    }

    suspend fun getPrefs(): CurrencyPrefsEntity? = prefsDao.get()
    suspend fun savePrefs(p: CurrencyPrefsEntity) = prefsDao.upsert(p)

}