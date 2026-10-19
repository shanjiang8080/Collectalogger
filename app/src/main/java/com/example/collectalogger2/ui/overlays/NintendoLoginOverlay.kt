package com.example.collectalogger2.ui.overlays

import android.annotation.SuppressLint
import android.webkit.CookieManager
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
import com.example.collectalogger2.util.libraryObjects.NintendoSource
import org.json.JSONObject

/**
 * Login overlay for the Nintendo Account Virtual Game Cards portal. Opens the
 * portal page in a WebView; once the user has signed in and the browser lands
 * back on the portal page, the portal's session cookies are captured and
 * saved (as a JSON object mapping base URLs to raw cookie header strings).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NintendoOverlay(
    onDismiss: () -> Unit,
    saveNintendoLogin: (String) -> Unit
) {
    val hasHandledResponse = remember { mutableStateOf(false) }

    val onPortalReached: () -> Unit = {
        if (!hasHandledResponse.value) {
            hasHandledResponse.value = true
            // Collect the portal's cookies on the main thread and save them
            val cookieManager = CookieManager.getInstance()
            val cookieInfo = JSONObject()
            cookieInfo.put(
                NintendoSource.PORTAL_COOKIE_URL,
                cookieManager.getCookie(NintendoSource.PORTAL_COOKIE_URL) ?: ""
            )
            saveNintendoLogin(cookieInfo.toString())
            onDismiss()
        }
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
                    private fun checkUrl(url: String?) {
                        if (url?.startsWith(NintendoSource.PORTAL_BASE_URL) == true) {
                            onPortalReached()
                        }
                    }

                    override fun doUpdateVisitedHistory(
                        view: WebView?,
                        url: String?,
                        isReload: Boolean
                    ) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        checkUrl(url)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        checkUrl(url)
                    }
                }
                loadUrl(NintendoSource.PORTAL_URL)
            }
        }, modifier = Modifier.size(400.dp, 600.dp))
    }
}

@Preview
@Composable
fun NintendoOverlayPreview() {
    NintendoOverlay(onDismiss = {}, saveNintendoLogin = {})
}
