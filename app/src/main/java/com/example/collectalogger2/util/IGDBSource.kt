package com.example.collectalogger2.util

import android.util.Log
import com.example.collectalogger2.BuildConfig
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
import org.json.JSONObject

object IGDBSource {
    private val client = HttpClient(Android)
    private val mutex = Mutex()
    private var lastRequestTime = 0L
    private const val RATE_LIMIT_DELAY_MS = 500L // Conservative compared to 4 per second

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
                if (igdbResponse[0] != '[') throw Exception("Did not return a JSON Array. Instead returned $igdbResponse")
            } catch (ex: Exception) {
                throw APIException("IGDB API call failed with message: ${ex.message}")
            }
            // parse igdbResponse as JSON
            return JSONArray(igdbResponse)

        }
    }
}