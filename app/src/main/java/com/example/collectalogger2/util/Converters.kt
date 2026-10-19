package com.example.collectalogger2.util

import androidx.room.TypeConverter


class Converters {
    @TypeConverter
    fun fromSet(items: Set<String>): String = encodeStringValues(items)

    @TypeConverter
    fun toSet(data: String): Set<String> = decodeStringValues(data).toSet()

    @TypeConverter
    fun fromGenres(set: Set<Int>): String = set.joinToString(",")

    @TypeConverter
    fun toGenres(data: String): Set<Int> {
        val genreSet: MutableSet<Int> = mutableSetOf()
        data.split(",").forEach { genreId ->
            if (genreId.isNotEmpty())
                genreSet.add(genreId.toInt())
        }
        return genreSet.toSet()
    }

    @TypeConverter
    fun fromList(items: List<String>): String = encodeStringValues(items)

    @TypeConverter
    fun toList(data: String): List<String> = decodeStringValues(data)
}

private const val VALUE_DELIMITER = "\u001F"

private fun encodeStringValues(items: Collection<String>): String {
    if (items.isEmpty()) return ""
    return VALUE_DELIMITER + items.joinToString(VALUE_DELIMITER)
}

private fun decodeStringValues(data: String): List<String> {
    if (data.isEmpty()) return listOf()
    if (data.startsWith(VALUE_DELIMITER)) {
        return data.split(VALUE_DELIMITER).drop(1)
    }
    return decodeLegacyCommaSeparatedValues(data)
}

private fun decodeLegacyCommaSeparatedValues(data: String): List<String> {
    val values = mutableListOf<String>()
    data.split(",").forEach { fragment ->
        if (values.isNotEmpty() && fragment.isNotEmpty() && fragment.first().isWhitespace()) {
            values[values.size - 1] = values.last() + "," + fragment
        } else {
            values.add(fragment)
        }
    }
    return values
}
