package com.example.collectalogger2.util


/**
 * A class for sorting.
 */
data class Sort(
    val criteria: SortBy,
    val ascending: Boolean
)

enum class SortBy {
    NAME,
    PLATFORM,
    RELEASED
}