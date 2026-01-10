package com.example.collectalogger2.util.libraryObjects

import com.example.collectalogger2.BuildConfig
import com.example.collectalogger2.util.APIException
import com.example.collectalogger2.util.APIStatusException
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import java.util.concurrent.CancellationException

/**
 * This handles all API requests to IGDB.
 * Raw IGDB requests are discouraged.
 */
object IGDBSource {
    private val client = HttpClient(Android)
    private val mutex = Mutex()
    private var lastRequestTime = 0L
    private const val RATE_LIMIT_DELAY_MS = 300L // Conservative compared to 4 per second

    /**
     * This method creates an API call to IGDB given an endpoint (e.g: games)
     * and a request body.
     * It automatically handles rate limits.
     * It returns a JSONArray.
     */
    suspend fun makeAPICall(
        endpoint: String,
        request_body: String,
        ): JSONArray {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < RATE_LIMIT_DELAY_MS) {
                delay(RATE_LIMIT_DELAY_MS - elapsed)
            }
            lastRequestTime = System.currentTimeMillis()

            val igdbResponse: String
            try {
                igdbResponse = client.request("https://igdbproxy.shanjiang.ca/igdb/${endpoint}") {
                    method = HttpMethod.Post
                    headers {
                        append("Client-ID", BuildConfig.IGDB_API_KEY)
                        append(HttpHeaders.ContentType, "text/plain")
                        append(HttpHeaders.Connection, "keep-alive")
                    }
                    setBody(request_body)
                }.bodyAsText()
                if (igdbResponse[0] != '[') {
                    // continue for other exception types
                    if (igdbResponse.contains("429")) throw APIStatusException(
                        "Too Many Requests",
                        429
                    )
                    if (igdbResponse.contains("Forbidden")) throw APIStatusException(
                        "Forbidden",
                        403
                    )
                    throw APIException("Did not return a JSON Array. Instead returned $igdbResponse")
                }
            } catch (ex: Exception) {
                if (ex is CancellationException)
                    throw APIException("IGDB API call failed due to the Job being cancelled. Is the IGDB API proxy reachable?")
                else
                    throw APIException("IGDB API call failed with Exception type \"${ex.toString()}\" and message: ${ex.message}")
            }
            // parse igdbResponse as JSON
            return JSONArray(igdbResponse)

        }
    }
}