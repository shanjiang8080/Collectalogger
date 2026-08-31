package com.example.collectalogger2.ui.overlays

import android.annotation.SuppressLint
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
import com.example.collectalogger2.util.libraryObjects.AmazonSource

/**
 * Login overlay for Amazon Games. Opens the Amazon sign-in page with a PKCE
 * code challenge (like Playnite's AmazonGamesLibrary) and intercepts the
 * redirect URL containing the authorization code, which is then exchanged for
 * device tokens.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AmazonOverlay(
    onDismiss: () -> Unit,
    saveAmazonLogin: (String, String) -> Unit
) {
    val hasHandledResponse = remember { mutableStateOf(false) }
    val codeVerifier = remember { AmazonSource.generateCodeVerifier() }
    val loginUrl = remember {
        AmazonSource.LOGIN_URL + AmazonSource.getCodeChallenge(codeVerifier)
    }

    val onCodeReceived: (String) -> Unit = { code ->
        if (!hasHandledResponse.value) {
            hasHandledResponse.value = true
            saveAmazonLogin(code, codeVerifier)
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        AndroidView(factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                // Use the same user agent as the Amazon Games launcher
                settings.userAgentString =
                    "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) @amzn/aga-electron-platform/1.0.0 Chrome/78.0.3904.130 Electron/7.1.9 Safari/537.36"
                webViewClient = object : WebViewClient() {
                    private fun checkUrl(url: String?) {
                        if (url?.contains("openid.oa2.authorization_code") == true) {
                            val code = url.toUri()
                                .getQueryParameter("openid.oa2.authorization_code")
                            if (code != null) onCodeReceived(code)
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
                loadUrl(loginUrl)
            }
        }, modifier = Modifier.size(400.dp, 600.dp))
    }
}

@Preview
@Composable
fun AmazonOverlayPreview() {
    AmazonOverlay(onDismiss = {}, { _, _ -> })
}
