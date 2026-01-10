package com.example.collectalogger2.util.libraryObjects

import com.example.collectalogger2.BuildConfig
import com.example.collectalogger2.util.APIException
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

object SteamSource {
    private val client = HttpClient(Android)
    private val mutex = Mutex()
    private var lastRequestTime = 0L
    private const val RATE_LIMIT_DELAY_MS = 300L // Not formally necessary but probably not a bottleneck

    /**
     * This method creates an API call to Steam given two endpoints
     * (e.g: ISteamUser, ResolveVanityURL), version number, and request parameters.
     * It automatically handles rate limits (if any exist).
     * It returns a JSONObject.
     */
    suspend fun makeAPICall(
        endpoint: String,
        endpoint2: String,
        version: Int,
        params: Map<String, String>,
    ): JSONObject {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < RATE_LIMIT_DELAY_MS) {
                delay(RATE_LIMIT_DELAY_MS - elapsed)
            }
            lastRequestTime = System.currentTimeMillis()

            val steamResponse: String
            try {
                steamResponse = client.get("https://api.steampowered.com/${endpoint}/${endpoint2}/v${version}/") {
                    url {
                        parameters.append("key", BuildConfig.STEAM_API_KEY)
                        params.forEach { param ->
                            parameters.append(param.key, param.value)
                        }
                    }

                }.bodyAsText()
                if (steamResponse[0] != '{') throw Exception("Did not return a JSON Object. Instead returned $steamResponse")
            } catch (ex: Exception) {
                throw APIException("Steam API call failed with message: ${ex.message}")
            }
            // parse steamResponse as JSON
            return JSONObject(steamResponse)

        }
    }
}