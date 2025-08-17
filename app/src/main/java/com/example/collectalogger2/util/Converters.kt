package com.example.collectalogger2.util

import androidx.room.TypeConverter
import org.json.JSONObject
import org.json.JSONArray


class Converters {
    @TypeConverter
    fun fromSet(items: Set<String>): String = items.joinToString(",")

    @TypeConverter
    fun toSet(data: String): Set<String> = if (data == "") setOf() else data.split(",").toSet()

    @TypeConverter
    fun fromMap(map: Map<String, Long>): String = JSONObject(map).toString()

    @TypeConverter
    fun toMap(data: String): Map<String, Long> = JSONObject(data).toMap() as Map<String, Long>

    @TypeConverter
    fun fromList(items: List<String>): String = items.joinToString(",")

    @TypeConverter
    fun toList(data: String): List<String> = if (data == "") listOf() else data.split(",").toList()
}

// Copied from StackOverflow.
// https://stackoverflow.com/a/64002903
fun JSONObject.toMap(): Map<String, *> = keys().asSequence().associateWith {
    when (val value = this[it])
    {
        is JSONArray ->
        {
            val map = (0 until value.length()).associate { Pair(it.toString(), value[it]) }
            JSONObject(map).toMap().values.toList()
        }
        is JSONObject -> value.toMap()
        JSONObject.NULL -> null
        else            -> value
    }
}
