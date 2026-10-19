package com.example.collectalogger2.util

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * A user-facing message that the UI layer resolves into localized text.
 * Repositories and ViewModels build these; only the UI resolves them,
 * since non-UI layers can't access string resources.
 */
sealed class UiText {
    data class Res(@StringRes val res: Int, val args: List<Any> = emptyList()) : UiText()
    data class Plural(@PluralsRes val res: Int, val count: Int) : UiText()

    fun resolve(resources: Resources): String = when (this) {
        is Res -> resources.getString(res, *args.toTypedArray())
        is Plural -> resources.getQuantityString(res, count, count)
    }
}
