package com.example.collectalogger2.util

import androidx.room.TypeConverter


class Converters {
    @TypeConverter
    fun fromSet(items: Set<String>): String = items.joinToString(",")

    @TypeConverter
    fun toSet(data: String): Set<String> = if (data == "") setOf() else data.split(",").toSet()

    @TypeConverter
    fun fromGenres(set: Set<Int>): String = set.joinToString(",")

    @TypeConverter
    fun toGenres(data: String): Set<Int> {
        var genreSet: MutableSet<Int> = mutableSetOf()
        data.split(",").forEach { genreId ->
            if (genreId.isNotEmpty())
                genreSet.add(genreId.toInt())
        }
        return genreSet.toSet()
    }

    @TypeConverter
    fun fromList(items: List<String>): String = items.joinToString(",")

    @TypeConverter
    fun toList(data: String): List<String> = if (data == "") listOf() else data.split(",").toList()
}

