package com.example.collectalogger2.util.libraryObjects

import com.example.collectalogger2.util.APIException
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import kotlin.time.Duration.Companion.milliseconds


object ItchSource {
    private val client = HttpClient(Android)
    private val mutex = Mutex()
    private var lastRequestTime = 0L

    // Not sure of the precise rate limit but this seems safe
    private val RATE_LIMIT_DELAY = 500.milliseconds

    suspend fun makeAPICall(
        secret: String,
        pageNumber: Int
    ): JSONObject {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = (now - lastRequestTime).milliseconds
            if (elapsed < RATE_LIMIT_DELAY) {
                delay(RATE_LIMIT_DELAY - elapsed)
            }
            lastRequestTime = now

            val itchResponse: String
            try {
                itchResponse = client.get(
                    "https://api.itch.io/profile/owned-keys" // use bearer auth
                ) {
                    headers {
                        append("Authorization", "Bearer $secret")
                    }
                    url {
                        parameters.append("page", pageNumber.toString())
                    }
                }.bodyAsText()
            } catch (ex: Exception) {
                throw APIException("Itch API call failed with message: ${ex.message}")
            }

            return JSONObject(itchResponse)
        }
    }
}