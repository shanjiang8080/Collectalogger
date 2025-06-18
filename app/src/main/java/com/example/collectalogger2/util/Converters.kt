package com.example.collectalogger2.util

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromSet(items: Set<String>): String = items.joinToString(",")

    @TypeConverter
    fun toSet(data: String): Set<String> = data.split(",").toSet()
}