package com.example.collectalogger2.util

data class Filter(
    val library: List<String>?,
    val platform: List<String>?,
    val genre: List<String>?,
    val developer: List<String>?,
    val publisher: List<String>?,
    val criticScoreComparison: Comparison?,
    val userScoreComparison: Comparison?,
    val yearComparison: Comparison?,
)