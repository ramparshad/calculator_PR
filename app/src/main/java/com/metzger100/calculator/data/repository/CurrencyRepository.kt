package com.metzger100.calculator.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.metzger100.calculator.data.local.dao.CurrencyListDao
import com.metzger100.calculator.data.local.dao.CurrencyRateDao
import com.metzger100.calculator.data.local.dao.CurrencyPrefsDao
import com.metzger100.calculator.data.local.entity.CurrencyListEntity
import com.metzger100.calculator.data.local.entity.CurrencyRateEntity
import com.metzger100.calculator.data.local.entity.CurrencyPrefsEntity
import com.metzger100.calculator.di.IoDispatcher
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val gson = Gson()
    private val TAG = "CurrencyRepository"

    /**
     * Liefert einen Flow<Map<CurrencyCode, Rate>>, der
     * 1) sofort die gecachten Raten emittet,
     * 2) bei Bedarf das Netzwerk befragt und
     * 3) nach erfolgreichem Upsert erneut emittet.
     *
     * @param base der ISO-Code der Basiswährung (z.B. "USD")
     * @param forceRefresh erzwingt immer ein Refetch
     * @param isOnline aktueller Netzwerkstatus
     */
    fun getRatesFlow(
        base: String,
        forceRefresh: Boolean = false,
        isOnline: Boolean = true
    ): Flow<Map<String, Double>> = flow {
        Log.d(TAG, "getRatesFlow(base=$base, forceRefresh=$forceRefresh, isOnline=$isOnline) START")

        // 1) Cache auslesen und emitten
        val cachedEntity = runCatching { rateDao.get(base) }
            .onFailure { Log.e(TAG, "getRatesFlow: DB read failed", it) }
            .getOrNull()
        val cachedJson = cachedEntity?.json
        val cachedRates = if (cachedJson != null) {
            parseRates(cachedJson, base).also {
                Log.d(TAG, "getRatesFlow: Emit cached rates (${it.size} entries)")
            }
        } else {
            Log.d(TAG, "getRatesFlow: No cache available → emit emptyMap()")
            emptyMap()
        }
        emit(cachedRates)

        // 2) Refresh-Entscheidung
        val nowUtc = Instant.now().atOffset(ZoneOffset.UTC)
        val cachedDate = cachedJson?.let { extractDate(it) }
        val shouldRefresh = forceRefresh
                || cachedJson == null
                || (nowUtc.hour >= 2 && cachedDate != nowUtc.toLocalDate())

        Log.d(TAG, "getRatesFlow: shouldRefresh=$shouldRefresh (cachedDate=$cachedDate, nowUtcHour=${nowUtc.hour})")

        if (shouldRefresh) {
            // 3) Netzwerk-Fetch (+ Upsert)
            if (isOnline) {
                try {
                    Log.d(TAG, "getRatesFlow: Online → start fetchRatesJson()")
                    val fresh = fetchRatesJson(base)
                    if (fresh.isNotBlank() && fresh != "{}") {
                        Log.d(TAG, "getRatesFlow: Received fresh data (${fresh.length} characters), upserting…")
                        runCatching {
                            rateDao.upsert(CurrencyRateEntity(base, fresh, System.currentTimeMillis()))
                        }.onFailure {
                            Log.e(TAG, "getRatesFlow: DB upsert failed", it)
                        }
                    } else {
                        Log.w(TAG, "getRatesFlow: Empty result from API → Fallback to cache")
                        cachedJson ?: "{}"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "getRatesFlow: Error during fetch → Fallback to cache", e)
                    cachedJson ?: "{}"
                }
            } else {
                Log.w(TAG, "getRatesFlow: Offline and refresh required → Fallback to cache")
                cachedJson ?: "{}"
            }

            // 4) Aktualisierte DB erneut auslesen und emitten
            val updatedEntity = runCatching { rateDao.get(base) }
                .onFailure { Log.e(TAG, "getRatesFlow: DB read failed", it) }
                .getOrNull()
            val updatedJson = updatedEntity?.json
            val updatedRates = if (updatedJson != null) {
                parseRates(updatedJson, base)
            } else emptyMap()

            if (updatedRates != cachedRates) {
                Log.d(TAG, "getRatesFlow: Emit updated rates (${updatedRates.size} entries)")
                emit(updatedRates)
            } else {
                Log.d(TAG, "getRatesFlow: No change to cached rates")
            }
        }

        Log.d(TAG, "getRatesFlow END")
    }.flowOn(ioDispatcher)

    /**
     * Liefert einen Flow<List<CurrencyCode, Title>>, der
     * 1) sofort die gecachte Liste emittet,
     * 2) bei Bedarf das Netzwerk befragt und
     * 3) nach erfolgreichem Upsert erneut emittet.
     */
    fun getCurrenciesFlow(
        forceRefresh: Boolean = false,
        isOnline: Boolean = true
    ): Flow<List<Pair<String, String>>> = flow {
        Log.d(TAG, "getCurrenciesFlow(forceRefresh=$forceRefresh, isOnline=$isOnline) START")

        // 1) Cache
        val cachedEntity = runCatching { listDao.get() }
            .onFailure { Log.e(TAG, "getCurrenciesFlow: DB read failed", it) }
            .getOrNull()
        val cachedJson = cachedEntity?.json
        val cachedList = if (cachedJson != null) {
            parseCurrencies(cachedJson).also {
                Log.d(TAG, "getCurrenciesFlow: Emit cached list (${it.size} entries)")
            }
        } else {
            Log.d(TAG, "getCurrenciesFlow: No Cache → emit emptyList()")
            emptyList()
        }
        emit(cachedList)

        // 2) Refresh?
        val nowUtc = Instant.now().atOffset(ZoneOffset.UTC)
        val cacheDate = cachedEntity
            ?.timestamp
            ?.let { Instant.ofEpochMilli(it).atOffset(ZoneOffset.UTC).toLocalDate() }
        val shouldRefresh = forceRefresh
                || cachedEntity == null
                || (nowUtc.hour >= 2 && cacheDate != nowUtc.toLocalDate())

        Log.d(TAG, "getCurrenciesFlow: shouldRefresh=$shouldRefresh (cacheDate=$cacheDate, nowUtcHour=${nowUtc.hour})")

        if (shouldRefresh) {
            // 3) Fetch & Upsert
            if (isOnline) {
                try {
                    Log.d(TAG, "getCurrenciesFlow: Online → fetchCurrenciesJson() starten")
                    val fresh = fetchCurrenciesJson()
                    if (fresh.isNotBlank() && fresh != "{}") {
                        Log.d(TAG, "getCurrenciesFlow: Received fresh list (${fresh.length} characters), upserting...")
                        runCatching {
                            listDao.upsert(CurrencyListEntity(json = fresh, timestamp = System.currentTimeMillis()))
                        }.onFailure {
                            Log.e(TAG, "getCurrenciesFlow: DB upsert failed", it)
                        }
                    } else {
                        Log.w(TAG, "getCurrenciesFlow: Empty result from API → Fallback to cache")
                        cachedJson ?: "{}"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "getCurrenciesFlow: Error during fetch → Fallback to cache", e)
                    cachedJson ?: "{}"
                }
            } else {
                Log.w(TAG, "getCurrenciesFlow: Offline and refresh required → Fallback to cache")
                cachedJson ?: "{}"
            }

            // 4) Emit aktualisiert
            val updatedJson = runCatching { listDao.get()?.json }
                .onFailure { Log.e(TAG, "getCurrenciesFlow: DB read (updated) failed", it) }
                .getOrNull()
            val updatedList = updatedJson?.let(::parseCurrencies) ?: emptyList()
            if (updatedList != cachedList) {
                Log.d(TAG, "getCurrenciesFlow: Emit updated list (${updatedList.size} entries)")
                emit(updatedList)
            } else {
                Log.d(TAG, "getCurrenciesFlow: No change to the cached list")
            }
        }

        Log.d(TAG, "getCurrenciesFlow END")
    }.flowOn(ioDispatcher)

    // ----------------------------------------
    // Hilfsfunktionen
    // ----------------------------------------

    /** Parst das API-JSON zu Map<Code, Rate> basierend auf dem übergebenen Base-Code. */
    private fun parseRates(rawJson: String, base: String): Map<String, Double> {
        return try {
            val root = gson.fromJson(rawJson, JsonObject::class.java)
            val key = base.lowercase()
            if (!root.has(key) || root.getAsJsonObject(key) == null) {
                Log.w(TAG, "parseRates: No field '$key' in JSON, returning emptyMap()")
                return emptyMap()
            }
            val ratesObj = root.getAsJsonObject(key)
            ratesObj.entrySet().associate { it.key to it.value.asDouble }
        } catch (e: Exception) {
            Log.e(TAG, "parseRates: JSON parsing failed", e)
            emptyMap()
        }
    }

    /** Extrahiert das "date"-Feld aus dem rohen JSON. */
    private fun extractDate(rawJson: String): LocalDate? = try {
        gson.fromJson(rawJson, JsonObject::class.java)
            .get("date").asString
            .let(LocalDate::parse)
    } catch (e: Exception) {
        Log.e(TAG, "extractDate: JSON parsing failed", e)
        null
    }

    /** Parst das JSON der Währungsliste zu List<Code, Title>. */
    private fun parseCurrencies(rawJson: String): List<Pair<String, String>> {
        return try {
            gson.fromJson(rawJson, JsonObject::class.java)
                .entrySet()
                .map { it.key.uppercase() to it.value.asString }
                .sortedBy { it.first }
        } catch (e: Exception) {
            Log.e(TAG, "parseCurrencies: JSON parsing failed", e)
            emptyList()
        }
    }

    // ----------------------------------------
    // Netzwerk-Fetch-Logik (3-Phasen)
    // ----------------------------------------

    private suspend fun fetchRatesJson(base: String): String {
        val todayUtc = LocalDate.now(ZoneOffset.UTC)
        val lower = base.lowercase()

        // Phase 1: jsDelivr mit Datumstag
        runCatching {
            val versionTag = "${todayUtc.year}.${todayUtc.monthValue}.${todayUtc.dayOfMonth}"
            val url = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@$versionTag/v1/currencies/$lower.json"
            Log.d(TAG, "fetchRatesJson[1]: Trying jsDelivr date=$versionTag: $url")
            val body = httpClient.get(url).bodyAsText()
            if (!body.contains("Couldn't find the requested release version")) {
                Log.d(TAG, "fetchRatesJson[1]: Success")
                return body
            }
            Log.d(TAG, "fetchRatesJson[1]: Not available yet")
        }.onFailure {
            Log.w(TAG, "fetchRatesJson[1]: Error, will try pages.dev", it)
        }

        // Phase 2: pages.dev mit yyyy-MM-dd
        runCatching {
            val dateTag = todayUtc.toString()
            val url = "https://$dateTag.currency-api.pages.dev/v1/currencies/$lower.json"
            Log.d(TAG, "fetchRatesJson[2]: Trying pages.dev date=$dateTag: $url")
            val body = httpClient.get(url).bodyAsText()
            if (!body.contains("<h1")) {
                Log.d(TAG, "fetchRatesJson[2]: Success")
                return body
            }
            Log.d(TAG, "fetchRatesJson[2]: Not ready yet")
        }.onFailure {
            Log.w(TAG, "fetchRatesJson[2]: Error, will try @latest", it)
        }

        // Phase 3: @latest
        runCatching {
            val url = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/$lower.json"
            Log.d(TAG, "fetchRatesJson[3]: Trying @latest: $url")
            return httpClient.get(url).bodyAsText()
        }.onFailure {
            Log.e(TAG, "fetchRatesJson[3]: @latest fetch failed", it)
        }

        Log.e(TAG, "fetchRatesJson: All attempts failed → returning empty JSON")
        return "{}"
    }

    private suspend fun fetchCurrenciesJson(): String {
        val todayUtc = LocalDate.now(ZoneOffset.UTC)

        // Phase 1: jsDelivr mit Datumstag
        runCatching {
            val versionTag = "${todayUtc.year}.${todayUtc.monthValue}.${todayUtc.dayOfMonth}"
            val url = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@$versionTag/v1/currencies.min.json"
            Log.d(TAG, "fetchCurrenciesJson[1]: Trying jsDelivr date=$versionTag: $url")
            val body = httpClient.get(url).bodyAsText()
            if (!body.contains("Couldn't find the requested release version")) {
                Log.d(TAG, "fetchCurrenciesJson[1]: Success")
                return body
            }
            Log.d(TAG, "fetchCurrenciesJson[1]: Not available yet")
        }.onFailure {
            Log.w(TAG, "fetchCurrenciesJson[1]: Error, will try pages.dev", it)
        }

        // Phase 2: pages.dev mit yyyy-MM-dd
        runCatching {
            val dateTag = todayUtc.toString()
            val url = "https://$dateTag.currency-api.pages.dev/v1/currencies.min.json"
            Log.d(TAG, "fetchCurrenciesJson[2]: Trying pages.dev date=$dateTag: $url")
            val body = httpClient.get(url).bodyAsText()
            if (!body.contains("<h1")) {
                Log.d(TAG, "fetchCurrenciesJson[2]: Success")
                return body
            }
            Log.d(TAG, "fetchCurrenciesJson[2]: Not ready yet")
        }.onFailure {
            Log.w(TAG, "fetchCurrenciesJson[2]: Error, will try @latest", it)
        }

        // Phase 3: @latest
        runCatching {
            val url = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies.min.json"
            Log.d(TAG, "fetchCurrenciesJson[3]: Trying @latest: $url")
            return httpClient.get(url).bodyAsText()
        }.onFailure {
            Log.e(TAG, "fetchCurrenciesJson[3]: @latest fetch failed", it)
        }

        Log.e(TAG, "fetchCurrenciesJson: All attempts failed → returning empty JSON")
        return "{}"
    }

    // ----------------------------------------
    // Zusätzliche Helfer
    // ----------------------------------------

    /**
     * Liest aus dem zuletzt gecachten JSON der Basis-Währung [base]
     * das "date"-Feld und gibt es als LocalDate zurück (oder null).
     */
    suspend fun getLastApiDateForBase(base: String): LocalDate? = withContext(ioDispatcher) {
        Log.d(TAG, "getLastApiDateForBase: loading cache for base=$base")
        val rawJson = runCatching { rateDao.get(base)?.json }
            .onFailure { Log.e(TAG, "getLastApiDateForBase: DB read failed", it) }
            .getOrNull()

        rawJson?.let {
            runCatching {
                gson.fromJson(it, JsonObject::class.java)
                    .get("date").asString
                    .let(LocalDate::parse)
            }.onFailure {
                Log.e(TAG, "getLastApiDateForBase: parse failed", it)
            }.getOrNull()
        }.also { Log.d(TAG, "getLastApiDateForBase: result = $it") }
    }

    // ----------------------------------------
    // Preferences
    // ----------------------------------------

    suspend fun getPrefs(): CurrencyPrefsEntity? = withContext(ioDispatcher) {
        Log.d(TAG, "getPrefs()")
        runCatching { prefsDao.get() }
            .onFailure { Log.e(TAG, "getPrefs: DB read failed", it) }
            .getOrNull()
    }

    suspend fun savePrefs(prefs: CurrencyPrefsEntity) = withContext(ioDispatcher) {
        Log.d(TAG, "savePrefs(prefs=$prefs)")
        runCatching { prefsDao.upsert(prefs) }
            .onFailure { Log.e(TAG, "savePrefs: DB upsert failed", it) }
    }
}
