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
import java.time.Instant
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
        val nowMillis    = System.currentTimeMillis()
        val nowUtcDate   = LocalDate.now(ZoneOffset.UTC)
        val nowUtcHour   = Instant.ofEpochMilli(nowMillis)
            .atOffset(ZoneOffset.UTC)
            .hour

        // 1) lade die gecachte Entity (falls vorhanden)
        val cachedEntity = rateDao.get(base)

        // 2) parse aus dem gecachten JSON das "date"-Feld (API-Datum)
        val cachedApiDate: LocalDate? = cachedEntity?.json?.let { json ->
            runCatching {
                gson.fromJson(json, JsonObject::class.java)
                    .get("date").asString
                    .let(LocalDate::parse)
            }.getOrNull()
        }

        val shouldRefresh = forceRefresh
                || cachedEntity == null
                || (nowUtcHour >= 2 && cachedApiDate != nowUtcDate)

        Log.d(TAG, "getRates: Checking for update - forceRefresh=$forceRefresh, cachedEntityIsNull=${cachedEntity == null}, timeConditionMet=${nowUtcHour >= 2 && cachedApiDate != nowUtcDate}")

        val rawJson = if (shouldRefresh) {
            try {
                Log.d(TAG, "getRates: Attempting to fetch fresh rates for $base...")
                val fresh = fetchRatesJson(base)
                // Nur wenn Daten nicht leer sind, den Cache überschreiben
                if (fresh != "{}") {
                    Log.d(TAG, "getRates: Fresh data fetched successfully, updating cache.")
                    rateDao.upsert(CurrencyRateEntity(base = base, json = fresh, timestamp = nowMillis))
                    fresh
                } else {
                    Log.w(TAG, "getRates: Fresh data is empty, falling back to cache.")
                    cachedEntity?.json ?: "{}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "getRates: Error fetching fresh rates: ${e.localizedMessage}")
                cachedEntity?.json ?: "{}"
            }
        } else {
            Log.d(TAG, "getRates: Using cached data.")
            cachedEntity?.json ?: "{}"
        }

        // Parsen und Rückgabe
        val root = try {
            gson.fromJson(rawJson, JsonObject::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "getRates: JSON parsing failed: ${e.localizedMessage}")
            null
        }

        if (root == null || !root.has(base.lowercase())) {
            Log.d(TAG, "getRates: No valid data for base $base.")
            emptyMap()
        } else {
            val ratesObj = root.getAsJsonObject(base.lowercase())
            ratesObj.entrySet().associate { it.key to it.value.asDouble }
        }
    }

    suspend fun getLastApiDateForBase(base: String): LocalDate? = withContext(ioDispatcher) {
        rateDao.get(base)?.json
            ?.let { json ->
                try {
                    val root = gson.fromJson(json, JsonObject::class.java)
                    if (root.has("date")) {
                        LocalDate.parse(root.get("date").asString)
                    } else null
                } catch (e: Exception) {
                    Log.e(TAG, "getLastApiDateForBase: JSON parsing failed: ${e.localizedMessage}")
                    null
                }
            }
    }

    private suspend fun fetchRatesJson(base: String): String {
        val todayUtc = LocalDate.now(ZoneOffset.UTC)
        val lower = base.lowercase()

        // Phase 1: jsDelivr genau für heute
        runCatching {
            val versionTag = "${todayUtc.year}.${todayUtc.monthValue}.${todayUtc.dayOfMonth}"
            val url = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@$versionTag/v1/currencies/$lower.json"
            Log.d(TAG, "fetchRatesJson: trying jsDelivr date=$versionTag → $url")
            val body = httpClient.get(url).bodyAsText()
            if (!body.contains("Couldn't find the requested release version")) {
                Log.d(TAG, "fetchRatesJson: success jsDelivr date=$versionTag")
                return body
            }
            Log.d(TAG, "fetchRatesJson: jsDelivr date=$versionTag not available yet")
        }.onFailure {
            Log.d(TAG, "fetchRatesJson: jsDelivr date fetch error, will try pages.dev")
        }

        // Phase 2: pages.dev genau für heute
        runCatching {
            val dateTag = todayUtc.toString()  // yyyy-MM-dd
            val url = "https://$dateTag.currency-api.pages.dev/v1/currencies/$lower.json"
            Log.d(TAG, "fetchRatesJson: trying pages.dev date=$dateTag → $url")
            val body = httpClient.get(url).bodyAsText()
            if (!body.contains("<h1")) {
                Log.d(TAG, "fetchRatesJson: success pages.dev date=$dateTag")
                return body
            }
            Log.d(TAG, "fetchRatesJson: pages.dev date=$dateTag not ready yet")
        }.onFailure {
            Log.d(TAG, "fetchRatesJson: pages.dev fetch error, will try @latest")
        }

        // Phase 3: jsDelivr @latest
        runCatching {
            val url = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/$lower.json"
            Log.d(TAG, "fetchRatesJson: falling back to @latest → $url")
            return httpClient.get(url).bodyAsText()
        }.onFailure {
            Log.d(TAG, "fetchRatesJson: @latest fetch failed, returning empty JSON")
        }

        return "{}"
    }

    suspend fun getCurrencies(forceRefresh: Boolean = false): List<Pair<String, String>> =
        withContext(ioDispatcher) {
            val nowMillis   = System.currentTimeMillis()
            val nowUtcDate  = LocalDate.now(ZoneOffset.UTC)
            val nowUtcHour  = Instant.ofEpochMilli(nowMillis)
                .atOffset(ZoneOffset.UTC)
                .hour

            val cachedEntity = listDao.get()

            val cacheDateUtc: LocalDate? = cachedEntity?.timestamp
                ?.let { ts ->
                    Instant.ofEpochMilli(ts)
                        .atOffset(ZoneOffset.UTC)
                        .toLocalDate()
                }

            val shouldRefresh = forceRefresh
                    || cachedEntity == null
                    || (nowUtcHour >= 2 && cacheDateUtc != nowUtcDate)

            Log.d(TAG, "getCurrencies: Checking for update - forceRefresh=$forceRefresh, cachedEntityIsNull=${cachedEntity == null}, timeConditionMet=${nowUtcHour >= 2 && cacheDateUtc != nowUtcDate}")

            val rawJson = if (shouldRefresh) {
                try {
                    Log.d(TAG, "getCurrencies: Fetching fresh currency list...")
                    val freshJson = fetchCurrenciesJson()
                    if (freshJson != "{}") {
                        Log.d(TAG, "getCurrencies: Successfully fetched fresh list, updating cache.")
                        listDao.upsert(CurrencyListEntity(json = freshJson, timestamp = nowMillis))
                        freshJson
                    } else {
                        Log.w(TAG, "getCurrencies: Received empty JSON, falling back to cache.")
                        cachedEntity?.json ?: "{}"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "getCurrencies: Error fetching list: ${e.localizedMessage}")
                    cachedEntity?.json ?: "{}"
                }
            } else {
                Log.d(TAG, "getCurrencies: Using cached currency list.")
                cachedEntity?.json ?: "{}"
            }

            // Parsen – "{}" ergibt einfach eine leere Liste
            return@withContext try {
                val root = gson.fromJson(rawJson, JsonObject::class.java)
                root.entrySet()
                    .map { it.key.uppercase() to it.value.asString }
                    .sortedBy { it.first }
            } catch (e: Exception) {
                Log.e(TAG, "getCurrencies: JSON parsing failed: ${e.localizedMessage}")
                emptyList()
            }
        }

    /** Holt currencies.min.json zuerst vom CDN, bei Fehlern vom Pages-dev-Fallback. */
    private suspend fun fetchCurrenciesJson(): String {
        val todayUtc = LocalDate.now(ZoneOffset.UTC)

        // Phase 1: jsDelivr genau für heute
        runCatching {
            val versionTag = "${todayUtc.year}.${todayUtc.monthValue}.${todayUtc.dayOfMonth}"
            val url = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@$versionTag/v1/currencies.min.json"
            Log.d(TAG, "fetchCurrenciesJson: trying jsDelivr date=$versionTag → $url")
            val body = httpClient.get(url).bodyAsText()
            if (!body.contains("Couldn't find the requested release version")) {
                Log.d(TAG, "fetchCurrenciesJson: success jsDelivr date=$versionTag")
                return body
            }
            Log.d(TAG, "fetchCurrenciesJson: jsDelivr date=$versionTag not available yet")
        }.onFailure {
            Log.d(TAG, "fetchCurrenciesJson: jsDelivr date fetch error, will try pages.dev")
        }

        // Phase 2: pages.dev genau für heute
        runCatching {
            val dateTag = todayUtc.toString()
            val url = "https://$dateTag.currency-api.pages.dev/v1/currencies.min.json"
            Log.d(TAG, "fetchCurrenciesJson: trying pages.dev date=$dateTag → $url")
            val body = httpClient.get(url).bodyAsText()
            if (!body.contains("<h1")) {
                Log.d(TAG, "fetchCurrenciesJson: success pages.dev date=$dateTag")
                return body
            }
            Log.d(TAG, "fetchCurrenciesJson: pages.dev date=$dateTag not ready yet")
        }.onFailure {
            Log.d(TAG, "fetchCurrenciesJson: pages.dev fetch error, will try @latest")
        }

        // Phase 3: jsDelivr @latest
        runCatching {
            val url = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies.min.json"
            Log.d(TAG, "fetchCurrenciesJson: falling back to @latest → $url")
            return httpClient.get(url).bodyAsText()
        }.onFailure {
            Log.d(TAG, "fetchCurrenciesJson: @latest fetch failed, returning empty JSON")
        }

        return "{}"
    }

    suspend fun getPrefs(): CurrencyPrefsEntity? = prefsDao.get()
    suspend fun savePrefs(p: CurrencyPrefsEntity) = prefsDao.upsert(p)
}