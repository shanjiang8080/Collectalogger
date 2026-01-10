package com.example.collectalogger2.util.libraryObjects

import com.example.collectalogger2.util.APIException
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

object EpicSource {
    private val client = HttpClient(Android)
    private val mutex = Mutex()
    private var lastRequestTime = 0L
    private const val RATE_LIMIT_DELAY_MS = 10L // Who knows the real rate limit ¯\_(ツ)_/¯

    /**
     * This method creates an API call to Epic Games with a domain, path, headers,
     * and params. It returns a JSONObject. Note that you need to specify the
     * auth token in headers, since it expires.
     * Example:
     *  domain: catalog-public-service-prod06.ol.epicgames.com
     *  path: catalog/api/shared/namespace/0584d2013f0149a791e7b9bad0eec102/bulk/items
     *  headers: mapOf("Authorization" to "bearer b7a378eebb9c47bf8a9eb4b03d95842f")
     *  params: mapOf("id" to "d2fb14a0fe4946d4b4dffa2750fce03a", "country" to "US") (etc)
     */
    suspend fun makeAPICall(
        domain: String,
        path: String,
        isGet: Boolean,
        headerss: Map<String, String>,
        params: Map<String, String>,
        bodyParams: Map<String, String>,
        isJsonArray: Boolean = false
    ): Any {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < RATE_LIMIT_DELAY_MS) {
                delay(RATE_LIMIT_DELAY_MS - elapsed)
            }
            lastRequestTime = System.currentTimeMillis()

            val epicResponse: String
            try {
                if (isGet) {
                    epicResponse = client.request("https://${domain}/${path}") {
                        method = HttpMethod.Get
                        url {
                            params.forEach { param ->
                                parameters.append(param.key, param.value)
                            }
                        }
                        headers {
                            headers.append("Content-Type", "application/x-www-form-urlencoded")
                            headerss.forEach { header ->
                                headers.append(header.key, header.value)
                            }
                        }
                    }.bodyAsText()
                } else {

                    epicResponse = client.submitForm("https://${domain}/${path}",
                        formParameters = Parameters.build {
                            bodyParams.forEach { param ->
                                append(param.key, param.value)
                            }
                        }) {
                        url {
                            params.forEach { param ->
                                parameters.append(param.key, param.value)
                            }
                        }
                        headers {
                            headers.append("Content-Type", "application/x-www-form-urlencoded")
                            headerss.forEach { header ->
                                headers.append(header.key, header.value)
                            }
                        }

                    }.bodyAsText()
                }
                if (!isJsonArray && epicResponse[0] != '{') throw Exception("Did not return a JSON Object. Instead returned $epicResponse")
                if (isJsonArray && epicResponse[0] != '[') throw Exception("Did not return a JSON Array. Instead returned $epicResponse")

            } catch (ex: Exception) {
                throw APIException("Epic Games API call failed with message: ${ex.message}")
            }
            // parse epicResponse as JSON
            if (isJsonArray)
                return JSONArray(epicResponse)
            return JSONObject(epicResponse)

        }
    }
}