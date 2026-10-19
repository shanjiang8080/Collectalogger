package com.example.collectalogger2.ui.overlays

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import com.example.collectalogger2.util.libraryObjects.NintendoSource
import org.json.JSONObject

/**
 * Login overlay for the Nintendo Account, serving both the library import
 * and the playtime import:
 *
 * - Library: the Virtual Game Cards portal needs the account portal's
 *   session cookies (captured via CookieManager once the user has signed in).
 * - Playtime: the Nintendo Store app's play activity API needs a
 *   session_token, obtained by authorizing the app's client through the
 *   OAuth login and intercepting the redirect to the npf<client id>://auth
 *   custom scheme, which carries the session_token_code.
 *
 * Both run through the same login: the overlay loads the OAuth authorize
 * URL, and once the user selects their account (which also establishes the
 * portal session), the custom-scheme redirect is captured together with the
 * cookies. The ViewModel then exchanges the code for the session token.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NintendoOverlay(
    onDismiss: () -> Unit,
    saveNintendoLogin: (cookies: String, sessionTokenCode: String, codeVerifier: String) -> Unit
) {
    val hasHandledResponse = remember { mutableStateOf(false) }
    val codeVerifier = remember { NintendoSource.generateCodeVerifier() }
    val loginUrl = remember {
        NintendoSource.getAuthorizeUrl(NintendoSource.getCodeChallenge(codeVerifier))
    }

    Dialog(onDismissRequest = onDismiss) {
        AndroidView(factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // Use a mobile user agent so Nintendo serves the mobile layout,
                // which fits the phone-sized login dialog (the portal data
                // fetching itself uses a desktop user agent)
                settings.userAgentString = NintendoSource.MOBILE_USER_AGENT
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        // The "Select this account" button navigates to the
                        // app's custom scheme redirect, carrying the
                        // session_token_code in the fragment
                        val code = request?.url?.toString()?.toUri()?.fragment
                            ?.split("&")
                            ?.firstOrNull { it.startsWith("session_token_code=") }
                            ?.substringAfter("=")
                        if (code != null && !hasHandledResponse.value) {
                            hasHandledResponse.value = true
                            val cookieManager = CookieManager.getInstance()
                            val cookieInfo = JSONObject()
                            cookieInfo.put(
                                NintendoSource.PORTAL_COOKIE_URL,
                                cookieManager.getCookie(NintendoSource.PORTAL_COOKIE_URL) ?: ""
                            )
                            saveNintendoLogin(cookieInfo.toString(), code, codeVerifier)
                            onDismiss()
                            return true
                        }
                        return false
                    }
                }
                loadUrl(loginUrl)
            }
        }, modifier = Modifier.size(400.dp, 600.dp))
    }
}

@Preview
@Composable
fun NintendoOverlayPreview() {
    NintendoOverlay(onDismiss = {}, saveNintendoLogin = { _, _, _ -> })
}
