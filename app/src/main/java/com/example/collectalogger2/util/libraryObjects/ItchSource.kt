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


object ItchSource {
    private val client = HttpClient(Android)
    private val mutex = Mutex()
    private var lastRequestTime = 0L

    // Not sure of the precise rate limit but this seems safe
    private const val RATE_LIMIT_DELAY_MS = 500L

    suspend fun makeAPICall(
        secret: String,
        pageNumber: Int
    ): JSONObject {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < RATE_LIMIT_DELAY_MS) {
                delay(RATE_LIMIT_DELAY_MS - elapsed)
            }
            lastRequestTime = System.currentTimeMillis()

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