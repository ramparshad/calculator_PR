package com.metzger100.calculator.data

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.metzger100.calculator.data.local.CurrencyListDao
import com.metzger100.calculator.data.local.CurrencyListEntity
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
    private val dao: CurrencyRateDao,
    private val listDao: CurrencyListDao,
    private val httpClient: HttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher  // z.B. Dispatchers.IO
) {
    private val gson = Gson()

    private val TAG = "CurrencyRepository"  // Tag für das Loggen

    /** Gibt eine Map von Währungscode → Kurs zurück (Basis = [base]). */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getRates(base: String): Map<String, Double> = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val cutoff = now - 24 * 3_600_000L  // vor 24 Stunden

        Log.d(TAG, "getRates: Checking cache for base currency: $base")

        val cached = dao.get(base)
        val rawJson = try {
            if (cached == null || cached.timestamp <= cutoff) {
                Log.d(TAG, "getRates: Cache expired or not available, fetching fresh rates for $base")
                val fresh = fetchRatesJson(base)
                dao.upsert(CurrencyRateEntity(base = base, json = fresh, timestamp = now))
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
        return dao.get(base)?.timestamp
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun fetchRatesJson(base: String): String {
        val date = LocalDate.now(ZoneOffset.UTC).toString()
        val lower = base.lowercase()
        val primary = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/$lower.json"
        val fallback = "https://$date.currency-api.pages.dev/v1/currencies/$lower.json"

        Log.d(TAG, "fetchRatesJson: Fetching rates from primary URL: $primary")
        return try {
            httpClient.get(primary).bodyAsText().also {
                Log.d(TAG, "fetchRatesJson: Successfully fetched from primary URL.")
            }
        } catch (_: Exception) {
            Log.d(TAG, "fetchRatesJson: Primary URL failed, trying fallback: $fallback")
            httpClient.get(fallback).bodyAsText().also {
                Log.d(TAG, "fetchRatesJson: Successfully fetched from fallback URL.")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getAvailableCurrenciesWithTitles(): List<Pair<String, String>> =
        withContext(ioDispatcher) {
            val now = System.currentTimeMillis()
            val cutoff = now - 24 * 3_600_000L  // vor 24 Stunden

            Log.d(TAG, "getAvailableCurrenciesWithTitles: Checking cache for currencies list.")

            val cached = listDao.get()
            val rawJson = if (cached != null && cached.timestamp > cutoff) {
                Log.d(TAG, "getAvailableCurrenciesWithTitles: Using cached currency list. Cached timestamp: ${cached.timestamp}, current time: $now")
                Log.d(TAG, "getAvailableCurrenciesWithTitles: Cached list age: ${(now - cached.timestamp) / 1000} seconds")
                cached.json
            } else {
                // Versuche Primary & Fallback, aber im Fehlerfall einfach "{}" nehmen
                Log.d(TAG, "getAvailableCurrenciesWithTitles: Cache expired or not available, fetching fresh currency list.")
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
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun fetchCurrenciesJsonWithNetworkFallback(): String {
        val date = LocalDate.now(ZoneOffset.UTC).toString()
        val primary = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies.min.json"
        val fallback = "https://$date.currency-api.pages.dev/v1/currencies.min.json"

        Log.d(TAG, "fetchCurrenciesJsonWithNetworkFallback: Fetching currencies list from primary URL: $primary")
        return try {
            httpClient.get(primary).bodyAsText().also {
                Log.d(TAG, "fetchCurrenciesJsonWithNetworkFallback: Successfully fetched from primary URL.")
            }
        } catch (_: Exception) {
            Log.d(TAG, "fetchCurrenciesJsonWithNetworkFallback: Primary URL failed, trying fallback: $fallback")
            httpClient.get(fallback).bodyAsText().also {
                Log.d(TAG, "fetchCurrenciesJsonWithNetworkFallback: Successfully fetched from fallback URL.")
            }
        }
    }
}