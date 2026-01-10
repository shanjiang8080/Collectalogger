package com.example.collectalogger2.util.libraryObjects

import com.example.collectalogger2.util.APIException
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

object GogSource {
    private val client = HttpClient(Android)
    private val mutex = Mutex()
    private var lastRequestTime = 0L
    private const val RATE_LIMIT_DELAY_MS = 300L // Not sure how often, but be safe?

    // Right now, this is limited to the library games endpoint.
    // Not sure if there are other endpoints necessary.
    suspend fun makeAPICall(
        username: String,
        page: Int
    ): JSONObject {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < RATE_LIMIT_DELAY_MS) {
                delay(RATE_LIMIT_DELAY_MS - elapsed)
            }
            lastRequestTime = System.currentTimeMillis()

            val gogResponse: String
            try {
                gogResponse =
                    client.get("https://www.gog.com/u/$username/games/stats?sort=recent_playtime&order=desc&page=$page") {
                    }.bodyAsText()
                if (gogResponse[0] != '{') throw Exception("Did not return a JSON Object. Instead returned $gogResponse")
            } catch (ex: Exception) {
                throw APIException("Steam API call failed with message: ${ex.message}")
            }
            // parse gogResponse as JSON
            return JSONObject(gogResponse)

        }
    }
}