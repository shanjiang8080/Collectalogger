package com.example.collectalogger2.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap

fun getExtensionFromUri(context: Context, uri: Uri): String {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri)

    return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
}