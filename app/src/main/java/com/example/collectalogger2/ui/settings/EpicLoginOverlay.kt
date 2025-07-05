package com.example.collectalogger2.ui.settings

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EpicOverlay(
    onDismiss: () -> Unit,
    saveEpicID: (String) -> Unit
) {
    var hasHandledResponse = remember { mutableStateOf(false) }
    val onContentReceived: (String) -> Unit = { html ->
        if (!hasHandledResponse.value) {
            hasHandledResponse.value = true
            // assuming JSON starts with { and ends with }
            val startIndex = html.indexOf("{")
            val endIndex = html.indexOf("}")
            val json = JSONObject(html.substring(startIndex, endIndex + 1))
            saveEpicID(json.getString("authorizationCode"))
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        AndroidView(factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                addJavascriptInterface(WebAppInterface(onContentReceived), "HTMLOUT")
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (url?.contains("epicgames.com/account/personal") == true) {
                            view?.loadUrl("https://www.epicgames.com/id/api/redirect?clientId=34a02cf8f4414e29b15921876da36f9a&responseType=code")
                        } else if (url?.contains("epicgames.com/id/api/redirect") == true) {
                            loadUrl("javascript:window.HTMLOUT.processHTML(document.documentElement.outerHTML)")
                        }
                    }
                }
                loadUrl("https://www.epicgames.com/id/login")
            }
        }, modifier = Modifier.size(400.dp, 600.dp))
    }
}

class WebAppInterface(private val onContentReceived: (String) -> Unit) {
    @JavascriptInterface
    fun processHTML(html: String) {
        onContentReceived(html)
    }
}

@Preview
@Composable
fun EpicOverlayPreview() {
    EpicOverlay(onDismiss = {}, {})
}



